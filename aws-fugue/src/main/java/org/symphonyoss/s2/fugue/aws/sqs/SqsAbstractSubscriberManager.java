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

package org.symphonyoss.s2.fugue.aws.sqs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.ISubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.Subscription;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/* package */ class SqsAbstractSubscriberManager<T extends ISubscriberManager<String, T>> extends AbstractSubscriberManager<String, T>
{
  private static final Logger          log_                     = LoggerFactory.getLogger(SqsSubscriberManager.class);

  /* package */ final INameFactory           nameFactory_;
  /* package */ final String                 region_;
  /* package */ final boolean                startSubscriptions_;

  /* package */ AmazonSQS                    sqsClient_;
  /* package */ ExecutorService              executor_;

  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param region                          The AWS region to use.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */  
  SqsAbstractSubscriberManager(Class<T> type, INameFactory nameFactory, String region,
      ITraceContextFactory traceFactory,
      IThreadSafeErrorConsumer<String> unprocessableMessageConsumer)
  {
    super(type, traceFactory, unprocessableMessageConsumer);
    
    if(unprocessableMessageConsumer==null)
      throw new NullPointerException("unprocessableMessageConsumer is required.");
    
    nameFactory_ = nameFactory;
    region_ = region;
    startSubscriptions_ = true;
    
    executor_ = Executors.newFixedThreadPool(20);
  }
  
  /**
   * Construct a subscriber manager without any subscription processing.
   * 
   * @param nameFactory                     A NameFactory.
   * @param region                          The AWS region to use.
   * @param traceFactory                    A trace context factory.
   */
  SqsAbstractSubscriberManager(Class<T> type, INameFactory nameFactory, String region,
      ITraceContextFactory traceFactory)
  {
    super(type, traceFactory, null);
    
    nameFactory_ = nameFactory;
    region_ = region;
    startSubscriptions_ = false;
  }

  @Override
  public void start()
  {
    sqsClient_ = AmazonSQSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    log_.info("Starting SQSSubscriberManager in " + region_ + "...");
    
    super.start();
  }

  @Override
  protected void startSubscription(Subscription<String> subscription)
  {
    if(startSubscriptions_)
    {
      for(String topic : subscription.getTopicNames())
      {
        TopicName topicName = nameFactory_.getTopicName(topic);
        
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionName());

        log_.info("Subscribing to queue " + subscriptionName + "...");
        
        String queueUrl = //"https://sqs.us-west-2.amazonaws.com/189141687483/s2-bruce2-trace-monitor"; 
            sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
        
        SqsSubscriber subscriber = new SqsSubscriber(this, sqsClient_, queueUrl, getTraceFactory(), subscription.getConsumer());

        log_.info("Subscribing to " + subscriptionName + "...");
      
        submit(subscriber);
      }
    }
  }

  @Override
  protected void stopSubscriptions()
  {
    if(startSubscriptions_)
    {
      executor_.shutdown();
      
      try {
        // Wait a while for existing tasks to terminate
        if (!executor_.awaitTermination(60, TimeUnit.SECONDS)) {
          executor_.shutdownNow(); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if (!executor_.awaitTermination(60, TimeUnit.SECONDS))
              System.err.println("Pool did not terminate");
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
        executor_.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
  }

  public void submit(SqsSubscriber subscriber)
  {
    executor_.submit(subscriber);
  }

}
