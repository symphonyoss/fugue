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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pubsub.QueueSubscription.Builder;

import com.google.common.collect.ImmutableSet;

/**
 * A subscription on a Topic.
 * 
 * @author Bruce Skingle
 */
@Immutable
public class TopicSubscription extends TopicSubscriptionAdmin implements ISubscription
{
  private final IThreadSafeRetryableConsumer<String> consumer_;

  protected TopicSubscription(Builder builder)
  {
    super(builder);
    
    consumer_ = builder.consumer_;
  }

  @Override
  public IThreadSafeRetryableConsumer<String> getConsumer()
  {
    return consumer_;
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends TopicSubscriptionAdmin.AbstractBuilder<Builder, TopicSubscription>
  {
    private Set<String>  subscriptionNames_ = new HashSet<>();
    private IThreadSafeRetryableConsumer<String> consumer_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    /**
     * Set the consumer for the subscription.
     * 
     * @param consumer A consumer for received messages.
     * 
     * @return this (fluent method)
     */
    public Builder withConsumer(IThreadSafeRetryableConsumer<String> consumer)
    {
      consumer_ = consumer;
      
      return this;
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(consumer_, "consumer");
    }

    @Override
    protected TopicSubscription construct()
    {
      return new TopicSubscription(this);
    }
  }
}
