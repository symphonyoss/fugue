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

package org.symphonyoss.s2.fugue.inmemory.pubsub;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.IPubSubMessage;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;
import org.symphonyoss.s2.fugue.pubsub.IPublisherAdmin;

/**
 * The admin variation of an InMemoryPublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class InMemoryPublisherAdmin extends InMemoryPublisherBase<InMemoryPublisherAdmin> implements IPublisherAdmin
{
  private static final Logger log_            = LoggerFactory.getLogger(InMemoryPublisherAdmin.class);

  private Set<TopicName>      topics_         = new HashSet<>();
  
  private InMemoryPublisherAdmin(Builder builder)
  {
    super(InMemoryPublisherAdmin.class, builder);
  }

  /**
   * Concrete builder.
   * 
   * @author Bruce Skingle
   */
  public static class Builder extends InMemoryPublisherBase.Builder<Builder, InMemoryPublisherAdmin>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected InMemoryPublisherAdmin construct()
    {
      return new InMemoryPublisherAdmin(this);
    }
  }

  @Override
  public boolean validateTopic(TopicName topicName)
  {
    // topics may not have been created yet
    return true;
  }

  @Override
  public IPublisher getPublisherByName(String topicId)
  {
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

  @Override
  public void createTopics(boolean dryRun)
  {
    for(TopicName topicName : topics_)
    {
      if(topicName.isLocal())
      {
        InMemoryPubSub.createTopic(topicName);
      }
      else
      {
        log_.info("Topic " + topicName + " does not belong to this service and is unaffected.");
      }
    }
  }

  @Override
  public void deleteTopics(boolean dryRun)
  {
    deleteTopics(dryRun, topics_);
  }

  private void deleteTopics(boolean dryRun, Set<TopicName> topics)
  {
    for(TopicName topicName : topics)
    {
      if(topicName.isLocal())
      {
        List<LinkedList<IPubSubMessage>> subscriptions = InMemoryPubSub.getSubscriptions(topicName);
        
        if(subscriptions == null)
        {
          log_.info("Topic " + topicName + " does not exist.");
        }
        else
        {
          if(dryRun)
          {
            if(subscriptions.isEmpty())
            {
              log_.info("Topic " + topicName + " has no subscriptions and would be deleted (dry run).");
            }
            else
            {
              log_.warn("Topic " + topicName + " has " + subscriptions.size() + " subscriptions and cannot be deleted (dry run).");
            }
          }
          else
          {
            if(subscriptions.isEmpty())
            {
              log_.info("Deleting topic " + topicName + "...");
              
              InMemoryPubSub.deleteTopic(topicName);
              log_.info("Deleted topic " + topicName);
            }
            else
            {
              log_.warn("Topic " + topicName + " has " + subscriptions.size() + " subscriptions and cannot be deleted (dry run).");
            }
          }
        }
      }
      else
      {
        log_.info("Topic " + topicName + " does not belong to this service and is unaffected.");
      }
    }
  }
}
