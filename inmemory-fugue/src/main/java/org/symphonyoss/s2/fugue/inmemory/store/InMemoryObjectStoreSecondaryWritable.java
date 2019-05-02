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

import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.store.IFugueObject;
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
  public String saveToSecondaryStorage(Hash absoluteHash, IFugueObject payload, ITraceContext trace)
  {
    String  payloadString;
    
    if(payload == null)
    {
      try
      {
        payloadString = fetchAbsolute(absoluteHash);
        //payloadBytes = ImmutableByteArray.newInstance(payload);
      }
      catch (NoSuchObjectException e)
      {
        throw new IllegalStateException(e);
      }
    }
    else
    {
      payloadString = payload.toString();
      trace.trace("READ_PAYLOAD_NOTIFICATION");
    }
    
    return payloadString;
  }
  
  @Override
  public void saveToSequences(Hash absoluteHash, IFugueObject payload,
      List<Hash> absoluteSequenceHashes, Instant createdDate)
  {
    doSaveToSequences(absoluteHash, payload,
        absoluteSequenceHashes, createdDate,
        null, null, null, null);
  }

  @Override
  public void saveToSequences(Hash absoluteHash, IFugueObject payload,
      List<Hash> currentSequenceHashes, List<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate)
  {
    doSaveToSequences(absoluteHash, payload,
        null, null,
        currentSequenceHashes, hashCurrentSequenceHashes, baseHash, baseCreatedDate);
  }
  
  @Override
  public void saveToSequences(Hash absoluteHash, IFugueObject payload,
      List<Hash> absoluteSequenceHashes, Instant createdDate,
      List<Hash> currentSequenceHashes, List<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate)
  {
    doSaveToSequences(absoluteHash, payload,
        absoluteSequenceHashes, createdDate,
        currentSequenceHashes, hashCurrentSequenceHashes, baseHash, baseCreatedDate);
  }

  private void doSaveToSequences(Hash absoluteHash, IFugueObject payload,
      List<Hash> absoluteSequenceHashes, Instant createdDate,
      List<Hash> currentSequenceHashes, List<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate)
  {
    String payloadString = payload.toString();
    
    if(absoluteSequenceHashes != null)
      processSequences(generateRangeKey(absoluteHash, createdDate), payloadString, absoluteSequenceHashes);

    if(currentSequenceHashes != null)
      processSequences(generateRangeKey(baseHash, baseCreatedDate), payloadString, currentSequenceHashes);
    
    if(hashCurrentSequenceHashes != null)
    {
      processSequences(baseHash.toStringBase64(), payloadString, hashCurrentSequenceHashes);
    } 
  }
  
  private void processSequences(String rangeKey, String payload, List<Hash> sequenceHashes)
  {
    synchronized (sequenceMap_)
    {
      for(Hash sequenceHash : sequenceHashes)
      {
        TreeMap<String, String> sequence = sequenceMap_.get(sequenceHash);
        
        if(sequence == null)
        {
          sequence = new TreeMap<>();
          
          sequenceMap_.put(sequenceHash, sequence);
        }
        
        sequence.put(rangeKey, payload);
        
        //System.err.println("put " + hash + " to " + rangeKey.toBase64String());
      }
    }
  }
}
