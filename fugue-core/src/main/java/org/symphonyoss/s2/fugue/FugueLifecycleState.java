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

package org.symphonyoss.s2.fugue;

/**
 * The life cycle state of a Fugue server.
 * 
 * @author Bruce Skingle
 *
 */
public enum FugueLifecycleState
{
  /** Failed. */
  Failed(false),
  
  /** Transitioning into Stopped */
  Stopping(false),
  
  /** Starting */
  Running(false),
  
  /** Transitioning into Running */
  Starting(false),
  
  /** Draining */
  Quiescent(false),
  

  /** Transitioning into Quiescent */
  Quiescing(false),
  
  /** Stopped. */
  Stopped(false),
  
  /** Initialising, not yet started. */
  Initializing(true)
  ;
  
  private final boolean configurable_;

  private FugueLifecycleState(boolean configurable)
  {
    configurable_ = configurable;
  }

  /**
   * Return true if configuration changes may be made (i.e. we have not started yet)
   * 
   * @return true if configuration changes may be made (i.e. we have not started yet)
   */
  public boolean isConfigurable()
  {
    return configurable_;
  }
}
