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

package org.symphonyoss.s2.fugue.google.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

/**
 * Amazon SNS implementation of PublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class GooglePublisherManager extends AbstractPublisherManager<String, GooglePublisherManager>
{
  private static final Logger          log_              = LoggerFactory.getLogger(GooglePublisherManager.class);

  static final int                     MAX_MESSAGE_SIZE  = 256 * 1024;                                        // 256K

  private final String                 region_;
  private final String                 accountId_;
  private final boolean                initialize_;

  private Map<TopicName, SnsPublisher> publisherNameMap_ = new HashMap<>();
  private List<SnsPublisher>           publishers_       = new ArrayList<>();

  /**
   * Constructor.
   * 
   * @param config                          Configuration
   * @param nameFactory                     A NameFactory.
   * @param projectId                       The Google project ID for the pubsub service.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public GooglePublisherManager(IConfiguration config, INameFactory nameFactory, String projectId,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeErrorConsumer<ImmutableByteArray> unprocessableMessageConsumer)
  {
    super(nameFactory, GoogleSubscriberManager.class);
    
    projectId_ = projectId;
    startSubscriptions_ = true;
    
    pubSubConfig_ = config.getConfiguration(GoogleConstants.CONFIG_PATH);
  }

  @Override
  public void start()
  {
    if(!initialize_)
    {
      // TODO: check that our topics are valid
    }
  }

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
  public void stop()
  {
    snsClient_.shutdown();
    
    for(SnsPublisher publisher : publishers_)
    {
      publisher.close();
    }
  }

  @Override
  protected synchronized IPublisher<String> getPublisherByName(TopicName topicName)
  {
    assertConfigurable();
    
    SnsPublisher publisher = publisherNameMap_.get(topicName);
    
    if(publisher == null)
    {
      publisher = new SnsPublisher(getTopicARN(topicName), this);
      publisherNameMap_.put(topicName, publisher);
    }
    
    return publisher;
  }
  
  protected void send(String topicArn, String msg, ITraceContext trace)
  {
    trace.trace("ABOUT_TO_PUBLISH0", "SNS_TOPIC", topicArn);
    try
    {
      trace.trace("ABOUT_TO_PUBLISH1", "SNS_TOPIC", topicArn);
      PublishRequest publishRequest = new PublishRequest(topicArn, msg);
      trace.trace("ABOUT_TO_PUBLISH", "SNS_TOPIC", topicArn);
      snsClient_.publish(publishRequest);
      trace.trace("PUBLISHED", "SNS_TOPIC", topicArn);
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
