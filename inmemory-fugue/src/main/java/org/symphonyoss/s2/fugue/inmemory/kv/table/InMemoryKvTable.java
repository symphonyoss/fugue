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

package org.symphonyoss.s2.fugue.inmemory.kv.table;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPartitionKeyProvider;
import org.symphonyoss.s2.fugue.kv.IKvPartitionSortKeyProvider;
import org.symphonyoss.s2.fugue.kv.table.IKvTable;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscription.AbstractBuilder;

/**
 * Base implementation of IKvTable.
 * 
 * @author Bruce Skingle
 */
public class InMemoryKvTable implements IKvTable
{
  protected static final String  Separator = "#";
  
  /** The serviceId forms part of the partition key for all values in this table. */
  protected final String         serviceId_;
  
  /** If true then data in this table is segregated by podId (i.e. podId forms part of the hash key for all values) */
  protected final boolean        podPrivate_;

  private final Map<String, TreeMap<String, String>>  partitionMap_ = new HashMap<>();
  
  protected InMemoryKvTable(AbstractBuilder<?,?> builder)
  {
    serviceId_    = builder.serviceId_;
    podPrivate_   = builder.podPrivate_;
  }

  @Override
  public void start()
  {
  }

  @Override
  public void stop()
  {
  }

  @Override
  public void store(IKvItem kvItem, ITraceContext trace)
  {
    String partitionKey = getPartitionKey(kvItem);
    String sortKey = kvItem.getSortKey().asString();
    
    Map<String, String> partition = getPartition(partitionKey);
    
    partition.put(sortKey, kvItem.getJson());
  }

  private synchronized TreeMap<String, String> getPartition(String partitionKey)
  {
    TreeMap<String, String> partition = partitionMap_.get(partitionKey);
    
    if(partition == null)
    {
      partition = new TreeMap<>();
      partitionMap_.put(partitionKey, partition);
    }
    
    return partition;
  }

  private String getPartitionKey(IKvPartitionKeyProvider kvItem)
  {
    if(podPrivate_)
      return serviceId_ + Separator + kvItem.getPodId() + Separator + kvItem.getPartitionKey();
    else
      return serviceId_ + Separator + kvItem.getPartitionKey();
  }

  @Override
  public void store(Collection<IKvItem> kvItems, ITraceContext trace)
  {
    for(IKvItem item : kvItems)
      store(item, trace);
  }

  @Override
  public String fetch(IKvPartitionSortKeyProvider partitionSortKey, ITraceContext trace) throws NoSuchObjectException
  {
    String partitionKey = getPartitionKey(partitionSortKey);
    String sortKey = partitionSortKey.getSortKey().asString();
    
    TreeMap<String, String> partition = getPartition(partitionKey);
    
    return partition.get(sortKey);
  }

  @Override
  public String fetchFirst(IKvPartitionKeyProvider partitionKeyProvider, ITraceContext trace) throws NoSuchObjectException
  {
    String partitionKey = getPartitionKey(partitionKeyProvider);
    
    TreeMap<String, String> partition = getPartition(partitionKey);
    
    return partition.firstEntry().getValue();
  }

  @Override
  public String fetchLast(IKvPartitionKeyProvider partitionKeyProvider, ITraceContext trace) throws NoSuchObjectException
  {
    String partitionKey = getPartitionKey(partitionKeyProvider);
    
    TreeMap<String, String> partition = getPartition(partitionKey);
    
    return partition.lastEntry().getValue();
  }

  @Override
  public void createTable(boolean dryRun)
  {
  }

  @Override
  public void deleteTable(boolean dryRun)
  {
  }

  @Override
  public String fetchPartitionObjects(IKvPartitionKeyProvider partitionKeyProvider, boolean scanForwards, Integer limit,
      String after, Consumer<String> consumer, ITraceContext trace)
  {
    String partitionKey = getPartitionKey(partitionKeyProvider);
    
    TreeMap<String, String> partition = getPartition(partitionKey);

    NavigableMap<String, String> map; 
    
    if(after == null)
    {
      if(scanForwards)
        map = partition;
      else
        map = partition.descendingMap();
    }
    else if(scanForwards)
      map = partition.tailMap(after, false);
    else
      map = partition.descendingMap().tailMap(after, false);
    
    if(limit == null)
      limit = 100;
    
    for(Entry<String, String> entry : map.entrySet())
    {
      consumer.accept(entry.getValue());
      
      if(--limit <= 0)
        return entry.getKey();
    }
        
    return null;
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, InMemoryKvTable>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected InMemoryKvTable construct()
    {
      return new InMemoryKvTable(this);
    }
  }

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends InMemoryKvTable> extends BaseAbstractBuilder<T,B>
  {
    protected String         serviceId_;
    protected boolean        podPrivate_ = true;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(serviceId_,   "serviceId");
    }

    /**
     * The serviceId forms part of the partition key for all values in this table.
     * 
     * @param serviceId The serviceId for this table.
     * 
     * @return This (fluent method).
     */
    public T withServiceId(String serviceId)
    {
      serviceId_ = serviceId;
      
      return self();
    }

    /**
     * If a table is podPrivate then data in it is segregated by podId (i.e. podId forms part of the hash key for all values).
     * 
     * @param podPrivate Set the podPrivate flag for this table.
     * 
     * @return This (fluent method).
     */
    public T withPodPrivate(boolean podPrivate)
    {
      podPrivate_ = podPrivate;
      
      return self();
    }
  }
}
