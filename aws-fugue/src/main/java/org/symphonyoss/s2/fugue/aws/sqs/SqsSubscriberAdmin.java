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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberAdmin;

import com.amazonaws.auth.AWSCredentialsProvider;
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
import com.google.common.collect.ImmutableMap;

/**
 * The admin variant of SqsSubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberAdmin extends AbstractSubscriberAdmin<SqsSubscriberAdmin>
{
  private static final Logger                log_ = LoggerFactory.getLogger(SqsSubscriberAdmin.class);
  private final String                       region_;
  private final String                       accountId_;
  private final ImmutableMap<String, String> tags_;

  private final AmazonSQS                    sqsClient_;
  private final AmazonSNS                    snsClient_;
  
  private SqsSubscriberAdmin(Builder builder)
  {
    super(SqsSubscriberAdmin.class, builder);
    
    accountId_  = builder.accountId_;
    region_     = builder.region_;
    tags_       = ImmutableMap.copyOf(builder.tags_);
    
    sqsClient_ = builder.sqsBuilder_.build();
    snsClient_ = builder.snsBuilder_.build();
    
    log_.info("Starting SQSSubscriberAdmin in " + region_ + "...");
  }
  
  /**
   * Builder for SqsSubscriberAdmin.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractSubscriberAdmin.Builder<Builder, SqsSubscriberAdmin>
  {
    private final AmazonSQSClientBuilder sqsBuilder_;
    private final AmazonSNSClientBuilder snsBuilder_;
    
    private  String                 region_;
    private  String                 accountId_;
    private  Map<String, String>    tags_ = new HashMap<>();

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
      
      sqsBuilder_ = AmazonSQSClientBuilder.standard();
      snsBuilder_ = AmazonSNSClientBuilder.standard();
    }
    
    /**
     * Set the AWS region.
     * 
     * @param region The AWS region in which to operate.
     * 
     * @return this (fluent method)
     */
    public Builder withRegion(String region)
    {
      region_ = region;
      
      sqsBuilder_.withRegion(region_);
      snsBuilder_.withRegion(region_);
      
      return self();
    }
    
    /**
     * Set the AWS account ID.
     * 
     * @param accountId The ID of the AWS account in which to operate.
     * 
     * @return this (fluent method)
     */
    public Builder withAccountId(String accountId)
    {
      accountId_  = accountId;
      
      return self();
    }
    
    /**
     * Set the AWS credentials provider.
     * 
     * @param credentialsProvider An AWS credentials provider.
     * 
     * @return this (fluent method)
     */
    public Builder withCredentials(AWSCredentialsProvider credentialsProvider)
    {
      sqsBuilder_.withCredentials(credentialsProvider);
      snsBuilder_.withCredentials(credentialsProvider);
      
      return self();
    }
    
    /**
     * Add the given tags to created queues.
     * Multiple calls to this method are cumulative.
     * 
     * @param tags Tags to add.
     * 
     * @return this (fluent method)
     */
    public Builder withTags(Map<String, String> tags)
    {
      tags_.putAll(tags);
      
      return self();
    }

    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(region_,    "region");
      faultAccumulator.checkNotNull(accountId_, "accountId");
    }

    @Override
    protected SqsSubscriberAdmin construct()
    {
      return new SqsSubscriberAdmin(this);
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
  
  @Override
  protected void createSubcription(SubscriptionName subscriptionName, boolean dryRun)
  {
    boolean subscriptionOk = false;
    String  queueUrl;
    
    try
    {
      queueUrl = sqsClient_.getQueueUrl(subscriptionName.toString()).getQueueUrl();
      
      String queueArn = getQueueARN(subscriptionName);
      
      ListSubscriptionsByTopicResult subscriptionList = snsClient_.listSubscriptionsByTopic(getTopicARN(subscriptionName.getTopicName()));
            
      for(com.amazonaws.services.sns.model.Subscription s : subscriptionList.getSubscriptions())
      {
        if(queueArn.equals(s.getEndpoint()))
        {
          subscriptionOk = true;
          break;
        }
      }
      
      if(subscriptionOk)
        log_.info("Subscription " + subscriptionName + " already exists as " + queueUrl);
      else
        log_.info("Subscription " + subscriptionName + " already exists as " + queueUrl + " but the SNS subscription is missing.");
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
        
        log_.info("Created subscription " + subscriptionName + " as " + queueUrl);
      }
    }
    
    if(!subscriptionOk)
    {
      if(dryRun)
      {
        log_.info("Subscription " + subscriptionName + " would be created (dry run)");
        return;
      }
      else
      {
        String subscriptionArn = Topics.subscribeQueue(snsClient_, sqsClient_, getTopicARN(subscriptionName.getTopicName()), queueUrl);
        
        snsClient_.setSubscriptionAttributes(new SetSubscriptionAttributesRequest(subscriptionArn, "RawMessageDelivery", "true"));
        
        log_.info("Created subscription " + subscriptionName + " as " + queueUrl + " with subscriptionArn " + subscriptionArn);
      }
    }
    
    sqsClient_.tagQueue(new TagQueueRequest()
        .withQueueUrl(queueUrl)
        .withTags(tags_)
        );
  }
  
  @Override
  protected void deleteSubcription(SubscriptionName subscriptionName, boolean dryRun)
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
        ListSubscriptionsByTopicResult subscriptionResult = snsClient_.listSubscriptionsByTopic(getTopicARN(subscriptionName.getTopicName()));
        
        if(subscriptionResult != null)
        {
          String queueArn = getQueueARN(subscriptionName);
          
          for(com.amazonaws.services.sns.model.Subscription subscription : subscriptionResult.getSubscriptions())
          {
            if(queueArn.equals(subscription.getEndpoint()))
            {
              snsClient_.unsubscribe(subscription.getSubscriptionArn());
              
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
