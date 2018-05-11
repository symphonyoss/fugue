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
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.aws.config.S3ConfigurationProvider;
import org.symphonyoss.s2.fugue.core.strategy.naming.DefaultNamingStrategy;
import org.symphonyoss.s2.fugue.core.strategy.naming.INamingStrategy;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;

public class SNSPublisherManager extends AbstractPublisherManager<String, SNSPublisherManager>
{
  private static final Logger log_ = LoggerFactory.getLogger(SNSPublisherManager.class);
  
  private final INamingStrategy        namingStrategy_;
  private final IConfigurationProvider config_;
  private final String                 configRoot_;

  private Map<String, SNSPublisher>    publisherNameMap_   = new HashMap<>();
  private Map<String, SNSPublisher>    publisherConfigMap_ = new HashMap<>();
  private List<SNSPublisher>           publishers_         = new ArrayList<>();
//  private IMessageBroker                    broker_;
//  private IToMessageBrokerProducer          producer_;

  private AmazonSNS                    snsClient_;

  private AmazonIdentityManagement iamClient_;

  private String accountId_;

  private String region_;
  
  public SNSPublisherManager(INamingStrategy namingStrategy, IConfigurationProvider config, String configRoot)
  {
    super(SNSPublisherManager.class);
    
    namingStrategy_ = namingStrategy;
    config_ = config;
    configRoot_ = configRoot;
  }

  @Override
  public void start()
  {
    IConfigurationProvider cf = config_.getConfiguration(configRoot_);
    IConfigurationProvider snscf = cf.getConfiguration("snssqs");
    
    region_ = snscf.getRequiredString("aws.region.name");
    
    log_.info("Starting SNSPublisherManager in " + region_ + "...");
    
    iamClient_ = AmazonIdentityManagementClientBuilder.standard()
        .withRegion(region_)
        .build();

    accountId_ = iamClient_.getUser().getUser().getArn().split(":")[4];
    
    snsClient_ = AmazonSNSClientBuilder.standard()
      .withRegion(region_)
      .build();
    
    for(Entry<String, SNSPublisher> entry : publisherNameMap_.entrySet())
    {
      entry.getValue().startByName(getTopicName(entry.getKey()));
      publishers_.add(entry.getValue());
    }
    
    for(Entry<String, SNSPublisher> entry : publisherConfigMap_.entrySet())
    {
      entry.getValue().startByName(getTopicName(config_.getRequiredString(entry.getKey())));
      publishers_.add(entry.getValue());
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
  private String getTopicName(String rawName)
  {
    return "arn:aws:sns:" + region_ + ":" + accountId_ + ":" + namingStrategy_.getTopicName(rawName);
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
      System.out.println("PUBLISH " + msg);
      
      PublishRequest publishRequest = new PublishRequest(topicArn, msg);
      PublishResult publishResult = snsClient_.publish(publishRequest);
      //print MessageId of message published to SNS topic
      System.out.println("MessageId - " + publishResult.getMessageId());
      
//      producer_.send(msg);
    }
    catch (Exception e)
    {
      throw new TransactionFault(e);
    }
  }

}
