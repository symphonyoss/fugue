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

package org.symphonyoss.s2.fugue.kv.table;

import java.util.Collection;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPartitionKeyProvider;
import org.symphonyoss.s2.fugue.kv.IKvPartitionSortKeyProvider;

/**
 * Low level storage of KV Items.
 * 
 * @author Bruce Skingle
 *
 */
public interface IKvTable extends IFugueComponent
{
  /**
   * Store the given item.
   * 
   * @param kvItem  Item to be stored.
   * @param trace   Trace context.
   */
  void store(IKvItem kvItem, ITraceContext trace);
  
  /**
   * Store the given collection of items.
   * 
   * @param kvItems Items to be stored.
   * @param trace   Trace context.
   */
  void store(Collection<IKvItem> kvItems, ITraceContext trace);

  /**
   * Fetch the object with the given partition key and sort key.
   * 
   * @param partitionSortKey  The key of the required object.
   * @param trace             Trace context.
   * 
   * @return                  The required object.
   * 
   * @throws NoSuchObjectException  If there is no object with the given baseHash.
   */
  String fetch(IKvPartitionSortKeyProvider partitionSortKey, ITraceContext trace) throws NoSuchObjectException;
  
  /**
   * Fetch the first object with the given partition key.
   * 
   * @param partitionKey    The partition key of the required object.
   * @param trace           Trace context.
   * 
   * @return                The required object.
   * 
   * @throws NoSuchObjectException  If there is no object with the given partition key.
   */
  String fetchFirst(IKvPartitionKeyProvider partitionKey, ITraceContext trace) throws NoSuchObjectException;

  /**
   * Fetch the last object with the given partition key.
   * 
   * @param partitionKey    The partition key of the required object.
   * @param trace           Trace context.
   * 
   * @return                The required object.
   * 
   * @throws NoSuchObjectException  If there is no object with the given partition key.
   */
  String fetchLast(IKvPartitionKeyProvider partitionKey, ITraceContext trace) throws NoSuchObjectException;

  /**
   * Create the table.
   * 
   * @param dryRun If true then no changes are made but log messages show what would happen.
   */
  void createTable(boolean dryRun);

  /**
   * Delete the table.
   * 
   * @param dryRun If true then no changes are made but log messages show what would happen.
   */
  void deleteTable(boolean dryRun);
}
