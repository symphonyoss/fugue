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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.fugue.di.ComponentDescriptor;
import org.symphonyoss.s2.fugue.di.IComponent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationProvider extends BaseConfigurationProvider implements IComponent
{
  private static final Logger log_ = LoggerFactory.getLogger(ConfigurationProvider.class);
  
  public ConfigurationProvider()
  {
    loadConfigSpec(System.getenv("FUGUE_CONFIG"));
  }
  
  /* package */ ConfigurationProvider(String fileName)
  {
    loadConfigSpec(fileName);
  }
  
  private void loadConfigSpec(String fileName)
  {
    if(fileName==null || fileName.trim().length()==0)
      throw new ProgramFault("FUGUE_CONFIG must be set as an environment variable.");
    
    InputStream in = null;
    
    try
    {
      try
      {
        URL configUrl = new URL(fileName);
        
        log_.info("Loading config spec from {}", configUrl);
        
        try
        {
          in = configUrl.openStream();
        }
        catch (IOException e)
        {
          throw new ProgramFault("FUGUE_CONFIG is " + configUrl + " but this URL is not readable", e);
        }
      }
      catch (MalformedURLException e)
      {
        File file = new File(fileName);
        
        if(!file.isFile())
          throw new ProgramFault("FUGUE_CONFIG \"" + fileName + "\" is neither a URL or a valid file name.");
        
        if(!file.canRead())
          throw new ProgramFault("FUGUE_CONFIG \"" + fileName + "\" is an unreadable file.");
        
        log_.info("Loading config spec from file {}", file.getAbsolutePath());
        try
        {
          in = new FileInputStream(file);
        }
        catch (FileNotFoundException e1)
        {
          // We already checked this but....
          throw new ProgramFault("FUGUE_CONFIG \"" + fileName + "\" is neither a URL or a valid file name.", e1);
        }
      }
      
      ObjectMapper mapper = new ObjectMapper();
      
      try
      {
        JsonNode configSpec = mapper.readTree(in);
        
        JsonNode n;
        
        if((n = configSpec.get("url")) != null)
        {
          loadDirectConfig(fileName, n);
        }
        else if((n = configSpec.get("config")) != null)
        {
          setTree(n);
        }
        else
        {
          throw new ProgramFault("FUGUE_CONFIG \"" + fileName + "\" is invalid.");
        }
      }
      catch (IOException e1)
      {
        throw new ProgramFault("Cannot parse config spec from FUGUE_CONFIG \"" + fileName + "\".", e1);
      }
    }
    finally
    {
      if(in != null)
      {
        try
        {
          in.close();
        }
        catch (IOException e)
        {
          log_.error("Failed to close config", e);
        }
      }
    }
  }
  
  private void loadDirectConfig(String fileName, JsonNode urlNode)
  {
    if(!urlNode.isTextual())
      throw new ProgramFault("FUGUE_CONFIG \"" + fileName + "\" has a non-textual url.");

    try
    {
      URL configUrl = new URL(urlNode.asText());
      
      String host = configUrl.getHost();
      
      switch(host)
      {
        case "api.github.com":
          loadFromGitHub(configUrl);
          break;
        
        default:
          // We will assume that the url just returns the raw config data
          loadFromUrl(configUrl);
      }
    }
    catch (MalformedURLException e)
    {
      throw new ProgramFault("FUGUE_CONFIG \"" + fileName + "\" has an invalid url \"" + urlNode + "\"", e);
    }
  }
  
  private void loadFromUrl(URL configUrl)
  {
    try(InputStream in =configUrl.openStream())
    {
      ObjectMapper mapper = new ObjectMapper();
      
      setTree(mapper.readTree(in));
    }
    catch (IOException e)
    {
      throw new ProgramFault("FUGUE_CONFIG is " + configUrl + " but this URL is not readable", e);
    }
  }

  private void loadFromGitHub(URL configUrl)
  {
    try(InputStream in =configUrl.openStream())
    {
      ObjectMapper mapper = new ObjectMapper();
      
      JsonNode tree = mapper.readTree(in);
      
      JsonNode content = tree.get("content");
      
      if(content == null || !content.isTextual())
        throw new RuntimeException("FUGUE_CONFIG is " + configUrl + " but there is no content node in the JSON there");
      
      byte[] bytes = Base64.decodeBase64(content.asText());
      
      setTree(mapper.readTree(bytes));
    }
    catch (IOException e)
    {
      throw new ProgramFault("FUGUE_CONFIG is " + configUrl + " but this URL is not readable", e);
    }
  }

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
        .addProvidedInterface(IConfigurationProvider.class);
  }
}
