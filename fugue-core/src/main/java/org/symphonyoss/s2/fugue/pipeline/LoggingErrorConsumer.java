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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * An implementation of IThreadSafeErrorConsumer which writes items to the log.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of payload consumed.
 */
public class LoggingErrorConsumer<T> implements IThreadSafeErrorConsumer<T>
{
  private static final Logger log_ = LoggerFactory.getLogger(LoggingErrorConsumer.class);
  
  private final String consumerName_;

  /**
   * Constructor.
   * 
   * @param consumerName A message which is written to each log entry. In many cases a class will
   * require more than one Error consumer and this can be used to distinguish between types of failures.
   */
  public LoggingErrorConsumer(String consumerName)
  {
    consumerName_ = consumerName;
  }

  @Override
  public void consume(T item, ITraceContext trace, String message, Throwable cause)
  {
    log_.error(consumerName_ + " " + message + "\n" + item, cause);
  }
  
  @Override
  public void close()
  {
  }
}
