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

/**
 * Actions for the FugueDeploy utility.
 * 
 * @author Bruce Skingle
 *
 */
@SuppressWarnings("javadoc")
public enum FugueDeployAction
{
  
  CreateEnvironmentType(  true,  false, true,  false,  false),
  CreateEnvironment(      true,  false, true,  false,  false),
  CreateRegion(           true,  false, true,  false,  false),
  DeployConfig(           true,  false, true,  false,  false),
  Deploy(                 true,  true, true,  false,  true),
  Undeploy(               false,  true, false,  true,  false),
  UndeployAll(            false,  true, false,  true,  true);
  
  public final boolean  processConfig_;
  public final boolean  processContainers_;
  public final boolean  processMultiTenant_;
  public final boolean  isDeploy_;
  public final boolean  isUndeploy_;
  private FugueDeployAction(boolean processConfig, boolean processContainers, boolean isDeploy, boolean isUndeploy, boolean processMultiTenant)
  {
    processConfig_ = processConfig;
    processContainers_ = processContainers;
    isDeploy_ = isDeploy;
    isUndeploy_ = isUndeploy;
    processMultiTenant_ = processMultiTenant;
  }
  
  
}
