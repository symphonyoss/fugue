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

package org.symphonyoss.s2.fugue.aws.sns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;
import org.symphonyoss.s2.fugue.pubsub.IPubSubMessage;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Amazon SNS implementation of PublisherManager.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete manager, needed for fluent methods.
 *
 */
public abstract class SnsPublisherBase<T extends SnsPublisherBase<T>> extends AbstractPublisherManager<T>
{
  private static final Logger                  log_              = LoggerFactory.getLogger(SnsPublisherBase.class);

  protected static final int                   MAX_MESSAGE_SIZE  = 256 * 1024; // 256K

  protected final Map<TopicName, SnsPublisher> publisherNameMap_ = new HashMap<>();
  protected final List<SnsPublisher>           publishers_       = new ArrayList<>();

  protected final String                       region_;
  protected final String                       accountId_;
  protected final AmazonSNS                    snsClient_;

  
  protected SnsPublisherBase(Builder<?,T> builder)
  {
    super(builder);
    
    region_    = builder.region_;
    accountId_ = builder.accountId_;
    snsClient_ = builder.snsBuilder_.build();
    
    int errorCnt = 0;
    
    for(TopicName topicName : builder.topicNames_)
    {
      if(!validateTopic(topicName))
        errorCnt++;
    }
    
    if(errorCnt > 0)
    {
      throw new IllegalStateException("There are " + errorCnt + " topic validation errors");
    }
    
    for(TopicName topicName : builder.topicNames_)
    {
      publisherNameMap_.put(topicName, new SnsPublisher(topicName, getTopicARN(topicName), this));
    }
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   * @param <T>   The concrete type returned by fluent methods.
   * @param <B>   The concrete type of the built object.
   */
  public static abstract class Builder<T extends Builder<T,B>, B extends AbstractPublisherManager<B>>
  extends AbstractPublisherManager.Builder<T,B>
  {
    protected final AmazonSNSClientBuilder snsBuilder_;
    protected final Set<TopicName>         topicNames_ = new HashSet<>();

    protected IConfiguration               config_;
    protected String                       region_;
    protected String                       accountId_;
    
    protected Builder(Class<T> type, Class<B> builtType)
    {
      super(type, builtType);
      
      snsBuilder_ = AmazonSNSClientBuilder.standard();
    }
    
    /**
     * Set the global application configuration.
     * 
     * @param config The global application configuration.
     * 
     * @return this (fluent method)
     */
    public T withConfig(IConfiguration config)
    {
      config_ = config;
      
      return self();
    }
    
    /**
     * Set the AWS region.
     * 
     * @param region The AWS region in which to operate.
     * 
     * @return this (fluent method)
     */
    public T withRegion(String region)
    {
      region_ = region;
      
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
    public T withAccountId(String accountId)
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
    public T withCredentials(AWSCredentialsProvider credentialsProvider)
    {
      snsBuilder_.withCredentials(credentialsProvider);
      
      return self();
    }
    
    @Override
    public T withTopic(TopicName name)
    {
      topicNames_.add(name);
      
      return self();
    }

    @Override
    public synchronized void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(config_,    "config");
      faultAccumulator.checkNotNull(region_,    "region");
      faultAccumulator.checkNotNull(accountId_, "accountId");
      
      IConfiguration      snsConfig     = config_.getConfiguration("org/symphonyoss/s2/fugue/aws/sns");
      ClientConfiguration clientConfig  = new ClientConfiguration()
          .withMaxConnections(          snsConfig.getInt( "maxConnections",           ClientConfiguration.DEFAULT_MAX_CONNECTIONS))
          .withClientExecutionTimeout(  snsConfig.getInt( "clientExecutionTimeout",   ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT))
          .withConnectionMaxIdleMillis( snsConfig.getLong("connectionMaxIdleMillis",  ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS))
          .withConnectionTimeout(       snsConfig.getInt( "connectionTimeout",        ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT))
          ;
      
      log_.info("Starting SNSPublisherManager in " + region_ + " with " + clientConfig.getMaxConnections() + " max connections...");

      snsBuilder_
          .withClientConfiguration(clientConfig)
          ;
    }
  }

//  /**
//   * Constructor.
//   * 
//   * @param config      The configuration provider.
//   * @param nameFactory A name factory.
//   * @param region      The AWS region to use.
//   * @param accountId   The AWS numeric account ID 
//   */
//  public SnsPublisherManager(IConfiguration config, INameFactory nameFactory, String region, String accountId)
//  {
//    this(config, nameFactory, region, accountId, null, false);
//  }
//  
//  /**
//   * Constructor.
//   * 
//   * @param config      The configuration provider.
//   * @param nameFactory A name factory.
//   * @param region      The AWS region to use.
//   * @param accountId   The AWS numeric account ID 
//   * @param credentials AWS credentials.
//   */
//  public SnsPublisherManager(IConfiguration config, INameFactory nameFactory, String region, String accountId, AWSCredentialsProvider credentials)
//  {
//    this(config, nameFactory, region, accountId, credentials, false);
//  }
//  
//  protected SnsPublisherManager(IConfiguration config, INameFactory nameFactory, String region, String accountId, AWSCredentialsProvider credentials, boolean initialize)
//  {
//    super(nameFactory, SnsPublisherManager.class);
//    
//    region_ = region;
//    accountId_ = accountId;
//    initialize_ = initialize;
//    
//    IConfiguration snsConfig = config.getConfiguration("org/symphonyoss/s2/fugue/aws/sns");
//    
//    ClientConfiguration clientConfig = new ClientConfiguration()
//        .withMaxConnections(          snsConfig.getInt( "maxConnections",           ClientConfiguration.DEFAULT_MAX_CONNECTIONS))
//        .withClientExecutionTimeout(  snsConfig.getInt( "clientExecutionTimeout",   ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT))
//        .withConnectionMaxIdleMillis( snsConfig.getLong("connectionMaxIdleMillis",  ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS))
//        .withConnectionTimeout(       snsConfig.getInt( "connectionTimeout",        ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT))
//        ;
//    
//    log_.info("Starting SNSPublisherManager in " + region_ + " with " + clientConfig.getMaxConnections() + " max connections...");
//    
//    AmazonSNSClientBuilder builder = AmazonSNSClientBuilder.standard()
//        .withRegion(region_)
//        .withClientConfiguration(clientConfig);
//    
//    if(credentials != null)
//    {
//      builder.withCredentials(credentials);
//    }
//    
//    snsClient_ = builder.build();
//    
//  }


  /**
   * Topic-arns can be constructed if the region, accountId, and topic name is known.
   * 
   * $topicArn = 'arn:aws:sns:REGION:ACCOUNT-ID:TOPIC-NAME'
   *
   * @param topicName - name of topic
   * 
   * @return The topic ARN
   */
  public String getTopicARN(TopicName topicName)
  {
    return "arn:aws:sns:" + region_ + ":" + accountId_ + ":" + topicName;
  }

  @Override
  public void start()
  {
  }

  /**
   * Validate the given topic name.
   * 
   * @param topicName The name of a topic.
   * 
   * @return true if the topic is valid.
   */
  protected abstract boolean validateTopic(TopicName topicName);

  @Override
  public void stop()
  {
    snsClient_.shutdown();
    
    for(SnsPublisher publisher : publishers_)
    {
      publisher.close();
    }
  }

  @Override
  public synchronized IPublisher getPublisherByName(TopicName topicName)
  {
    SnsPublisher publisher = publisherNameMap_.get(topicName);
    
    if(publisher == null)
    {
      throw new IllegalArgumentException("Unregistered topic \"" + topicName + "\"");
    }
    
    return publisher;
  }
  
  protected void send(String topicName, String topicArn, IPubSubMessage pubSubMessage, ITraceContext trace)
  {
    trace.trace("ABOUT-TO-PUBLISH", "SNS_TOPIC", topicName);
    try
    {
      PublishRequest publishRequest = new PublishRequest(topicArn, pubSubMessage.getPayload());
      
      if(!pubSubMessage.getAttributes().isEmpty())
      {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        
        for(Entry<String, String> entry : pubSubMessage.getAttributes().entrySet())
        {
          messageAttributes.put(entry.getKey(), new MessageAttributeValue()
              .withDataType("String")
              .withStringValue(entry.getValue()));
        }
        
        publishRequest.withMessageAttributes(messageAttributes);
      }
      
      snsClient_.publish(publishRequest);
      trace.trace("PUBLISHED", "SNS_TOPIC", topicName);
    }
    catch (RuntimeException e)
    {
      throw new TransactionFault(e);
    }
  }

  @Override
  public int getMaximumMessageSize()
  {
    return MAX_MESSAGE_SIZE;
  }
}
