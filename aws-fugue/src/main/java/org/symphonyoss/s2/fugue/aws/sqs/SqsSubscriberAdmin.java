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
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.TagQueueRequest;

/**
 * The admin variant of SqsSubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberAdmin extends AbstractSubscriberAdmin<String, SqsSubscriberAdmin>
{
  private static final Logger log_ = LoggerFactory.getLogger(SqsSubscriberAdmin.class);
  private final String        region_;
  private final String        accountId_;

  private AmazonSQS           sqsClient_;
  private Map<String, String> tags_;
  
  /**
   * Constructor.
   * 
   * @param nameFactory   A name factory.
   * @param region        The AWS region in which to operate.
   * @param accountId     The AWS account ID 
   * @param traceFactory  A trace factory.
   * @param tags          Tags to be applied to created queues.
   */
  public SqsSubscriberAdmin(INameFactory nameFactory, String region, String accountId,
      ITraceContextFactory traceFactory, Map<String, String> tags)
  {
    super(nameFactory, SqsSubscriberAdmin.class);
    
    accountId_ = accountId;
    region_ = region;
    tags_ = tags;
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
      for(TopicName topicName : subscription.getObsoleteTopicNames())
      {
        SubscriptionName subscriptionName = nameFactory_.getObsoleteSubscriptionName(topicName, subscription.getObsoleteSubscriptionId());
        
        deleteSubcription(snsClient, topicName, subscriptionName, dryRun);
      }
      
      for(TopicName topicName : subscription.getTopicNames())
      {
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());
        
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
  
  private String getQueueARN(SubscriptionName subscriptionName)
  {
    return "arn:aws:sqs:" + region_ + ":" + accountId_ + ":" + subscriptionName;
  }
  
  private void createSubcription(AmazonSNS snsClient, TopicName topicName, SubscriptionName subscriptionName, boolean dryRun)
  {
    String queueUrl;
    
    try
    {
      queueUrl = sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
      
      log_.info("Subscription " + subscriptionName + " already exists as " + queueUrl);
    }
    catch(QueueDoesNotExistException e)
    {
      if(dryRun)
      {
        log_.info("Subscription " + subscriptionName + " would be created (dry run)");
        return;
      }
      else
      {
        queueUrl = sqsClient_.createQueue(new CreateQueueRequest(subscriptionName.toString())).getQueueUrl();
        
        String subscriptionArn = Topics.subscribeQueue(snsClient, sqsClient_, getTopicARN(topicName), queueUrl);
        
        snsClient.setSubscriptionAttributes(new SetSubscriptionAttributesRequest(subscriptionArn, "RawMessageDelivery", "true"));
        
        log_.info("Created subscription " + subscriptionName + " as " + queueUrl);
      }
    }
    
    sqsClient_.tagQueue(new TagQueueRequest()
        .withQueueUrl(queueUrl)
        .withTags(tags_)
        );
  }

  @Override
  public void deleteSubscriptions(boolean dryRun)
  {
    AmazonSNS snsClient = AmazonSNSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    for(Subscription<?> subscription : getSubscribers())
    {
      for(TopicName topicName : subscription.getObsoleteTopicNames())
      {
        SubscriptionName subscriptionName = nameFactory_.getObsoleteSubscriptionName(topicName, subscription.getObsoleteSubscriptionId());
        
        deleteSubcription(snsClient, topicName, subscriptionName, dryRun);
      }
      

      for(TopicName topicName : subscription.getTopicNames())
      {
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());
        
        deleteSubcription(snsClient, topicName, subscriptionName, dryRun);
      }
    }
  }
  
  private void deleteSubcription(AmazonSNS snsClient, TopicName topicName, SubscriptionName subscriptionName, boolean dryRun)
  {
    try
    {
      String existingQueueUrl = sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
      
      if(dryRun)
      {
        log_.info("Subscription " + subscriptionName + " with URL " + existingQueueUrl + " would be deleted (dry run)");
      }
      else
      {
        //GetSubscriptionAttributesResult sa = snsClient.getSubscriptionAttributes(getQueueARN(subscriptionName));
        
        ListSubscriptionsByTopicResult subscriptionResult = snsClient.listSubscriptionsByTopic(getTopicARN(topicName));
        
        if(subscriptionResult != null)
        {
          String queueArn = getQueueARN(subscriptionName);
          
          for(com.amazonaws.services.sns.model.Subscription subscription : subscriptionResult.getSubscriptions())
          {
            if(queueArn.equals(subscription.getEndpoint()))
            {
              snsClient.unsubscribe(subscription.getSubscriptionArn());
              
              log_.info("Deleted subscription " + subscription.getSubscriptionArn());
            }
          }
        }

        sqsClient_.deleteQueue(existingQueueUrl);

        log_.info("Deleted subscription " + subscriptionName + " with URL " + existingQueueUrl);
      }
    }
    catch(QueueDoesNotExistException e)
    {
      log_.info("Subscription " + subscriptionName + " does not exist.");
    }
  }
}
