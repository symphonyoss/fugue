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

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.counter.IBusyCounter;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.counter.ScaleAction;
import org.symphonyoss.s2.fugue.deploy.ExecutorBatch;
import org.symphonyoss.s2.fugue.deploy.IBatch;
import org.symphonyoss.s2.fugue.pipeline.ICloseableConsumer;

public abstract class AbstractPullSubscriber implements Runnable
{
  private static final Logger log_ = LoggerFactory.getLogger(AbstractPullSubscriber.class);
  
  private final AbstractPullSubscriberManager<?, ?> manager_;
  private final String                              subscriptionName_;
  private final ICounter                            counter_;
  private final IBusyCounter                        busyCounter_;
  private final long                                extensionFrequency_;
  private final ICloseableConsumer                  consumer_;
  private boolean                                   running_ = true;

  
  public AbstractPullSubscriber(AbstractPullSubscriberManager<?,?> manager,
      String subscriptionName,
      
      ICounter counter, IBusyCounter busyCounter,
      long extensionFrequency, ICloseableConsumer consumer)
  {
    manager_ = manager;
    subscriptionName_ = subscriptionName;
    counter_ = counter;
    busyCounter_ = busyCounter;
    extensionFrequency_ = extensionFrequency;
    consumer_ = consumer;
  }

  public void close()
  {
    consumer_.close();
  }

  protected abstract IPullSubscriberContext getContext() throws IOException;
  
  protected abstract Runnable       getNonIdleSubscriber();
  
  protected void getSomeMessages()
  {
    // receive messages from the queue
        
    log_.info("About to read for " + subscriptionName_ + "...");
    try (IPullSubscriberContext context = getContext())
    {
      getSomeMessages(context);
    }
    catch (IOException e)
    {
      log_.error("Unable to pull messages", e);
    }
  }
  
  protected void getSomeMessages(IPullSubscriberContext context)
  {
    // receive messages from the queue
        
    log_.info("About to read for " + subscriptionName_ + "...");
    try
    {
      Collection<IPullSubscriberMessage> messages = context.nonBlockingPull();
      
     
      if(messages.isEmpty())
      {
        if(busyCounter_ != null)
        {
          if(busyCounter_.busy(0) == ScaleAction.ScaleDown)
          {
            stop();
            return;
          }
        }
        
        log_.info("Blocking read for " + subscriptionName_ + "...");
        
        messages = context.blockingPull();
        
        log_.info("Blocking read for " + subscriptionName_ + " returned " + messages.size());
        
        if(messages.isEmpty())
        {
          return;
        }
        
        scheduleExtra(1);
      }
      else
      {
        if(busyCounter_ != null)
          busyCounter_.busy(messages.size());
        
        scheduleExtra(1);
        
        log_.info("Non-Blocking read for " + subscriptionName_ + " returned " + messages.size());
      }
      
      
      
      IBatch<IPullSubscriberMessage>  batch = new ExecutorBatch<>(manager_.getHandlerExecutor());
      
      if(counter_ != null)
        counter_.increment(messages.size());
      
      for(IPullSubscriberMessage message : messages)
      {
        log_.debug("handle message " + message.getMessageId());
//        log_.debug("handle message " + message);
        batch.submit(message);
      }
      
      Collection<IPullSubscriberMessage> incompleteTasks;
      do
      {
        incompleteTasks = batch.waitForAllTasks(extensionFrequency_);
        
        for(IPullSubscriberMessage message : incompleteTasks)
        {
          log_.debug("extend message " + message.getMessageId());
          message.extend();
        }
      } while(!incompleteTasks.isEmpty());
    }
    catch(RuntimeException e)
    {
      log_.error("Error processing message", e);
      
      throw e;
    }
    catch (Error e)
    {
      /*
       * This method is called from an executor so I am catching Error because otherwise they will
       * be swallowed.
       * 
       * If we are catching an OutOfMemoryError then it may be futile to try to log this but on balance
       * I think it's worth trying.
       */
      
      try
      {
        log_.error("Error processing message", e);
      }
      finally
      {
        System.exit(1);
      }
    }
    finally
    {
      log_.debug("Done pull request");
    }
  }

  private void scheduleExtra(int count)
  {

    if(isRunning() && !Fugue.isDebugSingleThread())
    {
      for(int i=0 ; i<count ; i++)
        manager_.submit(getNonIdleSubscriber(), false);

      log_.debug("Extra schedule " + count + " for " + subscriptionName_);
    }
  }

  @Override
  public void run()
  {
    run(true);
  }

  protected void run(boolean runIfIdle)
  {
    if(isRunning())
    {
      if(runIfIdle)
      {
        try
        {
          while(isRunning())
          {
            getSomeMessages();
          }
        }
        finally
        {
          if(runIfIdle && isRunning())
          {
            // This "can't happen"
            log_.error("Main subscriber thread returned, rescheduling...");
            
            manager_.submit(this, true);
          }
        }
      }
      else
      {
        if(isRunning())
        {
          getSomeMessages();
        }
      }
    }
  }
  


  public synchronized boolean isRunning()
  {
    return running_;
  }
  
  public synchronized void stop()
  {
    running_ = false;
  }
}
