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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fugue components are required to provide a ComponentDescriptor which specifies what
 * dependencies it has and what interfaces it provides.
 * 
 * @author Bruce Skingle
 *
 */
public class ComponentDescriptor
{
  private List<Dependency<?>>                 dependencyList_     = new ArrayList<>();
  private Map<Class<?>, ProvidedInterface<?>> providedInterfaces_ = new HashMap<>();
  private List<Runnable>                      startHandlers_      = new ArrayList<>();
  private List<Runnable>                      stopHandlers_       = new ArrayList<>();
 
  /**
   * Add a simple (Cardinality.One) dependency.
   * 
   * Convenience method.
   * 
   * @param requiredInterface The interface of which an implementation is required.
   * @param binder            The binder to be called with the injected value.
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
   * @param binder            The binder to be called with the injected value.
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
    providedInterfaces_.put(providedInterface, new ProvidedInterface<>(providedInterface, null));
    
    return this;
  }
  
  /**
   * Add an interface which this component provided indirectly and which can
   * be provided to other components.
   * 
   * @param providedInterface An interface which is provided.
   * @param provider          A provider of the interface, usually a lambda
   * @return this - fluent interface.
   */
  public <T> ComponentDescriptor addProvidedInterface(Class<T> providedInterface, IProvider<T> provider)
  {
    providedInterfaces_.put(providedInterface, new ProvidedInterface<>(providedInterface, provider));
    
    return this;
  }
  
  /**
   * Add a Runnable which will be called during process initialisation.
   * 
   * @param handler A Runnable to be called at start time.
   * 
   * @return This (fluent method)
   */
  public ComponentDescriptor  addStart(Runnable handler)
  {
    startHandlers_.add(handler);
    
    return this;
  }
  
  /**
   * Add a Runnable which will be called during process termination.
   * 
   * @param handler A Runnable to be called at shutdown time.
   * 
   * @return This (fluent method)
   */
  public ComponentDescriptor  addStop(Runnable handler)
  {
    stopHandlers_.add(handler);
    
    return this;
  }

  /* package */ List<Dependency<?>> getDependencies()
  {
    return dependencyList_;
  }

  /* package */ Collection<ProvidedInterface<?>> getProvidedInterfaces()
  {
    return providedInterfaces_.values();
  }

  
  /* package */ List<Runnable> getStartHandlers()
  {
    return startHandlers_;
  }

  /* package */ List<Runnable> getStopHandlers()
  {
    return stopHandlers_;
  }
}


