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
import org.symphonyoss.s2.fugue.FugueConfigKey;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.aws.config.AwsConfigKey;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

public class SQSSubscriberManager extends AbstractSubscriberManager<String, SQSSubscriberManager>
{
  private static final Logger          log_                     = LoggerFactory.getLogger(SQSSubscriberManager.class);

  private final INameFactory           nameFactory_;
  private final IConfigurationProvider config_;
  private final boolean                initialize_;
  private final boolean                startSubscriptions_;

  private String                       region_;
  private AmazonSQS                    sqsClient_;

  private ExecutorService              executor_;

  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param config                          The configuration to use.
   * @param traceFactory                    A trace context factory.
   * @param consumer                        Consumer for valid messages
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   * @param initialize                      If true then create subscriptions (use PublisherManager to create topics)
   */
  public SQSSubscriberManager(INameFactory nameFactory, IConfigurationProvider config,
      ITraceContextFactory traceFactory,
      IThreadSafeRetryableConsumer<String> consumer, IThreadSafeConsumer<String> unprocessableMessageConsumer, boolean initialize)
  {
    super(SQSSubscriberManager.class, traceFactory, consumer, unprocessableMessageConsumer);
    
    if(consumer==null || unprocessableMessageConsumer==null)
      throw new NullPointerException("consumer and unprocessableMessageConsumer are both required.");
    
    nameFactory_ = nameFactory;
    config_ = config;
    initialize_ = initialize;
    startSubscriptions_ = true;
    
    executor_ = Executors.newFixedThreadPool(20);
  }
  
  /**
   * Construct a subscriber manager without and subscription processing.
   * 
   * This is only useful for initialization of subscriptions so initialize should be true.
   * 
   * @param nameFactory                     A NameFactory.
   * @param config                          The configuration to use.
   * @param traceFactory                    A trace context factory.
   * @param initialize                      If true then create subscriptions (use PublisherManager to create topics)
   */
  public SQSSubscriberManager(INameFactory nameFactory, IConfigurationProvider config,
      ITraceContextFactory traceFactory,
      boolean initialize)
  {
    super(SQSSubscriberManager.class, traceFactory, null, null);
    
    nameFactory_ = nameFactory;
    config_ = config;
    initialize_ = initialize;
    startSubscriptions_ = false;
  }

  @Override
  protected void startSubscriptions(Map<String, Set<String>> subscriptionsByTopic,
      Map<String, Set<String>> topicsBySubscription)
  {
    IConfigurationProvider awsConfig = config_.getConfiguration(AwsConfigKey.AMAZON);
    IConfigurationProvider metaConfig = config_.getConfiguration(FugueConfigKey.META);
    
    region_ = awsConfig.getRequiredString(AwsConfigKey.REGION_NAME);
    
    log_.info("Starting SQSSubscriberManager in " + region_ + "...");
    
    sqsClient_ = AmazonSQSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    AmazonSNS snsClient = null;
    
    if(initialize_)
    {
      snsClient = AmazonSNSClientBuilder.standard()
          .withRegion(region_)
          .build();
    }
    
    for(Entry<String, Set<String>> entry : subscriptionsByTopic.entrySet())
    {
      TopicName topicName = nameFactory_.getTopicName(metaConfig.getRequiredString(entry.getKey()));
      
      for(String subscription : entry.getValue())
      {
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, metaConfig.getRequiredString(subscription));
        
        if(initialize_)
          createSubcription(snsClient, topicName, subscriptionName);
        
        if(startSubscriptions_)
        {
          log_.info("Subscribing to queue " + subscriptionName + "...");
          
          String queueUrl = //"https://sqs.us-west-2.amazonaws.com/189141687483/s2-bruce2-trace-monitor"; 
              sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
          
          SQSSubscriber subscriber = new SQSSubscriber(this, sqsClient_, queueUrl, getTraceFactory());
  
          log_.info("Subscribing to " + subscriptionName + "...");
        
          submit(subscriber);
        }
      }
    }
    
    
  }

  private void createSubcription(AmazonSNS snsClient, TopicName topicName, SubscriptionName subscriptionName)
  {
    // TODO: this is creating the topic which it should not do. Implement an STS client and pass it in here and to SNSPublisherManager
    
    String myTopicArn = snsClient.createTopic(new CreateTopicRequest(topicName.toString())).getTopicArn();
    String myQueueUrl = sqsClient_.createQueue(new CreateQueueRequest(subscriptionName.toString())).getQueueUrl();
    
    String subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient_, myTopicArn, myQueueUrl);
    
    snsClient.setSubscriptionAttributes(new SetSubscriptionAttributesRequest(subscriptionArn, "RawMessageDelivery", "true"));
    
    log_.info("Created subscription " + subscriptionName + " as " + myQueueUrl);
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

  public void submit(SQSSubscriber subscriber)
  {
    executor_.submit(subscriber);
  }

}
