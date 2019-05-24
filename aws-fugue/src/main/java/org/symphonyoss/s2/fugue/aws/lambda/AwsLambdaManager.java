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

package org.symphonyoss.s2.fugue.aws.lambda;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.aws.sqs.SqsQueueManager.Builder;
import org.symphonyoss.s2.fugue.lambda.ILambdaManager;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.CreateEventSourceMappingResult;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.GetEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsRequest;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult;
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingResult;
import com.google.common.collect.ImmutableMap;

/**
 * AWS implementation of ILambdaManager.
 * 
 * @author Bruce Skingle
 *
 */
public class AwsLambdaManager implements ILambdaManager
{
  private static final Logger                log_ = LoggerFactory.getLogger(AwsLambdaManager.class);

  private static final Integer BATCH_SIZE = 10;

  private final String                       region_;
  private final String                       accountId_;
  private final ImmutableMap<String, String> tags_;

  private final AWSLambda                    lambdaClient_;
//  private Map<String, SqsQueueSender>          senderMap_ = new HashMap<>();

  private AwsLambdaManager(Builder builder)
  {
    region_     = builder.region_;
    accountId_  = builder.accountId_;
    tags_       = ImmutableMap.copyOf(builder.tags_);
    
    lambdaClient_ = builder.lambdaBuilder_.build();
  }
  
//  @Override
//  public synchronized IQueueSender getSender(String queueName)
//  {
//    SqsQueueSender sender = senderMap_.get(queueName);
//    
//    if(sender == null)
//    {
//      sender = new SqsQueueSender(lambdaClient_, queueName);
//      
//      senderMap_.put(queueName, sender);
//    }
//    
//    return sender;
//  }
//
//  @Override
//  public int getMaximumMessageSize()
//  {
//    return MAX_MESSAGE_SIZE;
//  }
  
  @Override
  public void subscribe(String functionName, String eventSourceArn)
  {
    ListEventSourceMappingsResult mappingResult = lambdaClient_.listEventSourceMappings(new ListEventSourceMappingsRequest()
        .withFunctionName(functionName)
        .withEventSourceArn(eventSourceArn)
        );
    
    for(EventSourceMappingConfiguration mapping : mappingResult.getEventSourceMappings())
    {
      if("Enabled".equals(mapping.getState()))
      {
        log_.info("Mapping exists.");
        return;
      }
      
      log_.info("Mapping exists but is " + mapping.getState());
      
      UpdateEventSourceMappingResult updateResult = lambdaClient_.updateEventSourceMapping(new UpdateEventSourceMappingRequest()
          .withUUID(mapping.getUUID())
          .withEnabled(true)
          );
      
      log_.info("Mapping updated to state " + updateResult.getState());
      
      return;
    }
    CreateEventSourceMappingResult result = lambdaClient_.createEventSourceMapping(new CreateEventSourceMappingRequest()
        .withEnabled(true)
        .withBatchSize(BATCH_SIZE)
        .withFunctionName(functionName)
        .withEventSourceArn(eventSourceArn)
        );
    
    log_.info("CreateEventSourceMappingResult=" + result);
    
  }

  /**
   * Concrete builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends BaseAbstractBuilder<Builder, AwsLambdaManager>
  {
    private AWSLambdaClientBuilder lambdaBuilder_;
    private String                 region_;
    private String                 accountId_;
    private Map<String, String>    tags_ = new HashMap<>();

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
      
      lambdaBuilder_ = AWSLambdaClientBuilder
          .standard()
//          .withClientConfiguration(new ClientConfiguration()
//              .withMaxConnections(200)
//              )
          ;
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
      
      lambdaBuilder_.withRegion(region_);
      
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
      lambdaBuilder_.withCredentials(credentialsProvider);
      
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
    protected AwsLambdaManager construct()
    {
      return new AwsLambdaManager(this);
    }
  }
  
//  @Override
//  public void createQueue(String queueName, Map<String, String> tags, boolean dryRun)
//  {
//    String  queueUrl;
//    
//    try
//    {
//      queueUrl = lambdaClient_.getQueueUrl(queueName.toString()).getQueueUrl();
//
//      log_.info("Queue " + queueName + " already exists as " + queueUrl);
//    }
//    catch(QueueDoesNotExistException e)
//    {
//      if(dryRun)
//      {
//        log_.info("Queue " + queueName + " would be created (dry run)");
//        return;
//      }
//      else
//      {
//        queueUrl = lambdaClient_.createQueue(new CreateQueueRequest(queueName.toString())).getQueueUrl();        
//        
//        log_.info("Created queue " + queueName + " as " + queueUrl);
//      }
//    }
//    
//    Map<String, String> effectiveTags;
//    
//    if(tags == null || tags.isEmpty())
//    {
//      effectiveTags = tags_;
//    }
//    else
//    {
//      effectiveTags = new HashMap<>(tags_);
//      effectiveTags.putAll(tags);
//    }
//    
//    
//    lambdaClient_.tagQueue(new TagQueueRequest()
//        .withQueueUrl(queueUrl)
//        .withTags(effectiveTags)
//        );
//  }
//  
//  @Override
//  public void deleteQueue(String queueName, boolean dryRun)
//  {
//    try
//    {
//      String existingQueueUrl = lambdaClient_.getQueueUrl(queueName.toString()).getQueueUrl();
//      
//      if(dryRun)
//      {
//        log_.info("Subscription " + queueName + " with URL " + existingQueueUrl + " would be deleted (dry run)");
//      }
//      else
//      {
//        lambdaClient_.deleteQueue(existingQueueUrl);
//
//        log_.info("Deleted queue " + queueName + " with URL " + existingQueueUrl);
//      }
//    }
//    catch(QueueDoesNotExistException e)
//    {
//      log_.info("Queue " + queueName + " does not exist.");
//    }
//  }
}
