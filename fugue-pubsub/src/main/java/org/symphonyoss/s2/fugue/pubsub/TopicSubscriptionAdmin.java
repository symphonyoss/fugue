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
  private final String                         filterPropertyName_;
  private final boolean                        filterExclude_;
  private final ImmutableSet<String>           filterPropertyValues_;
  private final String                         lambdaConsumer_;

  protected TopicSubscriptionAdmin(AbstractBuilder<?,?> builder)
  {
    subscriptionNames_    = builder.subscriptionNames_;
    filterPropertyName_   = builder.filterPropertyName_;
    filterExclude_        = builder.filterExclude_;
    filterPropertyValues_ = ImmutableSet.copyOf(builder.filterPropertyValues_);
    lambdaConsumer_       = builder.lambdaConsumer_;
  }
  
  @Override
  public ImmutableSet<SubscriptionName> getSubscriptionNames()
  {
    return subscriptionNames_;
  }

  @Override
  public String getFilterPropertyName()
  {
    return filterPropertyName_;
  }

  @Override
  public boolean isFilterExclude()
  {
    return filterExclude_;
  }

  @Override
  public ImmutableSet<String> getFilterPropertyValues()
  {
    return filterPropertyValues_;
  }

  @Override
  public String getLambdaConsumer()
  {
    return lambdaConsumer_;
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
    private Set<String>                    topicIds_ = new HashSet<>();
    private String                         subscriptionId_;
    private String                         serviceId_;
    private String                         filterPropertyName_;
    private boolean                        filterExclude_;
    private Set<String>                    filterPropertyValues_ = new HashSet<>();
    private ImmutableSet<SubscriptionName> subscriptionNames_;
    private String                         lambdaConsumer_;

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
    
    /**
     * Set the name of the property to be used for filtering.
     * 
     * @param filterPropertyName The name of the property to be used for filtering.
     * 
     * @return This (fluent method).
     */
    public T withFilterPropertyName(String filterPropertyName)
    {
      filterPropertyName_ = filterPropertyName;
      
      return self();
    }
    
    /**
     * Set the filtering to be inclusive or exclusive.
     * 
     * @param filterExclude If true then filtering is exclusive, the default is inclusive.
     * 
     * @return This (fluent method).
     */
    public T withFilterExclude(boolean filterExclude)
    {
      filterExclude_ = filterExclude;
      
      return self();
    }
    
    /**
     * Add the given values to the list of filterPropertyName values to be accepted or excluded.
     * 
     * @param value values of filterPropertyName values to be accepted or excluded.
     * 
     * @return This (fluent method).
     */
    public T withFilterPropertyValues(String ...value)
    {
      if(value != null)
      {
        for(String topicId : value)
          filterPropertyValues_.add(topicId);
      }
      
      return self();
    }

    /**
     * Set the name of a lambda function to be triggered to process messages on this subscription.
     * 
     * @param lambdaConsumer The name of a lambda function to be triggered to process messages on this subscription.
     * 
     * @return this (fluent method).
     */
    public T withLambdaConsumer(String lambdaConsumer)
    {
      lambdaConsumer_ = lambdaConsumer;
      
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
