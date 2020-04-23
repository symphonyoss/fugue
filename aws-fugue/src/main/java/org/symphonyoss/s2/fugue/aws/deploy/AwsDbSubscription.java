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
import org.symphonyoss.s2.fugue.deploy.DbSubscription;
import org.symphonyoss.s2.fugue.naming.INameFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.model.DescribeStreamRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeStreamResult;
import com.amazonaws.services.dynamodbv2.model.ListStreamsRequest;
import com.amazonaws.services.dynamodbv2.model.ListStreamsResult;
import com.amazonaws.services.dynamodbv2.model.Stream;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.DeleteEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration;
import com.amazonaws.services.lambda.model.EventSourcePosition;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsRequest;
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult;
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingRequest;
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingResult;

class AwsDbSubscription extends DbSubscription
{
  private static final Logger         log_ = LoggerFactory.getLogger(AwsDbSubscription.class);

  private final AWSLambda             lambdaClient_;
  private final AmazonDynamoDBStreams dynamoStreamsClient_;
  private final AmazonDynamoDB        amazonDynamoDB_;
  
  AwsDbSubscription(JsonObject<?> json, INameFactory nameFactory, AWSLambda lambdaClient, AmazonDynamoDBStreams dynamoStreamsClient,
      AmazonDynamoDB amazonDynamoDB)
  {
    super(json, nameFactory);
    
    lambdaClient_ = lambdaClient;
    dynamoStreamsClient_ = dynamoStreamsClient;
    amazonDynamoDB_ = amazonDynamoDB;
  }

  @Override
  public void create(String functionName)
  {
    String eventSourceArn = fetchEventSourceArn();
    
    if(eventSourceArn == null)
    {
      // create the stream
      amazonDynamoDB_.updateTable(new UpdateTableRequest()
          .withTableName(getTableName())
          .withStreamSpecification(new StreamSpecification()
              .withStreamEnabled(true)
              .withStreamViewType(StreamViewType.NEW_IMAGE)
              )
          );
      
      eventSourceArn = fetchEventSourceArn();
      
      if(eventSourceArn == null)
        throw new IllegalStateException("Stream does not exist after creating it!");
      
      log_.info("DynamoDb stream created as " + eventSourceArn);
    }
    
    deleteEventSourceMapping(functionName, eventSourceArn);
    createEventSourceMapping(functionName + ":" + AwsFugueDeploy.LAMBDA_ALIAS_NAME, eventSourceArn);
  }
  
  private void deleteEventSourceMapping(String functionName, String eventSourceArn)
  {
    ListEventSourceMappingsResult mappingResult = lambdaClient_.listEventSourceMappings(new ListEventSourceMappingsRequest()
        .withFunctionName(functionName)
        .withEventSourceArn(eventSourceArn)
        );
    
    for(EventSourceMappingConfiguration mapping : mappingResult.getEventSourceMappings())
    {
      if(mapping.getEventSourceArn().startsWith(eventSourceArn))
      {
//        if("Enabled".equals(mapping.getState()))
//        {
//          log_.info("Event source mapping to " + functionName + " exists and is " + mapping.getState());
//          
//          UpdateEventSourceMappingResult updateResult = lambdaClient_.updateEventSourceMapping(new UpdateEventSourceMappingRequest()
//              .withUUID(mapping.getUUID())
//              .withEnabled(false)
//              );
//          
//          log_.info("Event source mapping updated to state " + updateResult.getState());
//        }
        
        lambdaClient_.deleteEventSourceMapping(new DeleteEventSourceMappingRequest()
            .withUUID(mapping.getUUID())
            );
        
        log_.info("Event source mapping to " + functionName + " deleted");
        
        return;
      }
    }
    
    
  }

  private void createEventSourceMapping(String functionName, String eventSourceArn)
  {

    int batchSize = 5;
    int concurrency = 10;
    
    ListEventSourceMappingsResult mappingResult = lambdaClient_.listEventSourceMappings(new ListEventSourceMappingsRequest()
        .withFunctionName(functionName)
        .withEventSourceArn(eventSourceArn)
        );
    
    for(EventSourceMappingConfiguration mapping : mappingResult.getEventSourceMappings())
    {
      if(mapping.getEventSourceArn().startsWith(eventSourceArn))
      {
        if("Enabled".equals(mapping.getState()) && 
            intEquals(mapping.getBatchSize(), batchSize) && 
            intEquals(mapping.getParallelizationFactor(), concurrency))
        {
          log_.info("Event source mapping to " + functionName + " exists.");
          return;
        }
        log_.info("Event source mapping to " + functionName + " exists but is " + mapping.getState());
        
        UpdateEventSourceMappingResult updateResult = lambdaClient_.updateEventSourceMapping(new UpdateEventSourceMappingRequest()
            .withUUID(mapping.getUUID())
            .withBatchSize(batchSize)
            .withParallelizationFactor(concurrency)
            .withEnabled(true)
            );
        
        log_.info("Event source mapping to " + functionName + " updated to state " + updateResult.getState());
        
        return;
      }
    }
    
    lambdaClient_.createEventSourceMapping(new CreateEventSourceMappingRequest()
        .withEnabled(true)
        .withBatchSize(getBatchSize())
        .withFunctionName(functionName)
        .withEventSourceArn(eventSourceArn)
        .withStartingPosition(EventSourcePosition.LATEST)
        .withBatchSize(batchSize)
        .withParallelizationFactor(concurrency)
        );
    
    log_.info("Event source mapping to " + functionName + " created");
  }

  private boolean intEquals(Integer actual, int expected)
  {
    return actual != null && actual.equals(expected);
  }

  private String fetchEventSourceArn()
  {
    String eventSourceArn = null;
    
    ListStreamsResult streams = dynamoStreamsClient_.listStreams(new ListStreamsRequest()
        .withTableName(getTableName())
        );
    
    for(Stream stream : streams.getStreams())
    {
      DescribeStreamResult streamDescription = dynamoStreamsClient_.describeStream(new DescribeStreamRequest()
          .withStreamArn(stream.getStreamArn())
          );
      
      String status = streamDescription.getStreamDescription().getStreamStatus();
      
      switch(status)
      {
        case "ENABLED":
          eventSourceArn = stream.getStreamArn();
          log_.info("DynamoDb stream " + eventSourceArn + " exists.");
          return eventSourceArn;
          
        case "ENABLING":
          eventSourceArn = stream.getStreamArn();
          log_.info("DynamoDb stream " + eventSourceArn + " exists (although it's still enabling).");
          return eventSourceArn;
          
        case "DISABLING":
        case "DISABLED":
        default:
          log_.info("DynamoDb stream " + stream.getStreamArn() + " is " + status + ".");
          break;
      }
    }
    return eventSourceArn;
  }
}
