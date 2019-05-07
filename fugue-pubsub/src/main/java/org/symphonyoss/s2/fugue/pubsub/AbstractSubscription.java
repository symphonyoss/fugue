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

import javax.annotation.concurrent.Immutable;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.naming.INameFactory;

/**
 * An abstract subscription, subclasses deal with Topic and Queue subscriptions.
 * 
 * @author Bruce Skingle
 */
@Immutable
public abstract class AbstractSubscription // TODO: rename to Subscription
{
  /**
   * Constructor.
   * 
   * @param builder A builder.
   */
  protected AbstractSubscription(AbstractBuilder<?,?> builder)
  {
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
    protected INameFactory nameFactory_;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the name factory.
     * 
     * @param nameFactory A name factory.
     * 
     * @return This (fluent method).
     */
    public T withNameFactory(INameFactory nameFactory)
    {
      nameFactory_ = nameFactory;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(nameFactory_, "name factory");
    }
  }
}
