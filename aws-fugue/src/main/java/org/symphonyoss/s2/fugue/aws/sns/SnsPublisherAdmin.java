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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;
import org.symphonyoss.s2.fugue.pubsub.IPublisherAdmin;

import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.GetTopicAttributesResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;
import com.amazonaws.services.sns.model.Subscription;

/**
 * The admin variation of an SnsPublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SnsPublisherAdmin extends SnsPublisherBase<SnsPublisherAdmin> implements IPublisherAdmin
{
  private static final Logger log_            = LoggerFactory.getLogger(SnsPublisherAdmin.class);

  private static final Object POLICY = "Policy";

  private Set<TopicName>      obsoleteTopics_ = new HashSet<>();
  private Set<TopicName>      topics_         = new HashSet<>();
  
//  /**
//   * Constructor.
//   * 
//   * @param config      The configuration provider.
//   * @param nameFactory A name factory.
//   * @param region      The AWS region to use.
//   * @param accountId   The AWS numeric account ID 
//   */
//  public SnsPublisherAdmin(IConfiguration config, INameFactory nameFactory, String region, String accountId)
//  {
//    super(config, nameFactory, region, accountId, true);
//  }
  
  private SnsPublisherAdmin(Builder builder)
  {
    super(SnsPublisherAdmin.class, builder);
  }

  /**
   * Concrete builder.
   * 
   * @author Bruce Skingle
   */
  public static class Builder extends SnsPublisherBase.Builder<Builder, SnsPublisherAdmin>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected SnsPublisherAdmin construct()
    {
      return new SnsPublisherAdmin(this);
    }
  }

  @Override
  public boolean validateTopic(TopicName topicName)
  {
    // topics may not have been created yet
    return true;
  }

  @Override
  public void deleteObsoleteTopic(String topicId)
  {
    obsoleteTopics_.add(nameFactory_.getObsoleteTopicName(topicId));
  }

  @Override
  public IPublisher getPublisherByName(String topicId)
  {
    obsoleteTopics_.add(nameFactory_.getObsoleteTopicName(topicId));
    topics_.add(nameFactory_.getTopicName(topicId));
    
    return super.getPublisherByName(topicId);
  }

  @Override
  public IPublisher getPublisherByName(String serviceId, String topicId)
  {
    topics_.add(nameFactory_.getTopicName(serviceId, topicId));
    
    return super.getPublisherByName(serviceId, topicId);
  }
  
  @Override
  public IPublisher getPublisherByName(TopicName topicName)
  {
    topics_.add(topicName);
    
    return super.getPublisherByName(topicName);
  }
  
  private String getTopicPolicy(String topicArn)
  {
    StringBuilder s = new StringBuilder("{" + 
          "\"Version\":\"2008-10-17\"," + 
          "\"Id\":\"__default_policy_ID\"," + 
          "\"Statement\":[" + 
            "{" + 
              "\"Sid\":\"__default_statement_ID\"," + 
              "\"Effect\":\"Allow\"," + 
              "\"Principal\":{" + 
                "\"AWS\":\"*\"" + 
              "}," + 
              "\"Action\":[" + 
                "\"SNS:GetTopicAttributes\"," + 
                "\"SNS:SetTopicAttributes\"," + 
                "\"SNS:AddPermission\"," + 
                "\"SNS:RemovePermission\"," + 
                "\"SNS:DeleteTopic\"," +
                "\"SNS:Subscribe\"," + 
                "\"SNS:ListSubscriptionsByTopic\"," + 
                "\"SNS:Publish\"," +  
                "\"SNS:Receive\"" + 
              "]," + 
              "\"Resource\":\"" + topicArn + "\"," + 
              "\"Condition\":{" + 
                "\"StringEquals\":{" + 
                  "\"AWS:SourceOwner\":\"" + accountId_ + "\"" + 
                "}" + 
              "}" + 
            "}");
    
    if(subscriberAccountIds_.size() > 0)
    {
      s.append( 
            ",{" + 
            "\"Sid\":\"__console_sub_0\"," + 
            "\"Effect\":\"Allow\"," + 
            "\"Principal\":{" + 
              "\"AWS\":[");
    
      boolean first = true;
      
      for(String subscriberAccountId : subscriberAccountIds_)
      {
        if(first)
          first = false;
        else
          s.append(",");
        
        s.append("\"");
        s.append(subscriberAccountId);
        s.append("\"");
      }
      
      s.append( 
          "]" + 
              "}," +
              "\"Action\":[" + 
                "\"SNS:Subscribe\"," + 
                "\"SNS:Receive\"" + 
              "]," + 
              "\"Resource\":\"" + topicArn + "\"" + 
            "}");
    }
    
    s.append( 
          "]" + 
        "}");
    
    return s.toString();
  }

  @Override
  public void createTopics(boolean dryRun)
  {
    for(TopicName topicName : topics_)
    {
      if(topicName.isLocal())
      {
        String topicArn = getTopicARN(topicName);
        
        try
        {
          GetTopicAttributesResult topicAttributes = snsClient_.getTopicAttributes(topicArn);
          
          log_.info("Topic " + topicName + " exists as " + topicArn + " with attributes " + topicAttributes.getAttributes());

          String topicPolicy = getTopicPolicy(topicArn);
          
          if(topicPolicy.equals(topicAttributes.getAttributes().get(POLICY)))
          {
            log_.info("Topic policy is OK");
          }
          else
          {
            updateTopicPolicy(topicArn, topicPolicy);
          }
        }
        catch(NotFoundException e)
        {
          if(dryRun)
          {
            log_.info("Topic " + topicName + " does not exist and would be created (dry run).");
          }
          else
          {
            CreateTopicRequest createTopicRequest = new CreateTopicRequest(topicName.toString());
            CreateTopicResult createTopicResult = snsClient_.createTopic(createTopicRequest);

            log_.info("Created topic " + topicName + " as " + createTopicResult.getTopicArn());
            
            updateTopicPolicy(topicArn, getTopicPolicy(topicArn));
          }
        }
      }
      else
      {
        log_.info("Topic " + topicName + " does not belong to this service and is unaffected.");
      }
    }
    
    deleteTopics(dryRun, obsoleteTopics_);
  }

  private void updateTopicPolicy(String topicArn, String topicPolicy)
  {
    log_.info("Updating topic policy...");
    
    snsClient_.setTopicAttributes(new SetTopicAttributesRequest()
        .withTopicArn(topicArn)
        .withAttributeName("Policy")
        .withAttributeValue(topicPolicy)
        );
    
    log_.info("Updating topic policy...Done.");
  }

//  @Override
//  public void createTopics(boolean dryRun)
//  {
//    for(SnsPublisher publisher : publishers_)
//    {
//      CreateTopicRequest createTopicRequest = new CreateTopicRequest(topicName.toString());
//      CreateTopicResult createTopicResult = snsClient_.createTopic(createTopicRequest);
//      //print TopicArn
//      log_.info("Created topic " + topicName + " as " + createTopicResult.getTopicArn());
//      //get request id for CreateTopicRequest from SNS metadata   
//      System.out.println("CreateTopicRequest - " + snsClient_.getCachedResponseMetadata(createTopicRequest));
//    }
//  }

  @Override
  public void deleteTopics(boolean dryRun)
  {
    deleteTopics(dryRun, obsoleteTopics_);
    deleteTopics(dryRun, topics_);
  }

  private void deleteTopics(boolean dryRun, Set<TopicName> topics)
  {
    for(TopicName topicName : topics)
    {
      if(topicName.isLocal())
      {
        String topicArn = getTopicARN(topicName);
        
        try
        {
          ListSubscriptionsByTopicResult topicSubscriptions = snsClient_.listSubscriptionsByTopic(topicArn);
          List<Subscription> subscriptions = topicSubscriptions.getSubscriptions();
          
          if(dryRun)
          {
            if(subscriptions.isEmpty())
            {
              log_.info("Topic " + topicName + " has no subscriptions and would be deleted (dry run).");
            }
            else
            {
              log_.warn("Topic " + topicName + " has " + subscriptions.size() + " subscriptions and cannot be deleted (dry run).");
              
              for(Subscription s : subscriptions)
              {
                log_.info("Topic " + topicName + " has subscription to " + s.getEndpoint() + " as " + s.getSubscriptionArn());
              }
            }
          }
          else
          {
            if(subscriptions.isEmpty())
            {
              log_.info("Deleting topic " + topicName + "...");
              
              try
              {
                snsClient_.deleteTopic(topicArn);
                log_.info("Deleted topic " + topicName);
              }
              catch(AmazonSNSException e)
              {
                log_.error("Failed to delete topic " + topicName, e);
              }
            }
            else
            {
              log_.warn("Topic " + topicName + " has " + subscriptions.size() + " subscriptions and cannot be deleted (dry run).");
              
              for(Subscription s : subscriptions)
              {
                log_.info("Topic " + topicName + " has subscription to " + s.getEndpoint() + " as " + s.getSubscriptionArn());
              }
            }
          }
        }
        catch(NotFoundException e)
        {
          log_.info("Topic " + topicName + " does not exist.");
        }
      }
      else
      {
        log_.info("Topic " + topicName + " does not belong to this service and is unaffected.");
      }
    }
  }
}
