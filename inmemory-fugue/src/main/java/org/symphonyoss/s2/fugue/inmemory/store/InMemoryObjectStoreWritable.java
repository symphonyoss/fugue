/*
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

package org.symphonyoss.s2.fugue.inmemory.store;


import java.util.List;
import java.util.TreeMap;

import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.store.IFugueObject;
import org.symphonyoss.s2.fugue.store.IFugueObjectStoreWritable;
import org.symphonyoss.s2.fugue.store.IFugueVersionedObject;

/**
 * IFundamentalObjectStoreWritable implementation based on DynamoDB and S3.
 * 
 * @author Bruce Skingle
 *
 */
public class InMemoryObjectStoreWritable extends InMemoryObjectStoreSecondaryWritable implements IFugueObjectStoreWritable, IFugueComponent
{
 protected InMemoryObjectStoreWritable(AbstractBuilder<?,?> builder)
  {
    super(builder);
  }
  
  /**
   * Builder for InMemoryObjectStoreWritable.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, InMemoryObjectStoreWritable>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected InMemoryObjectStoreWritable construct()
    {
      return new InMemoryObjectStoreWritable(this);
    }
  }

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends InMemoryObjectStoreWritable> extends InMemoryObjectStoreReadOnly.AbstractBuilder<T,B>
  {
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
  }
  
  @Override
  public byte[] saveIfNotExists(IFugueObject idObject, ITraceContext trace,
      List<? extends IFugueObject> additionalObjects)
  {
    synchronized(absoluteMap_)
    {
      byte[] current = absoluteMap_.get(idObject.getAbsoluteHash());
      
      if(current == null)
      {
        doSave(idObject);
        
        for(IFugueObject additionalObject : additionalObjects)
        {
          doSave(additionalObject);
        }
        
        return null;
      }

      return current;
    }
  }
  
  private void doSave(IFugueObject fundamentalObject)
  {
    byte[] blob = fundamentalObject.serialize().toByteArray();
    
    absoluteMap_.put(fundamentalObject.getAbsoluteHash(), blob);
    
    if(fundamentalObject instanceof IFugueVersionedObject)
    {
      IFugueVersionedObject versionedObject = (IFugueVersionedObject)fundamentalObject;
      
      doSaveCurrent(versionedObject.getBaseHash(), versionedObject.getRangeKey(), blob);
    }
  }

  private void doSaveCurrent(Hash baseHash, ImmutableByteArray rangeKey, byte[] blob)
  {
    synchronized(currentMap_)
    {
      TreeMap<ImmutableByteArray, byte[]> versions = currentMap_.get(baseHash);
      
      if(versions == null)
      {
        versions = new TreeMap<>();
        currentMap_.put(baseHash, versions);
      }
      
      versions.put(rangeKey, blob);
    }
  }
  
  @Override
  public void save(IFugueObject fundamentalObject, ITraceContext trace)
  {
    ImmutableByteArray bytes = fundamentalObject.serialize();
    
    if(fundamentalObject.getPayload() instanceof IFugueVersionedObject)
    {
      IFugueVersionedObject versionedObject = (IFugueVersionedObject) fundamentalObject.getPayload();
      
      save(versionedObject.getAbsoluteHash(), bytes,
          versionedObject.getBaseHash(), versionedObject.getRangeKey());
    }
    else
    {
      save(fundamentalObject.getAbsoluteHash(), bytes); 
    }
  }

  private void save(Hash absoluteHash, ImmutableByteArray bytes)
  {
    synchronized(absoluteMap_)
    {
      absoluteMap_.put(absoluteHash, bytes.toByteArray());
    }
  }

  private void save( Hash absoluteHash, ImmutableByteArray bytes, Hash baseHash, ImmutableByteArray rangeKey)
  {
    if(Hash.NIL_HASH.equals(baseHash))
      baseHash = absoluteHash;
    
    byte[] blob = bytes.toByteArray();
    
    synchronized(absoluteMap_)
    {
      absoluteMap_.put(absoluteHash, blob);
    }
    
    doSaveCurrent(baseHash, rangeKey, blob);
    
  }
}
