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

package org.symphonyoss.s2.fugue.naming;

public class CredentialName extends Name
{
  private static final String SUFFIX = "cred";

  private final String environmentTypeId_;
  private final String environmentId_;
  private final String realmId_;
  private final String tenantId_;
  private final String owner_;

//  public CredentialName(String environmentId, String owner)
//  {
//    this(environmentId, null, null, owner);
//  }
//  
//  public CredentialName(String realmId, String environmentId, String owner)
//  {
//    this(realmId, environmentId, null, owner);
//  }
  
  public CredentialName(String environmentTypeId, String environmentId, String realmId, String tenantId, String owner)
  {
    super(environmentTypeId, environmentId, realmId, tenantId, owner, SUFFIX);

    environmentTypeId_ = environmentTypeId;
    environmentId_ = environmentId;
    realmId_ = realmId;
    tenantId_ = tenantId;
    owner_ = owner;
  }

  public String getEnvironmentTypeId()
  {
    return environmentTypeId_;
  }

  public String getEnvironmentId()
  {
    return environmentId_;
  }

  public String getRealmId()
  {
    return realmId_;
  }

  public String getTenantId()
  {
    return tenantId_;
  }

  public String getOwner()
  {
    return owner_;
  }

  
}
