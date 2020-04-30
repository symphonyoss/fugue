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

package org.symphonyoss.s2.fugue.pipeline;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A consumer of some payload which cannot be processed normally.
 * 
 * Implementations of this interface may, or may not, be thread
 * safe. Implementations which <i>are</i> thread safe should
 * implement {@link IThreadSafeErrorConsumer}.
 * 
 * Callers <b>MUST NOT</b> call methods on this interface concurrently
 * from multiple threads, they <b>MUST</b> require an {@link IThreadSafeErrorConsumer}
 * to do so.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of payload consumed.
 */
@NotThreadSafe
@FunctionalInterface
public interface ISimpleErrorConsumer<T>
{
  /**
   * Consume the given item which cannot be processed.
   * 
   * The given item has already failed to process normally, presumably because a call to
   * IConsumer.consume() from some normal processing consumer threw an exception.
   * 
   * If the implementation of this method throws a RuntimeException then the caller
   * <i>should</i> retry, but given that this is already a failure scenario implementations
   * should avoid throwing any kind of exception if at all possible. 
   * 
   * @param item The item which failed to be consumed normally.
   * @param trace A trace context.
   * @param message A diagnostic message.
   * @param cause A throwable indicating the cause of the failure to process normally.
   */
  void consume(T item, ITraceContext trace, @Nullable String message, @Nullable Throwable cause);
}
