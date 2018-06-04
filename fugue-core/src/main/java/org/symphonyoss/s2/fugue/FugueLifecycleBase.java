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

import org.symphonyoss.s2.common.fluent.Fluent;
import org.symphonyoss.s2.common.fluent.IFluent;

abstract class FugueLifecycleBase<T extends IFluent<T>> extends Fluent<T>
{
  private FugueLifecycleState lifecycleState_ = FugueLifecycleState.Initializing;
  
  FugueLifecycleBase(Class<T> type)
  {
    super(type);
  }

  /**
   * Constructor.
   * 
   * @param type The concrete type returned by fluent methods.
   */
  protected void assertConfigurable()
  {
    if(!lifecycleState_.isConfigurable())
      throw new IllegalStateException("Component has started, it is too late to make configuration changes.");
  }

  protected void setLifeCycleState(FugueLifecycleState state)
  {
    lifecycleState_ = state;
  }

  public FugueLifecycleState getLifecycleState()
  {
    return lifecycleState_;
  }
}
