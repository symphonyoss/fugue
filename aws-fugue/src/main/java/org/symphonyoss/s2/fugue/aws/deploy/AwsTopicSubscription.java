/*
 *
 *
 * Copyright 2019 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.aws.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.json.JsonObject;
import org.symphonyoss.s2.fugue.deploy.TopicSubscription;
import org.symphonyoss.s2.fugue.naming.INameFactory;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.CreateEventSourceMappingResult;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsRequest;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult;
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingResult;

class AwsTopicSubscription extends TopicSubscription
{
  private static final Logger log_ = LoggerFactory.getLogger(AwsTopicSubscription.class);

  private final AWSLambda     lambdaClient_;
  private final String        queueArnPrefix_;

  AwsTopicSubscription(JsonObject<?> json, INameFactory nameFactory, AWSLambda lambdaClient, String queueArnPrefix)
  {
    super(json, nameFactory);
    
    lambdaClient_ = lambdaClient;
    queueArnPrefix_ = queueArnPrefix;
  }

  @Override
  public void create(String functionName)
  {

    String queueName = getQueueName();
    String queueArn = queueArnPrefix_ + queueName;
    
    ListEventSourceMappingsResult mappingResult = lambdaClient_.listEventSourceMappings(new ListEventSourceMappingsRequest()
        .withFunctionName(functionName)
        .withEventSourceArn(queueArn)
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
        .withBatchSize(getBatchSize())
        .withFunctionName(functionName)
        .withEventSourceArn(queueArn)
        );
    
    log_.info("CreateEventSourceMappingResult=" + result);
  }

}
