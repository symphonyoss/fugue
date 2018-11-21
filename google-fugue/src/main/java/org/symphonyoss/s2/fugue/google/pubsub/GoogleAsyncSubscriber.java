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

import java.time.Instant;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.PubsubMessage;

/**
 * A subscriber to a single topic.
 * 
 * @author Bruce Skingle
 *
 */
public class GoogleAsyncSubscriber implements MessageReceiver
{
  private static final Logger           log_      = LoggerFactory.getLogger(GoogleAsyncSubscriber.class);
  private static final ScheduledThreadPoolExecutor  executor_ = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("google-failed-msg-ack", true));
  private static final int MAX_PENDING_RETRY_COUNT = 10000;
  
//  static PrintStream debug_;
//  
//  static
//  {
//    try
//    {
//      debug_ = new PrintStream(new FileOutputStream("/tmp/goog-" + new Date()));
//    }
//    catch (FileNotFoundException e)
//    {
//      e.printStackTrace();
//      
//      debug_ = System.err;
//    }
//  }
  
  private final GoogleAsyncSubscriberManager                     manager_;
  private final ITraceContextTransactionFactory                  traceFactory_;
  private final IThreadSafeRetryableConsumer<ImmutableByteArray> consumer_;
  private final SubscriptionName                                 subscriptionName_;
  private final ICounter                                         counter_;
  private final String                                           tenantId_;

  private AtomicBoolean                                          stopped_                = new AtomicBoolean();

  /* package */ GoogleAsyncSubscriber(GoogleAsyncSubscriberManager manager, ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<ImmutableByteArray> consumer, SubscriptionName subscriptionName, ICounter counter, String tenantId)
  {
    manager_ = manager;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    subscriptionName_ = subscriptionName;
    counter_ = counter;
    tenantId_ = tenantId;
  }

  @Override
  public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer)
  {
//    long now = System.currentTimeMillis();
//    debug_.println(">S" + now);
    Timestamp ts = message.getPublishTime();
    
    try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction(PubsubMessage.class.getSimpleName(), message.getMessageId(),
        tenantId_, Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())))
    {
      ITraceContext trace = traceTransaction.open();
      
      if(stopped_.get())
      {
        System.err.println("NAKing message");
        trace.trace("ABORTING_SHUTDOWN");
        traceTransaction.aborted();
        consumer.nack();
        return;
        
      }
      if(counter_ != null)
        counter_.increment(1);
      
      trace.trace("RECEIVED");
      ImmutableByteArray byteArray = ImmutableByteArray.newInstance(message.getData());
      
      long retryTime = manager_.handleMessage(consumer_, byteArray, trace, message.getMessageId());
      
      if(retryTime < 0)
      {
        trace.trace("ABOUT_TO_ACK");
        consumer.ack();
      }
      else
      {
        if(executor_.getQueue().size() > MAX_PENDING_RETRY_COUNT)
          log_.error("We are holding " + executor_.getQueue().size() + " failed messages, this message will not be re-tried for up to 60 mins.");
        else
          executor_.schedule(() -> {consumer.nack();}, retryTime, TimeUnit.MILLISECONDS);
      }
      traceTransaction.finished();
//      long end = System.currentTimeMillis();
//      debug_.println(">F" + (end - now) + " " + trace.getSubjectId());
//      debug_.flush();
    }
    catch (Throwable e)
    {
      /*
       * This method is called from an executor so I am catching Throwable because otherwise Errors will
       * cause the process to fail silently.
       * 
       * If we are catching an OutOfMemoryError then it may be futile to try to log this but on balance
       * I think it's worth trying.
       */
      log_.error("Failed to handle message from " + subscriptionName_, e);
    }
  }

  public void stop()
  {
    stopped_.set(true);
  }
}
