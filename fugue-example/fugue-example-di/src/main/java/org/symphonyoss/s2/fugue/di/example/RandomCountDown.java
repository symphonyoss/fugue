/*
 *
 *
 * Copyright 2017 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.di.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.symphonyoss.s2.fugue.di.IComponent;
import org.symphonyoss.s2.fugue.di.component.ILogComponent;
import org.symphonyoss.s2.fugue.di.component.IRandomNumberProvider;
import org.symphonyoss.s2.fugue.di.impl.ComponentDescriptor;
import org.symphonyoss.s2.fugue.di.impl.DIContext;

public class RandomCountDown implements IComponent
{
  private DIContext context_;
  private ILogComponent log_;
  private IRandomNumberProvider random_;

  public RandomCountDown(DIContext context)
  {
    context_ = context;
  }

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
        .addDependency(IRandomNumberProvider.class, (v) -> random_ = v)
        .addDependency(ILogComponent.class,         (v) -> log_ = v)
        .addStart(() -> start())
        .addStop(() -> stop());
  }
  
  public void start()
  {
    log_.info("RandomCountDown started.");
    
    ExecutorService exec = Executors.newSingleThreadExecutor();
    
    exec.submit(() ->
    {
      int count = random_.nextInt(10);
      
      while(count-- > 0)
      {
        log_.info("Count " + count);
        try
        {
          Thread.sleep(1000);
        } catch (InterruptedException e)
        {
          log_.error("Interrupted", e);
        }
      }
      
      context_.stop();
      exec.shutdown();
    });
  }

  public void stop()
  {
    log_.info("RandomCountDown stopped.");
  }

}
