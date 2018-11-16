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
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.Counter;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.deploy.ExecutorBatch;
import org.symphonyoss.s2.fugue.deploy.IBatch;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;


public abstract class AbstractPullSubscriberManager<P, T extends ISubscriberManager<P,T>> extends AbstractSubscriberManager<P,T>
{
  private static final Logger                 log_           = LoggerFactory
      .getLogger(AbstractPullSubscriberManager.class);

  private final IConfiguration                sqsConfig_;

  private ICounter                            counter_;
  private int                                 subscriberThreadPoolSize_;
  private int                                 handlerThreadPoolSize_;
  private final LinkedBlockingQueue<Runnable> executorQueue_ = new LinkedBlockingQueue<Runnable>();
  private final LinkedBlockingQueue<Runnable> handlerQueue_  = new LinkedBlockingQueue<Runnable>();
  private ThreadPoolExecutor                  subscriberExecutor_;
  private ThreadPoolExecutor                  handlerExecutor_;
  
  protected AbstractPullSubscriberManager(IConfiguration config, INameFactory nameFactory, Class<T> type,
      ITraceContextTransactionFactory traceFactory, IThreadSafeErrorConsumer<P> unprocessableMessageConsumer,
      String configPath)
  {
    super(nameFactory, type, traceFactory, unprocessableMessageConsumer);
    
    sqsConfig_ = config.getConfiguration(configPath);
  }

  public T withCounter(Counter counter)
  {
    counter_ = counter;
    
    return self();
  }
  
  protected ICounter getCounter()
  {
    return counter_;
  }

  @Override
  public void start()
  {
    initialize();
    
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
    
    log_.info("Starting AbstractPullSubscriberManager with " + subscriberThreadPoolSize_ +
        " subscriber threads and " + handlerThreadPoolSize_ + " handler threads for a total of " +
        getTotalSubscriptionCnt() + " subscriptions...");

    subscriberExecutor_ = new ThreadPoolExecutor(subscriberThreadPoolSize_, subscriberThreadPoolSize_,
        0L, TimeUnit.MILLISECONDS,
        executorQueue_, new NamedThreadFactory("PubSub-subscriber"));
    
    handlerExecutor_ = new ThreadPoolExecutor(subscriberThreadPoolSize_, handlerThreadPoolSize_,
        0L, TimeUnit.MILLISECONDS,
        handlerQueue_, new NamedThreadFactory("PubSub-handler", true));
      
    super.start();
  }

  protected void initialize()
  {
    subscriberThreadPoolSize_ = sqsConfig_.getInt("subscriberThreadPoolSize", 4);
    handlerThreadPoolSize_ = sqsConfig_.getInt("handlerThreadPoolSize", 9 * subscriberThreadPoolSize_);

//    subscriberThreadPoolSize_ = 4; //8 * getTotalSubscriptionCnt();
//    handlerThreadPoolSize_ = 9 * subscriberThreadPoolSize_;
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

  public void submit(Runnable subscriber, boolean force)
  {
    if(force || executorQueue_.size() < subscriberThreadPoolSize_)
      subscriberExecutor_.submit(subscriber);
  }

  protected void printQueueSize()
  {
    log_.debug("Queue size " + executorQueue_.size());
  }
  
  public IBatch newBatch()
  {
    return new ExecutorBatch(handlerExecutor_);
  }
}
