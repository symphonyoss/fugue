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

package org.symphonyoss.s2.fugue.aws.sqs;

import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;

/**
 * AWS SQS implementation of SubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
public class SqsSubscriberManager extends SqsAbstractSubscriberManager<SqsSubscriberManager>
{
  /**
   * Constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param config                          The configuration to use.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public SqsSubscriberManager(INameFactory nameFactory, IConfigurationProvider config,
      ITraceContextFactory traceFactory,
      IThreadSafeConsumer<String> unprocessableMessageConsumer)
  {
    super(SqsSubscriberManager.class, nameFactory, config, traceFactory, unprocessableMessageConsumer);
  }
}
