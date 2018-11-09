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

/**
 * A NO OP implementation of ITraceContextTransaction
 * 
 * @author Bruce Skingle
 *
 */
public class NoOpTraceContextTransaction implements ITraceContextTransaction
{
  /**
   * The singleton instance.
   */
  public static final NoOpTraceContextTransaction INSTANCE = new NoOpTraceContextTransaction();
  
  private NoOpTraceContextTransaction()
  {
  }

  @Override
  public NoOpTraceContext open()
  {
    return NoOpTraceContext.INSTANCE;
  }

  @Override
  public void finished()
  {
    // NO OP
  }

  @Override
  public void aborted()
  {
    // NO OP
  }

  @Override
  public void close()
  {
    // NO OP
  }
}
