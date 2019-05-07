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
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.naming.Name;

import com.google.common.collect.ImmutableSet;

/**
 * A subscription on a Queue.
 * 
 * @author Bruce Skingle
 */
@Immutable
public class QueueSubscriptionAdmin implements ISubscriptionAdmin
{
  private final ImmutableSet<Name> subscriptionNames_;

  protected QueueSubscriptionAdmin(AbstractBuilder<?,?> builder)
  {
    subscriptionNames_ = builder.subscriptionNames_;
  }
  
  @Override
  public ImmutableSet<Name> getSubscriptionNames()
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ISubscriptionAdmin> extends BaseAbstractBuilder<T,B>
  {
    private Set<Name>          builderSubscriptionNames_ = new HashSet<>();
    private ImmutableSet<Name> subscriptionNames_;

    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Add the given subscription names.
     * 
     * @param subscriptionNames subscription names.
     * 
     * @return This (fluent method).
     */
    public T withSubscriptionNames(Name ...subscriptionNames)
    {
      if(subscriptionNames != null)
      {
        for(Name subscriptionName : subscriptionNames)
          builderSubscriptionNames_.add(subscriptionName);
      }
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(builderSubscriptionNames_, "subscription names");
      
      subscriptionNames_ = ImmutableSet.copyOf(builderSubscriptionNames_);
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, QueueSubscriptionAdmin>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected QueueSubscriptionAdmin construct()
    {
      return new QueueSubscriptionAdmin(this);
    }
  }
}
