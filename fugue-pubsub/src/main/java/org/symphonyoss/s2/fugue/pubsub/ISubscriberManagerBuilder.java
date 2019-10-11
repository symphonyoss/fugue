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

import org.symphonyoss.s2.common.fluent.IBuilder;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;

/**
 * A builder for a subscriber manager of payload type P.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete builder, needed for fluent methods.
 * @param <P> Type of the payloads processed.
 * @param <B> Type of concrete manager (built object), needed for fluent methods.
 */
public interface ISubscriberManagerBuilder<T extends ISubscriberManagerBuilder<T,P,B>, P, B extends ISubscriberManager<B>> extends IBuilder<T,B>
{
  /**
   * Set the name factory to be used.
   * 
   * @param nameFactory The name factory to be used.
   * 
   * @return this (fluent method)
   */
  T withNameFactory(INameFactory nameFactory);

  /**
   * Set the configuration to be used.
   * 
   * @param config The configuration to be used.
   * 
   * @return this (fluent method)
   */
  T withConfig(IConfiguration config);

  /**
   * Set the metric counter to be used.
   * 
   * @param counter The metric counter to be used.
   * 
   * @return this (fluent method)
   */
  T withCounter(ICounter counter);

  /**
   * Set the trace context transaction factory to be used.
   * 
   * @param traceFactory The trace context transaction factory to be used.
   * 
   * @return this (fluent method)
   */
  T withTraceContextTransactionFactory(ITraceContextTransactionFactory traceFactory);

  /**
   * Set the consumer for unprocessable messages.
   * 
   * Messages will be sent to this consumer immediately of the handler throws a fatal exception and possibly after
   * some number of retries in the case of a retryable exception.
   * 
   * @param unprocessableMessageConsumer The consumer for unprocessable messages.
   * 
   * @return this (fluent method)
   */
  T withUnprocessableMessageConsumer(IThreadSafeErrorConsumer<P> unprocessableMessageConsumer);

  /**
   * 
   * @param subscription
   * 
   * @return this (fluent method)
   */
  T withSubscription(ISubscription<P> subscription);
}
