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

import java.util.concurrent.TimeUnit;

/**
 * An exception indicating that a consumer failed to process the given payload but that
 * a retry might succeed.
 * 
 * This exception allows the thrower to inidcate how long it things the caller should
 * wait before retrying.
 * 
 * @author Bruce Skingle
 *
 */
public class RetryableConsumerException extends Exception
{
  private final TimeUnit  retryTimeUnit_;
  private final Long      retryTime_;
  
  public RetryableConsumerException()
  {
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  public RetryableConsumerException(String message)
  {
    super(message);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  public RetryableConsumerException(Throwable cause)
  {
    super(cause);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  public RetryableConsumerException(String message, Throwable cause)
  {
    super(message, cause);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  public RetryableConsumerException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }
  
  public RetryableConsumerException(TimeUnit retryTimeUnit, Long retryTime)
  {
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  public RetryableConsumerException(String message, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(message);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  public RetryableConsumerException(Throwable cause, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(cause);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  public RetryableConsumerException(String message, Throwable cause, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(message, cause);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  public RetryableConsumerException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(message, cause, enableSuppression, writableStackTrace);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  public TimeUnit getRetryTimeUnit()
  {
    return retryTimeUnit_;
  }

  public Long getRetryTime()
  {
    return retryTime_;
  }

}
