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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.symphonyoss.s2.common.fluent.IFluent;
import org.symphonyoss.s2.fugue.FugueLifecycleComponent;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

/**
 * Base class for subscriber managers and admin controllers.
 * 
 * @author Bruce Skingle
 *
 * @param <P> Type of payload received.
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public abstract class AbstractSubscriberBase<P, T extends IFluent<T>> extends FugueLifecycleComponent<T>
{
  private List<Subscription<P>>             subscribers_                  = new ArrayList<>();
  
  protected AbstractSubscriberBase(Class<T> type)
  {
    super(type);
  }

  protected T withSubscription(@Nullable IThreadSafeRetryableConsumer<P> consumer, String subscriptionName, String topicName,
      String... additionalTopicNames)
  {
    assertConfigurable();
    
    List<String> topicNames = new ArrayList<>();

    topicNames.add(topicName);
    
    if(additionalTopicNames != null)
    {
      for(String name : additionalTopicNames)
      {
        topicNames.add(name);
      }
    }
    
    subscribers_.add(new Subscription<P>(topicNames, subscriptionName, consumer));
    
    return self();
  }

  protected T withSubscription(@Nullable IThreadSafeRetryableConsumer<P> consumer, String subscriptionName, List<String> topicNames)
  {
    assertConfigurable();
    
    if(topicNames.isEmpty())
      throw new IllegalArgumentException("At least one topic name is required");
    
    subscribers_.add(new Subscription<P>(topicNames, subscriptionName, consumer));
    
    return self();
  }
  
  protected T withSubscription(@Nullable IThreadSafeRetryableConsumer<P> consumer, String subscriptionName, String[] topicNames)
  {
    assertConfigurable();
    
    if(topicNames==null || topicNames.length==0)
      throw new IllegalArgumentException("At least one topic name is required");
    
    List<String> topicNameList = new ArrayList<>();

    for(String name : topicNames)
    {
      topicNameList.add(name);
    }
    
    subscribers_.add(new Subscription<P>(topicNameList, subscriptionName, consumer));
    
    return self();
  }

  protected List<Subscription<P>> getSubscribers()
  {
    return subscribers_;
  }
}
