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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.SubscriptionImpl;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/**
 * AWS SQS implementation of SubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberManager extends AbstractSubscriberManager<String, SqsSubscriberManager>
{
  private static final Logger log_ = LoggerFactory.getLogger(SqsSubscriberManager.class);

  private final int                           threadPoolSize_ = 50;
  private final String                        region_;
  private final boolean                       startSubscriptions_;
  private final LinkedBlockingQueue<Runnable> executorQueue_  = new LinkedBlockingQueue<Runnable>();

  private AmazonSQS                           sqsClient_;
  private ThreadPoolExecutor                  executor_;
  
  /**
   * Constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param region                          The AWS region in which to operate.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public SqsSubscriberManager(INameFactory nameFactory, String region,
      ITraceContextFactory traceFactory,
      IThreadSafeErrorConsumer<String> unprocessableMessageConsumer)
  {
    super(nameFactory, SqsSubscriberManager.class, traceFactory, unprocessableMessageConsumer);
    
    if(unprocessableMessageConsumer==null)
      throw new NullPointerException("unprocessableMessageConsumer is required.");
    
    region_ = region;
    startSubscriptions_ = true;
    
    executor_ = new ThreadPoolExecutor(threadPoolSize_, threadPoolSize_,
        0L, TimeUnit.MILLISECONDS,
        executorQueue_);
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
  protected void startSubscription(SubscriptionImpl<String> subscription)
  {
    if(startSubscriptions_)
    {
      for(TopicName topicName : subscription.getTopicNames())
      {
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());

        String queueUrl = //"https://sqs.us-west-2.amazonaws.com/189141687483/s2-bruce2-trace-monitor"; 
            sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
        
        SqsSubscriber subscriber = new SqsSubscriber(this, sqsClient_, queueUrl, getTraceFactory(), subscription.getConsumer());

        log_.info("Subscribing to " + subscriptionName + "...");
      
        submit(subscriber, true);
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

  /* package */ void submit(Runnable subscriber, boolean force)
  {
    if(force || executorQueue_.size() < threadPoolSize_)
      executor_.submit(subscriber);
  }

  void printQueueSize()
  {
    log_.debug("Queue size " + executorQueue_.size());
  }
}
