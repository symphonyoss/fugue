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
import java.util.Collection;
import java.util.TreeMap;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.store.FugueObjectDeletionType;
import org.symphonyoss.s2.fugue.store.IFugueObject;
import org.symphonyoss.s2.fugue.store.IFugueObjectPayload;
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
  public void saveToSequences(Hash absoluteHash, String payload, FugueObjectDeletionType deletionType,
      IFugueObjectPayload fugueObjectPayload, int payloadLimit,
      Collection<Hash> absoluteSequenceHashes, Instant createdDate, ITraceContext trace)
  {
    doSaveToSequences(absoluteHash, payload, deletionType,
        absoluteSequenceHashes, createdDate,
        null, null, null, null);
  }

  @Override
  public void saveToSequences(Hash absoluteHash, String payload, FugueObjectDeletionType deletionType,
      IFugueObjectPayload fugueObjectPayload, int payloadLimit,
      Collection<Hash> currentSequenceHashes, Collection<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate, ITraceContext trace)
  {
    doSaveToSequences(absoluteHash, payload, deletionType,
        null, null,
        currentSequenceHashes, hashCurrentSequenceHashes, baseHash, baseCreatedDate);
  }
  
  @Override
  public void saveToSequences(Hash absoluteHash, String payload, FugueObjectDeletionType deletionType,
      IFugueObjectPayload fugueObjectPayload, int payloadLimit,
      Collection<Hash> absoluteSequenceHashes, Instant createdDate,
      Collection<Hash> currentSequenceHashes, Collection<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate, ITraceContext trace)
  {
    doSaveToSequences(absoluteHash, payload, deletionType,
        absoluteSequenceHashes, createdDate,
        currentSequenceHashes, hashCurrentSequenceHashes, baseHash, baseCreatedDate);
  }

  private void doSaveToSequences(Hash absoluteHash, String payload, FugueObjectDeletionType deletionType,
      Collection<Hash> absoluteSequenceHashes, Instant createdDate,
      Collection<Hash> currentSequenceHashes, Collection<Hash> hashCurrentSequenceHashes, Hash baseHash, Instant baseCreatedDate)
  {
    String payloadString = payload.toString();
    
    if(absoluteSequenceHashes != null)
      processSequences(generateRangeKey(absoluteHash, createdDate), payloadString, deletionType.isAbsoluteIndex(), absoluteSequenceHashes);

    if(currentSequenceHashes != null)
      processSequences(generateRangeKey(baseHash, baseCreatedDate), payloadString, deletionType.isBaseIndex(), currentSequenceHashes);
    
    if(hashCurrentSequenceHashes != null)
    {
      processSequences(baseHash.toStringBase64(), payloadString, deletionType.isBaseIndex(), hashCurrentSequenceHashes);
    } 
  }
  
  private void processSequences(String rangeKey, String payload, boolean deleted, Collection<Hash> sequenceHashes)
  {
    synchronized (sequenceMap_)
    {
      for(Hash sequenceHash : sequenceHashes)
      {
        TreeMap<String, String> sequence = sequenceMap_.get(sequenceHash);
        
        if(deleted)
        {
          if(sequence != null)
            sequence.remove(rangeKey);
        }
        else
        {
          if(sequence == null)
          {
            sequence = new TreeMap<>();
            
            sequenceMap_.put(sequenceHash, sequence);
          }
          
          sequence.put(rangeKey, payload);
        }
        
        //System.err.println("put " + hash + " to " + rangeKey.toBase64String());
      }
    }
  }

//  @Override
//  public void delete(Hash hash, Hash baseHash, ITraceContext trace)
//  {
//    List<Hash> baseList = baseMap_.get(baseHash);
//    
//    if(baseList != null)
//    {
//      for(Hash absoluteHash : baseList)
//      {
//        absoluteMap_.remove(absoluteHash);
//      }
//    }
//    
//    absoluteMap_.remove(hash);
//    baseMap_.remove(baseHash);
//    currentMap_.remove(baseHash);
//  }
}
