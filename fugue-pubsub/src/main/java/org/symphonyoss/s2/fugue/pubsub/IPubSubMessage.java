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

package org.symphonyoss.s2.fugue.pubsub;

import java.util.Map;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A payload sent or received over a pub sub channel.
 * 
 * @author Bruce Skingle
 */
public interface IPubSubMessage
{
  static final String PAYLOAD_TYPE_ATTRIBUTE = "payloadType";
  static final String TENANT_ID_ATTRIBUTE    = "tenantId";
  
  /**
   * 
   * @return The message payload
   */
  @Nonnull String getPayload();
  
  /**
   * 
   * @return Any optional attributes. If the object was created with null attributes an empty map is returned.
   */
  @Nonnull Map<String, String> getAttributes();

  /**
   * 
   * @return The trace context.
   */
  @Nonnull ITraceContext getTraceContext();

}
