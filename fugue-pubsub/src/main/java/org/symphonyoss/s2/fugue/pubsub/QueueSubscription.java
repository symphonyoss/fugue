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

package org.symphonyoss.s2.fugue.pubsub;

import javax.annotation.concurrent.Immutable;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

/**
 * A subscription on a Queue.
 * 
 * @author Bruce Skingle
 */
@Immutable
public class QueueSubscription  extends QueueSubscriptionAdmin implements ISubscription<String>
{
  private final IThreadSafeRetryableConsumer<String> consumer_;
  
  private QueueSubscription(Builder builder)
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
  public static class Builder extends QueueSubscriptionAdmin.AbstractBuilder<Builder, QueueSubscription>
  {
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
    protected QueueSubscription construct()
    {
      return new QueueSubscription(this);
    }
  }
}
