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

import java.io.IOException;
import java.time.Instant;

import org.symphonyoss.s2.common.hash.Hash;

/**
 * A NO OP implementation of ITraceContext
 * 
 * @author Bruce Skingle
 *
 */
public class NoOpTraceContext implements ITraceContext
{
  /**
   * The singleton instance.
   */
  public static final NoOpTraceContext INSTANCE = new NoOpTraceContext();
  
  private NoOpTraceContext()
  {}
  
  @Override
  public void trace(String operationId)
  {
  }

  @Override
  public void trace(String operationId, Instant time)
  {
  }

  @Override
  public void trace(String operationId, String subjectType, String subjectId)
  {    
  }

  @Override
  public Instant getTimestamp()
  {
    return Instant.EPOCH;
  }

  @Override
  public ITraceContextTransaction createSubContext(String externalSubjectType, String externalSubjectId)
  {
    return NoOpTraceContextTransaction.INSTANCE;
  }

  @Override
  public ITraceContextTransaction createSubContext(String subjectType, String subjectId, String tenantId)
  {
    return NoOpTraceContextTransaction.INSTANCE;
  }

  @Override
  public ITraceContextTransaction createSubContext(String subjectType, String subjectId, Instant time)
  {
    return NoOpTraceContextTransaction.INSTANCE;
  }

  @Override
  public ITraceContextTransaction createSubContext(String subjectType, String subjectId, String tenantId, Instant time)
  {
    return NoOpTraceContextTransaction.INSTANCE;
  }

  @Override
  public Hash getHash()
  {
    return Hash.NIL_HASH;
  }

  @Override
  public void setCounter(int count)
  {
    // NO OP
  }
}
