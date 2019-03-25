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

package org.symphonyoss.s2.fugue.deploy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.common.dom.IStringProvider;
import org.symphonyoss.s2.common.dom.TypeAdaptor;
import org.symphonyoss.s2.common.dom.json.IJsonArray;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonObject;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonDom;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.common.dom.json.JsonObject;
import org.symphonyoss.s2.common.dom.json.MutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.cmd.CommandLineHandler;
import org.symphonyoss.s2.fugue.naming.INameFactory;

/**
 * Abstract base class for deployment utility implementations, to be subclassed for each cloud service provider.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class FugueDeploy extends CommandLineHandler
{
  /** The label for a configuration */
  public static final String   CONFIG           = "config";
  /** The label for an environment */
  public static final String   ENVIRONMENT      = "environment";
  /** The label for an environment type */
  public static final String   ENVIRONMENT_TYPE = "environmentType";
  /** The label for a cloud */
  public static final String   CLOUD            = "cloud";
  /** The label for a region */
  public static final String   REGION           = "region";
  /** The label for a track */
  public static final String   TRACK            = "track";
  /** The label for a station */
  public static final String   STATION          = "station";
  /** The label for a service */
  public static final String   SERVICE          = "service";
  /** The label for a policy / role */
  public static final String   POLICY           = "policy";
  /** The label for a pod */
  public static final String   POD              = "pod";
  /** The label for a podName */
  public static final String   POD_NAME           = "podName";
  /** The label for a podId */
  public static final String   POD_ID           = "podId";
  /** The label for an action */
  public static final String   ACTION           = "action";
  /** The file name extension for a JSON document */
  public static final String   DOT_JAR         = ".jar";
  /** The file name extension for a Jar file */
  public static final String   DOT_JSON         = ".json";
  /** The label for a build ID */
  public static final String   BUILD_ID         = "buildId";
  /** The label for an ID */
  public static final String   ID               = "id";

  /** The suffix for a label to indicate that it is the ID of the object rather than the object itself */
  public static final String   ID_SUFFIX        = "Id";
  
  /** The prefix for the names of fugue entities in the CSP */
  public static final String FUGUE_PREFIX = "fugue-";

  private static final String     CONFIG_DIR          = CONFIG + "/";
  private static final String     SERVICE_DIR         = CONFIG_DIR + SERVICE;
  private static final String     DEFAULTS            = "defaults";
  private static final String     FQ_SERVICE_NAME     = "fullyQualifiedServiceName";

  private static final Logger     log_                = LoggerFactory.getLogger(FugueDeploy.class);
  private static final String     SINGLE_TENANT       = "singleTenant";
  private static final String     MULTI_TENANT        = "multiTenant";
  private static final String     TRUST               = "trust";
  private static final String     PORT                = "port";
  private static final String     ROLE                = "role";
  private static final String     MEMORY              = "memory";
  private static final String     TIMEOUT             = "timeout";
  private static final String     HANDLER             = "handler";
  private static final String     PATHS               = "paths";
  private static final String     HEALTH_CHECK_PATH   = "healthCheckPath";
  private static final String     CONTAINERS          = "containers";
  private static final String     SCHEDULE            = "schedule";
  private static final String     INSTANCES           = "instances";

  private static final String     DNS_SUFFIX          = "dnsSuffix";

  private final String            cloudServiceProvider_;
  private final ConfigProvider    provider_;
  private final ConfigHelper[]    helpers_;

  private String                  track_;
  private String                  station_;
  private String                  service_;
  private String                  environment_;
  private String                  environmentType_;
  private String                  region_;
  private String                  podName_;
  private String                  instances_;
  private String                  buildId_;

  private boolean                 primaryEnvironment_ = false;
  private boolean                 primaryRegion_      = false;

  private FugueDeployAction       action_;
  private String                  dnsSuffix_;

  private ExecutorService         executor_           = Executors.newFixedThreadPool(20,
      new NamedThreadFactory("Batch", true));

  private List<DeploymentContext> tenantContextList_  = new LinkedList<>();
  private DeploymentContext       multiTenantContext_;

  private Map<String, String>     tags_               = new HashMap<>();
  
  private Map<String, String> policyTrust_;
  
  protected abstract DeploymentContext  createContext(String tenantId, INameFactory nameFactory);
  
  protected abstract void validateAccount(IJsonObject<?> config);
  
  
  /**
   * Constructor.
   * 
   * @param cloudServiceProvider  Name of the CSP "amazon" or "google".
   * @param provider              A config provider.
   * @param helpers               Zero or more config helpers.
   */
  public FugueDeploy(String cloudServiceProvider, ConfigProvider provider, ConfigHelper ...helpers)
  {
    cloudServiceProvider_ = cloudServiceProvider;
    provider_ = provider;
    helpers_ = helpers == null ? new ConfigHelper[0] : helpers;
    
    withFlag(null,  TRACK,                "FUGUE_TRACK",                String.class,   false, false,   (v) -> track_               = v);
    withFlag(null,  STATION,              "FUGUE_STATION",              String.class,   false, false,   (v) -> station_             = v);
    withFlag('s',   SERVICE,              "FUGUE_SERVICE",              String.class,   false, false,   (v) -> service_             = v);
    withFlag('v',   ENVIRONMENT_TYPE,     "FUGUE_ENVIRONMENT_TYPE",     String.class,   false, false,   (v) -> environmentType_     = v);
    withFlag('e',   ENVIRONMENT,          "FUGUE_ENVIRONMENT",          String.class,   false, false,   (v) -> environment_         = v);
    withFlag('g',   REGION,               "FUGUE_REGION",               String.class,   false, false,   (v) -> region_              = v);
    withFlag('p',   POD_NAME,             "FUGUE_POD_NAME",             String.class,   false, false,   (v) -> podName_              = v);
    withFlag('a',   ACTION,               "FUGUE_ACTION",               String.class,   false, true,    (v) -> setAction(v));
    withFlag('E',   "primaryEnvironment", "FUGUE_PRIMARY_ENVIRONMENT",  Boolean.class,  false, false,   (v) -> primaryEnvironment_  = v);
    withFlag('G',   "primaryRegion",      "FUGUE_PRIMARY_REGION",       Boolean.class,  false, false,   (v) -> primaryRegion_       = v);
    withFlag('i',   "instances",          "FUGUE_INSTANCES",            String.class,   false, false,   (v) -> instances_           = v);
    withFlag('b',   BUILD_ID,             "FUGUE_BUILD_ID",             String.class,   false, false,   (v) -> buildId_             = v);
    
    provider_.init(this);
    
    for(ConfigHelper helper : helpers_)
      helper.init(this);
  }
  
  protected abstract INameFactory createNameFactory(String environmentType, String environmentId, String regionId,
      String tenantId, String serviceId);

  private void setAction(String v)
  {
    action_ = FugueDeployAction.valueOf(v);
      
    if(action_ == null)
      throw new IllegalArgumentException("\"" + v + "\" is not a valid action");
  }

  /**
   * Verify that the given value is non-null
   * 
   * @param name  Name of the value for exception message
   * @param value A Value
   * @param <T>   The type of the value
   * 
   * @return      The given value, which is guaranteed to be non-null
   * 
   * @throws      IllegalArgumentException if value is null.
   */
  public @Nonnull <T> T require(String name, T value)
  {
    if(value == null)
      throw new IllegalArgumentException("\"" + name + "\" is a required parameter");
    
    return value;
  }

  /**
   * 
   * @return The service ID
   */
  public @Nonnull String getService()
  {
    return require(SERVICE, service_);
  }
  
  /**
   * 
   * @return The station ID.
   */
  public @Nonnull String getStation()
  {
    return require(STATION, station_);
  }
  
  /**
   * 
   * @return The track ID.
   */
  public @Nonnull String getTrack()
  {
    return require(TRACK, track_);
  }
  
  /**
   * 
   * @return The environment ID.
   */
  public @Nonnull String getEnvironment()
  {
    return require(ENVIRONMENT, environment_);
  }
  
  /**
   * 
   * @return The region ID
   */
  public @Nonnull String getRegion()
  {
    return require(REGION, region_);
  }
  
  /**
   * 
   * @return The environment type "dev", "qa" etc.
   */
  public @Nonnull String getEnvironmentType()
  {
    return require("environmentType", environmentType_);
  }
  
  /**
   * 
   * @return true if this is the primary environment for this tenant/service
   */
  public boolean isPrimaryEnvironment()
  {
    return primaryEnvironment_;
  }
  
  /**
   * 
   * @return true if this is the primary region for this tenant/service
   */
  public boolean isPrimaryRegion()
  {
    return primaryRegion_;
  }

  /**
   * 
   * @return the desired instance count
   */
  public String getInstances()
  {
    return instances_;
  }
  
  /**
   * @return the environment type dns suffix.
   */
  public String getDnsSuffix()
  {
    return dnsSuffix_;
  }
  
  protected void populateTags(Map<String, String> tags)
  {
    tagIfNotNull("FUGUE_ENVIRONMENT_TYPE",  environmentType_);
    tagIfNotNull("FUGUE_ENVIRONMENT",       environment_);
    tagIfNotNull("FUGUE_REGION",            region_);
    tagIfNotNull("FUGUE_SERVICE",           service_);
  }
  
  protected void tagIfNotNull(String name, String value)
  {
    if(value != null)
      tags_.put(name, value);
  }

  protected Map<String, String> getTags()
  {
    return tags_;
  }

  /**
   * Perform the deployment.
   */
  public void deploy()
  {    
    if(action_ == FugueDeployAction.DeployStation)
    {
      getStationConfig();
    }
    else
    {
      if(podName_ != null)
        tenantContextList_.add(createContext(podName_, createNameFactory(environmentType_, environment_, region_, podName_, service_)));
    }

    log_.info("FugueDeploy v1.1");
    log_.info("ACTION           = " + action_);
    log_.info("ENVIRONMENT_TYPE = " + environmentType_);
    log_.info("ENVIRONMENT      = " + environment_);
    log_.info("REGION           = " + region_);
    
    for(int i=0 ; i<tenantContextList_.size() ; i++)
      log_.info(String.format("TENANT[%3d]      = %s", i, tenantContextList_.get(i).getTenantId()));
    
    populateTags(tags_);
    
    // All actions need multi-tenant config
    ImmutableJsonObject multiTenantIdConfig   = createIdConfig(null);
    ImmutableJsonObject environmentConfig     = fetchEnvironmentConfig();
    ImmutableJsonObject multiTenantDefaults   = fetchMultiTenantDefaults(multiTenantIdConfig);
    ImmutableJsonObject multiTenantOverrides  = fetchMultiTenantOverrides(multiTenantIdConfig);
    ImmutableJsonObject multiTenantConfig     = overlay(
        multiTenantDefaults,
        environmentConfig,
        multiTenantOverrides,
        multiTenantIdConfig
        );

    validateAccount(multiTenantConfig);
    
    dnsSuffix_   = multiTenantConfig.getRequiredString(DNS_SUFFIX);
    
    final boolean deployConfig;
    final boolean deployContainers;
    
    switch (action_)
    {
      case CreateEnvironmentType:
        deployConfig = false;
        deployContainers = false;
        log_.info("Creating environment type \"" + environmentType_ + "\"");
        
        multiTenantContext_ = createContext(null, createNameFactory(environmentType_, null, null, null, null));
        multiTenantContext_.setConfig(multiTenantConfig);
        multiTenantContext_.createEnvironmentType();
        break;

      case CreateEnvironment:
        deployConfig = false;
        deployContainers = false;
        log_.info("Creating environment \"" + environment_ + "\"");
        
        multiTenantContext_ = createContext(null, createNameFactory(environmentType_, environment_, null, null, null));
        multiTenantContext_.setConfig(multiTenantConfig);
        multiTenantContext_.createEnvironment();
        break;

      case Deploy:
      case DeployStation:
        deployConfig = true;
        deployContainers = true;
//        MutableJsonObject serviceJson = deployConfig();
//        
//        if(serviceJson != null)
//          deployContainers(serviceJson);
        break;
        
      case DeployConfig:
//        deployConfig();
          deployConfig = true;
          deployContainers = false;
        break;
        
      default:
        throw new IllegalStateException("Unrecognized action " + action_);
    }
    
    

    if(deployConfig)
    {    
      multiTenantContext_ = createContext(null, createNameFactory(environmentType_, environment_, region_, null, service_));
      multiTenantContext_.setConfig(multiTenantConfig);
      
      ImmutableJsonObject serviceJson;
      String dir = SERVICE_DIR + "/" + getService();
      
      try
      {
        serviceJson = provider_.fetchConfig(dir, SERVICE + ".json").immutify();
        
        log_.info("Service=" + serviceJson);
      }
      catch(IOException e)
      {
        throw new IllegalArgumentException("Unknown service \"" + service_ + "\".", e);
      }
      
      dir = dir + "/" + cloudServiceProvider_ + "/" + POLICY;
      
      
      
      // deploy multi-tenant containers first
      

      policyTrust_ = fetchPolicies(dir, TRUST);

      multiTenantContext_.setPolicies(fetchPolicies(dir, MULTI_TENANT));
      
      multiTenantContext_.processConfigAndPolicies();
      
      IJsonObject<?>                  containerJson           = (IJsonObject<?>)serviceJson.get(CONTAINERS);
      
      Map<ContainerType, Map<String, JsonObject<?>>>  singleTenantContainerMap     = new HashMap<>();
      Map<ContainerType, Map<String, JsonObject<?>>>  multiTenantContainerMap      = new HashMap<>();
      
//      Map<String, JsonObject<?>>  singleTenantInitMap     = new HashMap<>();
//      Map<String, JsonObject<?>>  multiTenantInitMap      = new HashMap<>();
//      Map<String, JsonObject<?>>  singleTenantServiceMap  = new HashMap<>();
//      Map<String, JsonObject<?>>  multiTenantServiceMap   = new HashMap<>();
      
      if(deployContainers)
      {
        // Load all the containers
        
        Iterator<String>                it                      = containerJson.getNameIterator();
        
        while(it.hasNext())
        {
          String name = it.next();
          IJsonDomNode c = containerJson.get(name);
          
          if(c instanceof JsonObject)
          {
            JsonObject<?> container = (JsonObject<?>)c;
            Tenancy       tenancy   = Tenancy.parse(container.getRequiredString("tenancy"));
            
            Map<ContainerType, Map<String, JsonObject<?>>>  containerMap;
            
            switch(tenancy)
            {
              case SINGLE:
                containerMap = singleTenantContainerMap;
                break;
                
              case MULTI:
                containerMap = multiTenantContainerMap;
                break;
                
              default:
                throw new CodingFault("Unknown tenancy type " + tenancy);
            }
            
            ContainerType containerType = ContainerType.parse(container.getString("containerType", null));
            
            Map<String, JsonObject<?>> map = containerMap.get(containerType);
            
            if(map == null)
            {
              map = new HashMap<>();
              containerMap.put(containerType, map);
            }
            
            map.put(name, container);
          }
        }
        
        // Deploy multi-tenant init containers
        
        multiTenantContext_.setContainers(multiTenantContainerMap);
        
        multiTenantContext_.deployInitContainers();
      }
      
      // Now we can do all the single tenant processes in parallel
      
      
      IBatch<Runnable>    batch                 = createBatch();
      Map<String, String> singleTenantPolicies  = fetchPolicies(dir, SINGLE_TENANT);
      
      for(DeploymentContext context : tenantContextList_)
      {
        context.setPolicies(singleTenantPolicies);
        
        String tenantId = context.getTenantId();
        
        batch.submit(() ->
        {
          
          ImmutableJsonObject tenantIdConfig         = createIdConfig(tenantId);
          ImmutableJsonObject tenantConfig           = fetchTenantConfig(tenantId);
          ImmutableJsonObject singleTenantIdConfig   = overlay(
              multiTenantDefaults,
              environmentConfig,
              tenantConfig,
              multiTenantOverrides,
              tenantIdConfig);
          ImmutableJsonObject singleTenantDefaults   = fetchSingleTenantDefaults(singleTenantIdConfig, tenantId);
          ImmutableJsonObject singleTenantOverrides  = fetchSingleTenantOverrides(singleTenantDefaults, tenantId);
          
          /*
           * At this point the defaults and environment config have all been merged in, we now just need to overlay the 
           * multiTenantOverrides and tenantIdConfig to ensure that they win over anything else which happened previously.
           * 
           * This is a bit confusing but it allows ConfigHelpers to use config from previous steps to identify what needs to
           * be added, specifically we need this for tenantId -> podName mapping.
           * 
           * Perhaps this can be removed once this mapping is in consul.
           */
          ImmutableJsonObject singleTenantConfig     = overlay(
              singleTenantOverrides,
              multiTenantOverrides,
              tenantIdConfig);
          
          context.setConfig(singleTenantConfig);
          context.processConfigAndPolicies();
          context.setContainers(singleTenantContainerMap);
          
          if(deployContainers)
          {
            context.deployInitContainers();
          }
        });
      }
      
      batch.waitForAllTasks();
      
      if(deployContainers)
      {
        // Now launch all the service containers in parallel
        
        IBatch<Runnable> containerBatch = createBatch();
            
        
        multiTenantContext_.deployServiceContainers(containerBatch);
        
        for(DeploymentContext context : tenantContextList_)
        {
          context.deployServiceContainers(containerBatch);
        }
        
        containerBatch.waitForAllTasks();
      }
    }
  }
  
  
  


  private IBatch<Runnable> createBatch()
  {
    if(Fugue.isDebugSingleThread())
      return new SerialBatch<Runnable>();
    
    return new ExecutorBatch<Runnable>(executor_); 
  }

  private ImmutableJsonObject fetchTenantConfig(String tenantId)
  {
    String  dir = CONFIG + "/" + POD + "/" + tenantId;
    try
    {
      MutableJsonObject tenantConfig = fetchSpecificTenantConfig(dir);
      
      try
      {
        tenantConfig.addAll(provider_.fetchConfig(dir, service_ + DOT_JSON), "#");
      }
      catch(FileNotFoundException e)
      {
        log_.info("No tenant specific service config");
      }
      
      return tenantConfig.immutify();
    }
    catch(IOException e)
    {
      throw new IllegalStateException("Unable to read tenant config", e);
    }
  }
  
  private MutableJsonObject fetchSpecificTenantConfig(String dir) throws IOException
  {
    try
    {
      return provider_.fetchConfig(dir, FUGUE_PREFIX + POD + DOT_JSON);
    }
    catch(FileNotFoundException e)
    {
      log_.info("No tenant specific config");
      
      return new MutableJsonObject();
    }
  }

  private ImmutableJsonObject createIdConfig(String podName)
  {
    MutableJsonObject idConfig = new MutableJsonObject();
    MutableJsonObject id = new MutableJsonObject();
    
    idConfig.add(ID, id);
    
    id.addIfNotNull(ENVIRONMENT + ID_SUFFIX,  environment_);
    id.addIfNotNull(ENVIRONMENT_TYPE,         environmentType_);
    id.addIfNotNull(REGION + ID_SUFFIX,       region_);
    id.addIfNotNull(SERVICE + ID_SUFFIX,      service_);
    id.addIfNotNull(POD_NAME,                 podName);
    
    return idConfig.immutify();
  }
  
  

  private ImmutableJsonObject overlay(JsonObject<?> ...objects)
  {
    MutableJsonObject config = new MutableJsonObject();
    
    for(JsonObject<?> object : objects)
      config.addAll(object, "#");
    
    return config.immutify();
  }

  private ImmutableJsonObject fetchMultiTenantDefaults(IJsonObject<?> idConfig)
  {
    MutableJsonObject config = idConfig.newMutableCopy();
    
    provider_.overlayDefaults(config);
    
    for(ConfigHelper helper : helpers_)
      helper.overlayDefaults(config);
    
    return config.immutify();
  }
  
  private ImmutableJsonObject fetchMultiTenantOverrides(IJsonObject<?> idConfig)
  {
    MutableJsonObject config = idConfig.newMutableCopy();
    
    provider_.overlayOverrides(config);
    
    for(ConfigHelper helper : helpers_)
      helper.overlayOverrides(config);
    
    return config.immutify();
  }
  
  private ImmutableJsonObject fetchEnvironmentConfig()
  {
    try
    {
      MutableJsonObject json  = new MutableJsonObject();
      
      String dir = CONFIG + "/" + ENVIRONMENT;
      
      fetch(false, json, dir, DEFAULTS, service_, "defaults", "");
      
      dir = dir + "/" + environmentType_;
      
      fetch(true, json, dir, ENVIRONMENT_TYPE, service_, "environment type", environmentType_);  
      
      if(environment_ == null)
        return json.immutify();
      
      dir = dir + "/" + environment_;
      
      fetch(true, json, dir, ENVIRONMENT, service_, "environment", environment_);
      
      dir = dir + "/" + cloudServiceProvider_;
      
      fetch(true, json, dir, CLOUD, service_, "cloud", cloudServiceProvider_);
      
      if(region_ == null)
        return json.immutify();
      
      dir = dir + "/" + region_;
      
      fetch(true, json, dir, REGION, service_, "region", region_);
      
      return json.immutify();
    }
    catch(IOException e)
    {
      throw new IllegalStateException("Unable to load environment config", e);
    }
  }

  private ImmutableJsonObject fetchSingleTenantDefaults(IJsonObject<?> idConfig, String tenantId)
  {
    MutableJsonObject config = idConfig.newMutableCopy();
    
    provider_.overlayDefaults(tenantId, config);
    
    for(ConfigHelper helper : helpers_)
      helper.overlayDefaults(tenantId, config);
    
    return config.immutify();
  }
  
  private ImmutableJsonObject fetchSingleTenantOverrides(IJsonObject<?> idConfig, String tenantId)
  {
    MutableJsonObject config = idConfig.newMutableCopy();
    
    provider_.overlayOverrides(tenantId, config);
    
    for(ConfigHelper helper : helpers_)
      helper.overlayOverrides(tenantId, config);
    
    return config.immutify();
  }

  private void getStationConfig()
  {
    MutableJsonObject json  = new MutableJsonObject();
    
    try
    {
      String dir = CONFIG + "/" + TRACK;

      fetch(true, json, dir, getTrack(), null, "Release Track", getTrack());
    }
    catch(FileNotFoundException e)
    {
      throw new IllegalArgumentException("No such track config", e);
    }
    catch(IOException e)
    {
      throw new IllegalStateException("Unable to read track config", e);
    }

    IJsonDomNode stationsNode = json.get("stations");
    
    if(stationsNode == null)
    {
      throw new IllegalStateException("Unable to read stations from track config");
    }
    
    if(stationsNode instanceof IJsonArray)
    {
      for(IJsonDomNode stationNode : ((IJsonArray<?>)stationsNode))
      {
        if(stationNode instanceof IJsonObject)
        {
          IJsonObject<?> station = (IJsonObject<?>)stationNode;
          
          String name = station.getRequiredString("name");
          
          if(getStation().equals(name) && cloudServiceProvider_.equals(station.getRequiredString(CLOUD)))
          {
            environmentType_  = station.getRequiredString(ENVIRONMENT_TYPE);
            environment_      = station.getRequiredString(ENVIRONMENT);
            region_           = station.getRequiredString(REGION);
            
            IJsonDomNode tenantsNode = station.get("tenants");
            
            if(tenantsNode != null)
            {
              if(tenantsNode instanceof IJsonArray)
              {
                for(IJsonDomNode tenantNode : ((IJsonArray<?>)tenantsNode))
                {
                  if(tenantNode instanceof IStringProvider)
                  {
                     String tenantId = ((IStringProvider)tenantNode).asString();
                   
                     tenantContextList_.add(createContext(tenantId,
                        createNameFactory(environmentType_, environment_, region_, tenantId, service_)));
                  }
                  else
                  {
                    throw new IllegalStateException("Invalid station config - tenants contains a non-string value.");
                  }
                }
              }
              else
              {
                throw new IllegalStateException("Invalid station config - tenants is not an array.");
              }
            }
          }
        }
        else
        {
          throw new IllegalStateException("Invalid track config - station \"" + stationNode + "\" is not an object.");
        }
      }
    }
    else
    {
      throw new IllegalStateException("Invalid track config - stations is not an array.");
    }
  }
  
  private void fetch(boolean required, MutableJsonObject json, String dir, String fileName, String additionalFileName, String entityType, String entityName) throws IOException
  {
    try
    {
      json.addAll(provider_.fetchConfig(dir, fileName + DOT_JSON), "#");
    }
    catch(FileNotFoundException e)
    {
      if(required)
        throw new IllegalArgumentException("No such " + entityType + " \"" + entityName + "\"", e);
      
      log_.warn("No " + entityType + " config");
    }
    
    if(additionalFileName != null)
    {
      try
      {
        json.addAll(provider_.fetchConfig(dir, additionalFileName + DOT_JSON), "#");
      }
      catch(FileNotFoundException e)
      {
        log_.info("No " + entityType + " " + additionalFileName + " config");
      }
    }
  }
  
  protected Map<String, String> fetchPolicies(String parentDir, String subDir)
  {
    Map<String, String> policies = new HashMap<>();
    
    try
    {
      String              dir               = parentDir + "/" + subDir;
      List<String>        files             = provider_.fetchFiles(dir);
      
      for(String file : files)
      {
        if(file.endsWith(FugueDeploy.DOT_JSON))
        {
          String name     = file.substring(0, file.length() - FugueDeploy.DOT_JSON.length());
          String template = provider_.fetchConfig(dir, file).immutify().toString();
          
          policies.put(name, template);
        }
        else
          throw new IllegalStateException("Unrecognized file type found in config: " + dir + "/" + file);
      }
    }
    catch(IOException e)
    {
      throw new IllegalStateException("Unable to load " + subDir + " policies.", e);
    }
    
    return policies;
  }

  protected abstract class DeploymentContext
  {
    private static final String FUGUE_CONFIG = "FUGUE_CONFIG";
    
    private final String                                   tenantId_;
    private final INameFactory                             nameFactory_;

    private Map<String, String>                            policies_;
    private ImmutableJsonObject                            config_;
    private ImmutableJsonDom                               configDom_;
    private Map<String, String>                            templateVariables_;
    private StringSubstitutor                              sub_;

    private Map<ContainerType, Map<String, JsonObject<?>>> containerMap_;

    protected DeploymentContext(String tenantId, INameFactory nameFactory)
    {
      tenantId_ = tenantId;
      nameFactory_ = nameFactory;
    }

    protected abstract void createEnvironmentType();
    
    protected abstract void createEnvironment();
    
    protected abstract void processRole(String name, String roleSpec, String trustSpec);
    
    protected abstract void saveConfig();
    
    protected abstract void deployInitContainer(String name, int port, Collection<String> paths, String healthCheckPath); //TODO: maybe wrong signature
    
    protected abstract void configureServiceNetwork();
    
    protected abstract void deployServiceContainer(String name, int port, Collection<String> paths, String healthCheckPath, int instances);
    
    protected abstract void deployScheduledTaskContainer(String name, int port, Collection<String> paths, String schedule);

    protected abstract void deployLambdaContainer(String name, String roleId, String handler, int memorySize, int timeout, Map<String, String> variables);
    
    protected abstract void deployService();

    protected abstract String getFugueConfig();
    
    protected INameFactory getNameFactory()
    {
      return nameFactory_;
    }

    protected String getTenantId()
    {
       return tenantId_;
   }

    protected String getBuildId()
    {
      return buildId_;
    }

    protected Map<String, String> getPolicies()
    {
      return policies_;
    }

    protected ImmutableJsonDom getConfigDom()
    {
      return configDom_;
    }

    protected Map<String, String> getTemplateVariables()
    {
      return templateVariables_;
    }

    protected StringSubstitutor getSub()
    {
      return sub_;
    }

    protected ImmutableJsonObject getConfig()
    {
      return config_;
    }

    protected Map<ContainerType, Map<String, JsonObject<?>>> getContainerMap()
    {
      return containerMap_;
    }
    
    protected boolean hasDockerContainers()
    {
      return hasContainers(ContainerType.SERVICE) || hasContainers(ContainerType.SCHEDULED);
    }

    protected boolean hasContainers(ContainerType containerType)
    {
      Map<String, JsonObject<?>> map = containerMap_.get(containerType);
      
      return map != null && !map.isEmpty();
    }

    protected void setConfig(ImmutableJsonObject config)
    {
      config_              = config;
      configDom_           = new MutableJsonDom().add(config_).immutify();
      templateVariables_   = createTemplateVariables(config);
      sub_                 = new StringSubstitutor(templateVariables_);
    }

    protected void setPolicies(Map<String, String> policies)
    {
      policies_ = policies;
    }
    
    protected void setContainers(Map<ContainerType, Map<String, JsonObject<?>>> containerMap)
    {
      containerMap_  = containerMap;
    }

    protected Map<String, String> createTemplateVariables(ImmutableJsonObject config)
    {
      Map<String, String> templateVariables = new HashMap<>();
      
      provider_.populateTemplateVariables(config, templateVariables);
      
      for(ConfigHelper helper : helpers_)
        helper.populateTemplateVariables(config, templateVariables);
      
      populateTemplateVariables(config, templateVariables);
      
      return templateVariables;
    }
    
    protected void populateTemplateVariables(ImmutableJsonObject config, Map<String, String> templateVariables)
    {
      IJsonObject<?>      id = config.getRequiredObject(ID);
      Iterator<String>    it;
      
      it = config.getNameIterator();
      while(it.hasNext())
      {
        String name = it.next();
        
        if(name.startsWith("fugue"))
        {
          IJsonDomNode value = config.get(name);
          
          if(value instanceof IStringProvider)
          {
            templateVariables.put(name, ((IStringProvider)value).asString());
          }
        }
      }
      
      it = id.getNameIterator();
      while(it.hasNext())
      {
        String name = it.next();
        IJsonDomNode value = id.get(name);
        
        if(value instanceof IStringProvider)
        {
          templateVariables.put(name, ((IStringProvider)value).asString());
        }      
      }
      
      try
      {
        templateVariables.put(FQ_SERVICE_NAME, nameFactory_.getServiceName().toString());
      }
      catch(IllegalStateException e)
      {
        // This is actually OK.
      }
      

    }
    
    /**
     * Load a template and perform variable substitution.
     * 
     * The template is provided as a Java resource and the expanded template is returned as a String.
     * 
     * @param fileName The name of the resource containing the template.
     * 
     * @return The expanded template.
     */
    protected String loadTemplateFromResource(String fileName)
    {
      try(InputStream template = getClass().getClassLoader().getResourceAsStream(fileName))
      {
        if(template == null)
          throw new IllegalArgumentException("Template \"" + fileName + "\" not found");
        
        return loadTemplateFromStream(template, fileName);
      }
      catch (IOException e)
      {
        throw new IllegalArgumentException("Unable to read template \"" + fileName + "\"", e);
      }
    }

    /**
     * Load a template and perform variable substitution.
     * 
     * The template is provided as a Java resource and the expanded template is returned as a String.
     * 
     * @param template An InputStream containing the template.
     * @param fileName The "Name" of the template for error messages.
     * 
     * @return The expanded template.
     */
    protected String loadTemplateFromStream(InputStream template, String fileName)
    {
      StringBuilder s = new StringBuilder();
      
      try(BufferedReader in = new BufferedReader(new InputStreamReader(template)))
      {
        StringSubstitutor sub = new StringSubstitutor(templateVariables_);
        String            line;
        
        while((line=in.readLine()) != null)
        {
          s.append(sub.replace(line));
          s.append(System.lineSeparator());
        }
        
        return s.toString();
      }
      catch (IOException e)
      {
        throw new IllegalArgumentException("Unable to read template \"" + fileName + "\"", e);
      }
    }
    
    private void processConfigAndPolicies()
    {
      IBatch<Runnable> batch = createBatch();
      
      batch.submit(() ->
      { 
        saveConfig();
      });
      
      for(Entry<String, String> entry : policies_.entrySet())
      {
        String name     = entry.getKey();
        String template = entry.getValue();
        String trustTemplate = policyTrust_.get(name);
        
        batch.submit(() ->
        {
          String roleSpec = sub_.replace(template);
          String trustSpec = trustTemplate == null ? null : sub_.replace(trustTemplate);
          
          processRole(name, roleSpec, trustSpec);
        });
      }
      
      batch.waitForAllTasks();
    }
    
    protected void deployInitContainers()
    {
      Map<String, JsonObject<?>> initContainerMap = containerMap_.get(ContainerType.INIT);
      
      if(initContainerMap != null && !initContainerMap.isEmpty())
      {
        // Deploy service level assets, load balancers, DNS zones etc
        
        deployService();
        
        for(String name : initContainerMap.keySet())
        {
          JsonObject<?> container = initContainerMap.get(name);
          
          IJsonDomNode        portNode = container.get(PORT);
          int                 port = portNode == null ? 80 : TypeAdaptor.adapt(Integer.class, portNode);
          Collection<String>  paths = container.getListOf(String.class, PATHS);
          String              healthCheckPath = container.getString(HEALTH_CHECK_PATH, "/HealthCheck");
          
          deployInitContainer(name, port, paths, healthCheckPath);
        }
      }
    }
    
    protected void deployServiceContainers(IBatch<Runnable> batch)
    {
      if(!getContainerMap().isEmpty())
      {
        configureServiceNetwork();
  
        deployDockerContainers(batch, ContainerType.SERVICE,    false);
        deployDockerContainers(batch, ContainerType.SCHEDULED,  true);
        deployLambdaContainers(batch, ContainerType.LAMBDA);
      }
    }

    private void deployLambdaContainers(IBatch<Runnable> batch, ContainerType containerType)
    {
      Map<String, JsonObject<?>> map = containerMap_.get(containerType);
      
      if(map != null)
      {
        for(String name : map.keySet())
        {
          JsonObject<?> container = map.get(name);
          
          batch.submit(() ->
          {
            Map<String, String> environment = new HashMap<>();
            
            IJsonObject<?> envNode = container.getObject(ENVIRONMENT);
            
            if(envNode != null)
            {
              Iterator<String> it = envNode.getNameIterator();
              
              while(it.hasNext())
              {
                String key = it.next();
                environment.put(key, envNode.getRequiredString(key));
              }
            }
            
            if(!environment.containsKey(FUGUE_CONFIG))
            {
              environment.put(FUGUE_CONFIG, getFugueConfig());
            }
            
            deployLambdaContainer(name,
                container.getRequiredString(ROLE),
                container.getRequiredString(HANDLER),
                container.getRequiredInteger(MEMORY),
                container.getRequiredInteger(TIMEOUT),
                environment
                );
          });
        }
      }
    }

    private void deployDockerContainers(IBatch<Runnable> batch, ContainerType containerType, boolean scheduled)
    {
      Map<String, JsonObject<?>> map = containerMap_.get(containerType);
      
      if(map != null)
      {
        for(String name : map.keySet())
        {
          JsonObject<?> container = map.get(name);
          
          batch.submit(() ->
          {
            IJsonDomNode        portNode = container.get(PORT);
            int                 port = portNode == null ? 80 : TypeAdaptor.adapt(Integer.class, portNode);
            Collection<String>  paths = container.getListOf(String.class, PATHS);
            int                 instances = Integer.parseInt(container.getString(INSTANCES, "1"));
            
            if(scheduled)
              deployScheduledTaskContainer(name, port, paths, container.getRequiredString(SCHEDULE));
            else
              deployServiceContainer(name, port, paths, container.getString(HEALTH_CHECK_PATH, "/HealthCheck"), instances);
          });
        }
      }
    }
  }
}
