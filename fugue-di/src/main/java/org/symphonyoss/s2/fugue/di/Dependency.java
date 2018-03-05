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

package org.symphonyoss.s2.fugue.di;

/* package */ class Dependency<T>
{
  private final Class<T>    requiredInterface_;
  private IBinder<T>        binder_;
  private final Cardinality cardinality_;
  
  public Dependency(Class<T> requiredInterface, IBinder<T> binder, Cardinality cardinality)
  {
    requiredInterface_ = requiredInterface;
    binder_ = binder;
    cardinality_ = cardinality;
  }

  public Class<?> getRequiredInterface()
  {
    return requiredInterface_;
  }

  public Cardinality getCardinality()
  {
    return cardinality_;
  }
  
  /*
   * We are relying on the check performed in the context which is part of our package.
   */
  @SuppressWarnings("unchecked")
  /* package */ void bind(IComponent value)
  {
    binder_.bind((T)value);
  }
  
  @Override
  public String toString()
  {
    return "Dependency on " + requiredInterface_.getSimpleName() + ":" + cardinality_;
  }
}
