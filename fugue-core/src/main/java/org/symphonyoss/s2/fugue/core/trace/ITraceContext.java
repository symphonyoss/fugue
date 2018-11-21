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

package org.symphonyoss.s2.fugue.core.trace;

import java.time.Instant;

import org.symphonyoss.s2.common.hash.Hash;

/**
 * A Trace context against which events, or operations can be logged to gain a picture of the end to end business process of
 * a subject object across potentially multiple services.
 * 
 * The default implementation emits pubsub messages for each context and each event although other implementations
 * are possible.
 * 
 * @author Bruce Skingle
 *
 */
public interface ITraceContext
{
  /**
   * 
   * @return The Hash (ID) of this Trace Context.
   */
  Hash getHash();
  
  /**
   * 
   * @return The timestamp (start time) of this trace context.
   */
  Instant getTimestamp();

  /**
   * Record an operation having taken place within a trace context.
   * 
   * @param operationId The operation ID, which can be any String the caller chooses including one of the standard
   * values defined in ITraceContext
   */
  void trace(String operationId);
  
  /**
   * Record an operation having taken place within a trace context.
   * 
   * @param operationId The operation ID, which can be any String the caller chooses including one of the standard
   * values defined in ITraceContext
   * @param time The time at which the event took place.
   */
  void trace(String operationId, Instant time);
  
  /**
   * Record an operation having taken place within a trace context.
   * 
   * @param operationId The operation ID, which can be any String the caller chooses including one of the standard
   * values defined in ITraceContext
   * @param subjectType The type of the subject of this operation
   * @param subjectId The id of the subject of this operation.
   */
  void trace(String operationId, String subjectType, String subjectId);

  /**
   * Create a sub-context relating to the processing of the given external subject.
   *  
   * @param subjectType The type of the subject of this process.
   * @param subjectId   The ID of the subject of this process.
   * 
   * @return A new ITraceContext which is a sub-context of the current context.
   */
  ITraceContextTransaction createSubContext(String subjectType, String subjectId);

  /**
   * Create a sub-context relating to the processing of the given external subject.
   *  
   * @param subjectType The type of the subject of this process.
   * @param subjectId   The ID of the subject of this process.
   * @param tenantId    The tenantId of the tenant for whom this transaction is working.
   * 
   * @return A new ITraceContext which is a sub-context of the current context.
   */
  ITraceContextTransaction createSubContext(String subjectType, String subjectId, String tenantId);

  /**
   * Create a sub-context relating to the processing of the given external subject.
   *  
   * @param subjectType The type of the subject of this process.
   * @param subjectId   The ID of the subject of this process.
   * @param time        The time at which the sub-transaction started.
   * 
   * @return A new ITraceContext which is a sub-context of the current context.
   */
  ITraceContextTransaction createSubContext(String subjectType, String subjectId, Instant time);

  /**
   * Create a sub-context relating to the processing of the given external subject.
   *  
   * @param subjectType The type of the subject of this process.
   * @param subjectId   The ID of the subject of this process.
   * @param tenantId    The tenantId of the tenant for whom this transaction is working.
   * @param time        The time at which the sub-transaction started.
   * 
   * @return A new ITraceContext which is a sub-context of the current context.
   */
  ITraceContextTransaction createSubContext(String subjectType, String subjectId, String tenantId, Instant time);
}
