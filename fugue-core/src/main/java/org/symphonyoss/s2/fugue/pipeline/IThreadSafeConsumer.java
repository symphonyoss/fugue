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

import javax.annotation.concurrent.ThreadSafe;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A thread safe consumer of some payload.
 * 
 * Implementations of this interface <b>MUST</b> be thread
 * safe. Implementations which <i>are not</i> thread safe <b>MUST</b>
 * implement {@link IConsumer} instead.
 * 
 * Callers can safely call the consume method multiple times concurrently from
 * different threads.
 * 
 * Note that it is only the consume method of this interface which is thread safe
 * and this interface is not {@link ThreadSafe} (as in javax.annotation.ThreadSafe)
 * because it is an error to call consume after close.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of payload consumed.
 */
public interface IThreadSafeConsumer<T> extends IThreadSafeRetryableConsumer<T>, IConsumer<T>
{
  /**
   * Consume the given item.
   * 
   * A normal return from this method indicates that the item has been fully processed,
   * and the provider can discard the item. In the event that the item cannot be
   * processed then the implementation must throw some kind of Exception.
   * 
   * @param item The item to be consumed.
   * @param trace A trace context.
   */
  @Override
  void consume(T item, ITraceContext trace);
}