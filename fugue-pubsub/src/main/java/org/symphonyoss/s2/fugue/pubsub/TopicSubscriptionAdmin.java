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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;

import com.google.common.collect.ImmutableSet;

/**
 * A subscription on a Topic.
 * 
 * @author Bruce Skingle
 */
@Immutable
public class TopicSubscriptionAdmin implements ITopicSubscriptionAdmin
{
  private final ImmutableSet<SubscriptionName> subscriptionNames_;

  protected TopicSubscriptionAdmin(AbstractBuilder<?,?> builder)
  {
    subscriptionNames_ = builder.subscriptionNames_;
  }
  
  @Override
  public ImmutableSet<SubscriptionName> getSubscriptionNames()
  {
    return subscriptionNames_;
  }

  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   * 
   * @param <T> The concrete type of the builder for fluent methods.
   * @param <B> The type of the built object.
   *
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ISubscriptionAdmin> extends AbstractSubscription.AbstractBuilder<T,B>
  {
    private Set<String>          topicIds_ = new HashSet<>();
    private String               subscriptionId_;
    private String               serviceId_;
    private ImmutableSet<SubscriptionName> subscriptionNames_;

    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Set the optional subscriptionId.
     * 
     * The subscription (or queue in the case of SNS/SQS) will be named using the service name of the topic owner
     * and the subscriber. In the case where a single subscribing service requires more than one subscription
     * an additional subscription ID is needed to distinguish the multiple subscriptions. 
     * 
     * @param subscriptionId The subscription ID.
     * 
     * @return This (fluent method).
     */
    public T withSubscriptionId(String subscriptionId)
    {
      subscriptionId_ = subscriptionId;
      
      return self();
    }

    /**
     * Set the serviceId of the topic owner.
     * 
     * Defaults to the service ID in the provided nameFactory.
     * 
     * @param serviceId The serviceId of the topic owner.
     * 
     * @return This (fluent method).
     */
    public T withServiceId(String serviceId)
    {
      serviceId_ = serviceId;
      
      return self();
    }
    
    /**
     * Add the given topic IDs to the list of topics to be subscribed to.
     * 
     * @param topicIds topic IDs to the list of topics to be subscribed to.
     * 
     * @return This (fluent method).
     */
    public T withTopicIds(String ...topicIds)
    {
      if(topicIds != null)
      {
        for(String topicId : topicIds)
          topicIds_.add(topicId);
      }
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(topicIds_, "topic ID");
      
      Set<SubscriptionName> names = new HashSet<>();
       
      if(nameFactory_ != null)
      {
        /*
         * the super class validate method checks that nameFactory_ is not null, so if it is then the validation
         * will fail but without this guard we might get an NPE below.
         */
        for(String topicId : topicIds_)
        { 
          TopicName topicName = serviceId_ == null ? nameFactory_.getTopicName(topicId) : nameFactory_.getTopicName(serviceId_, topicId);
          
          names.add(nameFactory_.getSubscriptionName(topicName, subscriptionId_));
        }
      }
      
      subscriptionNames_ = ImmutableSet.copyOf(names);
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, TopicSubscriptionAdmin>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected TopicSubscriptionAdmin construct()
    {
      return new TopicSubscriptionAdmin(this);
    }
  }
}
