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

/**
 * A Dependency Injection Context.
 * 
 * @author Bruce Skingle
 *
 */
public interface IDIContext
{
  /**
   * Stop the context.
   */
  void stop();

  /**
   * Register the given component in the context.
   * 
   * @param component A component.
   * @return The context (fluent interface).
   */
  IDIContext register(IComponent component);

  /**
   * Resolve all components and start.
   * 
   * May only be called once.
   */
  void resolveAndStart();
  
  /**
   * Resolve all components.
   * 
   * May only be called once.
   */
  void resolve();
  
  /**
   * Start all components.
   * 
   * May only be called once, after the context is resolved.
   */
  
  void start();

  /**
   * Get the current lifeCycle state.
   * 
   * @return the current lifeCycle state.
   */
  DIContextState getLifeCycle();

  /**
   * Wait for the specified lifeCycle state to be reached.
   * 
   * @param lifeCycle The state to wait for.
   * @throws InterruptedException If the call is interrupted.
   */
  void waitForLifeCycle(DIContextState lifeCycle) throws InterruptedException;

  /**
   * Wait for the context to become stopped.
   * 
   * In a simple app where the main thread wants to block until the app is
   * complete, this is the method to call.
   * 
   * @throws InterruptedException If the call is interrupted.
   */
  void join() throws InterruptedException;

  /**
   * Resolve an additional component.
   * 
   * This method allows a new component to be resolved after the context
   * has been resolved and started. The new component cannot provide any
   * interfaces to other components and will not be visible to running
   * components, or components subsequently added by a further call to
   * this method. Dependencies of the given component will be resolved
   * and injected if possible.
   * 
   * @param component A component to be resolved.
   * @return True if all dependencies of the new component were resolved.
   */
  boolean resolveAdditionalComponent(IComponent component);
}
