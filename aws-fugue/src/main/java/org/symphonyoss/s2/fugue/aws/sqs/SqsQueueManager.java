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
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.naming.Name;
import org.symphonyoss.s2.fugue.pubsub.IQueueManager;
import org.symphonyoss.s2.fugue.pubsub.IQueueSender;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.TagQueueRequest;
import com.google.common.collect.ImmutableMap;

/**
 * AWS SQS implementation of QueueManager.
 * 
 * 
 * @author Bruce Skingle
 *
 */
public class SqsQueueManager implements IQueueManager
{
  protected static final int                   MAX_MESSAGE_SIZE  = 256 * 1024; // 256K
  
  private static final Logger                log_ = LoggerFactory.getLogger(SqsQueueManager.class);

  private final String                       region_;
  private final ImmutableMap<String, String> tags_;

  private final AmazonSQS                    sqsClient_;
  private Map<Name, SqsQueueSender>          senderMap_ = new HashMap<>();

  private SqsQueueManager(Builder builder)
  {
    region_     = builder.region_;
    tags_       = ImmutableMap.copyOf(builder.tags_);
    
    sqsClient_ = builder.sqsBuilder_.withRegion(region_).build();
  }
  
  @Override
  public synchronized IQueueSender getSender(Name queueName)
  {
    SqsQueueSender sender = senderMap_.get(queueName);
    
    if(sender == null)
    {
      sender = new SqsQueueSender(sqsClient_, queueName);
      
      senderMap_.put(queueName, sender);
    }
    
    return sender;
  }

  @Override
  public int getMaximumMessageSize()
  {
    return MAX_MESSAGE_SIZE;
  }
  
  /**
   * Concrete builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends BaseAbstractBuilder<Builder, SqsQueueManager>
  {
    private AmazonSQSClientBuilder sqsBuilder_;
    private String                 region_;
    private Map<String, String>    tags_ = new HashMap<>();
    //  private String configPath_ = "org/symphonyoss/s2/fugue/aws/sqs";

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
      
      sqsBuilder_ = AmazonSQSClientBuilder
          .standard()
          .withClientConfiguration(new ClientConfiguration()
              .withMaxConnections(200)
              );
    }
    
//    @Override
//    protected String getConfigPath()
//    {
//      return configPath_;
//    }
//    
//    /**
//     * Set the AWS region.
//     * 
//     * @param configPath The path in the global configuration from which to take config.
//     * 
//     * @return this (fluent method)
//     */
//    public Builder withConfigPath(String configPath)
//    {
//      configPath_ = configPath;
//      
//      return self();
//    }
    
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
    }

    @Override
    protected SqsQueueManager construct()
    {
      return new SqsQueueManager(this);
    }
  }
  
  @Override
  public void createQueue(Name queueName, Map<String, String> tags, boolean dryRun)
  {
    String  queueUrl;
    
    try
    {
      queueUrl = sqsClient_.getQueueUrl(queueName.toString()).getQueueUrl();

      log_.info("Queue " + queueName + " already exists as " + queueUrl);
    }
    catch(QueueDoesNotExistException e)
    {
      if(dryRun)
      {
        log_.info("Queue " + queueName + " would be created (dry run)");
        return;
      }
      else
      {
        queueUrl = sqsClient_.createQueue(new CreateQueueRequest(queueName.toString())).getQueueUrl();        
        
        log_.info("Created queue " + queueName + " as " + queueUrl);
      }
    }
    
    Map<String, String> effectiveTags;
    
    if(tags == null || tags.isEmpty())
    {
      effectiveTags = tags_;
    }
    else
    {
      effectiveTags = new HashMap<>(tags_);
      effectiveTags.putAll(tags);
    }
    
    
    sqsClient_.tagQueue(new TagQueueRequest()
        .withQueueUrl(queueUrl)
        .withTags(effectiveTags)
        );
  }
  
  @Override
  public void deleteQueue(Name queueName, boolean dryRun)
  {
    try
    {
      String existingQueueUrl = sqsClient_.getQueueUrl(queueName.toString()).getQueueUrl();
      
      if(dryRun)
      {
        log_.info("Subscription " + queueName + " with URL " + existingQueueUrl + " would be deleted (dry run)");
      }
      else
      {
        sqsClient_.deleteQueue(existingQueueUrl);

        log_.info("Deleted queue " + queueName + " with URL " + existingQueueUrl);
      }
    }
    catch(QueueDoesNotExistException e)
    {
      log_.info("Queue " + queueName + " does not exist.");
    }
  }
}
