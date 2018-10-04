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

package org.symphonyoss.s2.fugue.aws.util;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class CleanUp
{
  private final String        region_     = "us-east-1";
  private AmazonSQS           sqsClient_;
  private AmazonSNS snsClient_;
  
  public CleanUp()
  {
    sqsClient_ = AmazonSQSClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    snsClient_ = AmazonSNSClientBuilder.standard()
        .withRegion(region_)
        .build();
  }
  
  public void run() throws InterruptedException
  {
//    String queuePrefix = "sym-s2bruce";
//    
//    System.out.println("queuePrefix=" + queuePrefix);
//    
//    ListQueuesResult result = sqsClient_.listQueues(queuePrefix);
//    
//    for(String queueUrl : result.getQueueUrls())
//    {
//      System.out.println(queueUrl);
//    }
//
//    sqsClient_.get
//    String topicName = "dev-sym-perfdf-ingestion";
//    
//    ListSubscriptionsByTopicResult subscriptions = snsClient_.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest()
//        .withTopicArn("arn:aws:sns:us-east-1:189141687483:" + topicName)
//        );
//    
//    for(Subscription subscription : subscriptions.getSubscriptions())
//    {
//      System.out.println("arn=      " + subscription.getSubscriptionArn());
//      System.out.println("endpoint= " + subscription.getEndpoint());
//      
//    }
    
    String nextToken = null;
    int     cnt;
    int     total=0;
    
    do
    {
      cnt = 0;
      try
      {
        ListTopicsResult result = (nextToken == null)
          ? snsClient_.listTopics()
          : snsClient_.listTopics(new ListTopicsRequest()
            .withNextToken(nextToken)
            );
        nextToken = result.getNextToken();
        
        for(Topic topic : result.getTopics())
        {
          
          cnt++;
          
          //System.out.println(topic.getTopicArn());
          
          if(topic.getTopicArn().contains("-s2"))
          {
            System.out.println(topic.getTopicArn());
            
            for(Subscription subscription : snsClient_.listSubscriptionsByTopic(topic.getTopicArn()).getSubscriptions())
            {
              System.out.println("  " + subscription.getSubscriptionArn());
            }
          }
        }
        
        total += cnt;
        System.out.println("Done " + total);
        Thread.sleep(100);
      }
      catch(AmazonSNSException e)
      {
        String error = e.getErrorCode();
        
        System.err.println("Error " + error);
        
        Thread.sleep(10000);
      }
    }while(cnt > 0 && nextToken != null);
  }

  
  public static void main(String[] argv) throws InterruptedException
  {
    new CleanUp().run();
  }
}
