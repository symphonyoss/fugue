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

/**
 * A serial implementation of IBatch which just executes tasks in the submitting thread.
 * 
 * This is useful for debugging.
 * 
 * @param <T> The type of tasks in the batch
 * 
 * @author Bruce Skingle
 *
 */
public class SerialBatch<T extends Runnable> implements IBatch<T>
{
  /**
   * Constructor.
   * 
   */
  public SerialBatch()
  {
  }
  
  /**
   * Submit the given task.
   * 
   * @param task Some task to be executed as part of the batch.
   */
  @Override
  public void submit(T task)
  {
    task.run();
  }
  
  /**
   * Block until all tasks have completed.
   */
  @Override
  public void waitForAllTasks()
  {
  }

  @Override
  public Collection<T> waitForAllTasks(long timeoutMillis)
  {
    return new ArrayList<T>();
  }
}
