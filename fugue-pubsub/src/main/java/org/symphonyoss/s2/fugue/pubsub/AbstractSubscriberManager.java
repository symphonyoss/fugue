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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.FugueLifecycleComponent;
import org.symphonyoss.s2.fugue.FugueLifecycleState;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

/**
 * Base class for subscriber managers.
 * 
 * @author Bruce Skingle
 *
 * @param <P> Type of payload received.
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public abstract class AbstractSubscriberManager<P, T extends ISubscriberManager<P,T>> extends FugueLifecycleComponent<T> implements ISubscriberManager<P,T>
{
  protected static final long          FAILED_DEAD_LETTER_RETRY_TIME = TimeUnit.HOURS.toMillis(1);
  protected static final long          FAILED_CONSUMER_RETRY_TIME    = TimeUnit.SECONDS.toMillis(30);

  private static final Logger          log_                          = LoggerFactory.getLogger(AbstractSubscriberManager.class);

  private final ITraceContextFactory   traceFactory_;
  private final IThreadSafeConsumer<P> unprocessableMessageConsumer_;
  private List<Subscription<P>>        subscribers_                  = new ArrayList<>();

  
  protected AbstractSubscriberManager(Class<T> type, 
      ITraceContextFactory traceFactory,
      IThreadSafeConsumer<P> unprocessableMessageConsumer)
  {
    super(type);
    
    traceFactory_ = traceFactory;
    unprocessableMessageConsumer_ = unprocessableMessageConsumer;
  }

  @Override
  public synchronized T withSubscription(String topicName, String subscriptionName, 
      IThreadSafeRetryableConsumer<P> consumer)
  {
    assertConfigurable();
    
    subscribers_.add(new Subscription<P>(topicName, subscriptionName, consumer));
    
    return self();
  }
  
  @Override
  public synchronized T withSubscriptionsByConfig(List<String> topicNames, String subscriptionName, 
      IThreadSafeRetryableConsumer<P> consumer)
  {
    assertConfigurable();
    
    subscribers_.add(new Subscription<P>(topicNames, subscriptionName, consumer));
    
    return self();
  }

  protected abstract void startSubscription(Subscription<P> subscription);

  /**
   * Stop all subscribers.
   */
  protected abstract void stopSubscriptions();
  
  protected ITraceContextFactory getTraceFactory()
  {
    return traceFactory_;
  }

  protected List<Subscription<P>> getSubscribers()
  {
    return subscribers_;
  }

  @Override
  public synchronized void start()
  {
    setLifeCycleState(FugueLifecycleState.Starting);
    
    for(Subscription<P> s : subscribers_)
    {
      startSubscription(s);
    }
    
    setLifeCycleState(FugueLifecycleState.Running);
  }

  @Override
  public synchronized void stop()
  {
    setLifeCycleState(FugueLifecycleState.Stopping);
    
    stopSubscriptions();
    
    setLifeCycleState(FugueLifecycleState.Stopped);
  }

  /**
   * Handle the given message.
   * 
   * @param consumer  The consumer for the message.
   * @param payload   A received message.
   * @param trace     A trace context.
   * 
   * @return The number of milliseconds after which a retry should be made, or -1 if the message was
   * processed and no retry is necessary.
   */
  public long handleMessage(IThreadSafeRetryableConsumer<P> consumer, P payload, ITraceContext trace)
  {
    try
    {
      consumer.consume(payload, trace);
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
        unprocessableMessageConsumer_.consume(payload, trace);
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
