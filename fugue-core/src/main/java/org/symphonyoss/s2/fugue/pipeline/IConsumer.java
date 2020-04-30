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

import javax.annotation.concurrent.NotThreadSafe;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A consumer of some payload.
 * 
 * Implementations of this interface may, or may not, be thread
 * safe. Implementations which <i>are</i> thread safe should
 * implement {@link IThreadSafeConsumer}.
 * 
 * Callers <b>MUST NOT</b> call methods on this interface concurrently
 * from multiple threads, they <b>MUST</b> require an {@link IThreadSafeConsumer}
 * to do so.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of payload consumed.
 */
@NotThreadSafe
public interface IConsumer<T> extends ISimpleRetryableConsumer<T>, ICloseableConsumer
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
