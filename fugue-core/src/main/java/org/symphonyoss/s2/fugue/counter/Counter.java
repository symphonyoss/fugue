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
import org.symphonyoss.s2.common.fluent.Fluent;
import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.metrics.IMetricManager;

/**
 * An implementation of ICounter with lower and upper limit checking.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of the concrete subclass for fluent methods.
 */
public abstract class Counter<T extends Counter<T>> extends Fluent<T> implements Runnable, ICounter, IFugueComponent
{
  private static final long                 PERIOD    = 5;
  private static final Logger               log_      = LoggerFactory.getLogger(Counter.class);
  private static ScheduledExecutorService   executor_ = Executors
      .newSingleThreadScheduledExecutor(new NamedThreadFactory("counter", true));

  protected boolean  debug_;
  
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
  private IMetricManager metricManager_;
  private long currentMinute_;
  private int currentTotal_;
  
  /**
   * Constructor.
   * 
   * @param type                  The type of the concrete subclass for fluent methods.
   * @param upperLimit            The upper limit of the counter for a single period.
   * @param upperSampleCntWindow  The number of periods to consider for upper limit processing.
   * @param upperSampleCntBreak   The number of periods in the sample window which need to be above the limit for the limit to be considered broken.
   * @param upperCoolDown         The number of periods after a limit break before another break will be processed.
   * @param lowerLimit            The lower limit of the counter for a single period.
   * @param lowerSampleCntWindow  The number of periods to consider for lower limit processing.
   * @param lowerSampleCntBreak   The number of periods in the sample window which need to be below the limit for the limit to be considered broken.
   * @param lowerCoolDown         The number of periods after a limit break before another break will be processed.
   */
  public Counter(Class<T> type, int upperLimit, int upperSampleCntWindow, int upperSampleCntBreak, int upperCoolDown, int lowerLimit,
      int lowerSampleCntWindow, int lowerSampleCntBreak, int lowerCoolDown)
  {
    super(type);
    
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
    
    
  }
  
  /**
   * Agregate counts and pass to the given metric manager.
   * 
   * @param metricManager A metric manager.
   * 
   * @return This (fluent method)
   */
  public T withMetricManager(IMetricManager metricManager)
  {
    metricManager_ = metricManager;
    
    return self();
  }

  private long getCurrentSecond()
  {
    return System.currentTimeMillis() / 1000;
  }

  /**
   * 
   * @return The upper limit.
   */
  public int getUpperLimit()
  {
    return upperLimit_;
  }

  /**
   * Set the upper limit.
   * 
   * @param upperLimit The new upper limit.
   * 
   * @return This (fluent method)
   */
  public T withUpperLimit(int upperLimit)
  {
    upperLimit_ = upperLimit;
    
    return self();
  }

  /**
   * 
   * @return The lower limit.
   */
  public int getLowerLimit()
  {
    return lowerLimit_;
  }

  /**
   * Set the lower limit.
   * 
   * @param lowerLimit The new lower limit.
   * 
   * @return This (fluent method)
   */
  public T withLowerLimit(int lowerLimit)
  {
    lowerLimit_ = lowerLimit;
    
    return self();
  }

  @Override
  public T increment(int count)
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
    
    return self();
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
    try
    {
      CounterValue value;
      
      while((value = queue_.poll()) != null)
        process(value.timestamp_, value.count_);
    }
    catch(Exception e)
    {
      log_.error("Accumulator failed", e);
      e.printStackTrace();
    }
  }

  protected void process(long timestamp, int count)
  {
    if(debug_)
      System.out.println("> " + new Date(timestamp * 1000) + " " + count);
    
    if(metricManager_ != null)
      accumulate(timestamp, count);
    
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

  private void accumulate(long timestamp, int count)
  {
    long minute = timestamp / 60;
    
    if(currentMinute_ == minute)
    {
      currentTotal_ += count;
    }
    else
    {
      if(currentMinute_ > 0)
      {
        metricManager_.putMetric(currentMinute_ * 60000, currentTotal_);
      }
      
      currentMinute_ = minute;
      currentTotal_ = count;
    }
  }

  protected abstract void lowerLimitBreak(int max);

  protected abstract void upperLimitBreak(int max);

  @Override
  public void start()
  {
    baseTime_ = getCurrentSecond();
    accumulateTime_ = baseTime_;
    
    executor_.scheduleAtFixedRate(this, PERIOD, 1, TimeUnit.SECONDS);

    upperLimitValidTime_ = baseTime_ + upperCoolDown_ + upperSampleCntWindow_;
    lowerLimitValidTime_ = baseTime_ + lowerCoolDown_ + lowerSampleCntWindow_;
  }

  @Override
  public void stop()
  {
    if(currentTotal_ > 0)
    {
      metricManager_.putMetric(currentMinute_ * 60000, currentTotal_);
    }
  }

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
