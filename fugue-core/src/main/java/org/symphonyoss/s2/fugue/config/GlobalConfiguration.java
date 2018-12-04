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

import com.fasterxml.jackson.databind.JsonNode;

public class GlobalConfiguration extends ConfigurationDelegator implements IGlobalConfiguration
{
  // Top level keys in the flattened configuration
  public static final String     ID               = "id";

  // Keys in the ID configuration
  private static final String    ENVIRONMENT_TYPE = "environmentType";
  private static final String    ENVIRONMENT_ID   = "environmentId";
  private static final String    REALM_ID         = "realmId";
  private static final String    REGION_ID        = "regionId";
  private static final String    TENANT_ID        = "tenantId";
  private static final String    SERVICE_ID       = "serviceId";

  private final String           globalNamePrefix_;
  private final String           environmentType_;
  private final String           environmentId_;
  private final String           realmId_;
  private final String           regionId_;
  private final String           tenantId_;
  private final String           serviceId_;

  protected final IConfiguration id_;
  
  public GlobalConfiguration(IConfiguration delegate)
  {
    this(delegate, "");
  }
  
  public GlobalConfiguration(IConfiguration delegate, String globalNamePrefix)
  {
    super(delegate);
    
    globalNamePrefix_ = globalNamePrefix;
    
    id_ = delegate.getConfiguration(ID);
    
    environmentType_  = id_.getRequiredString(ENVIRONMENT_TYPE);
    environmentId_    = id_.getRequiredString(ENVIRONMENT_ID);
    realmId_          = id_.getRequiredString(REALM_ID);
    regionId_         = id_.getRequiredString(REGION_ID);
    tenantId_         = id_.getString(TENANT_ID, null);
    serviceId_        = id_.getRequiredString(SERVICE_ID);
  }
  
//  public IConfiguration getId()
//  {
//    return id_;
//  }

  @Override
  public String getGlobalNamePrefix()
  {
    return globalNamePrefix_;
  }

  @Override
  public String getEnvironmentType()
  {
    return environmentType_;
  }

  @Override
  public String getEnvironmentId()
  {
    return environmentId_;
  }

  @Override
  @Deprecated
  public String getRealmId()
  {
    return realmId_;
  }

  @Override
  public String getRegionId()
  {
    return regionId_;
  }

  @Override
  public String getTenantId()
  {
    return tenantId_;
  }

  @Override
  public String getServiceId()
  {
    return serviceId_;
  }

  @Override
  public String getCloudServiceProviderId()
  {
    // TODO FIXME: restructure configuration
    return "amazon";
  }

  @Override
  public String getCloudRegionId()
  {
 // TODO FIXME: restructure configuration
    return "us-east-1";
  }
}
