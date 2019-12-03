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

package org.symphonyoss.s2.fugue.inmemory;

import java.io.IOException;

import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.config.Configuration;
import org.symphonyoss.s2.fugue.config.IConfigurationFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of IConfiguration which reads a JSON document from a file.
 * 
 * @author Bruce Skingle
 *
 */
public class InMemoryConfiguration extends Configuration
{
  /** Public factory */
  public static final IConfigurationFactory FACTORY = new IConfigurationFactory()
  {
    @Override
    public InMemoryConfiguration newInstance()
    {
      return new InMemoryConfiguration();
    }

  };
  
  private InMemoryConfiguration()
  {
    this(new InMemoryConfig());
    
  }
  
  private InMemoryConfiguration(InMemoryConfig fileConfig)
  {
    super(fileConfig.tree_);
    setName(fileConfig.name_);
  }

  private static class InMemoryConfig
  {
    private JsonNode tree_;
    private String   name_ = "InMemoryConfig";

    private InMemoryConfig()
    {
      String fugueConfig = Fugue.getProperty(Fugue.FUGUE_CONFIG);
  
      if(fugueConfig == null)
        fugueConfig = "{\"id\":{"
            + "\"environmentId\":\"s2smoke2\","
            + "\"environmentType\":\"dev\","
            + "\"regionId\":\"us-east-1\","
            + "\"serviceId\":\"inmemory\"}}";
      
      loadConfig(fugueConfig);
    }
    
    private void loadConfig(String directConfig)
    {
      ObjectMapper mapper = new ObjectMapper();
      
      try
      {
        JsonNode configSpec = mapper.readTree(directConfig);
        
        tree_ = configSpec;
      }
      catch (IOException e1)
      {
        throw new ProgramFault("Cannot parse config.", e1);
      }
    }
  }
}
