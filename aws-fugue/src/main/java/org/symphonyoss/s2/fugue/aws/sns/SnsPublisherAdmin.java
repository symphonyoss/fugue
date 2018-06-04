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
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.IPublisherAdmin;

import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;

/**
 * The admin variation of an SnsPublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SnsPublisherAdmin extends SnsPublisherManager implements IPublisherAdmin<String>
{
  private static final Logger          log_                = LoggerFactory.getLogger(SnsPublisherAdmin.class);

  /**
   * Constructor.
   * 
   * @param nameFactory A name factory.
   * @param config      A configuration provider.
   */
  public SnsPublisherAdmin(INameFactory nameFactory, IConfigurationProvider config)
  {
    super(nameFactory, config, true);
  }

  @Override
  public void createTopics()
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
