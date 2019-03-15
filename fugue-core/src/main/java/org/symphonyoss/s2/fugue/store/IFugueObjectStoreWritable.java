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

package org.symphonyoss.s2.fugue.store;

import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A low level database within which Fugue can be stored.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueObjectStoreWritable extends IFugueObjectStoreSecondaryWritable
{
  /**
   * Save the given object.
   * 
   * @param object An object to be stored.
   * @param trace  A trace context.
   */
  void save(IFugueObject object, ITraceContext trace);

  /**
   * If the given ID object does not exist then save it and all of the additional objects in a single transaction and return null,
   * otherwise return the existing object.
   * 
   * @param idObject          An ID object.
   * @param trace             A trace context.
   * @param additionalObjects Additional objects to be stored if the given ID obejct does not already exist.
   * 
   * @return The existing ID object or null.
   */
  @Nullable byte[] saveIfNotExists(IFugueObject idObject, ITraceContext trace,
      IFugueObject ...additionalObjects);
}
