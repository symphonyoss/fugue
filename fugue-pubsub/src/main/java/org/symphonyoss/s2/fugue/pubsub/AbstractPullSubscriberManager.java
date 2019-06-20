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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.counter.IBusyCounter;
import org.symphonyoss.s2.fugue.counter.ITopicBusyCounterFactory;
import org.symphonyoss.s2.fugue.naming.Name;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;

/**
 * Base class for synchronous pull type implementations.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public abstract class AbstractPullSubscriberManager<T extends AbstractPullSubscriberManager<T>> extends AbstractSubscriberManager<T>
{
  private static final Logger                 log_           = LoggerFactory
      .getLogger(AbstractPullSubscriberManager.class);

  private final ITopicBusyCounterFactory      busyCounterFactory_;

  private int                                 subscriberThreadPoolSize_;
  private int                                 handlerThreadPoolSize_;
  private final LinkedBlockingQueue<Runnable> executorQueue_ = new LinkedBlockingQueue<Runnable>();
  private final LinkedBlockingQueue<Runnable> handlerQueue_  = new LinkedBlockingQueue<Runnable>();
  private ThreadPoolExecutor                  subscriberExecutor_;
  private ThreadPoolExecutor                  handlerExecutor_;

  
  protected AbstractPullSubscriberManager(Class<T> type, Builder<?,T> builder)
  {
    super(type, builder);
    
    busyCounterFactory_  = builder.busyCounterFactory_;
    
    IConfiguration subscriberConfig = config_.getConfiguration(builder.getConfigPath());
    
    subscriberThreadPoolSize_ = subscriberConfig.getInt("subscriberThreadPoolSize", 4);
    handlerThreadPoolSize_ = subscriberConfig.getInt("handlerThreadPoolSize", 9 * subscriberThreadPoolSize_);

//    subscriberThreadPoolSize_ = 4; //8 * getTotalSubscriptionCnt();
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   * @param <T>   The concrete type returned by fluent methods.
   * @param <B>   The concrete type of the built object.
   */
  public static abstract class Builder<T extends Builder<T,B>, B extends AbstractPullSubscriberManager<B>>
  extends AbstractSubscriberManager.Builder<T,B>
  implements IPullSubscriberManagerBuilder<T,B>
  {
    private ITopicBusyCounterFactory         busyCounterFactory_;

    protected Builder(Class<T> type)
    {
      super(type);
    }
    
    @Override
    public T withBusyCounterFactory(ITopicBusyCounterFactory busyCounterFactory)
    {
      busyCounterFactory_ = busyCounterFactory;
      
      return self();
    }
    
    protected abstract String getConfigPath();
  }

  
  protected ITopicBusyCounterFactory getBusyCounterFactory()
  {
    return busyCounterFactory_;
  }

  @Override
  public void start()
  {
    if(Fugue.isDebugSingleThread())
    {
      subscriberThreadPoolSize_ = totalSubscriptionCnt_ == 0 ? 1 : totalSubscriptionCnt_;
      handlerThreadPoolSize_ = 1;
    }
    else
    {
      int min = getTotalSubscriptionCnt() * 2;
      
      if(subscriberThreadPoolSize_ < min)
      { 
        log_.warn("Configured for " + subscriberThreadPoolSize_ +
          " subscriber threads for a total of " +
          getTotalSubscriptionCnt() + " subscriptions, using " + min + " subscriber threads");
        
        subscriberThreadPoolSize_ = min;
      }
      
      min = subscriberThreadPoolSize_ * 2;
      
      if(handlerThreadPoolSize_ < min)
      { 
        log_.warn("Configured for " + handlerThreadPoolSize_ + " handler threads for " +
          subscriberThreadPoolSize_ + " subscriber treads, using " + min + " handler threads");
        
        handlerThreadPoolSize_ = min;
      }
    }
    
    log_.info("Starting AbstractPullSubscriberManager with " + subscriberThreadPoolSize_ +
        " subscriber threads and " + handlerThreadPoolSize_ + " handler threads for a total of " +
        getTotalSubscriptionCnt() + " subscriptions...");

    subscriberExecutor_ = new ThreadPoolExecutor(subscriberThreadPoolSize_, subscriberThreadPoolSize_,
        10000L, TimeUnit.MILLISECONDS,
        executorQueue_, new NamedThreadFactory("PubSub-subscriber"));
    
    handlerExecutor_ = new ThreadPoolExecutor(handlerThreadPoolSize_, handlerThreadPoolSize_,
        10000L, TimeUnit.MILLISECONDS,
        handlerQueue_, new NamedThreadFactory("PubSub-handler", true));
      
    super.start();
  }
  
  @Override
  protected void stopSubscriptions()
  {
      subscriberExecutor_.shutdown();
      
    try {
      // Wait a while for existing tasks to terminate
      if (!subscriberExecutor_.awaitTermination(60, TimeUnit.SECONDS)) {
        subscriberExecutor_.shutdownNow(); // Cancel currently executing tasks
      // Wait a while for tasks to respond to being cancelled
      if (!subscriberExecutor_.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
      subscriberExecutor_.shutdownNow();
      // Preserve interrupt status
        Thread.currentThread().interrupt();
    }
  }

  protected void submit(Runnable subscriber, boolean force)
  {
    if(force || executorQueue_.size() < subscriberThreadPoolSize_)
      subscriberExecutor_.submit(subscriber);
  }

  protected void printQueueSize()
  {
    log_.debug("Queue size " + executorQueue_.size());
  }
  
//  public IBatch newBatch()
//  {
//    return new ExecutorBatch(handlerExecutor_);
//  }

  ThreadPoolExecutor getHandlerExecutor()
  {
    return handlerExecutor_;
  }
  
  protected IBusyCounter createBusyCounter(Name subscriptionName)
  {
    if(getBusyCounterFactory() == null)
      return null;
    
    if(subscriptionName instanceof SubscriptionName)
    {
      return getBusyCounterFactory().create(((SubscriptionName)subscriptionName).getTopicName().getTopicId());
    }
    
    throw new ProgramFault("BusyCounter can only be set with SubscriptionName");
  }
}
