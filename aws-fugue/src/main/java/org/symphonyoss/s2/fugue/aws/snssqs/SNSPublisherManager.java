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

package org.symphonyoss.s2.fugue.aws.snssqs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.aws.config.AwsConfigKey;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class SNSPublisherManager extends AbstractPublisherManager<String, SNSPublisherManager>
{
  private static final Logger          log_                = LoggerFactory.getLogger(SNSPublisherManager.class);

  private final INameFactory           nameFactory_;
  private final IConfigurationProvider config_;
  private final boolean                initialize_;

  private Map<String, SNSPublisher>    publisherNameMap_   = new HashMap<>();
  private Map<String, SNSPublisher>    publisherConfigMap_ = new HashMap<>();
  private List<SNSPublisher>           publishers_         = new ArrayList<>();
  private List<TopicName>              topicNames_         = new ArrayList<>();

  private AmazonSNS                    snsClient_;
  private String                       accountId_;
  private String                       region_;
  private AWSSecurityTokenService      stsClient_;


  
  public SNSPublisherManager(INameFactory nameFactory, IConfigurationProvider config, boolean initialize)
  {
    super(SNSPublisherManager.class);
    
    nameFactory_ = nameFactory;
    config_ = config;
    initialize_ = initialize;
  }

  @Override
  public void start()
  {
    region_ = config_.getRequiredString(AwsConfigKey.REGION_NAME);
    
    log_.info("Starting SNSPublisherManager in " + region_ + "...");
    
    stsClient_ = AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    GetCallerIdentityResult id = stsClient_.getCallerIdentity(new GetCallerIdentityRequest());
    
    accountId_ = id.getAccount();
    
    snsClient_ = AmazonSNSClientBuilder.standard()
      .withRegion(region_)
      .build();
    
    for(Entry<String, SNSPublisher> entry : publisherNameMap_.entrySet())
    {
      TopicName topicName = nameFactory_.getTopicName(entry.getKey());
      topicNames_.add(topicName);
      
      entry.getValue().startByName(getTopicARN(topicName));
      publishers_.add(entry.getValue());
    }
    
    for(Entry<String, SNSPublisher> entry : publisherConfigMap_.entrySet())
    {
      TopicName topicName = nameFactory_.getTopicName(config_.getRequiredString(entry.getKey())); 
      topicNames_.add(topicName);
      
      entry.getValue().startByName(getTopicARN(topicName));
      publishers_.add(entry.getValue());
    }
    
    if(initialize_)
      createTopics();
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

  @Override
  public void stop()
  {
    snsClient_.shutdown();
    
    for(SNSPublisher publisher : publishers_)
    {
      publisher.close();
    }
  }

  @Override
  public synchronized IThreadSafeConsumer<String> getPublisherByName(String topicName)
  {
    assertConfigurable();
    
    SNSPublisher publisher = publisherNameMap_.get(topicName);
    
    if(publisher == null)
    {
      publisher = new SNSPublisher(this);
      publisherNameMap_.put(topicName, publisher);
    }
    
    return publisher;
  }

  @Override
  public synchronized IThreadSafeConsumer<String> getPublisherByConfig(String topicConfigId)
  {
    assertConfigurable();
    
    SNSPublisher publisher = publisherConfigMap_.get(topicConfigId);
    
    if(publisher == null)
    {
      publisher = new SNSPublisher(this);
      publisherConfigMap_.put(topicConfigId, publisher);
    }
    
    return publisher;
  }
  
  protected void send(String topicArn, String msg)
  {
    try
    {
//      System.out.println("PUBLISH " + msg);
      
      PublishRequest publishRequest = new PublishRequest(topicArn, msg);
      PublishResult publishResult = snsClient_.publish(publishRequest);
      //print MessageId of message published to SNS topic
//      System.out.println("MessageId - " + publishResult.getMessageId());
    }
    catch (Exception e)
    {
      throw new TransactionFault(e);
    }
  }

  private void createTopics()
  {
    for(TopicName topicName : topicNames_)
    {
      CreateTopicRequest createTopicRequest = new CreateTopicRequest(topicName.toString());
      CreateTopicResult createTopicResult = snsClient_.createTopic(createTopicRequest);
      //print TopicArn
      log_.info("Created topic " + topicName + " as " + createTopicResult.getTopicArn());
      //get request id for CreateTopicRequest from SNS metadata   
      System.out.println("CreateTopicRequest - " + snsClient_.getCachedResponseMetadata(createTopicRequest));
    }
  }
}
