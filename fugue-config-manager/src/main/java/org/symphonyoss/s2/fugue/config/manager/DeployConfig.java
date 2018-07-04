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

package org.symphonyoss.s2.fugue.config.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.symphonyoss.s2.common.dom.IStringProvider;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.fugue.cmd.CommandLineHandler;

public abstract class DeployConfig extends CommandLineHandler
{
  public static final String CONFIG          = "config";
  public static final String ENVIRONMENT     = "environment";
  public static final String REALM           = "realm";
  public static final String REGION          = "region";
  public static final String SERVICE         = "service";
  public static final String DOT_JSON        = ".json";

  private static final String CONFIG_DIR      = CONFIG + "/";
  private static final String SERVICE_DIR     = CONFIG_DIR + SERVICE;
  private static final String DEFAULTS = "defaults";

  private String service_;
  private String environment_;
  private String realm_;
  private String region_ = "default";
  private String target_ = "-";
  
  public DeployConfig()
  {
    withFlag('s', "service",      String.class, false, true, (v) -> service_ = v);
    withFlag('e', "environment",  String.class, false, true, (v) -> environment_ = v);
    withFlag('r', "realm",        String.class, false, true, (v) -> realm_ = v);
    withFlag('g', "region",       String.class, false, false, (v) -> region_ = v);
    withFlag('t', "target",       String.class, false, false, (v) -> target_ = v);
  }
  
  public DeployConfig(DeployConfig master)
  {
    service_ = master.service_;
    environment_ = master.environment_;
    realm_ = master.realm_;
    region_ = master.region_;
  }

  public String getService()
  {
    return service_;
  }

  public void setService(String service)
  {
    service_ = service;
  }

  public String getEnvironment()
  {
    return environment_;
  }

  public void setEnvironment(String environment)
  {
    environment_ = environment;
  }

  public String getRealm()
  {
    return realm_;
  }

  public void setRealm(String realm)
  {
    realm_ = realm;
  }

  public String getRegion()
  {
    return region_;
  }

  public void setRegion(String region)
  {
    region_ = region;
  }

  public abstract MutableJsonObject fetchConfig(String folderName, String fileName) throws IOException;

  public void deployEnvironment() throws IOException
  {
//    try
//    {
      MutableJsonDom    dom   = new MutableJsonDom();
      
      dom.add(fetchService());
      
      
      
      processResult(target_, dom.immutify());
//    }
//    catch(FileNotFoundException e)
//    {
//      System.err.println("Unknown service \"" + service_ + "\"");
//    }
  }
  
  public void processResult(String target, ImmutableJsonDom dom)
  {
    System.out.println(dom);
  }

  public void fetchDefaults(MutableJsonObject json)
  {
  }
  
  public void fetchOverrides(MutableJsonObject json)
  {
  }
  
  public MutableJsonObject fetchService() throws IOException
  {
    MutableJsonObject json  = new MutableJsonObject();

    fetchDefaults(json);
    
    if(service_ != null)
    {
      MutableJsonObject serviceJson = fetchConfig(SERVICE_DIR, service_ + ".json");
      
      
      System.out.println("Service=" + serviceJson.immutify());
      
      IJsonDomNode serviceRepoStr = serviceJson.get("repo");
      
      if(serviceRepoStr instanceof IStringProvider)
      {
        URL serviceRepoUrl = new URL(((IStringProvider)serviceRepoStr).asString());
        
        DeployConfig serviceDeployConfig = getServiceConfig(serviceRepoUrl);
        
        json.addAll(serviceDeployConfig.fetch(false));
      }
    }
    
    json.addAll(fetch(true));
    
    fetchOverrides(json);
    
    return json;
    
  }
  
  public MutableJsonObject fetch(boolean required) throws IOException
  {  
    MutableJsonObject json  = new MutableJsonObject();
    
    String dir = CONFIG;
    
    try
    {
      json.addAll(fetchConfig(dir, DEFAULTS + DOT_JSON));
    }
    catch(FileNotFoundException e)
    {
      System.err.println("No defaults");
    }
    
    dir = dir + "/" + ENVIRONMENT + "/" + environment_;
    
    try
    {
      json.addAll(fetchConfig(dir, ENVIRONMENT + DOT_JSON));
    }
    catch(FileNotFoundException e)
    {
      if(required)
        throw new IllegalArgumentException("No such environment \"" + environment_ + "\"");
      
      System.err.println("No environment config");
    }
    
    dir = dir + "/" + realm_;
    
    try
    {
      json.addAll(fetchConfig(dir, REALM + DOT_JSON));
    }
    catch(FileNotFoundException e)
    {
      if(required)
        throw new IllegalArgumentException("No such realm \"" + realm_ + "\"");
      
      System.err.println("No realm config");
    }
    
    dir = dir + "/" + region_;
    
    try
    {
      json.addAll(fetchConfig(dir, REGION + DOT_JSON));
    }
    catch(FileNotFoundException e)
    {
      if(required)
        throw new IllegalArgumentException("No such region \"" + region_ + "\"");
      
      System.err.println("No region config");
    }
    
//    for(String realmFile : fetchDirs(environment))
//    {
//      String realm = environment + "/" + realmFile;
//      MutableJsonObject realmJson = json.newMutableCopy();
//      
//      mergeAllFiles(realmJson, realm);
//
//      for(String regionFile : fetchDirs(realm))
//      {
//        String region = realm + "/" + regionFile;
//        MutableJsonObject regionJson = realmJson.newMutableCopy();
//        
//        mergeAllFiles(regionJson, region);
//
//        System.out.println("region = " + regionFile + regionJson.immutify());
//      }
//    }
    
    
    
    
    
    
    
    return json;
  }


  
  private void mergeAllFiles(MutableJsonObject json, String folderName) throws IOException
  {
    for(String file : fetchFiles(folderName))
    {
      MutableJsonObject config = fetchConfig(folderName, file);
    
      System.out.println("config from " + file + " = " + config);
      
      json.addAll(config);
    }
  }

  public abstract DeployConfig getServiceConfig(URL serviceRepoUrl);
  public abstract List<String> fetchFiles(String folderName);
  public abstract List<String> fetchDirs(String folderName);
}
