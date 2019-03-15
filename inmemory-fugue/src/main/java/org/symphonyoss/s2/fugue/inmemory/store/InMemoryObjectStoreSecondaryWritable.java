/*
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

package org.symphonyoss.s2.fugue.inmemory.store;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.store.IFugueObjectStoreSecondaryWritable;

/**
 * IFundamentalObjectStoreSecondaryWritable implementation based on DynamoDB and S3.
 * 
 * @author Bruce Skingle
 */
public class InMemoryObjectStoreSecondaryWritable extends InMemoryObjectStoreReadOnly implements IFugueObjectStoreSecondaryWritable
{
  protected InMemoryObjectStoreSecondaryWritable(AbstractBuilder<?, ?> builder)
  {
    super(builder);
  }
  
  /**
   * Builder for InMemoryObjectStoreSecondaryWritable
   * 
   * @author Bruce Skingle
   */
  public static class Builder extends InMemoryObjectStoreReadOnly.AbstractBuilder<Builder, InMemoryObjectStoreSecondaryWritable>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected InMemoryObjectStoreSecondaryWritable construct()
    {
      return new InMemoryObjectStoreSecondaryWritable(this);
    }
    
  }

  @Override
  public void saveToSecondaryStorage(Hash absoluteHash, Instant createdDate, @Nullable ImmutableByteArray payload, List<Hash> sequenceHashes, ITraceContext trace)
  {
    byte[] payloadBytes;
    
    if(payload == null)
    {
      try
      {
        payloadBytes = fetchAbsolute(absoluteHash);
      }
      catch (NoSuchObjectException e)
      {
        throw new IllegalStateException(e);
      }
    }
    else
    {
      trace.trace("READ_PAYLOAD_NOTIFICATION");
      
      payloadBytes = payload.toByteArray();
    }

    if(!sequenceHashes.isEmpty())
    {
      ImmutableByteArray      rangeKey          = ImmutableByteArray.newInstance(generateRangeKey(absoluteHash, createdDate));
      
      synchronized (sequenceMap_)
      {
        for(Hash sequenceHash : sequenceHashes)
        {
          TreeMap<ImmutableByteArray, byte[]> sequence = sequenceMap_.get(sequenceHash);
          
          if(sequence == null)
          {
            sequence = new TreeMap<>();
            
            sequenceMap_.put(sequenceHash, sequence);
          }
          
          sequence.put(rangeKey, payloadBytes);
        }
      }
    }
  }
  
  private byte[] generateRangeKey(Hash absoluteHash, Instant createdDate)
  {
    ByteBuffer b = ByteBuffer.allocate(absoluteHash.toByteString().size() + 8 + 4);
    b.putLong(Long.MAX_VALUE - createdDate.getEpochSecond());
    b.putInt(Integer.MAX_VALUE - createdDate.getNano());
    b.put(absoluteHash.toByteString().toByteArray());
    
    return b.array();
  }
  
}
