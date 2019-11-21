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
 * Implementation of IKvPartitionKey for use in fetch operations.
 * 
 * @author Bruce Skingle
 *
 */
public class KvPartitionKeyProvider implements IKvPartitionKeyProvider
{
  private final IKvPartitionKey partitionKey_;
  private final IFuguePodId     podId_;

  /**
   * Constructor.
   * 
   * @param podId         ID of the pod which owns the item.
   * @param partitionKey  The application level partition key.
   */
  public KvPartitionKeyProvider(IFuguePodId podId, IKvPartitionKey partitionKey)
  {
    podId_ = podId;
    partitionKey_ = partitionKey;
  }

  /**
   * Constructor.
   * 
   * @param podId         ID of the pod which owns the item.
   * @param partitionKey  The application level partition key.
   */
  public KvPartitionKeyProvider(IFuguePodId podId, String partitionKey)
  {
    podId_ = podId;
    partitionKey_ = new KvPartitionKey(partitionKey);
  }

  /**
   * Copy constructor.
   * 
   * @param partitionKey Key to be copied.
   */
  public KvPartitionKeyProvider(IKvPartitionKeyProvider partitionKey)
  {
    podId_ = partitionKey.getPodId();
    partitionKey_ = partitionKey.getPartitionKey();
  }

  @Override
  public IKvPartitionKey getPartitionKey()
  {
    return partitionKey_;
  }

  @Override
  public IFuguePodId getPodId()
  {
    return podId_;
  }
}
