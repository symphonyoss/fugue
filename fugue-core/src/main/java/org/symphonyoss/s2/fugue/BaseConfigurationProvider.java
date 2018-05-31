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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.common.exception.NotFoundException;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.fault.ProgramFault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BaseConfigurationProvider implements IConfigurationProvider
{
  private JsonNode tree_;
  private Map<String, BaseConfigurationProvider> subConfigMap_ = new HashMap<>();
  private String name_ = "";

  protected BaseConfigurationProvider()
  {}
  
  BaseConfigurationProvider(JsonNode tree)
  {
    tree_ = tree;
  }

  
  protected void setTree(JsonNode tree)
  {
    tree_ = tree;
  }

  @Override
  public @Nonnull String getString(@Nonnull String name) throws NotFoundException
  {
    if(tree_ == null)
      throw new NotFoundException("No configuration loaded");
    
    return getString(name.split("/"), 0);
  }

  private @Nonnull String getString(String[] names, int index) throws NotFoundException
  {
    if(index < names.length - 1)
    {
      return getConfiguration(names[index]).getString(names, index+1);
    }
    
    JsonNode node = tree_.get(names[index]);
    
    if(node == null)
      throw new NotFoundException("No such property");
    
//    if(!node.isTextual())
//      throw new NotFoundException("Not a text value");
    
    return node.asText();
  }

  @Override
  public @Nonnull String getRequiredString(@Nonnull String name)
  {
    try
    {
      return getString(name);
    }
    catch (NotFoundException e)
    {
      throw new ProgramFault("Required property  \"" + name + "\" not found in " + name_, e);
    }
  }
  
  @Override
  public boolean getBoolean(@Nonnull String name)
  {
    try
    {
      return "true".equalsIgnoreCase(getString(name));
    }
    catch (NotFoundException e)
    {
      return false;
    }
  }
  
  @Override
  public boolean getRequiredBoolean(String name)
  {
    try
    {
      return "true".equalsIgnoreCase(getString(name));
    }
    catch (NotFoundException e)
    {
      throw new ProgramFault(e);
    }
  }
  
  @Override
  public @Nonnull List<String> getStringArray(@Nonnull String name) throws NotFoundException
  {
    if(tree_ == null)
      throw new NotFoundException("No configuration loaded");
    
    JsonNode node = tree_.get(name);
    
    if(node == null)
      throw new NotFoundException("No such property");
    
    List<String> result = new ArrayList<>();
    
    if(node.isArray())
    {
      for(JsonNode child : node)
      {
        result.add(child.asText());
      }
    }
    else
    {
      for(String s : node.asText().split(" *, *"))
      {
        result.add(s);
      }
    }
      
    return result;
  }

  @Override
  public @Nonnull List<String> getRequiredStringArray(@Nonnull String name)
  {
    try
    {
      return getStringArray(name);
    }
    catch (NotFoundException e)
    {
      throw new ProgramFault("Required array property  \"" + name + "\" not found", e);
    }
  }

  @Override
  public synchronized @Nonnull BaseConfigurationProvider getConfiguration(String name)
  {
    if(tree_ == null)
      throw new IllegalStateException("No configuration loaded");
    
    BaseConfigurationProvider subConfig = subConfigMap_.get(name);
    
    if(subConfig == null)
    {
      JsonNode node = tree_.get(name);
      
      if(node == null || !node.isObject())
      {
        try
        {
          node = new ObjectMapper().readTree("{}");
        }
        catch (IOException e)
        {
          throw new CodingFault(e);
        }
      }
      
      subConfig = new BaseConfigurationProvider((ObjectNode)node);
      
      subConfigMap_.put(name, subConfig);
    }
    
    subConfig.name_ = name_ + "/" + name;
    
    return subConfig;
  }

  public String getName()
  {
    return name_;
  }

  protected void setName(String name)
  {
    name_ = name;
  }
}
