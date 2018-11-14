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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.Counter;
import org.symphonyoss.s2.fugue.deploy.ExecutorBatch;
import org.symphonyoss.s2.fugue.deploy.IBatch;
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
 * The following configurayion is supported
 * 
 * 
  "org":
  {
    "symphonyoss":
    {
      "s2":
      {
        "fugue":
        {
          "aws":
          {
            "sqs":
            {
              "subscriberThreadPoolSize": 40,
              "handlerThreadPoolSize": 360
            }
          }
        }
      }
    }
  }
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberManager extends AbstractSubscriberManager<String, SqsSubscriberManager>
{
  private static final Logger log_ = LoggerFactory.getLogger(SqsSubscriberManager.class);

  private final String                        region_;
  private final boolean                       startSubscriptions_;
  private final LinkedBlockingQueue<Runnable> executorQueue_  = new LinkedBlockingQueue<Runnable>();
  private final LinkedBlockingQueue<Runnable> handlerQueue_  = new LinkedBlockingQueue<Runnable>();
  private final IConfiguration                sqsConfig_;

  private AmazonSQS                           sqsClient_;
  private int                                 subscriberThreadPoolSize_;
  private int                                 handlerThreadPoolSize_;
  
  private ThreadPoolExecutor                  subscriberExecutor_;
  private ThreadPoolExecutor                  handlerExecutor_;
  
  private List<SqsSubscriber>                 subscribers_ = new LinkedList<>();

  private Counter counter_;
  
  /**
   * Constructor.
   * 
   * @param config                          Configuration
   * @param nameFactory                     A NameFactory.
   * @param region                          The AWS region in which to operate.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public SqsSubscriberManager(IConfiguration config, INameFactory nameFactory, String region,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeErrorConsumer<String> unprocessableMessageConsumer)
  {
    super(nameFactory, SqsSubscriberManager.class, traceFactory, unprocessableMessageConsumer);
    
    if(unprocessableMessageConsumer==null)
      throw new NullPointerException("unprocessableMessageConsumer is required.");
    
    region_ = region;
    startSubscriptions_ = true;
    

    sqsConfig_ = config.getConfiguration("org/symphonyoss/s2/fugue/aws/sqs");
  }
  
  public SqsSubscriberManager withCounter(Counter counter)
  {
    counter_ = counter;
    
    return this;
  }

  @Override
  public void start()
  {
    if(startSubscriptions_)
    {
      
//      subscriberThreadPoolSize_ = sqsConfig_.getInt("subscriberThreadPoolSize", 8 * getTotalSubscriptionCnt());
//      handlerThreadPoolSize_ = sqsConfig_.getInt("handlerThreadPoolSize", 9 * subscriberThreadPoolSize_);

      subscriberThreadPoolSize_ = 8 * getTotalSubscriptionCnt();
      handlerThreadPoolSize_ = 9 * subscriberThreadPoolSize_;
      
      log_.info("Starting SQSSubscriberManager in " + region_ + " with " + subscriberThreadPoolSize_ + " subscriber threads and " + handlerThreadPoolSize_ + " handler threads...");

      subscriberExecutor_ = new ThreadPoolExecutor(subscriberThreadPoolSize_, subscriberThreadPoolSize_,
          0L, TimeUnit.MILLISECONDS,
          executorQueue_, new NamedThreadFactory("SQS-subscriber"));
      
      handlerExecutor_ = new ThreadPoolExecutor(subscriberThreadPoolSize_, handlerThreadPoolSize_,
          0L, TimeUnit.MILLISECONDS,
          handlerQueue_, new NamedThreadFactory("SQS-handler", true));
      
    }
    else
    {
      log_.info("Starting SQSSubscriberManager in " + region_ + "...");
    }
    
    sqsClient_ = AmazonSQSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    
    
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
        
        SqsSubscriber subscriber = new SqsSubscriber(this, sqsClient_, queueUrl, getTraceFactory(), subscription.getConsumer(), counter_);

        subscribers_.add(subscriber);
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
      for(SqsSubscriber subscriber : subscribers_)
        subscriber.stop();
      
      subscriberExecutor_.shutdown();
      
      try {
        // Wait a while for existing tasks to terminate
        if (!subscriberExecutor_.awaitTermination(60, TimeUnit.SECONDS)) {
          subscriberExecutor_.shutdownNow(); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if (!subscriberExecutor_.awaitTermination(60, TimeUnit.SECONDS))
              System.err.println("Pool did not terminate");
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
        subscriberExecutor_.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
  }

  /* package */ void submit(Runnable subscriber, boolean force)
  {
    if(force || executorQueue_.size() < subscriberThreadPoolSize_)
      subscriberExecutor_.submit(subscriber);
  }

  void printQueueSize()
  {
    log_.debug("Queue size " + executorQueue_.size());
  }
  
  /* package */ IBatch newBatch()
  {
    return new ExecutorBatch(handlerExecutor_);
  }
}
