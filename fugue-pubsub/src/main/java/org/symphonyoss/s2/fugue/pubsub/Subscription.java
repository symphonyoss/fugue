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

package org.symphonyoss.s2.fugue.pubsub;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;

/**
 * A subscription as represented in the Fugue API.
 * 
 * This is simply a representation of the parameters of a Subscription so that it can be passed through the API.
 * 
 * A Subscription may have an id, it has a serviceId which defaults to the current service, in which case it
 * is represented as null in this class. It may also have an obsolete ID, which may also be null in this
 * class.
 * 
 * The eventual name for a Subscription is 
 * 
 * ${environmentType}-${environmentId}-${serviceId}-${subscriptionId}-${topicServiceId}-${topicId}
 * 
 * ${subscriptionId} may be omitted, a subscription ID is only needed if a single service has multiple subscriptions
 * to the same topic.
 * 
 * ${topicServiceId} is the service ID of the topic owner.
 * ${topicId} is the id of the topic
 * 
 * @author Bruce Skingle
 *
 */
public class Subscription
{
  private String  id_;

  private List<Topic> topics_ = new LinkedList<>();
  
  /**
   * Set the optional subscriptionId.
   * 
   * @param id The subscriptionId.
   * 
   * @return This (fluent method)
   */
  public Subscription withId(String id)
  {
    id_ = id;
    
    return this;
  }

  /**
   * Add the given Topic to the subscription.
   * 
   * It is possible to create the same subscription on multiple topics in a single operation, this method
   * may therefore be called multiple times.
   * 
   * @param topic The Topic.
   * 
   * @return This (fluent method)
   */
  public Subscription withTopic(Topic topic)
  {
    topics_.add(topic);
    
    return this;
  }
  
  /**
   * Add the given locally owned Topic to the subscription.
   * 
   * It is possible to create the same subscription on multiple topics in a single operation, this method
   * may therefore be called multiple times.
   * 
   * @param topicId The id of the Topic, which belongs to the current service.
   * 
   * @return This (fluent method)
   */
  public Subscription withTopic(String topicId)
  {
    topics_.add(new Topic(topicId));
    
    return this;
  }

  /**
   * 
   * @return The topic ID (simple name).
   */
  public String getId()
  {
    return id_;
  }

  /**
   * Create a Collection of TopicNames for all topics for this subscription, using the given NameFactory.
   * 
   * @param nameFactory The name factory to use (which knows the current environment, service etc)
   * 
   * @return A Collection of fully qualified topic names.
   */
  public Collection<TopicName> createTopicNames(INameFactory nameFactory)
  {
    List<TopicName> names = new LinkedList<>();
    
    for(Topic t : topics_)
      names.add(t.createTopicName(nameFactory));
    
    return names;
  }
}
