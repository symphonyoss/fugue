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


import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.store.IFugueObject;
import org.symphonyoss.s2.fugue.store.IFugueObjectStoreWritable;
import org.symphonyoss.s2.fugue.store.IFugueVersionedObject;
import org.symphonyoss.s2.fugue.store.ObjectExistsException;

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
  public void saveIfNotExists(IFugueObject idObject, IFugueObject payload, ITraceContext trace) throws ObjectExistsException
  {
    if(payload.getPayload() instanceof IFugueVersionedObject && ((IFugueVersionedObject)payload.getPayload()).getBaseHash().equals(idObject.getAbsoluteHash()))
    {
      synchronized(absoluteMap_)
      {
        String current = absoluteMap_.get(idObject.getAbsoluteHash());
        
        if(current == null)
        {
          doSave(idObject);
          save(payload, trace);
        }
        else
        {
          throw new ObjectExistsException("Object exists");
        }
      }
    }
    else
    {
      throw new IllegalArgumentException("Payload baseHash must be ID absoluteHash");
    }
  }
  
  private void doSave(IFugueObject fundamentalObject)
  {
    String blob = fundamentalObject.toString();
    
    absoluteMap_.put(fundamentalObject.getAbsoluteHash(), blob);
    
    if(fundamentalObject.getPayload() instanceof IFugueVersionedObject)
    {
      IFugueVersionedObject versionedObject = (IFugueVersionedObject)fundamentalObject.getPayload();
      
      doSaveCurrent(versionedObject.getBaseHash(), versionedObject.getRangeKey(), blob, versionedObject.getAbsoluteHash());
    }
  }

  private void doSaveCurrent(Hash baseHash, String rangeKey, String blob, Hash absoluteHash)
  {
    synchronized(currentMap_)
    {
      TreeMap<String, String> versions = currentMap_.get(baseHash);
      
      if(versions == null)
      {
        versions = new TreeMap<>();
        currentMap_.put(baseHash, versions);
      }
      
      versions.put(rangeKey, blob);
      
      List<Hash> baseList = baseMap_.get(baseHash);
      
      if(baseList == null)
      {
        baseList = new LinkedList<>();
        baseMap_.put(baseHash, baseList);
      }
      
      baseList.add(absoluteHash);
    }
  }
  
  @Override
  public void save(IFugueObject fundamentalObject, ITraceContext trace)
  {
    String blob = fundamentalObject.toString();
    
    if(fundamentalObject.getPayload() instanceof IFugueVersionedObject)
    {
      IFugueVersionedObject versionedObject = (IFugueVersionedObject) fundamentalObject.getPayload();
      
      save(versionedObject.getAbsoluteHash(), blob,
          versionedObject.getBaseHash(), versionedObject.getRangeKey());
    }
    else
    {
      save(fundamentalObject.getAbsoluteHash(), blob); 
    }
  }

  private void save(Hash absoluteHash, String blob)
  {
    synchronized(absoluteMap_)
    {
      absoluteMap_.put(absoluteHash, blob);
    }
  }

  private void save( Hash absoluteHash, String blob, Hash baseHash, String rangeKey)
  {
    if(Hash.NIL_HASH.equals(baseHash))
      baseHash = absoluteHash;
    
    synchronized(absoluteMap_)
    {
      absoluteMap_.put(absoluteHash, blob);
    }
    
    doSaveCurrent(baseHash, rangeKey, blob, absoluteHash);
    
  }
}
