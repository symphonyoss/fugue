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

package org.symphonyoss.s2.fugue.config;

import java.util.List;

import org.symphonyoss.s2.common.exception.NotFoundException;

/**
 * A provider of configuration values.
 * 
 * The slash character is used as a separator to indicate levels of sub-configuration so
 * 
 * <code>getConfiguration("foo").getConfiguration("bar") == getConfiguration("foo/bar")</code>
 * 
 * and
 * 
 * <code>getConfiguration("foo").getRequiredString("bar") == getRequiredString("foo/bar")</code>
 * 
 * etc.
 * 
 * @author Bruce Skingle
 *
 */
public interface IConfiguration
{
  /**
   * Return the given sub-configuration.
   * 
   * @param name The name of a sub-configuration.
   * 
   * @return An IConfiguration which can be used to access the sub-configuration.
   */
  IConfiguration getConfiguration(String name);

  /**
   * Return the value of the given configuration property.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws NotFoundException  If the property is not defined in the current configuration.
   * 
   * @deprecated use getRequiredString(name) or getString(name, defaultValue) - will be deleted after 2018-12-31
   */
  @Deprecated
  String getString(String name) throws NotFoundException;

  /**
   * Return the value of the given configuration property.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * @param defaultValue  The value to be returned if it is absent from the config.
   */
  String getString(String name, String defaultValue);

  /**
   * Return the value of the given configuration property.
   * 
   * This method throws a IllegalStateException if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws IllegalStateException  If the property is not defined in the current configuration.
   */
  String getRequiredString(String name);
  
  /**
   * Return the value of the given configuration property as a long integer.
   * 
   * This method throws a IllegalStateException if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws IllegalStateException  If the property is not defined in the current configuration.
   */
  long getRequiredLong(String name);
  
  /**
   * Return the value of the given configuration property as a long integer.
   * 
   * @param name          The name of the required property.
   * @param defaultValue  The value to be returned if it is absent from the config.
   * @return              The value of the given property name.
   */
  long getLong(String name, long defaultValue);
  
  /**
   * Return the value of the given configuration property as an integer.
   * 
   * This method throws a IllegalStateException if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws IllegalStateException  If the property is not defined in the current configuration.
   */
  int getRequiredInt(String name);
  
  /**
   * Return the value of the given configuration property as an integer.
   * 
   * @param name          The name of the required property.
   * @param defaultValue  The value to be returned if it is absent from the config.
   * @return              The value of the given property name.
   */
  int getInt(String name, int defaultValue);
  
  /**
   * Return the value of the given configuration property as an integer.
   * 
   * @param name          The name of the required property.
   * @param defaultValue  The value to be returned if it is absent from the config.
   * @return              The value of the given property name.
   */
  Integer getInteger(String name, Integer defaultValue);

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
   * @param name          The name of the required property.
   * @param defaultValue  The value to be returned if it is absent from the config.
   * 
   * @return      The value of the given property name.
   * 
   */
  boolean getBoolean(String name, boolean defaultValue);

  /**
   * Return the value of the given configuration property as a boolean.
   * 
   * This method throws a IllegalStateException if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws IllegalStateException  If the property is not defined in the current configuration.
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
   * 
   * @deprecated use getRequiredListOfString(name) or getListOfString(name, defaultValue) - will be deleted after 2018-12-31
   */
  @Deprecated
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
   * This method throws a IllegalStateException if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws IllegalStateException  If the property is not defined in the current configuration.
   * @deprecated use getRequiredListOfString(name) or getListOfString(name, defaultValue) - will be deleted after 2018-12-31
   */
  @Deprecated
  List<String> getRequiredStringArray(String name);

  /**
   * Return the value of the given configuration property as a list of Strings.
   * 
   * The value must be represented as a JSON array.
   * 
   * @param name          The name of the required property.
   * @param defaultValue  The value to be returned if it is absent from the config.
   * @return              The value of the given property name.
   */
  List<String> getListOfString(String name, List<String> defaultValue);

  /**
   * Return the value of the given configuration property as a list of Strings.
   * 
   * 
   * The value must be represented as a JSON array.
   * 
   * This method throws a IllegalStateException if the value does not exist.
   * 
   * @param name  The name of the required property.
   * @return      The value of the given property name.
   * 
   * @throws IllegalStateException  If the property is not defined in the current configuration.
   */
  List<String> getRequiredListOfString(String name);

  /**
   * @return The name of this configuration.
   */
  String getName();
}
