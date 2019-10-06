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

/**
 * A closeable consumer of some payload.
 * 
 * @author Bruce Skingle
 */
@NotThreadSafe
public interface ICloseableConsumer extends AutoCloseable
{
  /**
   * An indication that all items have been presented.
   * 
   * It is an error to call consume() after this method has been called.
   * 
   * The implementation may release resources when this method is called.
   * Note that although this interface extends {@link AutoCloseable}
   * (with the effect that an IConsumer can be used in a try with resources
   * statement) that it throws no checked exceptions.
   * 
   * Since there is nothing the calling code can do about a failure to close
   * something it seems to be incorrect to declare a close method to throw
   * any checked exception.
   * 
   * It would, however, be appropriate for an implementation to throw an unchecked
   * exception such a {@link IllegalStateException} if this method is called twice
   * although it would also not be incorrect to allow close after successful
   * close to be a no op.
   */
  @Override
  void close();
}
