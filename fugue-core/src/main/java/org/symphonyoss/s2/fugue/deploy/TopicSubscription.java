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

package org.symphonyoss.s2.fugue.deploy;

import org.symphonyoss.s2.common.dom.json.JsonObject;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;

public abstract class TopicSubscription extends Subscription
{
  private final INameFactory nameFactory_;
  private final String       serviceId_;
  private final String       topicId_;
  private final String       subscriptionId_;
  private final String       queueName_;
  
  public TopicSubscription(JsonObject<?> json, INameFactory nameFactory)
  {
    super(json, 10);
    
    nameFactory_ = nameFactory;
    
    serviceId_ = json.getString("serviceId", null);
    topicId_ = json.getRequiredString("topicId");
    subscriptionId_ = json.getString("subscriptionId", null);
    
    TopicName topicName = serviceId_ == null ? nameFactory_.getTopicName(topicId_) : nameFactory_.getTopicName(serviceId_, topicId_);
    
    queueName_ = nameFactory_.getSubscriptionName(topicName, subscriptionId_).toString();
  }

  public String getQueueName()
  {
    return queueName_;
  }
}
