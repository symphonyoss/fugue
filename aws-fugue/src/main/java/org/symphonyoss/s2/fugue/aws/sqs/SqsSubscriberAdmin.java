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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberAdmin;
import org.symphonyoss.s2.fugue.pubsub.Subscription;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

/**
 * The admin variant of SqsSubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberAdmin extends AbstractSubscriberAdmin<String, SqsSubscriberAdmin>
{
  private static final Logger log_ = LoggerFactory.getLogger(SqsSubscriberAdmin.class);
  private final INameFactory  nameFactory_;
  private final String        region_;
  private final String        accountId_;

  private AmazonSQS           sqsClient_;
  
  /**
   * Constructor.
   * 
   * @param nameFactory   A name factory.
   * @param region        The AWS region in which to operate.
   * @param accountId     The AWS account ID 
   * @param traceFactory  A trace factory.
   */
  public SqsSubscriberAdmin(INameFactory nameFactory, String region, String accountId, ITraceContextFactory traceFactory)
  {
    super(SqsSubscriberAdmin.class);
    
    accountId_ = accountId;
    nameFactory_ = nameFactory;
    region_ = region;
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
  public void createSubscriptions(boolean dryRun)
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
        
        createSubcription(snsClient, topicName, subscriptionName, dryRun);
      }
    }
  }
  
  /**
   * Topic-arns can be constructed if the region, accountId, and topic name is known.
   * 
   * $topicArn = 'arn:aws:sns:<REGION></>:<ACCOUNT-ID>:<TOPIC-NAME>'
   *
   * @param topicName - name of topic
   * 
   * @return The topic ARN
   */
  private String getTopicARN(TopicName topicName)
  {
    return "arn:aws:sns:" + region_ + ":" + accountId_ + ":" + topicName;
  }
  
  private void createSubcription(AmazonSNS snsClient, TopicName topicName, SubscriptionName subscriptionName, boolean dryRun)
  {
    // TODO: implement dryRun
    
    log_.info("Creating subscription " + subscriptionName + "...");
    String myTopicArn = getTopicARN(topicName);
    String myQueueUrl = sqsClient_.createQueue(new CreateQueueRequest(subscriptionName.toString())).getQueueUrl();
    
    String subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient_, myTopicArn, myQueueUrl);
    
    snsClient.setSubscriptionAttributes(new SetSubscriptionAttributesRequest(subscriptionArn, "RawMessageDelivery", "true"));
    
    log_.info("Created subscription " + subscriptionName + " as " + myQueueUrl);
  }

  @Override
  public void deleteSubscriptions(boolean dryRun)
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
        
        deleteSubcription(snsClient, topicName, subscriptionName, dryRun);
      }
    }
  }
  
  private void deleteSubcription(AmazonSNS snsClient, TopicName topicName, SubscriptionName subscriptionName, boolean dryRun)
  {
    try
    {
      String queueUrl = sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
      Map<String, String> attr = sqsClient_.getQueueAttributes(new GetQueueAttributesRequest(subscriptionName.toString())).getAttributes();

      if(dryRun)
      {
        log_.info("Subscription " + subscriptionName + " with URL " + queueUrl + " would be deleted (dry run)");
      }
      else
      {
        // TODO: implement me
        log_.error("Delete is not yet implemented");
//        deleted queue object-subscriber but not subscription,
//        
//        snsClient.unsubscribe(subscriptionArn)
//        log_.info("Deleting subscription " + subscriptionName + " with URL " + queueUrl + "...");
//        
//        sqsClient_.deleteQueue(queueUrl);
//        
//        log_.info("Deleted.");
      }
    }
    catch(QueueDoesNotExistException e)
    {
      log_.info("Subscription " + subscriptionName + " does not exist.");
    }
//    sqsClient_.getQueueAttributes(new GetQueueAttributesRequest(queueUrl))
    
    
    
    // TODO: implement dryRun
    // TODO: this is creating the topic which it should not do. Implement an STS client and pass it in here and to SNSPublisherManager
    
//    String myTopicArn = snsClient.createTopic(new CreateTopicRequest(topicName.toString())).getTopicArn();
//    String myQueueUrl = sqsClient_.createQueue(new CreateQueueRequest(subscriptionName.toString())).getQueueUrl();
//    
//    String subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient_, myTopicArn, myQueueUrl);
//    
//    snsClient.setSubscriptionAttributes(new SetSubscriptionAttributesRequest(subscriptionArn, "RawMessageDelivery", "true"));
//    
//    log_.info("Created subscription " + subscriptionName + " as " + myQueueUrl);
  }
}
