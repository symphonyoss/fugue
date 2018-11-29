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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPullSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.SubscriptionImpl;

import com.amazonaws.auth.AWSCredentialsProvider;
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
public class SqsSubscriberManager extends AbstractPullSubscriberManager<String, SqsSubscriberManager>
{
  private static final Logger log_         = LoggerFactory.getLogger(SqsSubscriberManager.class);

  private AmazonSQS           sqsClient_;
  private List<SqsSubscriber> subscribers_ = new LinkedList<>();

  /**
   * Constructor.
   * 
   * @param config                          Configuration
   * @param nameFactory                     A NameFactory.
   * @param credentials 
   * @param region                          The AWS region in which to operate.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public SqsSubscriberManager(IConfiguration config, INameFactory nameFactory, AWSCredentialsProvider credentials, String region,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeErrorConsumer<String> unprocessableMessageConsumer)
  {
    super(config, nameFactory, SqsSubscriberManager.class, traceFactory, unprocessableMessageConsumer, "org/symphonyoss/s2/fugue/aws/sqs");
    
    AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard()
        .withRegion(region)
        ;
    
    if(credentials != null)
      builder.withCredentials(credentials);
    
    sqsClient_ = builder.build();
    
    log_.info("Starting SQSSubscriberManager in " + region + "...");
  }

  @Override
  protected void initSubscription(SubscriptionImpl<String> subscription)
  {
    for(TopicName topicName : subscription.getTopicNames())
    {
      SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());

      log_.info("Subscribing to " + subscriptionName + "..."); 
      
      String queueUrl = //"https://sqs.us-west-2.amazonaws.com/189141687483/s2-bruce2-trace-monitor"; 
          sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
      
      SqsSubscriber subscriber = new SqsSubscriber(this, sqsClient_, queueUrl, getTraceFactory(), subscription.getConsumer(),
          getCounter(), nameFactory_.getTenantId());

      subscribers_.add(subscriber); 
    }
  }

  @Override
  protected void startSubscriptions()
  {
    for(SqsSubscriber subscriber : subscribers_)
    {
      log_.info("Starting subscription to " + subscriber.getQueueUrl() + "...");
      submit(subscriber, true);
    }
  }

  @Override
  protected void stopSubscriptions()
  {
     for(SqsSubscriber subscriber : subscribers_)
        subscriber.stop();
      
     super.stopSubscriptions();
  }
}
