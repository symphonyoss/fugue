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

import javax.annotation.Nullable;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.IStringProvider;
import org.symphonyoss.s2.common.dom.TypeAdaptor;
import org.symphonyoss.s2.common.dom.json.IJsonArray;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonObject;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.common.exception.InvalidValueException;
import org.symphonyoss.s2.fugue.cmd.CommandLineHandler;
import org.symphonyoss.s2.fugue.naming.Name;

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
  /** The label for a realm */
  public static final String   REALM            = "realm";
  /** The label for a region */
  public static final String   REGION           = "region";
  /** The label for a service */
  public static final String   SERVICE          = "service";
  /** The label for a policy / role */
  public static final String   POLICY           = "policy";
  /** The label for a tenant */
  public static final String   TENANT           = "tenant";
  /** The label for an action */
  public static final String   ACTION           = "action";
  /** The file name extension for a JSON document */
  public static final String   DOT_JSON         = ".json";
  /** The label for an ID */
  public static final String   ID               = "id";

  /** The suffix for a label to indicate that it is the ID of the object rather than the object itself */
  public static final String   ID_SUFFIX        = "Id";
  
  /** The prefix for the names of fugue entities in the CSP */
  public static final String FUGUE_PREFIX = "fugue-";

  private static final String  CONFIG_DIR          = CONFIG + "/";
  private static final String  SERVICE_DIR         = CONFIG_DIR + SERVICE;
  private static final String  DEFAULTS            = "defaults";
  private static final String  ROLES               = "roles";
  private static final String  REGION_SHORTCODE    = "regionShortCode";
  private static final String  FQ_SERVICE_NAME    = "fullyQualifiedServiceName";
  private static final String  FQ_INSTANCE_NAME    = "fullyQualifiedInstanceName";
  private static final String  SHORT_INSTANCE_NAME = "shortInstanceName";

  private static final Logger log_ = LoggerFactory.getLogger(FugueDeploy.class);
  private static final String SINGLE_TENANT = "singleTenant";
  private static final String MULTI_TENANT = "multiTenant";
  private static final String PORT = "port";
  private static final String PATHS = "paths";
  private static final String HEALTH_CHECK_PATH = "healthCheckPath";
  private static final String CONTAINERS = "containers";

  private static final String            DNS_SUFFIX                      = "dnsSuffix";
  
  private final String         cloudServiceProvider_;
  private final ConfigProvider provider_;
  private final ConfigHelper[] helpers_;

  private String               service_;
  private String               environment_;
  private String               environmentType_;
  private String               realm_;
  private String               region_          = "default";
  private String               tenant_;

  private boolean              primaryEnvironment_ = false;
  private boolean              primaryRegion_ = false;
  
  private String               target_          = "-";
  private String               regionShortCode_;
  private FugueDeployAction          action_         = FugueDeployAction.DeployConfig;
  private MutableJsonObject    singleTenantConfig_;
  private MutableJsonDom       singleTenantConfigDom_;
  private MutableJsonObject    multiTenantConfig_;
  private MutableJsonDom       multiTenantConfigDom_;
  private MutableJsonObject    configId_;
  private Map<String, String>  templateVariables_ = new HashMap<>();
  private MutableJsonObject tenantConfig_;
  private String dnsSuffix_;
  
  protected abstract void createEnvironmentType();
  protected abstract void createEnvironment();
  protected abstract void processRole(String name, String roleSpec, String tenant);
  protected abstract void validateAccount(MutableJsonObject config);
  protected abstract void saveConfig(String target, ImmutableJsonDom multiTenantConfig, ImmutableJsonDom singleTenantConfig);
  protected abstract void deployServiceContainer(String name, int port, Collection<String> paths, String healthCheckPath, String tenantId);
  protected abstract void deployService(boolean hasSingleTenantContainer, boolean hasMultiTenantContainer);
  
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
    
    provider_.init(this);
    
    for(ConfigHelper helper : helpers_)
      helper.init(this);
    
    withFlag('s', SERVICE,              "FUGUE_SERVICE",          String.class, false, false, (v) -> service_ = v);
    withFlag('v', ENVIRONMENT_TYPE,     "FUGUE_ENVIRONMENT_TYPE", String.class, false, true,  (v) -> environmentType_ = v);
    withFlag('e', ENVIRONMENT,          "FUGUE_ENVIRONMENT",      String.class, false, true,  (v) -> environment_ = v);
    withFlag('r', REALM,                "FUGUE_REALM",            String.class, false, true,  (v) -> realm_ = v);
    withFlag('g', REGION,               "FUGUE_REGION",           String.class, false, false, (v) -> region_ = v);
    withFlag('o', "output",             "FUGUE_CONFIG_OUTPUT",    String.class, false, false, (v) -> target_ = v);
    withFlag('t', TENANT,               "FUGUE_TENANT",           String.class, false, false, (v) -> tenant_ = v);
    withFlag('a', ACTION,               "FUGUE_ACTION",           String.class, false, true,  (v) -> setAction(v));
    withFlag('E', "primaryEnvironment", "FUGUE_PRIMARY_ENVIRONMENT",      Boolean.class, false, true,  (v) -> primaryEnvironment_ = v);
    withFlag('G', "primaryRegion",      "FUGUE_PRIMARY_REGION",           Boolean.class, false, false, (v) -> primaryRegion_ = v);
    
  }

  private void setAction(String v)
  {
    action_ = FugueDeployAction.valueOf(v);
      
    if(action_ == null)
      throw new IllegalArgumentException("\"" + v + "\" is not a valid action");
  }

  /**
   * 
   * @return The service ID
   */
  public String getService()
  {
    return service_;
  }

  /**
   * 
   * @return The environment ID.
   */
  public String getEnvironment()
  {
    return environment_;
  }

  /**
   * 
   * @return The realm ID
   */
  public String getRealm()
  {
    return realm_;
  }
  
  /**
   * 
   * @return The region ID
   */
  public String getRegion()
  {
    return region_;
  }

  /**
   * 
   * @return the region short code.
   */
  public String getRegionShortCode()
  {
    return regionShortCode_;
  }
  /**
   * 
   * @return The environment type "dev", "qa" etc.
   */
  public String getEnvironmentType()
  {
    return environmentType_;
  }

  /**
   * 
   * @return The tenantId.
   */
  public @Nullable String getTenant()
  {
    return tenant_;
  }
  
//  protected MutableJsonObject getConfig()
//  {
//    return config_;
//  }
//
//  protected MutableJsonObject getConfigId()
//  {
//    return configId_;
//  }
  
  protected MutableJsonObject getSingleTenantConfig()
  {
    return singleTenantConfig_;
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
  
  protected MutableJsonObject getMultiTenantConfig()
  {
    return multiTenantConfig_;
  }
  
  protected FugueDeployAction getAction()
  {
    return action_;
  }

  protected Map<String, String> getTemplateVariables()
  {
    return templateVariables_;
  }

  /**
   * If there is no tenantId (we are processing for a multi-tenant service) then return ""
   * otherwise return Name.SEPARATOR + tenantId_
   * 
   * @return The tenantId as a suffx for a hyphen separated composite name.
   */
  public String getTenantSuffix()
  {
    if(tenant_ == null)
      return "";
    
    return Name.SEPARATOR + tenant_;
  }

  public String getDnsSuffix()
  {
    return dnsSuffix_;
  }
  
  /**
   * Perform the deployment.
   */
  public void deploy()
  {
    fetchConfig();

    dnsSuffix_   = multiTenantConfig_.getRequiredString(DNS_SUFFIX);
    
    validateAccount(multiTenantConfig_);
    
    switch (action_)
    {
      case CreateEnvironmentType:
        log_.info("Creating environment type \"" + environmentType_ + "\"");
        createEnvironmentType();
        break;

      case CreateEnvironment:
        log_.info("Creating environment \"" + environment_ + "\"");
        createEnvironment();
        break;
      
      case Deploy:
        MutableJsonObject serviceJson = deployConfig();
        
        if(serviceJson != null)
          deployContainers(serviceJson);
        break;
        
      case DeployConfig:
        deployConfig();
        break;
        
      default:
        throw new IllegalStateException("Unrecognized action " + action_);
    }
  }
  
  private MutableJsonObject deployConfig()
  {
    MutableJsonObject serviceJson = null;
    
    if(service_ != null)
    {
      try
      {
        String dir = SERVICE_DIR + "/" + service_;
        
        serviceJson = provider_.fetchConfig(dir, SERVICE + ".json");
        
        log_.info("Service=" + serviceJson.immutify());
        
        dir = dir + "/" + cloudServiceProvider_ + "/" + POLICY;
        
        
        if(tenant_ != null)
        {
          processPolicies(dir, SINGLE_TENANT, tenant_);
          
        }
        
        processPolicies(dir, MULTI_TENANT, null);
        
//        MutableJsonObject serviceJson = provider_.fetchConfig(SERVICE_DIR, service_ + ".json");
//        
//        log_.info("Service=" + serviceJson.immutify());
//        
//        processService(serviceJson);
      }
      catch(IOException e)
      {
        throw new IllegalArgumentException("Unknown service \"" + service_ + "\".", e);
      }
    }
    
    saveConfig(target_, multiTenantConfigDom_.immutify(), singleTenantConfigDom_ == null ? null : singleTenantConfigDom_.immutify());
    
    return serviceJson;
  }
  
  private void deployContainers(MutableJsonObject serviceJson)
  {
    IJsonObject<?>                  containerJson           = (IJsonObject<?>)serviceJson.get(CONTAINERS);
    Iterator<String>                it                      = containerJson.getNameIterator();
    Map<String, MutableJsonObject>  singleTenantInitMap     = new HashMap<>();
    Map<String, MutableJsonObject>  multiTenantInitMap      = new HashMap<>();
    Map<String, MutableJsonObject>  singleTenantServiceMap  = new HashMap<>();
    Map<String, MutableJsonObject>  multiTenantServiceMap   = new HashMap<>();
    
    while(it.hasNext())
    {
      String name = it.next();
      IJsonDomNode c = containerJson.get(name);
      
      if(c instanceof MutableJsonObject)
      {
        MutableJsonObject container = (MutableJsonObject)c;
        
        String tenancy = container.getRequiredString("tenancy");
        
        boolean singleTenant = "SINGLE".equals(tenancy);
        
        if("INIT".equals(container.getString("containerType", "SERVICE")))
        {
          if(singleTenant)
            singleTenantInitMap.put(name, container);
          else
            multiTenantInitMap.put(name, container);
        }
        else
        {
          if(singleTenant)
            singleTenantServiceMap.put(name, container);
          else
            multiTenantServiceMap.put(name, container);
        }
      }
    }
    
    deployService(!singleTenantServiceMap.isEmpty(), !multiTenantServiceMap.isEmpty());
    
    deployInitContainers(multiTenantInitMap, null);
    
    if(tenant_ != null)
    {
      deployInitContainers(singleTenantInitMap, tenant_);
    }
    
    deployServiceContainers(multiTenantServiceMap, null);
    
    if(tenant_ != null)
    {
      deployServiceContainers(singleTenantServiceMap, tenant_);
    }
    
  }
  
  private void deployServiceContainers(Map<String, MutableJsonObject> map, String tenantId)
  {
    for(String name : map.keySet())
    {
      MutableJsonObject container = map.get(name);
      
      deployServiceContainer(name, container, tenantId);
    }
  }
  
  private void deployInitContainers(Map<String, MutableJsonObject> map, String tenantId)
  {
    for(String name : map.keySet())
    {
      MutableJsonObject container = map.get(name);
      
      deployInitContainer(name, container, tenantId);
    }
  }
  
  private void deployInitContainer(String name, MutableJsonObject container, String tenantId)
  {
    // TODO Auto-generated method stub
    
  }
  
  private void deployServiceContainer(String name, MutableJsonObject container, String tenantId)
  {
    try
    {
      IJsonDomNode        portNode = container.get(PORT);
      int                 port = portNode == null ? 80 : TypeAdaptor.adapt(Integer.class, portNode);
      Collection<String>  paths = getListOfStrings(container, PATHS);
      String              healthCheckPath = container.getString(HEALTH_CHECK_PATH, "/HealthCheck");
      
      deployServiceContainer(name, port, paths, healthCheckPath, tenantId);
    }
    catch(InvalidValueException e)
    {
      throw new IllegalStateException(e);
    }
  }

  private Collection<String> getListOfStrings(MutableJsonObject object, String name) throws InvalidValueException
  {
    List<String> result = new LinkedList<>();
    IJsonDomNode node = object.get(name); 
    
    if(node instanceof IJsonArray)
    {
      for(IJsonDomNode v : ((IJsonArray<?>)node))
        result.add( TypeAdaptor.adapt(String.class, v));
    }
    
    return result;
  }
  
  private void processPolicies(String parentDir, String subDir, String tenant) throws IOException
  {
    String            dir   = parentDir + "/" + subDir;
    List<String>      files = provider_.fetchFiles(dir);
    StringSubstitutor sub   = new StringSubstitutor(templateVariables_);
    
    for(String file : files)
    {
      if(file.endsWith(DOT_JSON))
      {
        String name     = file.substring(0, file.length() - DOT_JSON.length());
        String template = provider_.fetchConfig(dir, file).immutify().toString();
        
        String roleSpec = sub.replace(template);
        
        processRole(name, roleSpec, tenant);
      }
      else
        throw new IllegalStateException("Unrecognized file type found in config: " + dir + "/" + file);
    }
  }
  
  private void fetchConfig()
  {
    multiTenantConfigDom_ = new MutableJsonDom();
    multiTenantConfig_    = new MutableJsonObject();
    multiTenantConfigDom_.add(multiTenantConfig_);
    
    if(tenant_ != null)
    {
      singleTenantConfigDom_ = new MutableJsonDom();
      singleTenantConfig_    = new MutableJsonObject();
      singleTenantConfigDom_.add(singleTenantConfig_);
      
      tenantConfig_ = new MutableJsonObject();
      
      try
      {
        fetch(true, tenantConfig_, CONFIG + "/" + TENANT, tenant_, "tenant", tenant_);
      }
      catch(IOException e)
      {
        throw new IllegalStateException("Unable to read tenant config", e);
      }
      
      singleTenantConfig_.addAll(tenantConfig_);
    }
    
    fetchService();
    
    if(tenant_ != null)
    {
      // ensure that our stuff was not overwritten by consul.
      
      singleTenantConfig_.addAll(tenantConfig_);
    }
    
    configId_ = fetchOverrides();
    
    regionShortCode_ = multiTenantConfig_.getRequiredString(REGION_SHORTCODE);
    
    Iterator<String> it = configId_.getNameIterator();
    
    while(it.hasNext())
    {
      String name = it.next();
      IJsonDomNode value = configId_.get(name);
      
      if(value instanceof IStringProvider)
      {
        templateVariables_.put(name, ((IStringProvider)value).asString());
      }      
    }
    
    templateVariables_.put(FQ_INSTANCE_NAME, new Name(environmentType_, environment_, realm_, region_, tenant_, service_).toString());
    templateVariables_.put(FQ_SERVICE_NAME, new Name(environmentType_, environment_, realm_, region_, service_).toString());
    templateVariables_.put(SHORT_INSTANCE_NAME, new Name(regionShortCode_, tenant_, service_).toString());
  }


  private void fetchDefaults()
  {
    provider_.fetchDefaults(multiTenantConfig_, singleTenantConfig_, templateVariables_);
    
    for(ConfigHelper helper : helpers_)
      helper.fetchDefaults(multiTenantConfig_, singleTenantConfig_, templateVariables_);
  }
  
  private MutableJsonObject fetchOverrides()
  {
    MutableJsonObject id  = (MutableJsonObject) multiTenantConfig_.get(ID);
    
    if(id == null)
    {
      id = new MutableJsonObject();
      multiTenantConfig_.add(ID, id);
    }
    
    populateId(id);
   
    
    if(singleTenantConfig_ != null)
    {
      id  = (MutableJsonObject) singleTenantConfig_.get(ID);
      
      if(id == null)
      {
        id = new MutableJsonObject();
        singleTenantConfig_.add(ID, id);
      }
      
      populateId(id);

      id.addIfNotNull(TENANT + ID_SUFFIX,       getTenant());
    }
    
    for(ConfigHelper helper : helpers_)
      helper.fetchOverrides(multiTenantConfig_, singleTenantConfig_, templateVariables_);
    
    provider_.fetchOverrides(multiTenantConfig_, singleTenantConfig_, templateVariables_);
    
    
    
    
    return id;
  }
  
  private void populateId(MutableJsonObject id)
  {
    id.addIfNotNull(ENVIRONMENT + ID_SUFFIX,  getEnvironment());
    id.addIfNotNull(ENVIRONMENT_TYPE,         getEnvironmentType());
    id.addIfNotNull(REALM + ID_SUFFIX,        getRealm());
    id.addIfNotNull(REGION + ID_SUFFIX,       getRegion());
    id.addIfNotNull(SERVICE + ID_SUFFIX,      getService());
  }
  
  private void fetchService()
  {
    fetchDefaults();
    
//    if(service_ != null)
//    {
//      MutableJsonObject serviceJson = provider_.fetchConfig(SERVICE_DIR, service_ + ".json");
//      
//      
//      System.out.println("Service=" + serviceJson.immutify());
//      
// We have removed service config, these are really just default values which should be managed by the service in source code.      
//      IJsonDomNode serviceRepoStr = serviceJson.get("repo");
//      
//      if(serviceRepoStr instanceof IStringProvider)
//      {
//        URL serviceRepoUrl = new URL(((IStringProvider)serviceRepoStr).asString());
//        
//        DeployConfig serviceDeployConfig = getServiceConfig(serviceRepoUrl);
//        
//        json.addAll(serviceDeployConfig.fetch(false), "#");
//      }
//    }
    
    try
    {
      multiTenantConfig_.addAll(fetch(true), "#");
      
      if(singleTenantConfig_ != null)
        singleTenantConfig_.addAll(fetch(true), "#");
    }
    catch(IOException e)
    {
      throw new IllegalStateException("Unable to read config", e);
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
  public String loadTemplateFromResource(String fileName)
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
  public String loadTemplateFromStream(InputStream template, String fileName)
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
  
//  private void processService(MutableJsonObject serviceJson)
//  {
//    processServiceRoles(serviceJson);
//  }
//
//  private void processServiceRoles(MutableJsonObject serviceJson)
//  {
//    IJsonDomNode roles = serviceJson.get(ROLES);
//    
//    if(roles instanceof IJsonObject)
//    {
//      IJsonObject<?> rolesObject = (IJsonObject<?>)roles;
//      Iterator<String> it = rolesObject.getNameIterator();
//      
//      while(it.hasNext())
//      {
//        String name = it.next();
//        IJsonDomNode roleNode = rolesObject.get(name);
//        
//        if(roleNode instanceof IJsonObject)
//        {
//          IJsonDomNode role = ((IJsonObject<?>)roleNode).get(cloudServiceProvider_);
//          
//          if(role == null)
//          {
//            throw new IllegalArgumentException("Role \"" + name + "\" has no definition for CSP " + cloudServiceProvider_);
//          }
//          
//          String template = role.immutify().toString();
//          StringSubstitutor sub = new StringSubstitutor(templateVariables_);
//          String roleSpec = sub.replace(template);
//          
//          processRole(name, roleSpec);
//        }
//        else
//        {
//          throw new IllegalArgumentException("Role \"" + name + "\" must be an object");
//        }
//      }
//    }
//    else
//    {
//      throw new IllegalArgumentException("Roles must be an object");
//    }
//  }


  private MutableJsonObject fetch(boolean required) throws IOException
  {  
    MutableJsonObject json  = new MutableJsonObject();
    
    String dir = CONFIG;
    
    fetch(false, json, dir, DEFAULTS, "defaults", "");
    
    dir = dir + "/" + ENVIRONMENT + "/" + environmentType_;
    
    fetch(required, json, dir, ENVIRONMENT_TYPE, "environment type", environmentType_);  
    
    dir = dir + "/" + environment_;
    
    fetch(required, json, dir, ENVIRONMENT, "environment", environment_);
    
    dir = dir + "/" + realm_;
    
    fetch(required, json, dir, REALM, "realm", realm_);
    
    dir = dir + "/" + region_;
    
    fetch(required, json, dir, REGION, "region", region_);
    
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


  
  private void fetch(boolean required, MutableJsonObject json, String dir, String fileName, String entityType, String entityName) throws IOException
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
    
//    try
//    {
//      json.addAll(provider_.fetchConfig(dir, action_ + DOT_JSON), "#");
//      log_.info("Loaded " + entityType + " action config for " + action_);
//    }
//    catch(FileNotFoundException e)
//    {
//      log_.debug("No " + entityType + " action config for " + action_);
//    }
  }

//  private void mergeAllFiles(MutableJsonObject json, String folderName) throws IOException
//  {
//    for(String file : provider_.fetchFiles(folderName))
//    {
//      MutableJsonObject config = provider_.fetchConfig(folderName, file);
//    
//      log_.info("config from " + file + " = " + config);
//      
//      json.addAll(config, "#");
//    }
//  }
//
  
  protected String getConfigName(String tenant)
  {
    return new Name(getEnvironmentType(), getEnvironment(), getRealm(), getRegion(), tenant, getService()).toString();
  }
}
