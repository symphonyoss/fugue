/*
 *
 *
 * Copyright 2018-19 Symphony Communication Services, LLC.
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

import javax.annotation.Nullable;

/**
 * An exception indicating that a consumer failed to process the given payload but that
 * a retry might succeed.
 * 
 * This exception allows the thrower to indicate how long it thinks the caller should
 * wait before retrying.
 * 
 * @author Bruce Skingle
 *
 */
public class RetryableConsumerException extends Exception
{
  private static final long serialVersionUID = 1L;
  
  private final TimeUnit  retryTimeUnit_;
  private final Long      retryTime_;
  
  /**
   * Default constructor.
   */
  public RetryableConsumerException()
  {
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  /**
   * Constructs a new exception with the specified detail message.  The
   * cause is not initialized, and may subsequently be initialized by
   * a call to {@link #initCause}.
   *
   * @param   message   the detail message. The detail message is saved for
   *          later retrieval by the {@link #getMessage()} method.
   */
  public RetryableConsumerException(String message)
  {
    super(message);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  /**
   * Constructs a new exception with the specified cause and a detail
   * message of (cause==null ? null : cause.toString()) (which
   * typically contains the class and detail message of cause).
   * This constructor is useful for exceptions that are little more than
   * wrappers for other throwables (for example, {@link
   * java.security.PrivilegedActionException}).
   *
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A null value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   */
  public RetryableConsumerException(Throwable cause)
  {
    super(cause);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  /**
   * Constructs a new exception with the specified detail message and
   * cause.  <p>Note that the detail message associated with
   * {@code cause} is <i>not</i> automatically incorporated in
   * this exception's detail message.
   *
   * @param  message the detail message (which is saved for later retrieval
   *         by the {@link #getMessage()} method).
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A null value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   */
  public RetryableConsumerException(String message, Throwable cause)
  {
    super(message, cause);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }

  /**
   * Constructs a new exception with the specified detail message,
   * cause, suppression enabled or disabled, and writable stack
   * trace enabled or disabled.
   *
   * @param  message the detail message.
   * @param cause the cause.  (A {@code null} value is permitted,
   * and indicates that the cause is nonexistent or unknown.)
   * @param enableSuppression whether or not suppression is enabled
   *                          or disabled
   * @param writableStackTrace whether or not the stack trace should
   *                           be writable
   */
  public RetryableConsumerException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
    retryTimeUnit_ = null;
    retryTime_ = null;
  }
  
  /**
   * Constructor.
   * 
   * @param  retryTimeUnit The units of the given retryTime
   * @param  retryTime  The suggested amount of time which should be allowed
   *         before a retry of the operation throwing this exception.
   */
  public RetryableConsumerException(TimeUnit retryTimeUnit, Long retryTime)
  {
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  /**
   * Constructs a new exception with the specified detail message.  The
   * cause is not initialized, and may subsequently be initialized by
   * a call to {@link #initCause}.
   *
   * @param   message   the detail message. The detail message is saved for
   *          later retrieval by the {@link #getMessage()} method.
   * @param  retryTimeUnit The units of the given retryTime
   * @param  retryTime  The suggested amount of time which should be allowed
   *         before a retry of the operation throwing this exception.
   */
  public RetryableConsumerException(String message, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(message);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  /**
   * Constructs a new exception with the specified cause and a detail
   * message of (cause==null ? null : cause.toString()) (which
   * typically contains the class and detail message of cause).
   * This constructor is useful for exceptions that are little more than
   * wrappers for other throwables (for example, {@link
   * java.security.PrivilegedActionException}).
   *
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A null value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   * @param  retryTimeUnit The units of the given retryTime
   * @param  retryTime  The suggested amount of time which should be allowed
   *         before a retry of the operation throwing this exception.
   */
  public RetryableConsumerException(Throwable cause, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(cause);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  /**
   * Constructs a new exception with the specified detail message and
   * cause.  <p>Note that the detail message associated with
   * {@code cause} is <i>not</i> automatically incorporated in
   * this exception's detail message.
   *
   * @param  message the detail message (which is saved for later retrieval
   *         by the {@link #getMessage()} method).
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A null value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   * @param  retryTimeUnit The units of the given retryTime
   * @param  retryTime  The suggested amount of time which should be allowed
   *         before a retry of the operation throwing this exception.
   */
  public RetryableConsumerException(String message, Throwable cause, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(message, cause);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }


  /**
   * Constructs a new exception with the specified detail message,
   * cause, suppression enabled or disabled, and writable stack
   * trace enabled or disabled.
   *
   * @param  message the detail message.
   * @param cause the cause.  (A {@code null} value is permitted,
   * and indicates that the cause is nonexistent or unknown.)
   * @param enableSuppression whether or not suppression is enabled
   *                          or disabled
   * @param writableStackTrace whether or not the stack trace should
   *                           be writable
   * @param  retryTimeUnit The units of the given retryTime
   * @param  retryTime  The suggested amount of time which should be allowed
   *         before a retry of the operation throwing this exception.
   */
  public RetryableConsumerException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, TimeUnit retryTimeUnit, Long retryTime)
  {
    super(message, cause, enableSuppression, writableStackTrace);
    retryTimeUnit_ = retryTimeUnit;
    retryTime_ = retryTime;
  }

  /**
   * 
   * @return The retry time unit.
   */
  public @Nullable TimeUnit getRetryTimeUnit()
  {
    return retryTimeUnit_;
  }

  /**
   * 
   * @return The suggested amount of time which should be allowed
   *         before a retry of the operation throwing this exception.
   */
  public @Nullable Long getRetryTime()
  {
    return retryTime_;
  }

}
