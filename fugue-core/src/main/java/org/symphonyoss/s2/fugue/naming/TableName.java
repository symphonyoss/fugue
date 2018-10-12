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

import javax.annotation.Nonnull;

public class TableName extends Name
{
  private final String serviceId_;
  private final String tableId_;

  /**
   * Connstructor.
   * 
   * @param serviceId       The ID of the service which owns this table.
   * @param tableId         The tableId (simple name).
   * @param name            The first element of the actual name.
   * @param additional      Zero or more optional suffix elements.
   */
  protected TableName(String serviceId, String tableId, @Nonnull String name, String ...additional)
  {
    super(name, additional);
    
    serviceId_ = serviceId;
    tableId_ = tableId;
  }

  /**
   * 
   * @return The id of the service which owns this table.
   */
  public String getServiceId()
  {
    return serviceId_;
  }

  /**
   * 
   * @return The table ID (simple name)
   */
  public String getTableId()
  {
    return tableId_;
  }
  
  
}
