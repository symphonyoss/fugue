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

public class GlobalConfigurationDelegator extends ConfigurationDelegator implements IGlobalConfiguration
{
  private final IGlobalConfiguration delegate_;

  public GlobalConfigurationDelegator(IGlobalConfiguration delegate)
  {
    super(delegate);
    
    delegate_ = delegate;
  }

  @Override
  public String getGlobalNamePrefix()
  {
    return delegate_.getGlobalNamePrefix();
  }

  @Override
  public String getEnvironmentType()
  {
    return delegate_.getEnvironmentType();
  }

  @Override
  public String getEnvironmentId()
  {
    return delegate_.getEnvironmentId();
  }

  @Override
  public String getRegionId()
  {
    return delegate_.getRegionId();
  }

  @Override
  public String getPodName()
  {
    return delegate_.getPodName();
  }

  @Override
  public Integer getPodId()
  {
    return delegate_.getPodId();
  }

  @Override
  public String getServiceId()
  {
    return delegate_.getServiceId();
  }

  @Override
  public String getCloudServiceProviderId()
  {
    return delegate_.getCloudServiceProviderId();
  }

  @Override
  public String getCloudRegionId()
  {
    return delegate_.getCloudRegionId();
  }
}
