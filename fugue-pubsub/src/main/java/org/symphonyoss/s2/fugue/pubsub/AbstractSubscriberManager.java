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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.FugueLifecycleComponent;
import org.symphonyoss.s2.fugue.FugueLifecycleState;
import org.symphonyoss.s2.fugue.core.strategy.naming.INamingStrategy;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

public abstract class AbstractSubscriberManager<T extends AbstractSubscriberManager<T>> extends FugueLifecycleComponent<T>
{
  protected static final long                        FAILED_DEAD_LETTER_RETRY_TIME = TimeUnit.HOURS.toMillis(1);
  protected static final long                        FAILED_CONSUMER_RETRY_TIME    = TimeUnit.SECONDS.toMillis(30);

  private static final Logger                        log_                          = LoggerFactory
      .getLogger(AbstractSubscriberManager.class);

  private final ITraceContextFactory                 traceFactory_;
  private final IThreadSafeRetryableConsumer<ImmutableByteArray> consumer_;
  private final IThreadSafeConsumer<ImmutableByteArray>          unprocessableMessageConsumer_;
  

  private Map<String, Set<String>>                   subscriptionsByTopic_            = new HashMap<>();
  private Map<String, Set<String>>                   topicsBySubscription_            = new HashMap<>();

  
  public AbstractSubscriberManager(Class<T> type, ITraceContextFactory traceFactory,
      IThreadSafeRetryableConsumer<ImmutableByteArray> consumer,
      IThreadSafeConsumer<ImmutableByteArray> unprocessableMessageConsumer)
  {
    super(type);
    
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    unprocessableMessageConsumer_ = unprocessableMessageConsumer;
  }

  /**
   * Subscribe to the given subscription on the given topic.
   * 
   * @param topicName           The name of the topic to subscribe to.
   * @param subscriptionName    The name of the subscription to subscribe to.
   * 
   * @throws IllegalArgumentException If a duplicate request is made.
   * 
   * @return this (fluent method)
   */
  public synchronized T withSubscription(String topicName, String subscriptionName)
  {
    assertConfigurable();
    
    Set<String> set = subscriptionsByTopic_.get(topicName);
    
    if(set == null)
    {
      set = new HashSet<>();
      subscriptionsByTopic_.put(topicName, set);
    }
    
    if(!set.add(subscriptionName))
    {
      throw new IllegalArgumentException("Subscription " + subscriptionName + " on topic " + topicName + " already exists.");
    }
    
    set = topicsBySubscription_.get(subscriptionName);
    
    if(set == null)
    {
      set = new HashSet<>();
      topicsBySubscription_.put(subscriptionName, set);
    }
    
    set.add(topicName);
    
    return self();
  }

  /**
   * Start with the given subscriptions.
   * 
   * We guarantee not to provide the same topic/subscription name more than once, it would be an error
   * to do so and we throw IllegalArgumentException to our caller if they request a duplicate.
   * 
   * We provide the same set of information indexed both ways as a convenience, the implementor should
   * process one or the other but not both.
   * 
   * @param subscriptionsByTopic  A map of topic names, each entry is a set of subscription names.
   * @param topicsBySubscription  A map of subscription names, each entry is a set of topic names.
   */
  protected abstract void startSubscriptions(Map<String, Set<String>> subscriptionsByTopic, Map<String, Set<String>> topicsBySubscription);

  /**
   * Stop all subscribers.
   */
  protected abstract void stopSubscriptions();
  
  protected ITraceContextFactory getTraceFactory()
  {
    return traceFactory_;
  }

  @Override
  public synchronized final void start()
  {
    setLifeCycleState(FugueLifecycleState.Starting);
    
    startSubscriptions(subscriptionsByTopic_, topicsBySubscription_);
    
    setLifeCycleState(FugueLifecycleState.Running);
  }

  @Override
  public synchronized final void stop()
  {
    setLifeCycleState(FugueLifecycleState.Stopping);
    
    stopSubscriptions();
    
    setLifeCycleState(FugueLifecycleState.Stopped);
  }

  /**
   * Handle the given message.
   * 
   * @param immutableByteArray A received message.
   * @param trace A trace context.
   * 
   * @return The number of milliseconds after which a retry should be made, or -1 if the message was
   * processed and no retry is necessary.
   */
  public long handleMessage(ImmutableByteArray immutableByteArray, ITraceContext trace)
  {
    try
    {
      consumer_.consume(immutableByteArray, trace);
    }
    catch (RetryableConsumerException e)
    {
      log_.warn("Unprocessable message, will retry", e);
      
      // TODO: how do we break an infinite loop?
      
      if(e.getRetryTime() == null || e.getRetryTimeUnit() == null)
        return FAILED_CONSUMER_RETRY_TIME;
      
      return e.getRetryTimeUnit().toMillis(e.getRetryTime());
    }
    catch (RuntimeException  e)
    {
      log_.warn("Unprocessable message, will retry", e);
      
      // TODO: how do we break an infinite loop?
      
      return FAILED_CONSUMER_RETRY_TIME;
    }
    catch (FatalConsumerException e)
    {
      log_.error("Unprocessable message, aborted", e);
      trace.trace("MESSAGE_IS_UNPROCESSABLE");
      try
      {
        unprocessableMessageConsumer_.consume(immutableByteArray, trace);
      }
      catch(RuntimeException e2)
      {
        log_.error("Unprocessable message consumer failed", e);
        return FAILED_DEAD_LETTER_RETRY_TIME;
      }
    }
    
    return -1;
  }
}
