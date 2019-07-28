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

/**
 * The type of deletion to be performed in an object store delete.
 * 
 * @author Bruce Skingle
 *
 */
public enum FugueObjectDeletionType
{
  /**
   * Not a deletion operation.
   */
  NONE(false, false),
  
  /**
   * A Logical delete, a tombstone object is added as a new version of the object.
   * 
   * After an object has been deleted in this way:
   * 
   * A fetchCurrent operation on the baseHash will result in a DeletedException (HTTP 410 GONE).
   * A fetchAbsolute operation on the old absoluteHash will return the old object.
   * A fetchAbsolute operation on the new absoluteHash will return the tombstone object.
   */
  LOGICAL(true, false),
  
  /**
   * An index delete, all versions of the object will be physically removed from the index.
   * 
   * After an object has been deleted in this way:
   * 
   * A fetchCurrent operation on the baseHash will result in a NotFoundException (HTTP 404 NOT FOUND).
   * A fetchAbsolute operation on the old absoluteHash will return the old object.
   * A fetchAbsolute operation on the new absoluteHash will result in a NotFoundException (HTTP 404 NOT FOUND).
   * 
   * fetchAbsolute operations can still be answered from secondary storage. The main use of this feature is to
   * reduce storage costs for objects which are no longer required.
   * 
   * In any event objects may be aggressively cached by absoluteHash so there can never be a guarantee of
   * deletion of an object.
   */
  INDEX(true, true), 
  ;
  
  private final boolean baseIndex_;
  private final boolean absoluteIndex_;
  
  private FugueObjectDeletionType(boolean logical, boolean index)
  {
    baseIndex_ = logical;
    absoluteIndex_ = index;
  }

  /**
   * 
   * @return true if this type of deletion includes removal from the baseHash index.
   */
  public boolean isBaseIndex()
  {
    return baseIndex_;
  }

  /**
   * 
   * @return true if this type of deletion includes removal from the absoluteHash index.
   */
  public boolean isAbsoluteIndex()
  {
    return absoluteIndex_;
  }
}
