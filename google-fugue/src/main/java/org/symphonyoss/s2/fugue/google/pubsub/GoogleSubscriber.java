/*
 *
 *
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.IBusyCounter;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPullSubscriber;
import org.symphonyoss.s2.fugue.pubsub.IPullSubscriberContext;
import org.symphonyoss.s2.fugue.pubsub.IPullSubscriberMessage;

import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.ReceivedMessage;

/**
 * A subscriber to a single topic.
 * 
 * @author Bruce Skingle
 *
 */
public class GoogleSubscriber extends AbstractPullSubscriber
{
  private static final int EXTENSION_TIMEOUT_SECONDS = 10;
  private static final int EXTENSION_FREQUENCY_MILLIS = 5000;
  
  private static final Logger                                    log_     = LoggerFactory
      .getLogger(GoogleSubscriber.class);

  private final GoogleSubscriberManager                          manager_;
  private final ITraceContextTransactionFactory                  traceFactory_;
  private final IThreadSafeRetryableConsumer<ImmutableByteArray> consumer_;
  private final NonIdleSubscriber                                nonIdleSubscriber_;
  private final String                                           subscriptionName_;
  private final String                                           tenantId_;
  private int                                                    batchSize_ = 10;
  private SubscriberStubSettings subscriberStubSettings_;

  private final PullRequest blockingPullRequest_;
  private final PullRequest nonBlockingPullRequest_;

  /* package */ GoogleSubscriber(GoogleSubscriberManager manager,
      String subscriptionName, ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<ImmutableByteArray> consumer, ICounter counter, IBusyCounter busyCounter, String tenantId)
  {
    super(manager, subscriptionName, counter, busyCounter, EXTENSION_FREQUENCY_MILLIS);
    
    if(Fugue.isDebugSingleThread())
    {
      batchSize_ = 1;
    }
    
    manager_ = manager;
    subscriptionName_ = subscriptionName;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
    tenantId_ = tenantId;
    
    try
    {
      subscriberStubSettings_ = SubscriberStubSettings.newBuilder().build();
    }
    catch (IOException e)
    {
      throw new CodingFault(e);
    }

    blockingPullRequest_ = PullRequest.newBuilder().setMaxMessages(batchSize_)
        .setReturnImmediately(false) // return immediately if messages are not available
        .setSubscription(subscriptionName_)
        .build();
    
    nonBlockingPullRequest_ = PullRequest.newBuilder().setMaxMessages(batchSize_)
        .setReturnImmediately(true) // return immediately if messages are not available
        .setSubscription(subscriptionName_)
        .build();
  }
  
  String getSubscriptionName()
  {
    return subscriptionName_;
  }

  class NonIdleSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      GoogleSubscriber.this.run(false);
    }
  }

  @Override
  protected NonIdleSubscriber getNonIdleSubscriber()
  {
    return nonIdleSubscriber_;
  }

  @Override
  protected IPullSubscriberContext getContext() throws IOException
  {
    return new GooglePullSubscriberContext();
  }
  
  class GooglePullSubscriberContext implements IPullSubscriberContext
  {
    private final GrpcSubscriberStub subscriber_;

    GooglePullSubscriberContext() throws IOException
    {
      subscriber_ = GrpcSubscriberStub.create(subscriberStubSettings_);
    }
    
    @Override
    public Collection<IPullSubscriberMessage> nonBlockingPull()
    {
      return pull(nonBlockingPullRequest_);
    }

    @Override
    public Collection<IPullSubscriberMessage> blockingPull()
    {
      return pull(blockingPullRequest_);
    }

    private Collection<IPullSubscriberMessage> pull(PullRequest pullRequest)
    {
      List<IPullSubscriberMessage>result = new LinkedList<>();
      
      for(ReceivedMessage receivedMessage : subscriber_.pullCallable().call(pullRequest).getReceivedMessagesList())
      {
        result.add(new GooglePullSubscriberMessage(subscriber_, receivedMessage));
      }
      
      return result;
    }

    @Override
    public void close()
    {
      subscriber_.close();
    }
  }

  private class GooglePullSubscriberMessage implements IPullSubscriberMessage
  {
    private final GrpcSubscriberStub subscriber_;
    private final ReceivedMessage    receivedMessage_;
    private boolean                  running_ = true;
    
    private GooglePullSubscriberMessage(GrpcSubscriberStub subscriber, ReceivedMessage receivedMessage)
    {
      subscriber_ = subscriber;
      receivedMessage_ = receivedMessage;
    }

    @Override
    public String getMessageId()
    {
      return receivedMessage_.getMessage().getMessageId();
    }

    @Override
    public void run()
    {
      PubsubMessage message = receivedMessage_.getMessage();
      Timestamp     ts      = message.getPublishTime();
      
      try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("PubSub:Google", message.getMessageId(),
          tenantId_, Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())))
      {
        ITraceContext trace = traceTransaction.open();
        
        trace.trace("RECEIVED");
        ImmutableByteArray byteArray = ImmutableByteArray.newInstance(message.getData());
        
        long retryTime = manager_.handleMessage(consumer_, byteArray, trace, message.getMessageId());
        
        synchronized(this)
        {
          // There is no point trying to extend the ack deadline now
          running_ = false;
        
          if(retryTime < 0)
          {
            trace.trace("ABOUT_TO_ACK");
            
            AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest
                .newBuilder()
                .setSubscription(subscriptionName_)
                .addAckIds(receivedMessage_.getAckId())
                .build();
            
            subscriber_.acknowledgeCallable().call(acknowledgeRequest);
            traceTransaction.finished();
          }
          else
          {
            trace.trace("ABOUT_TO_NACK");
            
            int visibilityTimout = (int) (retryTime / 1000);
            
            ModifyAckDeadlineRequest request = ModifyAckDeadlineRequest
              .newBuilder()
              .setSubscription(subscriptionName_)
              .setAckDeadlineSeconds(visibilityTimout)
              .addAckIds(receivedMessage_.getAckId())
              .build();
  
            subscriber_.modifyAckDeadlineCallable().call(request);
            
            traceTransaction.aborted();
          }
        }
      }
      catch(RuntimeException e)
      {
        log_.error("Failed to process message " + getMessageId(), e);
      }
    }

    @Override
    public synchronized void extend()
    {
      if(running_)
      {
        try
        {
          ModifyAckDeadlineRequest request = ModifyAckDeadlineRequest
            .newBuilder()
            .setSubscription(subscriptionName_)
            .setAckDeadlineSeconds(EXTENSION_TIMEOUT_SECONDS)
            .addAckIds(receivedMessage_.getAckId())
            .build();
  
          subscriber_.modifyAckDeadlineCallable().call(request);
        }
        catch(RuntimeException e)
        {
          log_.error("Failed to extend message " + getMessageId(), e);
        }
      }
    }
  }
}
