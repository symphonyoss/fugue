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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An implementation of IBatch based on an Executor.
 * 
 * @author Bruce Skingle
 *
 */
public class ExecutorBatch implements IBatch
{
  private ExecutorService executor_;
  private List<Future<?>> futures_ = new LinkedList<>();
  
  /**
   * Constructor.
   * 
   * @param executor The executor to process tasks.
   */
  public ExecutorBatch(ExecutorService executor)
  {
    executor_ = executor;
  }
  
  /**
   * Submit the given task.
   * 
   * @param task Some task to be executed as part of the batch.
   */
  @Override
  public void submit(Runnable task)
  {
    // TODO: implement me
    // futures_.add(executor_.submit(task));
    
    task.run();
  }
  
  /**
   * Block until all tasks have completed.
   */
  @Override
  public void waitForAllTasks()
  {
    // TODO: implement me
  }
}
