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

import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ExecutorBatchTest
{
  private static final Runnable sleep2 = () ->
  {
    try
    {
      Thread.sleep(200);
    }
    catch (InterruptedException e)
    {
      throw new IllegalStateException(e);
    }
  };
  private static final Runnable throwUp = () ->
  {
    throw new IllegalStateException("Barf!");
  };
  private static final Runnable sleep5throwUp = () ->
  {
    try
    {
      Thread.sleep(500);
    }
    catch (InterruptedException e)
    {
      throw new IllegalStateException(e);
    }
    throw new IllegalStateException("Barf!");
  };
  private static final ExecutorService executor_ = Executors.newFixedThreadPool(20);
  
  @Test
  public void testNormal()
  {
    ExecutorBatch batch = new ExecutorBatch(executor_);
    
    for(int i=0 ; i<25 ; i++)
      batch.submit(sleep2);
    
    batch.waitForAllTasks();
  }

  @Test
  public void testFailFast()
  {
    ExecutorBatch batch = new ExecutorBatch(executor_);
    
    for(int i=0 ; i<5 ; i++)
      batch.submit(sleep2);
    
    batch.submit(throwUp);
    
    try
    {
      // just in case we need a to force a context switch
      Thread.sleep(50);
    }
    catch (InterruptedException e)
    {
      throw new IllegalStateException(e);
    }
    
    try
    {
      batch.submit(sleep2);
      
      fail("Submit should throw exception");
    }
    catch(IllegalStateException e)
    {
      // expected
    }

    batch.waitForAllTasks();
  }
  
  @Test
  public void testFailSlow()
  {
    ExecutorBatch batch = new ExecutorBatch(executor_);
    
    for(int i=0 ; i<5 ; i++)
      batch.submit(sleep2);
    
    batch.submit(sleep5throwUp);
    
    batch.submit(sleep2);
    
    try
    {
      // just in case we need a to force a context switch
      Thread.sleep(50);
    }
    catch (InterruptedException e)
    {
      throw new IllegalStateException(e);
    }
    
    try
    {
      batch.waitForAllTasks();
      
      fail("waitForAllTasks should throw exception");
    }
    catch(IllegalStateException e)
    {
      // expected
    }

    
  }

}
