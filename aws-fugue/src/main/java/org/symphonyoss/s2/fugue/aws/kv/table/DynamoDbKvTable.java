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

package org.symphonyoss.s2.fugue.aws.kv.table;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPartitionSortKeyProvider;

/**
 * DynamoDB implementation of IKvTable.
 * 
 * @author Bruce Skingle
 *
 */
public class DynamoDbKvTable extends AbstractDynamoDbKvTable<DynamoDbKvTable>
{ 
  protected DynamoDbKvTable(AbstractDynamoDbKvTable.AbstractBuilder<?,?> builder)
  {
    super(builder);
  }
  
  @Override
  protected @Nonnull String fetchFromSecondaryStorage(Hash absoluteHash, ITraceContext trace) throws NoSuchObjectException
  {
    throw new NoSuchObjectException("This table does not support secondary storage.");
  }
  
  @Override
  protected void storeToSecondaryStorage(IKvItem kvItem, boolean payloadNotStored, ITraceContext trace)
  {
    if(payloadNotStored)
      throw new IllegalArgumentException("This table does not support secondary storage and the payload is too large to store in primary storage.");
  }
  
  @Override
  protected void deleteFromSecondaryStorage(Hash absoluteHash, ITraceContext trace)
  {
  }

  /**
   * Builder for DynamoDbKvTable.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractDynamoDbKvTable.AbstractBuilder<Builder, DynamoDbKvTable>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class); 
    }

    @Override
    protected DynamoDbKvTable construct()
    {
      return new DynamoDbKvTable(this);
    }

    @Override
    public Builder withEnableSecondaryStorage(boolean enableSecondaryStorage)
    {
      throw new IllegalArgumentException("Secondary storage is not supported by this implementation. Try S3DynamoDbKvTable");
    }
  }
}
