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
 * A provider of additional components.
 * 
 * If this interface is provided by any Component then when the context is resolved the registerComponents
 * method will be called so that additional components can be registered into the DI context. 
 * @author Bruce Skingle
 *
 */
public interface IComponentProvider
{
  /**
   * Called at start time, during the resolution process, before any component is started.
   * 
   * @param diContext The DI Context.
   */
  void registerComponents(IDIContext diContext);
}
