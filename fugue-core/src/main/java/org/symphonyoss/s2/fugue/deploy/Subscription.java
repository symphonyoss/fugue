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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.symphonyoss.s2.common.dom.json.IJsonArray;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.JsonObject;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;

public class Subscription
{
  private static final String MUST_BE_ARRAY_OF_OBJECTS = "\"subscriptions\" must be an array of objects.";
  
  private final String serviceId_;
  private final String topicId_;
  private final String subscriptionId_;
  private final int    batchSize_;
  
  public Subscription(JsonObject<?> json)
  {
    serviceId_ = json.getString("serviceId", null);
    topicId_ = json.getRequiredString("topicId");
    subscriptionId_ = json.getString("subscriptionId", null);
    batchSize_ = json.getInteger("batchSize", 10);
  }
  
  public static Collection<Subscription> getSubscriptions(JsonObject<?> json)
  {
    List<Subscription> result = new LinkedList<>();
    
    IJsonDomNode node = json.get("subscriptions");
    
    if(node instanceof IJsonArray)
    {
      IJsonArray<?> subscriptions = (IJsonArray<?>)node;
      
      for(IJsonDomNode sn : subscriptions)
      {
        if(sn instanceof JsonObject)
        {
          result.add(new Subscription((JsonObject<?>) sn));
        }
        else
        {
          throw new IllegalStateException(MUST_BE_ARRAY_OF_OBJECTS);
        }
      }
    }
    else if(node != null)
    {
      throw new IllegalStateException(MUST_BE_ARRAY_OF_OBJECTS);
    }
    
    return result;
  }

  public String getQueueName(INameFactory nameFactory)
  {
    TopicName topicName = serviceId_ == null ? nameFactory.getTopicName(topicId_) : nameFactory.getTopicName(serviceId_, topicId_);
    
    return nameFactory.getSubscriptionName(topicName, subscriptionId_).toString();
  }

  public int getBatchSize()
  {
    return batchSize_;
  }
}
