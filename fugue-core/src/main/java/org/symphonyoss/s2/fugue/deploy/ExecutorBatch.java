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

package org.symphonyoss.s2.fugue.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.CodingFault;

/**
 * An implementation of IBatch based on an Executor.
 * 
 * If any task throws an exception this is propagated out as an IllegalStateException as soon
 * as possible, so a call to submit may result in an exception originating from a previous task.
 * 
 * We assume that any exception from any task is program fatal.
 * 
 * @param <T> The type of tasks in the batch
 * 
 * @author Bruce Skingle
 *
 */
public class ExecutorBatch<T extends Runnable> implements IBatch<T>
{
  private static Logger        log_      = LoggerFactory.getLogger(ExecutorBatch.class);

  private CompletionService<T> completionService_;
  private boolean              closed_;
  private List<T>              taskList_ = new LinkedList<>();
  private int                  taskCnt_;
  
  /**
   * Constructor.
   * 
   * @param executor The executor to process tasks.
   */
  public ExecutorBatch(ExecutorService executor)
  {
//    executor_ = executor;
    
    completionService_ = new ExecutorCompletionService<>(executor);
  }
  
  /**
   * Submit the given task.
   * 
   * @param task Some task to be executed as part of the batch.
   */
  @Override
  public void submit(T task)
  {
    T completedTask;
    
    while((completedTask = poll()) != null)
    {
      log_.debug("Task completed: " + completedTask);
    }
    
    doSubmit(task);
  }
  
  private synchronized T poll()
  {
    Future<T> future = completionService_.poll();
    
    if(future == null)
      return null;
    

    taskCnt_--;
    return remove(future);
  }
  
  private T remove(Future<T> future)
  {
    try
    {
      T task = future.get();
      
      if(!taskList_.remove(task))
      {
        throw new CodingFault("Unrecognized task returned: " + task);
      }
      
      return task;
    }
    catch (InterruptedException | ExecutionException e)
    {
      throw new IllegalStateException("Batch task failed", e);
    }
  }

  private synchronized void doSubmit(T task)
  {
    if(closed_)
      throw new IllegalStateException("waitForAllTasks() has already been called");
    
    taskList_.add(task);
    taskCnt_++;
    
    completionService_.submit(task, task);
  }

  /**
   * Block until all tasks have completed.
   */
  @Override
  public synchronized void waitForAllTasks()
  {
    if(closed_)
      throw new IllegalStateException("waitForAllTasks() has already been called");
    
    closed_ = true;

    while(taskCnt_>0)
    {
//      log_.info("Waiting for " + taskCnt_ + " tasks...");

      taskCnt_--;
      
      try
      {
        remove(completionService_.take());
      }
      catch (InterruptedException e)
      {
        throw new IllegalStateException("Batch task failed", e);
      }
    }
  }
  


  /**
   * Block until all tasks have completed or the given timeout expires.
   * 
   * @param timeoutMillis Timeout in milliseconds.
   * @return The number of incomplete tasks in the batch.
   */
  @Override
  public synchronized Collection<T> waitForAllTasks(long timeoutMillis)
  {
    log_.debug("Wait for " + timeoutMillis + ", taskCnt_=" + taskCnt_);
    closed_ = true;
    
    long expiryTime = System.currentTimeMillis() + timeoutMillis;

    while(taskCnt_>0)
    {
      long timeout = expiryTime - System.currentTimeMillis();
      
      if(timeout < 1)
      {
        log_.debug("Time is up, taskCnt=" + taskCnt_);
        return new ArrayList<T>(taskList_);
      }
      
      try
      {
        Future<T> future = completionService_.poll(timeout, TimeUnit.MILLISECONDS);
        
        if(future == null)
        {
          log_.debug("All done, taskCnt=" + taskCnt_);
          return new ArrayList<T>(taskList_);
        }

        taskCnt_--;
        
        remove(future);
      }
      catch (InterruptedException e)
      {
        throw new IllegalStateException("Batch task failed", e);
      }
    }
    log_.debug("All done2, taskCnt_=" + taskCnt_);
    
    return new ArrayList<T>(taskList_);
  }
}
