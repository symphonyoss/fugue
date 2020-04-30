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

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;

/**
 * Base class for config helpers and providers.
 * 
 * @author Bruce Skingle
 *
 */
public class ConfigHelper
{
  private FugueDeploy fugueDeploy_;

  /**
   * Init method, allows program parameters to be added.
   * 
   * @param fugueDeploy the main program to which parameters may be added.
   */
  public void init(FugueDeploy fugueDeploy)
  {
    fugueDeploy_ = fugueDeploy;
  }
  
  /**
   * Get the multi-tenant default config.
   * 
   * @param config The JsonObject onto which the config should be overlaid.
   * @param pathFilter A set of paths for which config is required.
   */
  public void overlayDefaults(@Nonnull MutableJsonObject config, Set<String> pathFilter)
  {
  }
  
  /**
   * Get the multi-tenant override config.
   * 
   * @param config The JsonObject onto which the config should be overlaid.
   * @param pathFilter A set of paths for which config is required.
   */
  public void overlayOverrides(@Nonnull MutableJsonObject config, Set<String> pathFilter)
  {
  }
  
  /**
   * Get default config for the given tenant.
   * 
   * @param tenantId            The ID of the required tenant configuration.
   * 
   * @param config The JsonObject onto which the config should be overlaid.
   * @param pathFilter A set of paths for which config is required.
   */
  public void overlayDefaults(@Nonnull String tenantId, @Nonnull MutableJsonObject config, Set<String> pathFilter)
  {
  }
  
  /**
   * Get override config for the given tenant.
   * 
   * @param tenantId The ID of the required tenant configuration.
   * @param config The JsonObject onto which the config should be overlaid.
   * @param pathFilter A set of paths for which config is required.
   */
  public void overlayOverrides(@Nonnull String tenantId, @Nonnull MutableJsonObject config, Set<String> pathFilter)
  {
  }
  
  /**
   * Populate any template variables needed from the given final config.
   * 
   * @param config              A final flattened config.
   * @param templateVariables   A map of template variables.
   */
  public void populateTemplateVariables(ImmutableJsonObject config, Map<String, String> templateVariables)
  {
  }
  
  protected String getService()
  {
    return fugueDeploy_.getService();
  }

  protected String getEnvironment()
  {
    return fugueDeploy_.getEnvironment();
  }

  protected String getRegion()
  {
    return fugueDeploy_.getRegion();
  }

  protected String getEnvironmentType()
  {
    return fugueDeploy_.getEnvironmentType();
  }
}
