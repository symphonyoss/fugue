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

import java.util.List;

import org.symphonyoss.s2.common.fluent.IFluent;
import org.symphonyoss.s2.fugue.http.IResourceProvider;
import org.symphonyoss.s2.fugue.http.IServletProvider;
import org.symphonyoss.s2.fugue.http.IUrlPathServlet;
import org.symphonyoss.s2.fugue.http.ui.servlet.ICommand;

/**
 * A fluent container of Fugue components.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The concrete type to be returned by fluent methods.
 */
public interface IFugeComponentContainer<T extends IFluent<T>> extends IFluent<T>
{
  /**
   * Add each of the given objects as components.
   * 
   * The container is aware of a number of types of component and treats them appropriately.
   * 
   * @param components A varargs list of components.
   * 
   * @return This (fluent method).
   */
  T withComponents(Object... components);

  /**
   * Register the given component.
   * 
   * The container is aware of a number of types of component and treats them appropriately.
   * 
   * This method returns its parameter so that it can be called from a constructor assignment, e.g:
   * 
   * <code>nameFactory_ = register(new SystemNameFactory(config_));</code>
   * 
   * @param <C>       The type of the component to be registered. 
   * @param component The component to be registered.
   * 
   * @return The component.
   */
  <C> C register(C component);

  /**
   * @return all of the registered components which implement IFugueComponent.
   */
  List<IFugueComponent> getComponents();

  /**
   * @return all of the registered components which implement IFugueLifecycleComponent.
   */
  List<IFugueLifecycleComponent> getLifecycleComponents();

  /**
   * @return all of the registered components which implement IResourceProvider.
   */
  List<IResourceProvider> getResourceProviders();

  /**
   * @return all of the registered components which implement IServletProvider.
   */
  List<IServletProvider> getServletProviders();

  /**
   * @return all of the registered components which implement IUrlPathServlet.
   */
  List<IUrlPathServlet> getServlets();

  /**
   * @return all of the registered components which implement ICommand.
   */
  List<ICommand> getCommands();

  /**
   * Start the container.
   * 
   * Calls start() on all registered components which implement IFugueComponent.
   * 
   * @return This (fluent method).
   */
  T start();

  /**
   * Stop the container.
   * 
   * Calls quiesce() on all registered components which implement IFugueComponent.
   * 
   * @return This (fluent method).
   */
  T quiesce();

  /**
   * Stop the container.
   * 
   * Calls stop() on all registered components which implement IFugueComponent.
   * 
   * @return This (fluent method).
   */
  T stop();

}
