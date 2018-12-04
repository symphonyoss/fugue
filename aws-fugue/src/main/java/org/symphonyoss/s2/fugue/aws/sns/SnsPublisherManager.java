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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;

/**
 * Amazon SNS implementation of PublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SnsPublisherManager extends SnsPublisherBase<SnsPublisherManager>
{
  private static final Logger          log_              = LoggerFactory.getLogger(SnsPublisherManager.class);

  private SnsPublisherManager(Builder builder)
  {
    super(builder);
    
    log_.info("Starting SnsPublisherManager in " + builder.region_ + "...");
  }
  
  /**
   * Concrete builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends SnsPublisherBase.Builder<Builder, SnsPublisherManager>
  {
    /** Constructor. */
    public Builder()
    {
      super(Builder.class, SnsPublisherManager.class);
    }

    @Override
    public SnsPublisherManager build()
    {
      validate();
      
      return new SnsPublisherManager(this);
    }
  }

  @Override
  public boolean validateTopic(TopicName topicName)
  {
    // TODO: validate topics
    return true;
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

//  protected void validateTopics()
//  {
//    // TODO: check that our topics are valid
//  }
//
//  /**
//   * Topic-arns can be constructed if the region, accountId, and topic name is known.
//   * 
//   * $topicArn = 'arn:aws:sns:REGION:ACCOUNT-ID:TOPIC-NAME'
//   *
//   * @param topicName - name of topic
//   * 
//   * @return The topic ARN
//   */
//  public String getTopicARN(TopicName topicName)
//  {
//    return "arn:aws:sns:" + region_ + ":" + accountId_ + ":" + topicName;
//  }
//
//  @Override
//  public void start()
//  {
//    // TODO Auto-generated method stub
//    
//  }
//
//  @Override
//  public void stop()
//  {
//    snsClient_.shutdown();
//    
//    for(SnsPublisher publisher : publishers_)
//    {
//      publisher.close();
//    }
//  }
//
//  @Override
//  public synchronized IPublisher getPublisherByName(TopicName topicName)
//  {
//    assertConfigurable();
//    
//    SnsPublisher publisher = publisherNameMap_.get(topicName);
//    
//    if(publisher == null)
//    {
//      publisher = new SnsPublisher(topicName, getTopicARN(topicName), this);
//      publisherNameMap_.put(topicName, publisher);
//    }
//    
//    return publisher;
//  }
//  
//  protected void send(String topicName, String topicArn, IPubSubMessage pubSubMessage, ITraceContext trace)
//  {
//    trace.trace("ABOUT-TO-PUBLISH", "SNS_TOPIC", topicName);
//    try
//    {
//      PublishRequest publishRequest = new PublishRequest(topicArn, pubSubMessage.getPayload());
//      
//      if(!pubSubMessage.getAttributes().isEmpty())
//      {
//        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
//        
//        for(Entry<String, String> entry : pubSubMessage.getAttributes().entrySet())
//        {
//          messageAttributes.put(entry.getKey(), new MessageAttributeValue()
//              .withDataType("String")
//              .withStringValue(entry.getValue()));
//        }
//        
//        publishRequest.withMessageAttributes(messageAttributes);
//      }
//      
//      snsClient_.publish(publishRequest);
//      trace.trace("PUBLISHED", "SNS_TOPIC", topicName);
//    }
//    catch (RuntimeException e)
//    {
//      throw new TransactionFault(e);
//    }
//  }
//
//  @Override
//  public int getMaximumMessageSize()
//  {
//    return MAX_MESSAGE_SIZE;
//  }
}
