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

package org.symphonyoss.s2.fugue.naming;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The name of a subscription.
 * 
 * This is normally constructed from the topic name and the subscription simple name, in that order
 * so that AWS policies can be written with resource rules of the form ${ENVIRONMENT}-*
 * 
 * In situations where the subscription may belong to an external environment, it is necessary to construct
 * the name from the subscription simple name and the topic name, in that order. This is done by calling the
 * newExternalInstance static factory method.
 * 
 * @author Bruce Skingle
 *
 */
public class SubscriptionName extends Name
{
  private final TopicName topicName_;
  private final String serviceId_;
  private final @Nullable String subscriptionId_;

//  /**
//   * Factory method for internal (normal) subscription names.
//   * 
//   * @param topicName     The TopicName of the topic to be subscribed to.
//   * @param subscription  The simple subscription name.
//   * 
//   * @return A new SubscriptionName 
//   */
//  public static SubscriptionName newInstance(TopicName topicName, String subscription)
//  {
//    return new SubscriptionName(topicName, subscription, topicName.toString(), subscription);
//  }
//  
//  /**
//   * Factory method for external subscription names.
//   * 
//   * In situations where the subscription may belong to an external environment, it is necessary to construct
//   * the name from the subscription simple name and the topic name, in that order.
//   * 
//   * @param topicName         The TopicName of the topic to be subscribed to.
//   * @param subscription      The simple subscription name.
//   * @param name              The name
//   * @param additional        Zero or more optional suffix elements.
//   * 
//   * @return A new SubscriptionName 
//   */
//  public static SubscriptionName newExternalInstance(TopicName topicName, String subscription, @Nonnull String name, String ...additional)
//  {
//    return new SubscriptionName(topicName, subscription, name, additional);
//  }
  
  /**
   * Connstructor.
   * 
   * @param topicName       The TopicName of the topic to be subscribed to.
   * @param serviceId       The ID of the service which owns this subscription.
   * @param subscriptionId  The optional subscriptionId (simple subscription name).
   * @param name            The first element of the actual name.
   * @param additional      Zero or more optional suffix elements.
   */
  public SubscriptionName(TopicName topicName, String serviceId, String subscriptionId, @Nonnull String name, String ...additional)
  {
    super(name, additional);
    
    topicName_ = topicName;
    serviceId_ = serviceId;
    subscriptionId_ = subscriptionId;
  }

  /**
   * 
   * @return The TopicName of the topic to which this subscription relates.
   */
  public TopicName getTopicName()
  {
    return topicName_;
  }

  /**
   * 
   * @return The ID of the service which owns this subscription.
   */
  public String getServiceId()
  {
    return serviceId_;
  }

  /**
   * 
   * @return The optional subscriptionId (simple subscription name).
   */
  public @Nullable String getSubscriptionId()
  {
    return subscriptionId_;
  }
}
