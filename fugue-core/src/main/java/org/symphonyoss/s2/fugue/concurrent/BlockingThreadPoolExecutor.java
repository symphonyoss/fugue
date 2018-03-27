/*
 *
 *
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The SSF licenses this file
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

package org.symphonyoss.s2.fugue.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.IFugueServer;

public class BlockingThreadPoolExecutor implements Executor
{
  private static Logger log_ = LoggerFactory.getLogger(BlockingThreadPoolExecutor.class);
  
  private final IFugueServer    server_;
  private final Thread[] threads_;
  private final LinkedBlockingQueue<FugueRunnable> queue_;
  
  private boolean running_ = true;

  public BlockingThreadPoolExecutor(IFugueServer server, int poolSize, ThreadFactory threadFactory)
  {
    server_ = server;
    queue_ = new LinkedBlockingQueue<FugueRunnable>(poolSize);
    threads_ = new Thread[poolSize];
    
    for(int i=0 ; i<poolSize ; i++)
    {
      threads_[i] = threadFactory.newThread(new Worker());
      threads_[i].start();
    }
  }
  
  @Override
  public void execute(Runnable task)
  {
    try
    {
      if(!queue_.offer(new FugueRunnable(server_, task)))
      {
        long start = System.currentTimeMillis();
        
        log_.info("Task blocked...");
        
        while(!queue_.offer(new FugueRunnable(server_, task), 500, TimeUnit.MILLISECONDS))
          log_.info("still blocked...");
        
        log_.info("Task queued after " + (System.currentTimeMillis() - start) + "ms.");
      }
    }
    catch (InterruptedException e)
    {
      throw new TransactionFault(e);
    }
  }
  
  public synchronized void shutdown()
  {
    running_ = false;
    
    for(Thread t : threads_)
      t.interrupt();
  }

  public synchronized boolean isRunning()
  {
    return running_;
  }

  class Worker implements Runnable
  {

    @Override
    public void run()
    {
      while(isRunning())
      {
        try
        {
          FugueRunnable task = queue_.take();
          
          task.run();
        }
        catch (InterruptedException e)
        {
          // If we were shutdown then we will exit.
        }
      }
    }
    
  }
}
