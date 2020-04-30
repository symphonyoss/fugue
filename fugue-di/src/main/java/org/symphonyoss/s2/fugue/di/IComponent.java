/*
 *
 *
 * Copyright 2017-2018 Symphony Communication Services, LLC.
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
 * All Fugue components must implement this interface. 
 * 
 * @author Bruce Skingle
 *
 */
public interface IComponent
{
  /**
   * Create a component descriptor which describes the dependencies and
   * provided interfaces of this component.
   * 
   * This method may create the descriptor and will only be called once.
   * A component sub-class should call super.getComponentDescriptor().
   * ComponentDescriptor has a fluent interface.
   * 
   * @return A ComponentDescriptor for the component.
   */
  ComponentDescriptor getComponentDescriptor();
}
