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
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.FugueLifecycleState;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;

/**
 * Base class for subscriber managers.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public abstract class AbstractSubscriberAdmin<T extends AbstractSubscriberAdmin<T>> extends AbstractSubscriberBase<Void, T> implements ISubscriberAdmin<T>
{
  private static final Logger log_ = LoggerFactory.getLogger(AbstractSubscriberAdmin.class);
  
  protected final List<SubscriptionImpl<?>> obsoleteSubscribers_;
  
  protected AbstractSubscriberAdmin(Class<T> type, Builder<?,T> builder)
  {
    super(type, builder);
    
    obsoleteSubscribers_ = builder.obsoleteSubscribers_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   * @param <T>   The concrete type returned by fluent methods.
   * @param <B>   The concrete type of the built object.
   */
  public static abstract class Builder<T extends Builder<T,B>, B extends AbstractSubscriberAdmin<B>>
  extends AbstractSubscriberBase.Builder<Void,T,B>
  implements ISubscriberAdminBuilder<T,B>
  {
    protected List<SubscriptionImpl<?>> obsoleteSubscribers_ = new ArrayList<>();
    
    protected Builder(Class<T> type)
    {
      super(type);
    }

    @Override
    public T withSubscription(Subscription subscription)
    {
      return super.withSubscription(null, subscription);
    }
    
    @Override
    public T withObsoleteSubscription(String subscriptionId, String topicId,
        String... additionalTopicIds)
    {
      taskList_.add(() ->
      {
        Collection<TopicName> topicNames = nameFactory_.getTopicNameCollection(topicId, additionalTopicIds);
        
        obsoleteSubscribers_.add(new SubscriptionImpl<T>(
            topicNames,
            subscriptionId, null));
      });
      
      return self();
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
    log_.info("About to create subscriptions...");
    for(SubscriptionImpl<?> subscription : getSubscribers())
    {
      for(TopicName topicName : subscription.getTopicNames())
      {
        createSubcription(topicName, nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId()), dryRun);
      }
    }

    deleteObsoleteSubscriptions(dryRun);
  }
  
  @Override
  public void deleteSubscriptions(boolean dryRun)
  {
    log_.info("About to delete subscriptions...");
    for(SubscriptionImpl<?> subscription : getSubscribers())
    {

      log_.info("About to delete subscriptions... subscriptionId=" + subscription.getSubscriptionId());
      for(TopicName topicName : subscription.getTopicNames())
      {
        deleteSubcription(topicName, nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId()), dryRun);
      }
    }
    
    deleteObsoleteSubscriptions(dryRun);
  }
  
  private void deleteObsoleteSubscriptions(boolean dryRun)
  {
    for(SubscriptionImpl<?> subscription : obsoleteSubscribers_)
    {
      log_.info("About to delete obsolete subscriptions... subscriptionId=" + subscription.getSubscriptionId());
      for(TopicName topicName : subscription.getTopicNames())
      {
        deleteSubcription(topicName, nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId()), dryRun);
      }
    }
  }
}
