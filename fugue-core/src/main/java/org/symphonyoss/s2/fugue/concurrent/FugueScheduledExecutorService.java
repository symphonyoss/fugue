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

package org.symphonyoss.s2.fugue.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.fugue.IFugueServer;

/**
 * An implementation of ScheduledExecutorService which handles Faults and RuntimeExceptions the Fugue way.
 * 
 * @author Bruce Skingle
 *
 */
public class FugueScheduledExecutorService extends FugueExecutorService implements ScheduledExecutorService
{
  private final ScheduledExecutorService         exec_;

  /**
   * Create an instance with the given ExecutorService.
   * 
   * @param server    The IFugueServer in which this task will run.
   * @param exec      A ScheduledExecutorService to do the actual work.
   */
  public FugueScheduledExecutorService(IFugueServer server, ScheduledExecutorService exec)
  {
    super(server, exec);
    exec_ = exec;
  }

  /**
   * Create an instance with a ScheduledThreadPool ExecutorService.
   * 
   * @param server    The IFugueServer in which this task will run.
   * @param name      The name for the thread pool.
   */
  public FugueScheduledExecutorService(IFugueServer server, String name)
  {
    this(server, Executors.newScheduledThreadPool(0, new NamedThreadFactory(name)));
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    return exec_.schedule(new FugueRunnable(getServer(), command), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
  {
    return exec_.schedule(new FugueCallable<V>(getServer(), callable), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
  {
    return exec_.scheduleAtFixedRate(new FugueRunnable(getServer(), command), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
  {
    return exec_.scheduleWithFixedDelay(new FugueRunnable(getServer(), command), initialDelay, delay, unit);
  }


}
