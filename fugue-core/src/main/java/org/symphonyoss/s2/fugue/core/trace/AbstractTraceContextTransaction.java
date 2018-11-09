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
 * A base implementation of ITraceContextTransaction.
 * 
 * Real implementations will need to override the close method to record the transaction.
 * 
 * @author Bruce Skingle
 * @param <C> The type of the ITraceContext
 *
 */
public abstract class AbstractTraceContextTransaction<C extends ITraceContext> implements ITraceContextTransaction
{
  private final C context_;
  private boolean opened_;
  private boolean completed_;
  
  /**
   * Constructor.
   * 
   * @param context The ITraceContext for this transaction.
   */
  public AbstractTraceContextTransaction(C context)
  {
    context_ = context;
  }
  
  protected C getContext()
  {
    return context_;
  }

  @Override
  public C open()
  {
    if(opened_)
      throw new IllegalStateException("Already opened");
    
    opened_ = true;
    
    return context_;
  }

  @Override
  public void finished()
  {
    complete(FINISHED);
  }
  
  @Override
  public void aborted()
  {
    complete(ABORTED);
  }
  
  private void complete(String state)
  {
    if(completed_)
      throw new IllegalStateException("Trace context is already completed");
    
    context_.trace(state);
    completed_ = true;
  }

  @Override
  public void close()
  {
    if(!completed_)
      finished();
  }
}
