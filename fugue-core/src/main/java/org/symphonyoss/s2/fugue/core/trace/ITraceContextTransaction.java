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

import java.io.Closeable;

/**
 * A Trace context against which events, or operations can be logged to gain a picture of the end to end business process of
 * a subject object across potentially multiple services.
 * 
 * The default implementation emits pubsub messages for each context and each event although other implementations
 * are possible.
 * 
 * This class implements Closable and it is recommended that it should be created in a try-with-resources block.
 * 
 * The close method implies aborted() unless aborted() or finished() has been called, so it is important to call
 * finished() as the last statement in the try-with-resource block.
 * 
 * Typical usage is:
 * <code>
    ITraceContextTransactionFactory factory;
    String                          subjectId;
    
    try(ITraceContextTransaction traceTransaction = factory.createTransaction("Subject Type", subjectId))
    {
      ITraceContext trace = traceTransaction.open()
      // Business logic here
      trace("SOME_EVENT")
      // More business logic, maybe multiple calls to trace

      traceTransaction.finished(); // if an exception id thrown before here then the close will abort the transaction.
    }
 * </code>
 * 
 * Calling the open() method on this class returns an ITraceContext on which events may be logged. This ITraceContext
 * should be passed to other methods rather than passing this class directly. This avoids the possibility that called code 
 * closes the ITraceContextTransaction by mistake.
 * 
 * @author Bruce Skingle
 *
 */
public interface ITraceContextTransaction extends Closeable
{
  /** Standard normal termination of context operation */
  String STARTED = "TRACE_STARTED";
  
  /** Standard normal termination of context operation */
  String FINISHED = "TRACE_FINISHED";

  /** Standard abnormal termination of context operation */
  String ABORTED = "TRACE_ABORTED";

  /**
   * Open the transaction
   * @throws IllegalStateException If the transaction is already open
   * @return An ITraceContext on which events can be logged. 
   */
  ITraceContext open();
  
//  /**
//   * 
//   * @return The Hash (ID) of this Trace Context.
//   */
//  Hash getHash();
//  
//  /**
//   * 
//   * @return The timestamp (start time) of this trace context.
//   */
//  Instant getTimestamp();
  
  /**
   * Record the normal completion of a trace context.
   * 
   * This is implied by calling close() unless aborted() or finished() have already been called.
   * @throws IllegalStateException If the transaction is already closed.
   */
  void finished();
  
  /**
   * Record the abnormal completion of a trace context.
   * @throws IllegalStateException If the transaction is already closed.
   */
  void aborted();
  
  /**
   * Closes the transaction, implies finished() unless finished or aborted have already been called.
   */
  @Override
  void close();
}
