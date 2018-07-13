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

import java.util.List;

import org.symphonyoss.s2.common.fluent.IFluent;
import org.symphonyoss.s2.fugue.FugueLifecycleState;

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
  protected AbstractSubscriberAdmin(Class<T> type)
  {
    super(type);
  }

  @Override
  public T withSubscription(String subscriptionName, String topicName,
      String... additionalTopicNames)
  {
    return super.withSubscription(null, subscriptionName, topicName, additionalTopicNames);
  }

  @Override
  public T withSubscription(String subscriptionName, List<String> topicNames)
  {
    return super.withSubscription(null, subscriptionName, topicNames);
  }
  
  @Override
  public T withSubscription(String subscriptionName, String[] topicNames)
  {
    return super.withSubscription(null, subscriptionName, topicNames);
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
}
