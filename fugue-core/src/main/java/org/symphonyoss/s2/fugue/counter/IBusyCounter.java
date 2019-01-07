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

/**
 * A counter for busy/idle cycles.
 * 
 * @author Bruce Skingle
 *
 */
public interface IBusyCounter
{
  /**
   * Indicates how busy the caller is for a single cycle.
   * 
   * @param count The number of work units done in the current cycle.
   * @return The action the caller should take, this may be DoNothing if action has already been taken.
   */
  ScaleAction busy(int count);
  
//  /**
//   * Indicates that the caller is busy.
//   * 
//   * This method would usually be called when the calling process has work to do without waiting or when there is a backlog.
//   * 
//   * In many cases the return value will always be false because scaling causes other instances to start or stop.
//   * 
//   * @return true iff the caller should scale up.
//   */
//  boolean busy();
//  
//  /**
//   * Indicates that the caller is idle.
//   * 
//   * This method would usually be called when the calling process has no work to do of had to do a blocking request for work.
//   * 
//   * In many cases the return value will always be false because scaling causes other instances to start or stop.
//   * 
//   * @return true iff the caller should scale down.
//   */
//  boolean  idle();
}
