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

import org.symphonyoss.s2.common.exception.NotFoundException;
import org.symphonyoss.s2.common.fault.ProgramFault;

public interface IConfigurationProvider
{
  /**
   * Return the value of the given configuration property.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws NotFoundException  If the property is not defined in the current configuration.
   */
  String  getProperty(String name) throws NotFoundException;
  
  /**
   * Return the value of the given configuration property.
   * 
   * This method throws a ProgramFault if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws ProgramFault  If the property is not defined in the current configuration.
   */
  String  getRequiredProperty(String name);

  List<String> getArray(String name) throws NotFoundException;

  List<String> getRequiredArray(String name);
  
  IConfigurationProvider  getConfiguration(String name) throws NotFoundException;
  
  IConfigurationProvider  getRequiredConfiguration(String name);

  boolean getBooleanProperty(String name);
}
