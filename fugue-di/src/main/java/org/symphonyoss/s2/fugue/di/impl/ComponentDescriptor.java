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

package org.symphonyoss.s2.fugue.di.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.symphonyoss.s2.fugue.di.Cardinality;
import org.symphonyoss.s2.fugue.di.IBinder;

public class ComponentDescriptor
{
  private List<Dependency<?>> dependencyList_     = new ArrayList<>();
  private Set<Class<?>>       providedInterfaces_ = new HashSet<>();
  private List<Runnable>      startHandlers_      = new ArrayList<>();
  private List<Runnable>      stopHandlers_       = new ArrayList<>();
 
  /**
   * Add a simple (Cardinality.One) dependency.
   * 
   * Convenience method.
   * 
   * @param requiredInterface The interface of which an implementation is required.
   * @return this - fluent interface.
   */
  public <T> ComponentDescriptor addDependency(Class<T> requiredInterface,
      IBinder<T> binder)
  {
    return addDependency(requiredInterface, binder, Cardinality.one);
  }
  
  /**
   * Add a dependency.
   * 
   * @param requiredInterface The interface of which an implementation is required.
   * @param cardinality       The number of instances which is required.
   * @param binder 
   * @return this - fluent interface.
   */
  public <T> ComponentDescriptor addDependency(Class<T> requiredInterface,
      IBinder<T> binder, 
      Cardinality cardinality)
  {
    dependencyList_.add(new Dependency<T>(requiredInterface, binder, cardinality));
    
    return this;
  }
  
  /**
   * Add an interface which this component implements and which can
   * be provided to other components.
   * 
   * If the class provided does not actually implement this interface
   * then a runtime exception will be thrown from the resolve method of
   * the context.
   * 
   * @param providedInterface An interface which is provided.
   * @return this - fluent interface.
   */
  public ComponentDescriptor addProvidedInterface(Class<?> providedInterface)
  {
    providedInterfaces_.add(providedInterface);
    
    return this;
  }
  
  public ComponentDescriptor  addStart(Runnable handler)
  {
    startHandlers_.add(handler);
    
    return this;
  }
  
  public ComponentDescriptor  addStop(Runnable handler)
  {
    stopHandlers_.add(handler);
    
    return this;
  }

  public List<Dependency<?>> getDependencies()
  {
    return dependencyList_;
  }

  public Set<Class<?>> getProvidedInterfaces()
  {
    return providedInterfaces_;
  }

  public List<Runnable> getStartHandlers()
  {
    return startHandlers_;
  }

  public List<Runnable> getStopHandlers()
  {
    return stopHandlers_;
  }

  public ComponentDescriptor removeProvidedInterface(Class<?> providedInterface)
  {    
    providedInterfaces_.remove(providedInterface);
    
    return this;
  }
}


