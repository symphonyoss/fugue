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

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;

public abstract class Counter implements Runnable
{
  private static final long                 PERIOD    = 5;
  private static final Logger               log_      = LoggerFactory.getLogger(Counter.class);

  public boolean  debug_;
  
  private int                               upperLimit_;
  private final int                         upperSampleCntWindow_;
  private int                               upperSampleCntBreak_;
  private int                               upperCoolDown_;
  private int                               lowerLimit_;
  private final int                         lowerSampleCntWindow_;
  private int                               lowerSampleCntBreak_;
  private int                               lowerCoolDown_;

  private int[]                             upperSampleBuffer_;
  private int                               upperSampleIndex_;
  private long                              upperLimitValidTime_;
  private int[]                             lowerSampleBuffer_;
  private int                               lowerSampleIndex_;
  private long                              lowerLimitValidTime_;

  private long                              now_;
  private long                              baseTime_;
  private long                              accumulateTime_;
  private boolean                           secondHalf_;
  private AtomicInteger[]                   counter_;
  private AtomicInteger                     nowCounter_;
  private LinkedBlockingQueue<CounterValue> queue_    = new LinkedBlockingQueue<>();
  private ScheduledExecutorService          executor_ = Executors
      .newSingleThreadScheduledExecutor(new NamedThreadFactory("counter", true));

  public Counter(int upperLimit, int upperSampleCntWindow, int upperSampleCntBreak, int upperCoolDown, int lowerLimit,
      int lowerSampleCntWindow, int lowerSampleCntBreak, int lowerCoolDown)
  {
    if(upperLimit <= lowerLimit)
      throw new IllegalArgumentException("Upper limit must be greater than lower limit");
    
    upperLimit_ = upperLimit;
    upperSampleCntWindow_ = upperSampleCntWindow;
    upperSampleCntBreak_ = upperSampleCntBreak;
    upperCoolDown_ = upperCoolDown;
    lowerLimit_ = lowerLimit;
    lowerSampleCntWindow_ = lowerSampleCntWindow;
    lowerSampleCntBreak_ = lowerSampleCntBreak;
    lowerCoolDown_ = lowerCoolDown;

    upperSampleBuffer_ = new int[upperSampleCntWindow];
    lowerSampleBuffer_ = new int[lowerSampleCntWindow];
    
    counter_ = new AtomicInteger[60];
    
    for(int i=0 ; i<counter_.length ; i++)
      counter_[i] = new AtomicInteger();
    
    baseTime_ = getCurrentSecond();
    accumulateTime_ = baseTime_;
    
    executor_.scheduleAtFixedRate(this, PERIOD, 1, TimeUnit.SECONDS);

    upperLimitValidTime_ = baseTime_ + upperCoolDown_ + upperSampleCntWindow_;
    lowerLimitValidTime_ = baseTime_ + lowerCoolDown_ + lowerSampleCntWindow_;
  }

  private long getCurrentSecond()
  {
    return System.currentTimeMillis() / 1000;
  }

  public int getUpperLimit()
  {
    return upperLimit_;
  }

  public void setUpperLimit(int upperLimit)
  {
    upperLimit_ = upperLimit;
  }

  public int getLowerLimit()
  {
    return lowerLimit_;
  }

  public void setLowerLimit(int lowerLimit)
  {
    lowerLimit_ = lowerLimit;
  }

  public void increment(int count)
  {
    AtomicInteger counter;
    
    synchronized(this)
    {
      long now = getCurrentSecond();
      
      if(now == now_)
      {
        counter = nowCounter_;
      }
      else
      {
        int index = getCounterIndex(now);
        
        if(index == -1)
          counter = null;
        else
          counter = counter_[index];
        
        nowCounter_ = counter;
        now_ = now;
        
        if(debug_)
        {
          System.out.println();
          System.out.println("now_            =" + new Date(now_ * 1000));
          System.out.println("index           =" + index);
        }
      }
    }
    
    if(counter != null)
    {
      counter.addAndGet(count);
    }
  }

  private synchronized int getCounterIndex(long now)
  {
    int index = (int)(now - baseTime_);
    
    if(secondHalf_)
    {
      if(index < -30 || index > 30)
      {
        // too far ahead
        return -1;
      }
      else if(index < 0)
        return index + 60;
      else
        return index;
    }
    else if(index < 0 || index > 60)
    {
      // too far ahead
      return -1;
    }
    else
      return index;
  }
  
  static class CounterValue
  {
    long  timestamp_;
    int   count_;
    
    public CounterValue(long timestamp, int count)
    {
      super();
      timestamp_ = timestamp;
      count_ = count;
    }
  }
  
  
  @Override
  public void run()
  {
    try
    {
      synchronized(this)
      {
        int startOffset = (int)(accumulateTime_ - baseTime_);
        
        if(debug_)
        {
          System.out.println();
          System.out.println("startOffset     =" + startOffset);
          System.out.println("baseTime_       =" + new Date(baseTime_ * 1000));
          System.out.println("accumulateTime_ =" + new Date(accumulateTime_ * 1000));
        }
        
        if(secondHalf_ && startOffset >= 0)
        {
          if(debug_)
          {
            System.out.println("change to first half");
          }
          secondHalf_ = false;
        }
        
        long now      = getCurrentSecond();
        long baseTime = secondHalf_ ? baseTime_ - 60 : baseTime_; 
        int start     = (secondHalf_ ? 60 : 0) + startOffset;
        int limit     = (secondHalf_ ? 60 : 0) + (int)(now - baseTime_ - PERIOD);
        
        if(debug_)
        {
          System.out.println("now             =" + new Date(now * 1000));
          System.out.println("start           =" + start);
          System.out.println("limit           =" + limit);
        }
        
        if(start < 0 || limit >= 60)
        {
          // We have fallen behind....
          for(int i=0 ; i<60 ; i++)
            counter_[i].set(0);
          
          accumulateTime_ = baseTime_ = getCurrentSecond();
          secondHalf_ = false;
          return;
        }
        
        for(int i = start ; i < limit ; i++)
        {
          if(debug_)
          {
            System.out.println("i=" + i + " date=" + new Date((baseTime + i) * 1000) + " counter=" + counter_[i]);
          }
          
          queue_.add(new CounterValue((baseTime + i), counter_[i].getAndSet(0)));

          accumulateTime_ = baseTime + i + 1;
        }
        
        if(limit>29 && !secondHalf_)
        {
          if(debug_)
          {
            System.out.println("change to second half");
          }
          
          secondHalf_ = true;
          baseTime_ += 60;
        }
      }
      executor_.submit(() -> process());
    }
    catch(Exception e)
    {
      log_.error("Accumulator failed", e);
      e.printStackTrace();
    }
  }
  
  private void process()
  {
    CounterValue value;
    
    while((value = queue_.poll()) != null)
      process(value.timestamp_, value.count_);
  }

  protected void process(long timestamp, int count)
  {
    if(debug_)
      System.out.println("> " + new Date(timestamp * 1000) + " " + count);
    
    upperSampleBuffer_[upperSampleIndex_++] = count;
    if(upperSampleIndex_ >= upperSampleCntWindow_)
      upperSampleIndex_ = 0;
    
    lowerSampleBuffer_[lowerSampleIndex_++] = count;
    if(lowerSampleIndex_ >= lowerSampleCntWindow_)
      lowerSampleIndex_ = 0;
    
    if(timestamp > upperLimitValidTime_)
    {
      int cnt = 0;
      int max = 0;
      
      for(int i=0 ; i<upperSampleCntWindow_ ; i++)
      {
        int v = upperSampleBuffer_[i];
        
        if(v > upperLimit_)
          cnt++;
        
        if(v > max)
          max = v;
      }
      
      if(debug_)
        System.err.println("upper break=" + cnt + " windowBreak=" + upperSampleCntBreak_ + " window=" + upperSampleCntWindow_);
      
      if(cnt >= upperSampleCntBreak_)
      {
        upperLimitValidTime_ = timestamp + upperCoolDown_;
        upperLimitBreak(max);
      }
    }
    

    
    if(timestamp > lowerLimitValidTime_)
    {
      int cnt = 0;
      int max = 0;
      
      for(int i=0 ; i<lowerSampleCntWindow_ ; i++)
      {
        int v = lowerSampleBuffer_[i];
        
        if(v < lowerLimit_)
          cnt++;
        
        if(v > max)
          max = v;
      }

      if(debug_)
        System.err.println("lower break=" + cnt + " windowBreak=" + lowerSampleCntBreak_ + " window=" + lowerSampleCntWindow_);
      
      if(cnt >= lowerSampleCntBreak_)
      {
        lowerLimitValidTime_ = timestamp + lowerCoolDown_;
        lowerLimitBreak(max);
      }
    }
  }

  protected abstract void lowerLimitBreak(int max);

  protected abstract void upperLimitBreak(int max);

//  public static void main(String[] argv) throws InterruptedException
//  {
//    Counter counter = new Counter();
//    
//    while(true)
//    {
//      counter.increment(1);
////      System.out.print("+");
//      Thread.sleep(1);
//    }
//  }
}
