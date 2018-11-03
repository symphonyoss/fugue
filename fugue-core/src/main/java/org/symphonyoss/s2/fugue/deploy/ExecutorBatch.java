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

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IBatch based on an Executor.
 * 
 * If any task throws an exception this is propagated out as an IllegalStateException as soon
 * as possible, so a call to submit may result in an exception originating from a previous task.
 * 
 * We assume that any exception from any task is program fatal.
 * 
 * @author Bruce Skingle
 *
 */
public class ExecutorBatch implements IBatch
{
  private static Logger log_ = LoggerFactory.getLogger(ExecutorBatch.class);
  
  private CompletionService<Void> completionService_;
  private boolean                 closed_;
  private int                     taskCnt_;
  
  /**
   * Constructor.
   * 
   * @param executor The executor to process tasks.
   */
  public ExecutorBatch(ExecutorService executor)
  {
//    executor_ = executor;
    
    completionService_ = new ExecutorCompletionService<Void>(executor);
  }
  
  /**
   * Submit the given task.
   * 
   * @param task Some task to be executed as part of the batch.
   */
  @Override
  public void submit(Runnable task)
  {
    Future<Void> future;
    
    while((future = poll()) != null)
    {
      try
      {
        future.get();
      }
      catch (InterruptedException | ExecutionException e)
      {
        throw new IllegalStateException("Batch task failed", e);
      }
    }
    
    doSubmit(task);
  }
  
  private synchronized Future<Void> poll()
  {
    Future<Void> future = completionService_.poll();
    
    if(future != null)
      taskCnt_--;
    
    return future;
  }
  
  private synchronized void doSubmit(Runnable task)
  {
    if(closed_)
      throw new IllegalStateException("waitForAllTasks() has already been called");
    
    taskCnt_++;
    completionService_.submit(task, null);
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
      try
      {
        completionService_.take().get();
        taskCnt_--;
      }
      catch (InterruptedException | ExecutionException e)
      {
        throw new IllegalStateException("Batch task failed", e);
      }
    }
  }
}
