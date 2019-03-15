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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.hash.HashProvider;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContext;
import org.symphonyoss.s2.fugue.store.IFugueObject;
import org.symphonyoss.s2.fugue.store.IFugueObjectPayload;

/**
 * Unit test for in memory object store.
 * 
 * @author Bruce Skingle
 *
 */
public class TestInMemoryObjectStore
{
  private InMemoryObjectStoreWritable objectStore_ = new InMemoryObjectStoreWritable.Builder()
      .build();
  
  /**
   * Test store and retrieve.
   * 
   * @throws NoSuchObjectException If there is a problem.
   */
  @Test
  public void testStore() throws NoSuchObjectException
  {
    FugueObject  objectOne = new FugueObject("Object One");
    FugueObject  objectTwo = new FugueObject("Object Two");

    objectStore_.save(objectOne, NoOpTraceContext.INSTANCE);
    objectStore_.save(objectTwo, NoOpTraceContext.INSTANCE);
    
    byte[] retOne = objectStore_.fetchAbsolute(objectOne.getAbsoluteHash());
    byte[] retTwo = objectStore_.fetchAbsolute(objectTwo.getAbsoluteHash());
    
    assertEquals(objectOne.serialize(), ImmutableByteArray.newInstance(retOne));
    assertEquals(objectTwo.serialize(), ImmutableByteArray.newInstance(retTwo));
  }
  
  class FugueObject implements IFugueObject
  {
    private ImmutableByteArray value_;
    private String description_;
    private Hash absoluteHash_;
    private IFugueObjectPayload payload_;

    FugueObject(String value)
    {
      value_= ImmutableByteArray.newInstance(value);
      description_ = "FugueObject(" + value + ")";
      absoluteHash_ = HashProvider.getHashOf(value_);
      payload_ = new IFugueObjectPayload()
      {

        @Override
        public int hashCode()
        {
          return value_.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
          return value_.equals(obj);
        }

        @Override
        public String toString()
        {
          return value_.toString();
        }
        
      };
    }
    
    @Override
    public String getDescription()
    {
      return description_;
    }

    @Override
    public ImmutableByteArray serialize()
    {
      return value_;
    }

    @Override
    public Hash getAbsoluteHash()
    {
      return absoluteHash_;
    }

    @Override
    public ImmutableByteArray getRangeKey()
    {
      return value_;
    }

    @Override
    public IFugueObjectPayload getPayload()
    {
      return payload_;
    }
    
  }
}
