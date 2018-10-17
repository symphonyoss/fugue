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

import org.symphonyoss.s2.common.fluent.IFluent;
import org.symphonyoss.s2.fugue.FugueLifecycleState;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;

/**
 * Base class for subscriber managers.
 * 
 * @author Bruce Skingle
 *
 * @param <P> Type of payload received.
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public abstract class AbstractSubscriberAdmin<P, T extends ISubscriberAdmin & IFluent<T>> extends AbstractSubscriberBase<P, T> implements ISubscriberAdmin
{
  protected AbstractSubscriberAdmin(INameFactory nameFactory, Class<T> type)
  {
    super(nameFactory, type);
  }

  @Override
  public ISubscriberAdmin withSubscription(Subscription subscription)
  {
    return super.withSubscription(null, subscription);
  }

  @Override
  public T withSubscription(String subscriptionId, String topicId,
      String... additionalTopicIds)
  {
    return super.withSubscription(null, subscriptionId, topicId, additionalTopicIds);
  }

  @Override
  public T withSubscription(String subscriptionId, Collection<TopicName> topicNames)
  {
    return super.withSubscription(null, subscriptionId, topicNames);
  }
  
  @Override
  public T withSubscription(String subscriptionId, String[] topicNames)
  {
    return super.withSubscription(null, subscriptionId, topicNames);
  }

  @Override
  public synchronized void start()
  {
    setLifeCycleState(FugueLifecycleState.Running);
  }

  @Override
  public synchronized void stop()
  {
    setLifeCycleState(FugueLifecycleState.Stopped);
  }
  
  protected abstract void createSubcription(TopicName topicName, SubscriptionName subscriptionName, boolean dryRun);
  
  protected abstract void deleteSubcription(TopicName topicName, SubscriptionName subscriptionName, boolean dryRun);
  
  @Override
  public void createSubscriptions(boolean dryRun)
  {
    for(SubscriptionImpl<?> subscription : getSubscribers())
    {
      for(TopicName topicName : subscription.getObsoleteTopicNames())
      {
        deleteSubcription(topicName, nameFactory_.getObsoleteSubscriptionName(topicName, subscription.getObsoleteSubscriptionId()), dryRun);
      }
      
      for(TopicName topicName : subscription.getTopicNames())
      {
        deleteSubcription(topicName, nameFactory_.getObsoleteSubscriptionName(topicName, subscription.getObsoleteSubscriptionId()), dryRun);
        
        createSubcription(topicName, nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId()), dryRun);
      }
    }
  }
  
  @Override
  public void deleteSubscriptions(boolean dryRun)
  {
    for(SubscriptionImpl<?> subscription : getSubscribers())
    {
      for(TopicName topicName : subscription.getObsoleteTopicNames())
      {
        deleteSubcription(topicName, nameFactory_.getObsoleteSubscriptionName(topicName, subscription.getObsoleteSubscriptionId()), dryRun);
      }
      
      for(TopicName topicName : subscription.getTopicNames())
      {
        deleteSubcription(topicName, nameFactory_.getObsoleteSubscriptionName(topicName, subscription.getObsoleteSubscriptionId()), dryRun);
        
        deleteSubcription(topicName, nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId()), dryRun);
      }
    }
  }
}
