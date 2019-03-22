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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;
import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.store.IFugueObjectStoreReadOnly;

/**
 * IFundamentalObjectStoreReadOnly implementation based on DynamoDB and S3.
 * 
 * @author Bruce Skingle
 *
 */
public class InMemoryObjectStoreReadOnly implements IFugueObjectStoreReadOnly, IFugueComponent
{
  protected Map<Hash, byte[]>                     absoluteMap_ = new HashMap<>();
  protected Map<Hash, TreeMap<ImmutableByteArray, byte[]>>    currentMap_ = new HashMap<>();
  protected Map<Hash, TreeMap<ImmutableByteArray, byte[]>>    sequenceMap_ = new HashMap<>();
  
  /**
   * Constructor.
   * 
   * @param builder A builder.
   */
  public InMemoryObjectStoreReadOnly(AbstractBuilder<?,?> builder)
  {
  }
  
  /**
   * Builder for DynamoDbObjectStoreReadOnly.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, InMemoryObjectStoreReadOnly>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class); 
    }

    @Override
    protected InMemoryObjectStoreReadOnly construct()
    {
      return new InMemoryObjectStoreReadOnly(this);
    }
  }

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends InMemoryObjectStoreReadOnly> extends BaseAbstractBuilder<T,B>
  {
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
  }
  
  @Override
  public void start()
  {}

  @Override
  public void stop()
  {}
  
  @Override
  public @Nonnull byte[] fetchAbsolute(Hash absoluteHash)
      throws NoSuchObjectException
  {
    synchronized(absoluteMap_)
    {
      byte[] result = absoluteMap_.get(absoluteHash);
      
      if(result == null)
        throw new NoSuchObjectException(absoluteHash + " not found");
      
      return result;
    }
  }
  
  @Override
  public @Nonnull byte[] fetchCurrent(Hash baseHash)
      throws NoSuchObjectException
  {
    synchronized(currentMap_)
    {
      TreeMap<ImmutableByteArray, byte[]> versions = currentMap_.get(baseHash);
      
      if(versions == null || versions.isEmpty())
        throw new NoSuchObjectException(baseHash + " not found");
      
      return versions.values().iterator().next();
    }
  }
  
  @Override
  public String fetchVersions(Hash baseHash, @Nullable Integer pLimit, @Nullable String after, Consumer<byte[]> consumer)
  {
    TreeMap<ImmutableByteArray, byte[]> sequence          = currentMap_.get(baseHash);
    int                                 limit             = pLimit == null ? Integer.MAX_VALUE : pLimit;
    ImmutableByteArray                  afterBytes        = after == null ? null : ImmutableByteArray.newInstance(Base64.decodeBase64(after));
    ImmutableByteArray                  lastEvaluatedKey  = null;
    
    if(sequence != null)
    {
      for(Entry<ImmutableByteArray, byte[]> entry : sequence.entrySet())
      {
        if(afterBytes != null)
        {
          if(entry.getKey().equals(afterBytes))
            afterBytes = null;
        }
        else
        {
          consumer.accept(entry.getValue());
          limit--;
        }
        lastEvaluatedKey = entry.getKey();
        
        if(limit < 0)
          break;
      }
    }
    
    
    if(lastEvaluatedKey != null)
    {
      return lastEvaluatedKey.toBase64String();
    }
    
    return null;
  }
  
  @Override
  public @Nonnull String fetchSequenceRecentObjects(Hash sequenceHash, @Nullable Integer pLimit, @Nullable String after, Consumer<byte[]> consumer)
  {
    TreeMap<ImmutableByteArray, byte[]> sequence          = sequenceMap_.get(sequenceHash);
    int                                 limit             = pLimit == null ? Integer.MAX_VALUE : pLimit;
    ImmutableByteArray                  afterBytes        = after == null ? null : ImmutableByteArray.newInstance(Base64.decodeBase64(after));
    ImmutableByteArray                  lastEvaluatedKey  = null;
    
    if(sequence != null)
    {
      for(Entry<ImmutableByteArray, byte[]> entry : sequence.entrySet())
      {
        if(afterBytes != null)
        {
          if(entry.getKey().equals(afterBytes))
            afterBytes = null;
        }
        else
        {
          consumer.accept(entry.getValue());
          limit--;
        }
        lastEvaluatedKey = entry.getKey();
        
        if(limit < 0)
          break;
      }
    }
    
    
    if(lastEvaluatedKey != null)
    {
      return lastEvaluatedKey.toBase64String();
    }
    
    return null;
  }
}
