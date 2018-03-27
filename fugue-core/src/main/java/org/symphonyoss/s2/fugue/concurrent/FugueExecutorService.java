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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.fugue.IFugueServer;

/**
 * An implementation of ExecutorService which handles Faults and RuntimeExceptions the Fugue way.
 * 
 * @author Bruce Skingle
 *
 */
public class FugueExecutorService implements ExecutorService
{
  private final IFugueServer    server_;
  private final ExecutorService exec_;

  /**
   * Create an instance with the given ExecutorService.
   * 
   * @param server    The IFugueServer in which this task will run.
   * @param exec      An ExecutorService to do the actual work.
   */
  public FugueExecutorService(IFugueServer server, ExecutorService exec)
  {
    server_ = server;
    exec_ = exec;
  }

  /**
   * Create an instance with a CachedThreadPool ExecutorService.
   * 
   * @param server    The IFugueServer in which this task will run.
   * @param name      The name for the thread pool.
   */
  public FugueExecutorService(IFugueServer server, String name)
  {
    this(server, newDefaultExecutor(name));
  }

  private static ExecutorService newDefaultExecutor(String name)
  {
    return new ThreadPoolExecutor(5, 20,
        500L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new NamedThreadFactory(name));
  }

  protected IFugueServer getServer()
  {
    return server_;
  }

  @Override
  public void execute(Runnable command)
  {
    exec_.execute(new FugueRunnable(server_, command));
  }

  @Override
  public void shutdown()
  {
    exec_.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow()
  {
    return exec_.shutdownNow();
  }

  @Override
  public boolean isShutdown()
  {
    return exec_.isShutdown();
  }

  @Override
  public boolean isTerminated()
  {
    return exec_.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
  {
    return exec_.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task)
  {
    return exec_.submit(new FugueCallable<T>(server_, task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result)
  {
    return exec_.submit(new FugueRunnable(server_, task), result);
  }

  @Override
  public Future<?> submit(Runnable task)
  {
    return exec_.submit(new FugueRunnable(server_, task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
  {
    return exec_.invokeAll(getFugueTasks(tasks));
  }

  private <T> Collection<FugueCallable<T>> getFugueTasks(Collection<? extends Callable<T>> tasks)
  {
    List<FugueCallable<T>>  fugueTasks = new ArrayList<>(tasks.size());
    
    for(Callable<T> task : tasks)
      fugueTasks.add(new FugueCallable<>(server_, task));
    
    return fugueTasks;
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException
  {
    return exec_.invokeAll(getFugueTasks(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
  {
    return exec_.invokeAny(getFugueTasks(tasks));
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    return exec_.invokeAny(getFugueTasks(tasks), timeout, unit);
  }
}
