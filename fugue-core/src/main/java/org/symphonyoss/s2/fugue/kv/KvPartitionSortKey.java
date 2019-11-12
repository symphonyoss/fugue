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

package org.symphonyoss.s2.fugue.kv;

import org.symphonyoss.s2.fugue.store.IFuguePodId;


/**
 * Implementation of IKvPartitionSortKey for use in fetch operations.
 * 
 * @author Bruce Skingle
 *
 */
public class KvPartitionSortKey extends KvPartitionKey implements IKvPartitionSortKey
{
  private final String      sortKey_;
  
  /**
   * Constructor.
   * 
   * @param podId         ID of the pod which owns the item.
   * @param partitionKey  The application level partition key.
   * @param sortKey       The application level sort key.
   */
  public KvPartitionSortKey(IFuguePodId podId, String partitionKey, String sortKey)
  {
    super(podId, partitionKey);
    sortKey_ = sortKey;
  }
  
  /**
   * Constructor from a partition key.
   * 
   * @param partitionKey  An existing partition key.
   * @param sortKey       The application level sort key.
   */
  public KvPartitionSortKey(IKvPartitionKey partitionKey, String sortKey)
  {
    super(partitionKey);
    sortKey_ = sortKey;
  }

  @Override
  public String getSortKey()
  {
    return sortKey_;
  }
}
