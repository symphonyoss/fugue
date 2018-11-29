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

package org.symphonyoss.s2.fugue.counter;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusyCounter implements IBusyCounter
{
  private static final Logger               log_      = LoggerFactory.getLogger(BusyCounter.class);
  
  private AtomicInteger busyCycles_ = new AtomicInteger();
  private AtomicInteger idleCycles_ = new AtomicInteger();
  private int busyLimit_ = 5;
  private int idleLimit_ = 2;
  private long coolDown_ = 20000;
  private long lastEvent_;
  
  @Override
  public boolean busy()
  {
    idleCycles_.set(0);
    int busyCnt = busyCycles_.incrementAndGet();
    
    System.err.println("Busy " + busyCnt);
    
    if(busyCnt >= busyLimit_)
      return doScaleUp();
    
    return false;
  }

  @Override
  public boolean idle()
  {
    busyCycles_.set(0);
    int idleCnt = idleCycles_.incrementAndGet();
    
    System.err.println("Idle " + idleCnt);
    
    if(idleCnt >= idleLimit_)
      return doScaleDown();
    
    return false;
  }

  protected boolean doScaleUp()
  {
    if(checkCoolDown())
    {
      log_.info("Scale UP");
      return scaleUp();
    }
    return false;
  }

  protected boolean scaleUp()
  {
    return false; 
  }

  protected boolean doScaleDown()
  {
    if(checkCoolDown())
    {
      log_.info("Scale DOWN");
      return scaleDown();
    }
    return false;
  }

  protected boolean scaleDown()
  {
    return false;
  }

  private synchronized boolean checkCoolDown()
  {
    long now = System.currentTimeMillis();
    
    if(now - lastEvent_ > coolDown_)
    {
      lastEvent_ = now;
      idleCycles_.set(0);
      busyCycles_.set(0);
      
      return true;
    }
    return false;
  }

}
