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

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.store.AbstractFugueObjectStore;
import org.symphonyoss.s2.fugue.store.IFugueObjectStoreReadOnly;

/**
 * IFundamentalObjectStoreReadOnly implementation based on DynamoDB and S3.
 * 
 * @author Bruce Skingle
 *
 */
public class InMemoryObjectStoreReadOnly extends AbstractFugueObjectStore implements IFugueObjectStoreReadOnly
{
  protected Map<Hash, String>                     absoluteMap_ = new HashMap<>();
  protected Map<Hash, TreeMap<String, String>>    currentMap_ = new HashMap<>();
  protected Map<Hash, TreeMap<String, String>>    sequenceMap_ = new HashMap<>();
  
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
  public @Nonnull String fetchAbsolute(Hash absoluteHash)
      throws NoSuchObjectException
  {
    synchronized(absoluteMap_)
    {
      String result = absoluteMap_.get(absoluteHash);
      
      if(result == null)
        throw new NoSuchObjectException(absoluteHash + " not found");
      
      return result;
    }
  }
  
  @Override
  public @Nonnull String fetchCurrent(Hash baseHash)
      throws NoSuchObjectException
  {
    synchronized(currentMap_)
    {
      TreeMap<String, String> versions = currentMap_.get(baseHash);
      
      if(versions == null || versions.isEmpty())
        throw new NoSuchObjectException(baseHash + " not found");
      
      return versions.lastEntry().getValue();
      //return versions.values().iterator().next();
    }
  }

  @Override
  public String fetchVersions(Hash baseHash, @Nullable Integer pLimit, @Nullable String after, Consumer<String> consumer)
  {
    TreeMap<String, String> sequence          = currentMap_.get(baseHash);
    int                     limit             = pLimit == null ? Integer.MAX_VALUE : pLimit;
    String                  lastEvaluatedKey  = null;
    
    if(sequence != null)
    {
      for(Entry<String, String> entry : sequence.entrySet())
      {
        if(after != null)
        {
          if(entry.getKey().equals(after))
            after = null;
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
    
    return lastEvaluatedKey;
  }
  
  @Override
  public @Nonnull String fetchSequenceRecentObjects(Hash sequenceHash, @Nullable Integer pLimit, @Nullable String after, Consumer<String> consumer)
  {
    TreeMap<String, String>             sequence          = sequenceMap_.get(sequenceHash);
    int                                 limit             = pLimit == null ? Integer.MAX_VALUE : pLimit;
    String                              lastEvaluatedKey  = null;
    
    if(sequence != null)
    {
      for(Entry<String, String> entry : sequence.entrySet())
      {
        if(after != null)
        {
          if(entry.getKey().equals(after))
            after = null;
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
    
    return lastEvaluatedKey;
  }
}
