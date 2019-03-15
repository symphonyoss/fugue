/*
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
 * A versioned Fugue Object.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueVersionedObject extends IFugueObjectPayload
{
  /**
   * Return the absolute hash for this object.
   * 
   * This will be a hash of the type which was current when the object was created, i.e. using the hash type
   * which is encoded in the object, not the current default hash type.
   * 
   * @return the absolute hash for this object.
   */
  Hash getAbsoluteHash();
  
  /**
   * Versioned objects represent a snapshot of a mutable object. The initial absolute hash of the
   * initial (or base) version of the object is the baseHash of all subsequent versions.
   * 
   * @return The base hash of this object.
   */
  Hash getBaseHash();
  
  /**
   * When a new version of an object is created the creator should set the previous hash to the
   * absolute hash of what it believes is the current version. This attribute therefore describes
   * which prior version of the object the current object was created from.
   * 
   * @return the previous hash of this object.
   */
  Hash getPrevHash();
  
  /**
   * Return the range key for this object, which consists of the create timestamp concatenated with the hash.
   * 
   * @return The range key for this object.
   */
  ImmutableByteArray  getRangeKey();
}
