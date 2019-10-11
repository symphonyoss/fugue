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

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;

/**
 * A low level database within which Fugue objects can be stored.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueObjectStoreReadOnly
{
  /**
   * Fetch the object with the given absoluteHash.
   * 
   * @param absoluteHash  The ID of the required object.
   * @return              The required object.
   * 
   * @throws NoSuchObjectException  If there is no object with the given absoluteHash.
   */
  @Nonnull String fetchAbsolute(Hash absoluteHash) throws NoSuchObjectException;

  /**
   * Fetch the current (latest) version of the object with the given baseHash.
   * 
   * @param baseHash  The ID of the required object.
   * @return          The required object.
   * 
   * @throws NoSuchObjectException  If there is no object with the given baseHash.
   * 
   * Equivalent to fetchCurrent(baseHash, false);
   */
  @Nonnull String fetchCurrent(Hash baseHash) throws NoSuchObjectException;
  
  /**
   * Return objects from the given sequence, with more recent objects before older ones.
   * 
   * @param sequenceHash  The hash ID of the sequence.
   * @param scanForwards  If true then scan objects in chronological order, else in reverse order.
   * @param limit         An optional limit to the number of objects retrieved.
   * @param after         An optional page cursor to continue a previous query.
   * @param consumer      A consumer to receive the retrieved objects.
   * 
   * @return              A new after token to allow a continuation query to be made.
   */
  String fetchSequenceObjects(Hash sequenceHash, boolean scanForwards, @Nullable Integer limit, @Nullable String after, Consumer<String> consumer);

  /**
   * Return versions of the given object, with more recent versions before older ones.
   * 
   * @param baseHash      The ID of the required object.
   * @param scanForwards  If true then scan objects in chronological order, else in reverse order.
   * @param limit         An optional limit to the number of versions retrieved.
   * @param after         An optional page cursor to continue a previous query.
   * @param consumer      A consumer to receive the retrieved objects.
   * @return              A new after token to allow a continuation query to be made.
   */
  String fetchVersions(Hash baseHash, boolean scanForwards, @Nullable Integer limit, @Nullable String after, Consumer<String> consumer);
}
