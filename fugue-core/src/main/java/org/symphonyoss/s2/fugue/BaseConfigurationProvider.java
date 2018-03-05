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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.exception.NotFoundException;
import org.symphonyoss.s2.common.fault.ProgramFault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BaseConfigurationProvider implements IConfigurationProvider
{
  private static final Logger log_ = LoggerFactory.getLogger(BaseConfigurationProvider.class);
  
  private JsonNode tree_;

  protected BaseConfigurationProvider()
  {}
  
  private BaseConfigurationProvider(JsonNode tree)
  {
    tree_ = tree;
  }

  
  protected void setTree(JsonNode tree)
  {
    tree_ = tree;
  }

  @Override
  public @Nonnull String getProperty(@Nonnull String name) throws NotFoundException
  {
    if(tree_ == null)
      throw new NotFoundException("No configuration loaded");
    
    JsonNode node = tree_.get(name);
    
    if(node == null)
      throw new NotFoundException("No such property");
    
//    if(!node.isTextual())
//      throw new NotFoundException("Not a text value");
    
    return node.asText();
  }

  @Override
  public @Nonnull String getRequiredProperty(@Nonnull String name)
  {
    try
    {
      return getProperty(name);
    }
    catch (NotFoundException e)
    {
      throw new ProgramFault("Required property  \"" + name + "\" not found", e);
    }
  }
  
  @Override
  public boolean getBooleanProperty(@Nonnull String name)
  {
    try
    {
      return "true".equalsIgnoreCase(getProperty(name));
    }
    catch (NotFoundException e)
    {
      return false;
    }
  }
  
  @Override
  public @Nonnull List<String> getArray(@Nonnull String name) throws NotFoundException
  {
    if(tree_ == null)
      throw new NotFoundException("No configuration loaded");
    
    JsonNode node = tree_.get(name);
    
    if(node == null)
      throw new NotFoundException("No such property");
    
    if(!node.isArray())
      throw new NotFoundException("Not an array value");
    
    List<String> result = new ArrayList<>();
    
    for(JsonNode child : node)
    {
      result.add(child.asText());
    }
      
    return result;
  }

  @Override
  public @Nonnull List<String> getRequiredArray(@Nonnull String name)
  {
    try
    {
      return getArray(name);
    }
    catch (NotFoundException e)
    {
      throw new ProgramFault("Required array property  \"" + name + "\" not found", e);
    }
  }

  @Override
  public @Nonnull IConfigurationProvider getConfiguration(String name) throws NotFoundException
  {
    if(tree_ == null)
      throw new NotFoundException("No configuration loaded");
    
    JsonNode node = tree_.get(name);
    
    if(node == null)
      throw new NotFoundException("No such property");
    
    if(!node.isObject())
      throw new NotFoundException("Not an object value");
    
    return new BaseConfigurationProvider((ObjectNode)node);
  }

  @Override
  public @Nonnull IConfigurationProvider getRequiredConfiguration(String name)
  {
    try
    {
      return getConfiguration(name);
    }
    catch (NotFoundException e)
    {
      throw new ProgramFault("Required configuration  \"" + name + "\" not found", e);
    }
  }
}
