/*
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.IPubSubMessage;

/**
 * In memory implementation of pub sub.
 * 
 * @author Bruce Skingle
 *
 */
class InMemoryPubSub
{
  static Map<TopicName, List<LinkedList<IPubSubMessage>>>  topicMap_ = new HashMap<>();
  
  synchronized static void send(TopicName topicName, IPubSubMessage item)
  {
    List<LinkedList<IPubSubMessage>> list = topicMap_.get(topicName);
    
    if(list != null)
    {
      for(LinkedList<IPubSubMessage> queue : list)
      {
        queue.add(item);
      }
    }
  }

  synchronized static void createTopic(TopicName topicName)
  {
    List<LinkedList<IPubSubMessage>> list = topicMap_.get(topicName);
    
    if(list == null)
    {
      list = new LinkedList<>();
    }
  }
  
  synchronized static List<LinkedList<IPubSubMessage>> getSubscriptions(TopicName topicName)
  {
    return topicMap_.get(topicName);
  }

  synchronized static void deleteTopic(TopicName topicName)
  {
    topicMap_.remove(topicName);
  }
}

  
