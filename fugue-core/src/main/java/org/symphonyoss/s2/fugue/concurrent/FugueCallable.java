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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.IFugueServer;

/**
 * An implementation of Callable which handles Faults and RuntimeExceptions the Fugue way.
 * 
 * @author Bruce Skingle
 * @param <V> the result type of method {@code call}
 *
 */
public class FugueCallable<V> implements Callable<V>
{
  private static Logger      log_ = LoggerFactory.getLogger(FugueRunnable.class);

  private final IFugueServer server_;
  private final Callable<V>  callable_;
  
  /**
   * Create a FugueCallable for the given server and vanilla Callable.
   *  
   * @param server    The IFugueServer in which this task will run.
   * @param callable  The actual task.
   */
  public FugueCallable(IFugueServer server, Callable<V> callable)
  {
    server_ = server;
    callable_ = callable;
  }

  @Override
  public V call()
  {
    try
    {
      return callable_.call();
    }
    catch(ProgramFault e)
    {
      log_.error("Task completed in program fatal error, shutting down...", e);
      server_.fail();
      throw e;
    }
    catch(TransactionFault e)
    {
      log_.error("Task completed with a fault", e);
      throw e;
    }
    catch(RuntimeException e)
    {
      log_.error("Task completed with unexpected runtime error", e);
      throw e;
    }
    catch(Exception e)
    {
      log_.error("Task completed with unexpected checked exception", e);
      throw new TransactionFault(e);
    }
  }
}
