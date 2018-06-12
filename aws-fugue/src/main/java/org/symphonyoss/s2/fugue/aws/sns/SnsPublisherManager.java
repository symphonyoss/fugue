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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Amazon SNS implementation of PublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SnsPublisherManager extends AbstractPublisherManager<String, SnsPublisherManager>
{
  private static final Logger          log_                = LoggerFactory.getLogger(SnsPublisherManager.class);

  static final int MAX_MESSAGE_SIZE = 256 * 1024; // 256K

  private final INameFactory              nameFactory_;
  private final String                    region_;
  private final boolean                   initialize_;

  /* package */ Map<String, SnsPublisher> publisherNameMap_   = new HashMap<>();
  /* package */ List<SnsPublisher>        publishers_         = new ArrayList<>();
  /* package */ List<TopicName>           topicNames_         = new ArrayList<>();

  /* package */ AmazonSNS                 snsClient_;
  /* package */ String                    accountId_;
  /* package */ AWSSecurityTokenService   stsClient_;

  /**
   * Constructor.
   * 
   * @param nameFactory A name factory.
   * @param region      The AWS region to use.
   */
  public SnsPublisherManager(INameFactory nameFactory, String region)
  {
    this(nameFactory, region, false);
  }
  
  protected SnsPublisherManager(INameFactory nameFactory, String region, boolean initialize)
  {
    super(SnsPublisherManager.class);
    
    nameFactory_ = nameFactory;
    region_ = region;
    initialize_ = initialize;
    
    log_.info("Starting SNSPublisherManager in " + region_ + "...");
    
    stsClient_ = AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    GetCallerIdentityResult id = stsClient_.getCallerIdentity(new GetCallerIdentityRequest());
    
    accountId_ = id.getAccount();
    
    snsClient_ = AmazonSNSClientBuilder.standard()
      .withRegion(region_)
      .build();
    

  }

  @Override
  public void start()
  {
    for(Entry<String, SnsPublisher> entry : publisherNameMap_.entrySet())
    {
      TopicName topicName = nameFactory_.getTopicName(entry.getKey());
      topicNames_.add(topicName);
      
      entry.getValue().startByName(getTopicARN(topicName));
      publishers_.add(entry.getValue());
    }
    
    if(!initialize_)
    {
      // TODO: check that our topics are valid
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
  public synchronized IPublisher<String> getPublisherByName(String topicName)
  {
    assertConfigurable();
    
    SnsPublisher publisher = publisherNameMap_.get(topicName);
    
    if(publisher == null)
    {
      publisher = new SnsPublisher(this);
      publisherNameMap_.put(topicName, publisher);
    }
    
    return publisher;
  }
  
  protected void send(String topicArn, String msg)
  {
    try
    {
      PublishRequest publishRequest = new PublishRequest(topicArn, msg);
      snsClient_.publish(publishRequest);
    }
    catch (Exception e)
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
