/*
 *
 *
 * Copyright 2019 Symphony Communication Services, LLC.
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

import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;

/**
 * An immutable ID  capable of storage in an IFugueObjectStore.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueId
{
  /**
   * Serialize this object.
   * 
   * @return The serialized form of this object.
   */
  ImmutableByteArray serialize();
  
  /**
   * Return the absolute hash for this object.
   * 
   * This will be a hash of the type which was current when the object was created, i.e. using the hash type
   * which is encoded in the object, not the current default hash type.
   * 
   * @return the absolute hash for this object.
   */
  Hash getAbsoluteHash();
}
