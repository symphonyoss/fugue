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

import java.time.Instant;
import java.util.List;

import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A low level store within which Fugue Objects can be stored.
 * 
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueObjectStoreSecondaryWritable extends IFugueObjectStoreReadOnly
{
  /**
   * Save the given object to secondary storage.
   * 
   * @param absoluteHash            The absolute hash of the object to store.
   * @param payload                 The payload (if available).
   * @param saveToSecondary         Iff true then save to secondary storage.
   * @param trace                   A trace context.
   */
  void saveToSecondaryStorage(Hash absoluteHash, IFugueObject payload, boolean saveToSecondary, ITraceContext trace);
  
  /**
   * Save the given object to secondary storage.
   * 
   * @param absoluteHash              The absolute hash of the object to store.
   * @param payload                   The payload (if available).
   * @param saveToSecondary           Iff true then save to secondary storage.
   * @param trace                     A trace context.
   * @param absoluteSequenceHashes    The list of absolute sequences to which the object should be added.
   * @param hashCurrentSequenceHashes The list of hash current sequences to which the object should be added.
   * @param createdDate               The created date of the object for sequencing.
   */
  void saveToSecondaryStorage(Hash absoluteHash, IFugueObject payload, boolean saveToSecondary, ITraceContext trace,
      List<Hash> absoluteSequenceHashes, Instant createdDate);
  
  /**
   * Save the given object to secondary storage.
   * 
   * @param absoluteHash              The absolute hash of the object to store.
   * @param payload                   The payload (if available).
   * @param saveToSecondary           Iff true then save to secondary storage.
   * @param trace                     A trace context.
   * @param currentSequenceHashes     The list of current sequences to which the object should be added.
   * @param hashCurrentSequenceHashes The list of hash current sequences to which the object should be added.
   * @param baseHash                  The base hash of the object to store.
   * @param baseCreatedDate           The created date of the base object.
   */
  void saveToSecondaryStorage(Hash absoluteHash, IFugueObject payload, boolean saveToSecondary, ITraceContext trace,
      List<Hash> currentSequenceHashes, List<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate);
  
  /**
   * Save the given object to secondary storage.
   * 
   * @param absoluteHash              The absolute hash of the object to store.
   * @param payload                   The payload (if available).
   * @param saveToSecondary           Iff true then save to secondary storage.
   * @param trace                     A trace context.
   * @param absoluteSequenceHashes    The list of absolute sequences to which the object should be added.
   * @param createdDate               The created date of the object for sequencing.
   * @param currentSequenceHashes     The list of current sequences to which the object should be added.
   * @param hashCurrentSequenceHashes The list of hash current sequences to which the object should be added.
   * @param baseHash                  The base hash of the object to store.
   * @param baseCreatedDate           The created date of the base object.
   */
  void saveToSecondaryStorage(Hash absoluteHash, IFugueObject payload, boolean saveToSecondary, ITraceContext trace,
      List<Hash> absoluteSequenceHashes, Instant createdDate,
      List<Hash> currentSequenceHashes, List<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate);
}
