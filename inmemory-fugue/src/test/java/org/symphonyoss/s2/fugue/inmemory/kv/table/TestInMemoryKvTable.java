/*
 * Copyright 2019 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.inmemory.kv.table;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;
import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.hash.HashProvider;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPagination;
import org.symphonyoss.s2.fugue.kv.IKvPartitionKey;
import org.symphonyoss.s2.fugue.kv.IKvSortKey;
import org.symphonyoss.s2.fugue.kv.KvComparison;
import org.symphonyoss.s2.fugue.kv.KvCondition;
import org.symphonyoss.s2.fugue.kv.KvPartitionKey;
import org.symphonyoss.s2.fugue.kv.KvPartitionKeyProvider;
import org.symphonyoss.s2.fugue.kv.KvPartitionSortKeyProvider;
import org.symphonyoss.s2.fugue.kv.KvSortKey;
import org.symphonyoss.s2.fugue.store.IFuguePodId;

public class TestInMemoryKvTable
{
  private static final IFuguePodId    POD_ID = new IFuguePodId()
      {
        @Override
        public Integer getValue()
        {
          return 167;
        }
      };

  private static final String PART1 = "PART1";
  private static final KvPartitionKey PARTITION_KEY1 = new KvPartitionKey(PART1);

  private static final IKvItem[] ITEMS = new IKvItem[]
  {
    new KvItem(PART1, "1", "One"),
    new KvItem(PART1, "2", "Two"),
    new KvItem(PART1, "3", "Three"),
    new KvItem(PART1, "4", "Four"),
    new KvItem(PART1, "5", "Five"),
    new KvItem(PART1, "6", "Six")
  };

  private ITraceContext trace = NoOpTraceContext.INSTANCE;
  
  @Test
  public void testGet() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    for(IKvItem item : ITEMS)
    {
      String it = table.fetch(
          new KvPartitionSortKeyProvider(PARTITION_KEY1, item.getSortKey()), trace);
      
      assertEquals(item.getJson(), it);
    }
  }
  
  @Test
  public void testConditionalPut() throws NoSuchObjectException
  {
    InMemoryKvTable table = new InMemoryKvTable.Builder().withServiceId("test").build();
    String sortKey = "1";
    KvPartitionSortKeyProvider partitionSortKeyProvider = new KvPartitionSortKeyProvider(PARTITION_KEY1, sortKey);
    
    List<IKvItem> kvItems = new ArrayList<>(1);
    
    IKvItem item1 = new KvItem(PART1, sortKey, "One", "foo", "1");
    
    kvItems.add(item1);
    
    table.store(kvItems, trace);
    
    assertEquals(item1.getJson(), table.fetch(partitionSortKeyProvider, trace));
    
    String attrName = "foo";
    IKvItem item2 = new KvItem(PART1, sortKey, "Two", attrName, "1");
    
    table.store(item2, new KvCondition(attrName, KvComparison.LESS_THAN, "1"), trace);
    
    assertEquals(item1.getJson(), table.fetch(partitionSortKeyProvider, trace));
    
    table.store(item2, new KvCondition(attrName, KvComparison.GREATER_THAN, "1"), trace);
    
    assertEquals(item1.getJson(), table.fetch(partitionSortKeyProvider, trace));
    
    table.store(item2, new KvCondition(attrName, KvComparison.EQUALS, "1"), trace);
    
    assertEquals(item2.getJson(), table.fetch(partitionSortKeyProvider, trace));
    
    IKvItem item3 = new KvItem(PART1, sortKey, "Three", attrName, "3");
    
    table.store(item3, new KvCondition(attrName, KvComparison.EQUALS, "3"), trace);
    
    assertEquals(item2.getJson(), table.fetch(partitionSortKeyProvider, trace));
    
    table.store(item3, new KvCondition(attrName, KvComparison.GREATER_THAN, "3"), trace);
    
    assertEquals(item2.getJson(), table.fetch(partitionSortKeyProvider, trace));
    
    table.store(item3, new KvCondition(attrName, KvComparison.LESS_THAN, "3"), trace);
    
    assertEquals(item3.getJson(), table.fetch(partitionSortKeyProvider, trace));
  }
  
  static class Checker implements Consumer<String>
  {
    int index_;
    int inc_;
    
    public Checker(int index)
    {
      index_ = index;
      inc_ = 1;
    }

    public Checker(int index, int inc)
    {
      super();
      index_ = index;
      inc_ = inc;
    }

    @Override
    public void accept(String value)
    {
      if(value != ITEMS[index_].getJson())
        throw new IllegalArgumentException("Unexpected value " + value);
      
      index_ += inc_;
    }
   }
  
  @Test
  public void testGetAll() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    Checker consumer = new Checker(0);
       
    IKvPagination pagination = table.fetchPartitionObjects(new KvPartitionKeyProvider(PARTITION_KEY1), true, null, null, null, null, consumer, trace);
   
    assertEquals(null, pagination.getBefore());
    assertEquals(null, pagination.getAfter());
    assertEquals(ITEMS.length, consumer.index_);
  }
  
  @Test
  public void testGetAllReverse() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    Checker consumer = new Checker(5, -1);
       
    IKvPagination pagination = table.fetchPartitionObjects(new KvPartitionKeyProvider(PARTITION_KEY1), false, null, null, null, null, consumer, trace);

    assertEquals(null, pagination.getBefore());
    assertEquals(null, pagination.getAfter());
    assertEquals(-1, consumer.index_);
  }
  
  @Test
  public void testGetTwo() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    Checker consumer = new Checker(0);
       
    IKvPagination pagination = table.fetchPartitionObjects(new KvPartitionKeyProvider(PARTITION_KEY1), true, 2, null, null, null, consumer, trace);

    assertEquals(null, pagination.getBefore());
    assertEquals(ITEMS[1].getSortKey().asString(), pagination.getAfter());
    assertEquals(2, consumer.index_);
  }
  
  @Test
  public void testGetThreeFour() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    Checker consumer = new Checker(2);
       
    IKvPagination pagination = table.fetchPartitionObjects(new KvPartitionKeyProvider(PARTITION_KEY1), true, 2, ITEMS[1].getSortKey().asString(), null, null, consumer, trace);

    assertEquals(ITEMS[2].getSortKey().asString(), pagination.getBefore());
    assertEquals(ITEMS[3].getSortKey().asString(), pagination.getAfter());
    assertEquals(4, consumer.index_);
  }
  
  @Test
  public void testGetFourThree() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    Checker consumer = new Checker(2, -1);
       
    IKvPagination pagination = table.fetchPartitionObjects(
        new KvPartitionKeyProvider(PARTITION_KEY1), false, 2, ITEMS[3].getSortKey().asString(), null, null, consumer, trace);
   
    assertEquals(ITEMS[2].getSortKey().asString(), pagination.getBefore());
    assertEquals(ITEMS[1].getSortKey().asString(), pagination.getAfter());
    assertEquals(0, consumer.index_);
  }
  
  @Test
  public void testUpdate() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    String object = table.fetch(ITEMS[1], trace);
    
    assertEquals(ITEMS[1].getJson(), object);
    
    Set<IKvItem> items = new HashSet<>();
    
    items.add(new KvItem(PART1, "2", "Updated Two"));
    
    table.update(ITEMS[1], ITEMS[1].getAbsoluteHash(), 
        items, trace);
  }
  
  @Test(expected = NoSuchObjectException.class)
  public void testUpdate2() throws NoSuchObjectException
  {
    InMemoryKvTable table = createTable();
    
    String object = table.fetch(ITEMS[1], trace);
    
    assertEquals(ITEMS[1].getJson(), object);
    
    Set<IKvItem> items = new HashSet<>();
    
    items.add(new KvItem(PART1, "2", "Updated Two"));
    
    table.update(ITEMS[1], ITEMS[1].getAbsoluteHash(), 
        items, trace);
    
    table.update(ITEMS[1], ITEMS[1].getAbsoluteHash(), 
        items, trace);
  }
  
  private InMemoryKvTable createTable()
  {
    InMemoryKvTable table = new InMemoryKvTable.Builder().withServiceId("test").build();
    
    List<IKvItem> items = new ArrayList<>(ITEMS.length);
    
    for(IKvItem item : ITEMS)
      items.add(item);
    
    table.store(items, trace);
    
    return table;
  }

  static class KvItem implements IKvItem
  {
    private final String partitionKey_;
    private final String sortKey_;
    private final String value_;
    private final Map<String, Object> additionalAttributes_;
    
    public KvItem(String partitionKey, String sortKey, String value)
    {
      partitionKey_ = partitionKey;
      sortKey_ = sortKey;
      value_ = value;
      additionalAttributes_ = null;
    }

    public KvItem(String partitionKey, String sortKey, String value, String attrName, Object attrValue)
    {
      super();
      partitionKey_ = partitionKey;
      sortKey_ = sortKey;
      value_ = value;
      additionalAttributes_ = new HashMap<>();
      additionalAttributes_.put(attrName, attrValue);
    }

    @Override
    public IKvSortKey getSortKey()
    {
      return new KvSortKey(sortKey_);
    }

    @Override
    public IKvPartitionKey getPartitionKey()
    {
      return new KvPartitionKey(partitionKey_);
    }

    @Override
    public IFuguePodId getPodId()
    {
      return POD_ID;
    }

    @Override
    public String getJson()
    {
      return value_;
    }

    @Override
    public String getType()
    {
      return "TestItem";
    }

    @Override
    public Instant getPurgeDate()
    {
      return null;
    }

    @Override
    public boolean isSaveToSecondaryStorage()
    {
      return false;
    }

    @Override
    public Hash getAbsoluteHash()
    {
      return HashProvider.getHashOf(value_.getBytes());
    }

    @Override
    public Map<String, Object> getAdditionalAttributes()
    {
      return additionalAttributes_;
    }
    
  }
}
