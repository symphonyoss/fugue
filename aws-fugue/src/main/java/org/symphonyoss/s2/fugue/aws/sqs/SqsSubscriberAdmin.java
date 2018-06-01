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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.ISubscriberAdmin;
import org.symphonyoss.s2.fugue.pubsub.Subscription;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

/**
 * The admin variant of SqsSubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberAdmin extends SqsAbstractSubscriberManager<SqsSubscriberAdmin> implements ISubscriberAdmin<String, SqsSubscriberAdmin>
{
  private static final Logger log_ = LoggerFactory.getLogger(SqsSubscriberAdmin.class);

  /**
   * Constructor.
   * 
   * @param nameFactory   A name factory.
   * @param config        A configuration provider.
   * @param traceFactory  A trace factory.
   */
  public SqsSubscriberAdmin(INameFactory nameFactory, IConfigurationProvider config, ITraceContextFactory traceFactory)
  {
    super(SqsSubscriberAdmin.class, nameFactory, config, traceFactory);
  }

  @Override
  public void createSubscriptions()
  {
    AmazonSNS snsClient = AmazonSNSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    for(Subscription<?> subscription : getSubscribers())
    {
      for(String topic : subscription.getTopicNames())
      {
        TopicName topicName = nameFactory_.getTopicName(topic);
        
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionName());
        
        createSubcription(snsClient, topicName, subscriptionName);
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
}
