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
   * Return the given sub-configuration.
   * 
   * @param name The name of a sub-configuration.
   * 
   * @return An IConfigurationProvider which can be used to access the sub-configuration.
   */
  IConfigurationProvider getConfiguration(String name);

  /**
   * Return the value of the given configuration property.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws NotFoundException  If the property is not defined in the current configuration.
   */
  String getString(String name) throws NotFoundException;

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
  String getRequiredString(String name);

  /**
   * Return the value of the given configuration property as a boolean.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws NotFoundException  If the property is not defined in the current configuration.
   */
  boolean getBoolean(String name) throws NotFoundException;

  /**
   * Return the value of the given configuration property as a boolean.
   * 
   * This method throws a ProgramFault if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws ProgramFault  If the property is not defined in the current configuration.
   */
  boolean getRequiredBoolean(String name);

  /**
   * Return the value of the given configuration property as a list of Strings.
   * 
   * This is a poor man's implementation since the underlying IConfigurationStore does not
   * provide access to the actual structure of the config.
   * 
   * I would have preferred for the list to be represented as a JSON array, but in fact
   * it must be a string containing comma separated values.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws NotFoundException  If the property is not defined in the current configuration.
   */
  List<String> getStringArray(String name) throws NotFoundException;

  /**
   * Return the value of the given configuration property as a list of Strings.
   * 
   * This is a poor man's implementation since the underlying IConfigurationStore does not
   * provide access to the actual structure of the config.
   * 
   * I would have preferred for the list to be represented as a JSON array, but in fact
   * it must be a string containing comma separated values.
   * 
   * This method throws a ProgramFault if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws ProgramFault  If the property is not defined in the current configuration.
   */
  List<String> getRequiredStringArray(String name);
}
