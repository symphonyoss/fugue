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

/**
 * The name of a subscription.
 * 
 * This is constructed from the subscription simple name and the topic name, in that order
 * so that AWS policies can be written with resource rules of the form ${ENVIRONMENT}-*
 * 
 * SBE creates subscriptions with the topic followed by the simple subscription name.
 * @author Bruce Skingle
 *
 */
public class SubscriptionName extends Name
{
  private final TopicName topicName_;
  private final String subscription_;

  /**
   * Constructor.
   * 
   * @param topicName     The TopicName of the topic to be subscribed to.
   * @param subscription  The simple subscription name.
   */
  public SubscriptionName(TopicName topicName, String subscription)
  {
    super(subscription, topicName.toString());
    
    topicName_ = topicName;
    subscription_ = subscription;
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
   * @return The simple subscription name.
   */
  public String getSubscription()
  {
    return subscription_;
  }
}
