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

import java.util.HashSet;
import java.util.Set;

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
  Running(false, Stopping),
  
  /** Transitioning into Running */
  Starting(false),
  
  /** Stopped. */
  Stopped(false, Starting),
  
  /** Initializing, not yet started. */
  Initializing(true, Starting)
  ;
  
  private final boolean configurable_;
  private final Set<FugueLifecycleState> allowedTransitions_ = new HashSet<>();

  private FugueLifecycleState(boolean configurable, FugueLifecycleState ...allowedTransitions)
  {
    configurable_ = configurable;
    
    for(FugueLifecycleState state : allowedTransitions)
      allowedTransitions_.add(state);
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
  
  public boolean canTransaitionTo(FugueLifecycleState other)
  {
    return allowedTransitions_.contains(other);
  }
}
