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
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.deploy.IBatch;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;

/**
 * A subscriber to a single topic.
 * 
 * @author Bruce Skingle
 *
 */
public class GoogleSubscriber implements Runnable
{
  private static final Logger                                    log_     = LoggerFactory
      .getLogger(GoogleSubscriber.class);

  private final GoogleSubscriberManager                          manager_;
  private final ITraceContextTransactionFactory                  traceFactory_;
  private final IThreadSafeRetryableConsumer<ImmutableByteArray> consumer_;
  private final NonIdleSubscriber                                nonIdleSubscriber_;
  private final String                                           subscriptionName_;
  private final ICounter                                         counter_;
  private boolean                                                running_   = true;
  private int                                                    batchSize_ = 10;
  private SubscriberStubSettings subscriberStubSettings_;

  /* package */ GoogleSubscriber(GoogleSubscriberManager manager,
      String subscriptionName, ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<ImmutableByteArray> consumer, ICounter counter)
  {
    manager_ = manager;
    subscriptionName_ = subscriptionName;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
    counter_ = counter;
    
    try
    {
      subscriberStubSettings_ = SubscriberStubSettings.newBuilder().build();
    }
    catch (IOException e)
    {
      throw new CodingFault(e);
    }

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
  public void run()
  {
    run(true);
  }

  void run(boolean runIfIdle)
  {
    if(isRunning())
    {
      if(runIfIdle)
      {
        try
        {
          while(isRunning())
          {
            getSomeMessages();
          }
        }
        finally
        {
          if(runIfIdle && isRunning())
          {
            // This "can't happen"
            log_.error("Main PubSub thread returned, rescheduling...");
            
            manager_.submit(this, true);
          }
        }
      }
      else
      {
        if(isRunning())
        {
          getSomeMessages();
        }
      }
    }
  }
  
  private void getSomeMessages()
  {
    // receive messages from the queue
        
    try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings_))
    {
      PullRequest pullRequest = PullRequest.newBuilder().setMaxMessages(batchSize_)
          .setReturnImmediately(false) // return immediately if messages are not available
          .setSubscription(subscriptionName_)
          .build();

      PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);
      List<ReceivedMessage> messages = pullResponse.getReceivedMessagesList();
      
      
      /*****
      List<String> ackIds = new ArrayList<>();
      for (ReceivedMessage message : messages)
      {
        // handle received message
        // ...
        ackIds.add(message.getAckId());
      }
      // acknowledge received messages
      AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder().setSubscription(subscriptionName)
          .addAllAckIds(ackIds).build();
      // use acknowledgeCallable().futureCall to asynchronously perform this
      // operation
      subscriber.acknowledgeCallable().call(acknowledgeRequest);
******/
  
      log_.info("Read " + messages.size() + " for " + subscriptionName_);
      
      switch(messages.size())
      {
        case 0:
          // Nothing to do...
          break;
          
        case 1:
          // Single message, just process in the current thread
          if(counter_ != null)
            counter_.increment(1);
          handleMessage(subscriber, messages.get(0));
          break;
          
        default:
          if(counter_ != null)
            counter_.increment(messages.size());
          if(messages.size() > 2 && isRunning())
          {
            manager_.submit(nonIdleSubscriber_, false);

            log_.debug("Extra schedule " + subscriptionName_);
          }
          
          // Fire off all but one envelopes in its own thread, do the final one in the current thread
          int     index = 0;
          IBatch  batch = manager_.newBatch();
          
          try
          {
            while(index < messages.size() - 1)
            {
              final int myIndex = index++;
              
              batch.submit(() -> 
              {
                handleMessage(subscriber, messages.get(myIndex));
              });
            }
            handleMessage(subscriber, messages.get(index));
            
            batch.waitForAllTasks();
          }
          catch(RuntimeException e)
          {
            Throwable cause = e.getCause();
            
            if(cause instanceof ExecutionException)
              cause = cause.getCause();
            
            if(cause instanceof RetryableConsumerException)
            {
              throw (RetryableConsumerException)cause;
            }            
            if(cause instanceof FatalConsumerException)
            {
              throw (FatalConsumerException)cause;
            }
            throw e;
          }
      }
    }
    catch(RuntimeException e)
    {
      log_.error("Error processing message", e);
    }
    catch (Throwable e)
    {
      /*
       * This method is called from an executor so I am catching Throwable because otherwise Errors will
       * be swallowed.
       * 
       * If we are catching an OutOfMemoryError then it may be futile to try to log this but on balance
       * I think it's worth trying.
       */
      
      try
      {
        log_.error("Error processing message", e);
      }
      finally
      {
        System.exit(1);
      }
    }
  }

  private void handleMessage(SubscriberStub subscriber, ReceivedMessage receivedMessage)
  {
    PubsubMessage message = receivedMessage.getMessage();
    Timestamp     ts      = message.getPublishTime();
    
    try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("PubSub:Google", message.getMessageId(),
        Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())))
    {
      ITraceContext trace = traceTransaction.open();
      
      trace.trace("RECEIVED");
      ImmutableByteArray byteArray = ImmutableByteArray.newInstance(message.getData());
      
      long retryTime = manager_.handleMessage(consumer_, byteArray, trace, message.getMessageId());
      
      if(retryTime < 0)
      {
        trace.trace("ABOUT_TO_ACK");
        
        AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest
            .newBuilder()
            .setSubscription(subscriptionName_)
            .addAckIds(receivedMessage.getAckId())
            .build();
        // use acknowledgeCallable().futureCall to asynchronously perform this
        // operation
        subscriber.acknowledgeCallable().call(acknowledgeRequest);
        traceTransaction.finished();
      }
      else
      {
        trace.trace("ABOUT_TO_NACK");
        
        int visibilityTimout = (int) (retryTime / 1000);
        
        ModifyAckDeadlineRequest request = ModifyAckDeadlineRequest
          .newBuilder()
          .setAckDeadlineSeconds(visibilityTimout)
          .addAckIds(receivedMessage.getAckId())
          .build();

        subscriber.modifyAckDeadlineCallable().call(request);
        
        traceTransaction.aborted();
      }
    }
  }

  synchronized boolean isRunning()
  {
    return running_;
  }
  
  synchronized void stop()
  {
    running_ = false;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
//  
//  
//  
//  private static final ScheduledThreadPoolExecutor  executor_ = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("google-failed-msg-ack", true));
//  private static final int MAX_PENDING_RETRY_COUNT = 10000;
//  
////  static PrintStream debug_;
////  
////  static
////  {
////    try
////    {
////      debug_ = new PrintStream(new FileOutputStream("/tmp/goog-" + new Date()));
////    }
////    catch (FileNotFoundException e)
////    {
////      e.printStackTrace();
////      
////      debug_ = System.err;
////    }
////  }
//  
//
//  
//  /* package */ GoogleSubscriber(GoogleSubscriberManager manager, ITraceContextTransactionFactory traceFactory,
//      IThreadSafeRetryableConsumer<ImmutableByteArray> consumer, SubscriptionName subscriptionName, ICounter counter)
//  {
//    manager_ = manager;
//    traceFactory_ = traceFactory;
//    consumer_ = consumer;
//    subscriptionName_ = subscriptionName;
//    counter_ = counter;
//  }
//
//  @Override
//  public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer)
//  {
////    long now = System.currentTimeMillis();
////    debug_.println(">S" + now);
//    Timestamp ts = message.getPublishTime();
//    
//    try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction(PubsubMessage.class.getSimpleName(), message.getMessageId(),
//        Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())))
//    {
//      ITraceContext trace = traceTransaction.open();
//      
//      if(stopped_.get())
//      {
//        System.err.println("NAKing message");
//        trace.trace("ABORTING_SHUTDOWN");
//        traceTransaction.aborted();
//        consumer.nack();
//        return;
//        
//      }
//      if(counter_ != null)
//        counter_.increment(1);
//      
//      trace.trace("RECEIVED");
//      ImmutableByteArray byteArray = ImmutableByteArray.newInstance(message.getData());
//      
//      long retryTime = manager_.handleMessage(consumer_, byteArray, trace, message.getMessageId());
//      
//      if(retryTime < 0)
//      {
//        trace.trace("ABOUT_TO_ACK");
//        consumer.ack();
//      }
//      else
//      {
//        if(executor_.getQueue().size() > MAX_PENDING_RETRY_COUNT)
//          log_.error("We are holding " + executor_.getQueue().size() + " failed messages, this message will not be re-tried for up to 60 mins.");
//        else
//          executor_.schedule(() -> {consumer.nack();}, retryTime, TimeUnit.MILLISECONDS);
//      }
//      traceTransaction.finished();
////      long end = System.currentTimeMillis();
////      debug_.println(">F" + (end - now) + " " + trace.getSubjectId());
////      debug_.flush();
//    }
//    catch (Throwable e)
//    {
//      /*
//       * This method is called from an executor so I am catching Throwable because otherwise Errors will
//       * cause the process to fail silently.
//       * 
//       * If we are catching an OutOfMemoryError then it may be futile to try to log this but on balance
//       * I think it's worth trying.
//       */
//      log_.error("Failed to handle message from " + subscriptionName_, e);
//    }
//  }
//
//  public void stop()
//  {
//    stopped_.set(true);
//  }
}
