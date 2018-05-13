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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.core.strategy.naming.INamingStrategy;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class SQSSubscriberManager extends AbstractSubscriberManager<String, SQSSubscriberManager>
{
  private static final Logger          log_ = LoggerFactory.getLogger(SQSSubscriberManager.class);

  private static final int THREADS_PER_SUBSCRIPTION = 5;

  private final INamingStrategy        namingStrategy_;
  private final IConfigurationProvider config_;
  private final String                 configRoot_;

  private String                       region_;
  private AmazonSQS                    sqsClient_;

  private ExecutorService executor_;
  
  public SQSSubscriberManager(INamingStrategy namingStrategy, IConfigurationProvider config, String configRoot,
      ITraceContextFactory traceFactory,
      IThreadSafeRetryableConsumer<String> consumer, IThreadSafeConsumer<String> unprocessableMessageConsumer)
  {
    super(SQSSubscriberManager.class, traceFactory, consumer, unprocessableMessageConsumer);
    
    namingStrategy_ = namingStrategy;
    config_ = config;
    configRoot_ = configRoot;
    
    executor_ = Executors.newFixedThreadPool(20);
  }

  @Override
  protected void startSubscriptions(Map<String, Set<String>> subscriptionsByTopic,
      Map<String, Set<String>> topicsBySubscription)
  {
    IConfigurationProvider cf = config_.getConfiguration(configRoot_);
    IConfigurationProvider snscf = cf.getConfiguration("snssqs");
    
    region_ = snscf.getRequiredString("aws.region.name");
    
    log_.info("Starting SQSSubscriberManager in " + region_ + "...");
    
    sqsClient_ = AmazonSQSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    for(Entry<String, Set<String>> entry : subscriptionsByTopic.entrySet())
    {
      for(String subscriptionName : entry.getValue())
      {
        String queueName = namingStrategy_.getSubscriptionName(entry.getKey(), subscriptionName);
        
        log_.info("Subscribing to queue " + queueName + "...");
        
        String queueUrl = //"https://sqs.us-west-2.amazonaws.com/189141687483/s2-bruce2-trace-monitor"; 
            sqsClient_.getQueueUrl(queueName).getQueueUrl();
        
        SQSSubscriber subscriber = new SQSSubscriber(this, sqsClient_, queueUrl, getTraceFactory());

        log_.info("Subscribing to " + queueName + "...");
        
        submit(subscriber);
      }
    }
  }

  @Override
  protected void stopSubscriptions()
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

  public void submit(SQSSubscriber subscriber)
  {
    executor_.submit(subscriber);
  }

}
