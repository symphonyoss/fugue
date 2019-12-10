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

package org.symphonyoss.s2.fugue.aws.kv.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.fault.TransientTransactionFault;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.aws.AwsTags;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPartitionKeyProvider;
import org.symphonyoss.s2.fugue.kv.IKvPartitionSortKeyProvider;
import org.symphonyoss.s2.fugue.kv.table.AbstractKvTable;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.BatchWriteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CancellationReason;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.Tag;
import com.amazonaws.services.dynamodbv2.model.TagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveDescription;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.amazonaws.services.dynamodbv2.model.Update;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;

/**
 * DynamoDB implementation of IKvTable
 * 
 * @author Bruce Skingle
 *
 * @param <T> Concrete type for fluent methods.
 */
public abstract class AbstractDynamoDbKvTable<T extends AbstractDynamoDbKvTable<T>> extends AbstractKvTable<T>
{
  private static final Logger         log_                   = LoggerFactory.getLogger(AbstractDynamoDbKvTable.class);

  public static final String       ColumnNamePartitionKey = "pk";
  public static final String       ColumnNameSortKey      = "sk";
  public static final String       ColumnNameDocument     = "d";
  public static final String       ColumnNamePodId        = "p";
  public static final String       ColumnNamePayloadType  = "pt";
  public static final String       ColumnNameTTL          = "t";
  public static final String       ColumnNameCreatedDate  = "c";
  public static final String       ColumnNameAbsoluteHash = "h";

  protected static final int          MAX_RECORD_SIZE        = 400 * 1024;

  protected final String              region_;

  protected AmazonDynamoDB            amazonDynamoDB_;
  protected DynamoDB                  dynamoDB_;
  protected Table                     objectTable_;

  protected final String              objectTableName_;
  protected final int                 payloadLimit_;
  protected final boolean             validate_;
  protected final boolean             enableSecondaryStorage_;
  protected final StreamSpecification streamSpecification_;
  
  protected AbstractDynamoDbKvTable(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    region_ = builder.region_;
    payloadLimit_ = builder.payloadLimit_;
    validate_ = builder.validate_;
    enableSecondaryStorage_ = builder.enableSecondaryStorage_;
    streamSpecification_ = builder.streamSpecification_;
  
    log_.info("Starting storage...");
    
    
    
    amazonDynamoDB_ = builder.amazonDynamoDBClientBuilder_.build();
    
    dynamoDB_               = new DynamoDB(amazonDynamoDB_);
    objectTableName_        = nameFactory_.getTableName("objects").toString();
    objectTable_            = dynamoDB_.getTable(objectTableName_);
    
        
    validate();
    
    log_.info("storage started.");
  }
  
  protected void validate()
  {
    if(validate_)
    {
      try(FaultAccumulator report = new FaultAccumulator())
      {
        try
        {
          objectTable_.describe();
        }
        catch(ResourceNotFoundException e)
        {
          report.error("Object table does not exist");
        }
      }
    }
  }

  @Override
  public String fetch(IKvPartitionSortKeyProvider partitionSortKey, ITraceContext trace) throws NoSuchObjectException
  {
    return doDynamoReadTask(() ->
    {
      GetItemSpec spec = new GetItemSpec().withPrimaryKey(ColumnNamePartitionKey, getPartitionKey(partitionSortKey), ColumnNameSortKey, partitionSortKey.getSortKey().toString());

      Item item = objectTable_.getItem(spec);
      
      if(item == null)
        throw new NoSuchObjectException("Item (" + getPartitionKey(partitionSortKey) + ", " + partitionSortKey.getSortKey() + ") not found.");
      
      String payloadString = item.getString(ColumnNameDocument);
      
      if(payloadString == null)
      {
        Hash absoluteHash = Hash.ofBase64String(item.getString(ColumnNameAbsoluteHash));
        
        payloadString = fetchFromSecondaryStorage(absoluteHash, trace);
      }
      
      return payloadString;
    });
  }

  @Override
  public String fetchFirst(IKvPartitionKeyProvider partitionKey, ITraceContext trace) throws NoSuchObjectException
  {
    return fetchOne(partitionKey, true, trace);
  }

  @Override
  public String fetchLast(IKvPartitionKeyProvider partitionKey, ITraceContext trace) throws NoSuchObjectException
  {
    return fetchOne(partitionKey, false, trace);
  }

  private String fetchOne(IKvPartitionKeyProvider partitionKey, boolean scanForwards, ITraceContext trace) throws NoSuchObjectException
  {
    return doDynamoReadTask(() ->
    {
      trace.trace("START_FETCH_ONE");
      QuerySpec spec = new QuerySpec()
        .withKeyConditionExpression(ColumnNamePartitionKey + " = :v_partition")
        .withMaxResultSize(1)
        .withValueMap(new ValueMap()
            .withString(":v_partition", getPartitionKey(partitionKey)))
        .withScanIndexForward(scanForwards)
        ;
    
      ItemCollection<QueryOutcome> items = objectTable_.query(spec);
      
      Iterator<Item> it = items.firstPage().iterator();
      
      if(it.hasNext())
      {
        Item item = it.next();
        
        String payloadString = item.getString(ColumnNameDocument);
            
        if(payloadString == null)
        {
          Hash absoluteHash = Hash.ofBase64String(item.getString(ColumnNameAbsoluteHash));
          
          payloadString = fetchFromSecondaryStorage(absoluteHash, trace);
        }
        
        trace.trace("DONE_FETCH_ONE");
        return payloadString;
      }
      
      throw new NoSuchObjectException(partitionKey + " not found");
    });
  }
  
  protected <CT> CT doDynamoQueryTask(Callable<CT> task)
  {
    try
    {
      return doDynamoReadTask(task);
    }
    catch (NoSuchObjectException e)
    {
      // This "can't happen"
      throw new TransactionFault(e);
    }
  }

  protected <CT> CT doDynamoReadTask(Callable<CT> task) throws NoSuchObjectException
  {
    return doDynamoReadTask(task, NoOpTraceContext.INSTANCE);
  }

  protected <CT> CT doDynamoReadTask(Callable<CT> task, ITraceContext trace) throws NoSuchObjectException
  {
    return doDynamoTask(task, "read", trace);
  }

  @Override
  public void store(Collection<IKvItem> kvItems, ITraceContext trace)
  {
    if(kvItems.isEmpty())
      return;

    Hash        absoluteHash = null;
    List<TransactWriteItem> actions = new ArrayList<>(kvItems.size());
    
    for(IKvItem kvItem : kvItems)
    {
      String partitionKey = getPartitionKey(kvItem);
      String sortKey = kvItem.getSortKey().asString();
      
      UpdateOrPut updateOrPut = new UpdateOrPut(kvItem, partitionKey, sortKey, payloadLimit_);
      
      absoluteHash = kvItem.getAbsoluteHash();
      
      if(kvItem.isSaveToSecondaryStorage())
        storeToSecondaryStorage(kvItem, updateOrPut.payloadNotStored_, trace);
      
      actions.add(new TransactWriteItem().withPut(updateOrPut.createPut()));
    }
    
    try
    {
      write(actions, absoluteHash, trace);
    }
    catch (NoSuchObjectException e)
    {
      throw new TransactionFault(e);
    }
  }
  
  class Condition
  {
    String                      expression_;
    Map<String, AttributeValue> attributeValues_ = new HashMap<>();
    
    Condition(String expression)
    {
      expression_ = expression;
    }
    
    Condition withString(String name, String value)
    {
      attributeValues_.put(name, new AttributeValue().withS(value));
      
      return this;
    }
  }
  
  class DeleteConsumer extends AbstractItemConsumer
  {
    List<PrimaryKey>            primaryKeysToDelete_ = new ArrayList<>(24);
    IKvPartitionSortKeyProvider absoluteHashPrefix_;
    
    public DeleteConsumer(IKvPartitionSortKeyProvider absoluteHashPrefix)
    {
      absoluteHashPrefix_ = absoluteHashPrefix;
    }

    @Override
    void consume(Item item, ITraceContext trace)
    {
      primaryKeysToDelete_.add(new PrimaryKey(
          new KeyAttribute(ColumnNamePartitionKey,  item.getString(ColumnNamePartitionKey)),
          new KeyAttribute(ColumnNameSortKey,       item.getString(ColumnNameSortKey))
          )
        );
      
      Hash absoluteHash = Hash.newInstance(item.getString(ColumnNameAbsoluteHash));
      
      primaryKeysToDelete_.add(new PrimaryKey(
          new KeyAttribute(ColumnNamePartitionKey,  getPartitionKey(absoluteHashPrefix_) + absoluteHash),
          new KeyAttribute(ColumnNameSortKey,       absoluteHashPrefix_.getSortKey().asString())
          )
        );
      
      deleteFromSecondaryStorage(absoluteHash, trace);
    }
    
    void dynamoBatchWrite()
    {
      if(primaryKeysToDelete_.isEmpty())
        return;
      
      TableWriteItems tableWriteItems = new TableWriteItems(objectTable_.getTableName())
          .withPrimaryKeysToDelete(primaryKeysToDelete_.toArray(new PrimaryKey[primaryKeysToDelete_.size()]));
      
      BatchWriteItemOutcome outcome = dynamoDB_.batchWriteItem(tableWriteItems);
      int totalRequestItems = primaryKeysToDelete_.size();
      long  delay = 4;
      do
      {
          Map<String, List<WriteRequest>> unprocessedItems = outcome.getUnprocessedItems();

          if (outcome.getUnprocessedItems().size() > 0)
          {
            int requestItems = 0;
            
            for(List<WriteRequest> ui : unprocessedItems.values())
            {
              requestItems += ui.size();
            }
      
            log_.info("Retry " + requestItems + " of " + totalRequestItems + " items after " + delay + "ms.");
            
            totalRequestItems = requestItems;
            
            try
            {
              Thread.sleep(delay);
              
              if(delay < 1000)
                delay *= 1.2;
            }
            catch (InterruptedException e)
            {
              log_.warn("Sleep interrupted", e);
            }
            
            outcome = dynamoDB_.batchWriteItemUnprocessed(unprocessedItems);
          }
      } while (outcome.getUnprocessedItems().size() > 0);
    }
  }
  
  @Override
  public void delete(IKvPartitionSortKeyProvider partitionSortKeyProvider, Hash absoluteHash,
      IKvPartitionKeyProvider versionPartitionKey, IKvPartitionSortKeyProvider absoluteHashPrefix, ITraceContext trace) throws NoSuchObjectException
  {
    List<TransactWriteItem> actions = new ArrayList<>(2);
    
    String    existingPartitionKey = getPartitionKey(partitionSortKeyProvider);
    String    existingSortKey = partitionSortKeyProvider.getSortKey().asString();
    Condition condition = new Condition(ColumnNameAbsoluteHash + " = :ah").withString(":ah", absoluteHash.toStringBase64());
    
    deleteFromSecondaryStorage(absoluteHash, trace);
    
    String after = null;
    do
    {
      DeleteConsumer deleteConsumer = new DeleteConsumer(absoluteHashPrefix);
      
      after = doFetchPartitionObjects(versionPartitionKey, true, 12, after, deleteConsumer, trace);
      
      deleteConsumer.dynamoBatchWrite();
    } while (after != null);
    
    final Map<String, AttributeValue> itemKey = new HashMap<>();
    
    itemKey.put(ColumnNamePartitionKey, new AttributeValue(existingPartitionKey));
    itemKey.put(ColumnNameSortKey, new AttributeValue(existingSortKey));
   
    Delete delete = new Delete()
        .withTableName(objectTable_.getTableName())
        .withConditionExpression(condition.expression_)
        .withExpressionAttributeValues(condition.attributeValues_)
        .withKey(itemKey)
        ;
    
    actions.add(new TransactWriteItem().withDelete(delete));
    
    write(actions, absoluteHash, trace);
  }
  
  @Override
  public void update(IKvPartitionSortKeyProvider partitionSortKeyProvider, Hash absoluteHash, Set<IKvItem> kvItems,
      ITraceContext trace) throws NoSuchObjectException
  {
    List<TransactWriteItem> actions = new ArrayList<>(kvItems.size() + 2);
    
    String    existingPartitionKey = getPartitionKey(partitionSortKeyProvider);
    String    existingSortKey = partitionSortKeyProvider.getSortKey().asString();
    Condition condition = new Condition(ColumnNameAbsoluteHash + " = :ah").withString(":ah", absoluteHash.toStringBase64());
    
    for(IKvItem kvItem : kvItems)
    {
      String partitionKey = getPartitionKey(kvItem);
      String sortKey = kvItem.getSortKey().asString();
      
      UpdateOrPut updateOrPut = new UpdateOrPut(kvItem, partitionKey, sortKey, payloadLimit_);
      
      if(existingPartitionKey.equals(partitionKey) && existingSortKey.equals(sortKey))
      {
        actions.add(new TransactWriteItem()
            .withUpdate(
                updateOrPut.createUpdate(condition)
                )
            );
        
        condition = null;
      }
      else
      {
        actions.add(new TransactWriteItem()
            .withPut(updateOrPut.createPut()
                )
            );
                
      }
      
      if(kvItem.isSaveToSecondaryStorage())
        storeToSecondaryStorage(kvItem, updateOrPut.payloadNotStored_, trace);
    }
    
    if(condition != null)
    {
      // The prev version has a different sort key

      final Map<String, AttributeValue> itemKey = new HashMap<>();
      
      itemKey.put(ColumnNamePartitionKey, new AttributeValue(existingPartitionKey));
      itemKey.put(ColumnNameSortKey, new AttributeValue(existingSortKey));
     
      Delete delete = new Delete()
          .withTableName(objectTable_.getTableName())
          .withConditionExpression(condition.expression_)
          .withExpressionAttributeValues(condition.attributeValues_)
          .withKey(itemKey)
          ;
      
      actions.add(new TransactWriteItem().withDelete(delete));
    }
    
    write(actions, absoluteHash, trace);
  }
  
  protected void write(Collection<TransactWriteItem> actions, Hash absoluteHash, ITraceContext trace) throws NoSuchObjectException
  {
    TransactWriteItemsRequest request = new TransactWriteItemsRequest()
        .withTransactItems(actions);
     
    doDynamoConditionalWriteTask(() -> 
    {
      int                           retryCnt = 0;
      long                          delay = 4;
      TransactionCanceledException  lastException = null;
      
      while(retryCnt++ < 11)
      {
        try
        {
  //        long start = System.currentTimeMillis();
          amazonDynamoDB_.transactWriteItems(request);
  //        long end = System.currentTimeMillis();
  //        
  //        System.err.println("T " + (end - start));
          return null;
        }
        catch (TransactionCanceledException tce)
        {
          lastException = tce;
          
          for(CancellationReason reason : tce.getCancellationReasons())
          {
            switch(reason.getCode())
            {
              case "ConditionalCheckFailed":
                throw new NoSuchObjectException("Object " + absoluteHash + " has changed.");
                
              case "None":
                // there is an entry for each item in the transaction, this means nothing and should be ignored.
                break;
                
              case "TransactionConflict":
                log_.info("Retry transaction after " + delay + "ms.");
                try
                {
                  Thread.sleep(delay);
                  
                  if(delay < 1000)
                    delay *= 1.2;
                }
                catch (InterruptedException e)
                {
                  log_.warn("Sleep interrupted", e);
                }
                break;
                
              default:
                throw new TransientTransactionFault("Transient failure to update object " + absoluteHash, tce);
            }
          }
        }
      }
      
      throw new TransientTransactionFault("Transient failure to update (after " + retryCnt + " retries) object " + absoluteHash, lastException);
    }
    , trace);  
    
  }

  /**
   * Fetch the given item from secondary storage.
   * 
   * @param absoluteHash      Absolute hash of the required object.
   * @param trace             Trace context.
   * 
   * @return The required object.
   * 
   * @throws NoSuchObjectException If the required object does not exist.
   */
  protected abstract @Nonnull String fetchFromSecondaryStorage(Hash absoluteHash, ITraceContext trace) throws NoSuchObjectException;
  
  /**
   * Store the given item to secondary storage.
   * 
   * @param kvItem            An item to be stored.
   * @param payloadNotStored  The payload is too large to store in primary storage.
   * @param trace             A trace context.
   */
  protected abstract void storeToSecondaryStorage(IKvItem kvItem, boolean payloadNotStored, ITraceContext trace);

  /**
   * Delete the given object from secondary storage.
   * 
   * @param absoluteHash  Hash of the item to be deleted.
   * @param trace         A trace context.
   */
  protected abstract void deleteFromSecondaryStorage(Hash absoluteHash, ITraceContext trace);
  
//  protected void write(List<Put> items, ITraceContext trace)
//  {
//    doDynamoWriteTask(() -> 
//    {
//      dynamoBatchWrite(items);
//      trace.trace("WRITTEN-DYNAMODB");
//      
//      return null;
//     }
//    , trace);
//  }

  protected <TT> TT doDynamoTask(Callable<TT> task, String accessMode, ITraceContext trace) throws NoSuchObjectException
  {
    String message = "Failed to " + accessMode + " object";
    
    try
    {
      return task.call();
    }
    catch(ProvisionedThroughputExceededException e)
    {
      log_.warn(message + " - Provisioned Throughput Exceeded", e);
      trace.trace("FAILED-THROUGHPUT-DYNAMODB");
//      try
//      {
//        objectTableHelper_.scaleOutWrite();
//      }
//      catch(RuntimeException e2)
//      {
//        log_.error("Failed to scale out", e2);
//      }
      
      throw new TransientTransactionFault(message, e);
    } 
    catch (AmazonServiceException e)
    {
      trace.trace("FAILED-AWSEXCEPTION-DYNAMODB");
      log_.error(message, e);
      throw new TransactionFault(message, e);
    } 
    catch (TransactionFault | NoSuchObjectException e)
    {
      throw e;
    } 
    catch (Exception e) // Callable made me do this...
    {
      trace.trace("FAILED-UNEXPECTED-DYNAMODB");
      log_.error("UNEXPECTED EXCEPTION", e);
      throw new TransactionFault(message, e);
    }
  }
  
  protected void doDynamoWriteTask(Callable<Void> task, ITraceContext trace)
  {
    try
    {
      doDynamoTask(task, "write", trace);
    }
    catch (NoSuchObjectException e)
    {
      trace.trace("FAILED-UNEXPECTED-DYNAMODB");
      log_.error("UNEXPECTED EXCEPTION", e);
      throw new TransactionFault("Failed to write object", e);
    }
  }
  
  protected void doDynamoConditionalWriteTask(Callable<Void> task, ITraceContext trace) throws NoSuchObjectException
  {
    try
    {
      doDynamoTask(task, "write", trace);
    }
    catch (NoSuchObjectException e)
    {
      throw e;
    }
  }

//  protected void dynamoBatchWrite(Collection<Put> itemsToPut)
//  {
//    TableWriteItems tableWriteItems = new TableWriteItems(objectTable_.getTableName())
//        .withItemsToPut(itemsToPut)
//        ;
//    
//    if(primaryKeysToDelete != null)
//      tableWriteItems = tableWriteItems.withPrimaryKeysToDelete(primaryKeysToDelete.toArray(new PrimaryKey[primaryKeysToDelete.size()]));
//    
//    Map<String, List<WriteRequest>> requestItems = new HashMap<>();
//    
//    new WriteRequest().withPutRequest(new PutRequest()
//    
//    List<WriteRequest> value;
//    requestItems.put(objectTable_.getTableName(), value);
//    BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);
//    BatchWriteItemResult outcome = amazonDynamoDB_.batchWriteItem(batchWriteItemRequest);
//    
//    
//    int requestItems = itemsToPut.size();
//    long  delay = 4;
//    do
//    {
//        Map<String, List<WriteRequest>> unprocessedItems = outcome.getUnprocessedItems();
//
//        if (outcome.getUnprocessedItems().size() > 0)
//        {
//          requestItems = 0;
//          
//          for(List<WriteRequest> ui : unprocessedItems.values())
//          {
//            requestItems += ui.size();
//          }
//    
//          log_.info("Retry " + requestItems + " of " + requestItems + " items after " + delay + "ms.");
//          try
//          {
//            Thread.sleep(delay);
//            
//            if(delay < 1000)
//              delay *= 1.2;
//          }
//          catch (InterruptedException e)
//          {
//            log_.warn("Sleep interrupted", e);
//          }
//          
//          outcome = dynamoDB_.batchWriteItemUnprocessed(unprocessedItems);
//        }
//    } while (outcome.getUnprocessedItems().size() > 0);
//  }
  
  class UpdateOrPut
  {
    Map<String, AttributeValue> key_              = new HashMap<>();
    ValueMap                    putItem_          = new ValueMap();
    Map<String, AttributeValue> updateItem_       = new HashMap<>();
    StringBuilder               updateExpression_ = new StringBuilder("SET ");
    int                         baseLength_;
    boolean                     payloadNotStored_;
    boolean                     first_            = true;
    
    UpdateOrPut(IKvItem kvItem, String partitionKey, String sortKey, int payloadLimit)
    {
      putItem_.withString(ColumnNamePartitionKey,  partitionKey);
      putItem_.withString(ColumnNameSortKey,       sortKey);
      
      key_.put(ColumnNamePartitionKey,  new AttributeValue(partitionKey));
      key_.put(ColumnNameSortKey,       new AttributeValue(sortKey));
      
      baseLength_ = ColumnNamePartitionKey.length() + partitionKey.length() + 
          ColumnNameSortKey.length() + sortKey.length();
      
      withHash(   ColumnNameAbsoluteHash, kvItem.getAbsoluteHash());
      withNumber( ColumnNamePodId,        kvItem.getPodId().getValue());
      withString( ColumnNamePayloadType,  kvItem.getType());
      
      if(kvItem.getPurgeDate() != null)
      {
        Long ttl = kvItem.getPurgeDate().toEpochMilli() / 1000;
        
        withNumber( ColumnNameTTL,          ttl);
      }
      
      int length = baseLength_ + ColumnNameDocument.length() + kvItem.getJson().length();
      
      if(length < payloadLimit)
      {
        payloadNotStored_ = false;
        withString(ColumnNameDocument, kvItem.getJson());
      }
      else
      {
        payloadNotStored_ = true;
      }
    }
    
    Put createPut()
    {
      return new Put()
        .withTableName(objectTable_.getTableName())
        .withItem(ItemUtils.fromSimpleMap(putItem_));
    }
    
    Update createUpdate(Condition condition)
    {
      updateItem_.putAll(condition.attributeValues_);
      
      return new Update()
        .withTableName(objectTable_.getTableName())
        .withConditionExpression(condition.expression_)
        .withExpressionAttributeValues(updateItem_)
        .withKey(key_)
        .withUpdateExpression(updateExpression_.toString())
      ;
    }

    private void withNumber(String name, Number value)
    {
      separator();
      updateExpression_.append(name + " = :" + name);
      
      if(value == null)
      {
        putItem_.withNull(name);
        updateItem_.put(":" + name, new AttributeValue().withNULL(true));
        baseLength_ += name.length();
      }
      else
      {
        putItem_.withNumber(name, value);
        updateItem_.put(":" + name, new AttributeValue().withN(value.toString()));
        baseLength_ += name.length() + value.toString().length();
      }
    }

    private void separator()
    {
      if(first_)
        first_ = false;
      else
        updateExpression_.append(", ");
      
    }

    private void withHash(String name, Hash value)
    {
      withString(name, value == null ? null : value.toStringBase64());
    }

    private void withString(String name, String value)
    {
      separator();
      updateExpression_.append(name + " = :" + name);
    
      if(value == null)
      {
        baseLength_ += name.length();
        putItem_.withNull(name);
        updateItem_.put(":" + name, new AttributeValue().withNULL(true));
      }
      else
      {
        baseLength_ += name.length() + value.length();
        putItem_.withString(name, value);
        updateItem_.put(":" + name, new AttributeValue().withS(value));
      }
    }

//    private void withJSON(String name, String value)
//    {
//      updateExpression_.append(name + " = :" + name);
//      
//      if(value == null)
//      {
//        baseLength_ += name.length();
//        putItem_.withNull(name);
//        updateItem_.put(":" + name, new AttributeValue().withNULL(true));
//      }
//      else
//      {
//        baseLength_ += name.length() + value.length();
//        putItem_.withJSON(name, value);
//        updateItem_.put(":" + name, new AttributeValue().with(value));
//        updateItem_.withJSON(":" + name, value);
//      }
//    }
  }
  
//  protected boolean createPut(IKvItem kvItem, String partitionKey, String sortKey, int payloadLimit, List<Put> items, Condition condition)
//  {
//    ValueMap putItem = new ValueMap();
//    ValueMap updateItem = new ValueMap();
//    StringBuilder updateExpression = new StringBuilder();
//    
//    putItem.withString(ColumnNamePartitionKey,  partitionKey);
//    putItem.withString(ColumnNameSortKey,       sortKey);
//    
//    int baseLength = ColumnNamePartitionKey.length() + partitionKey.length() + 
//        ColumnNameSortKey.length() + sortKey.length();
//    
//    if(kvItem.getAbsoluteHash() == null)
//    {
//      updateExpression.append(ColumnNameAbsoluteHash + " = null");
//    }
//    else
//    {
//      String ah = kvItem.getAbsoluteHash().toStringBase64();
//      
//      baseLength += ColumnNameAbsoluteHash.length() + ah.length();
//      putItem.withString(ColumnNameAbsoluteHash, ah);
//    }
//    
//    if(kvItem.getPodId() != null)
//    {
//      baseLength += ColumnNamePodId.length() + kvItem.getPodId().toString().length();
//      
//      putItem.withNumber(ColumnNamePodId, kvItem.getPodId().getValue());
//    }
//
//    if(kvItem.getType() != null)
//    {
//      baseLength += ColumnNamePayloadType.length() + kvItem.getType().length();
//      
//      putItem.withString(ColumnNamePayloadType, kvItem.getType());
//    }
//    
//    if(kvItem.getPurgeDate() != null)
//    {
//      long ttl = kvItem.getPurgeDate().toEpochMilli() / 1000;
//      
//      baseLength += ColumnNameTTL.length() + String.valueOf(ttl).length();
//      
//      putItem.withNumber(ColumnNameTTL, ttl);
//    }
//    
//    int length = baseLength + ColumnNameDocument.length() + kvItem.getJson().length();
//    
//    if(length < payloadLimit)
//    {
//      putItem.withJSON(ColumnNameDocument, kvItem.getJson());
//    }
//    
//    
//    
//    if(condition == null)
//    {
//      
//      Put put = new Put()
//          .withTableName(objectTable_.getTableName())
//          .withItem(ItemUtils.fromSimpleMap(putItem));
//    }
//    else
//    {
//      Update update = new Update()
//          .withTableName(objectTable_.getTableName())
//          .with
//          ;
//      put
//        .withConditionExpression(condition.expression_)
//        .withExpressionAttributeValues(condition.attributeValues_)
//        ;
//    }
//    
//    items.add(put);
//    
//    return length >= payloadLimit;
//  }
  

//  protected Put createPut2(IKvItem kvItem, String partitionKey, String sortKey, int payloadLimit)
//  {
//    HashMap<String, AttributeValue> item = new HashMap<>();
//    
//    item.put(ColumnNamePartitionKey,  new AttributeValue(partitionKey));
//    item.put(ColumnNameSortKey,       new AttributeValue(sortKey));
//    
//    int baseLength = ColumnNamePartitionKey.length() + partitionKey.length() + 
//        ColumnNameSortKey.length() + sortKey.length();
//    
//    if(kvItem.getAbsoluteHash() != null)
//    {
//      String ah = kvItem.getAbsoluteHash().toStringBase64();
//      
//      baseLength += ColumnNameAbsoluteHash.length() + ah.length();
//      item.put(ColumnNameAbsoluteHash, new AttributeValue(ah));
//    }
//    
//    if(kvItem.getPodId() != null)
//    {
//      baseLength += ColumnNamePodId.length() + kvItem.getPodId().toString().length();
//      
//      item.put(ColumnNamePodId, new AttributeValue().withN(kvItem.getPodId().getValue()));
//    }
//
//    if(kvItem.getType() != null)
//    {
//      baseLength += ColumnNamePayloadType.length() + kvItem.getType().length();
//      
//      item.put(ColumnNamePayloadType, new AttributeValue(kvItem.getType()));
//    }
//    
//    if(kvItem.getPurgeDate() != null)
//    {
//      String ttl = String.valueOf(kvItem.getPurgeDate().toEpochMilli() / 1000);
//      
//      baseLength += ColumnNameTTL.length() + ttl.length();
//      
//      item.put(ColumnNameTTL, new AttributeValue().withN(ttl));
//    }
//    
//    int length = baseLength + ColumnNameDocument.length() + kvItem.getJson().length();
//    
//    if(length < payloadLimit)
//    {
//      
//      item.put(ColumnNameDocument, new AttributeValue().withM(
//          valueConformer.transform(Jackson.fromJsonString(kvItem.getJson(), Object.class))));
//      return false;
//    }
//    else
//    {
//      return true;
//    }
//  }

//  protected boolean createPutItem(List<Item> items, IKvItem kvItem, String partitionKey, String sortKey, int payloadLimit)
//  {
//    Item item = new Item()
//        .withPrimaryKey(ColumnNamePartitionKey, 
//            partitionKey, 
//            ColumnNameSortKey, sortKey);
//    
//    int baseLength = ColumnNamePartitionKey.length() + partitionKey.length() + 
//        ColumnNameSortKey.length() + sortKey.length();
//    
//    if(kvItem.getAbsoluteHash() != null)
//    {
//      String ah = kvItem.getAbsoluteHash().toStringBase64();
//      
//      baseLength += ColumnNameAbsoluteHash.length() + ah.length();
//      
//      item.withString(ColumnNameAbsoluteHash, ah);
//    }
//    
//    if(kvItem.getPodId() != null)
//    {
//      baseLength += ColumnNamePodId.length() + kvItem.getPodId().toString().length();
//      
//      item.withInt(ColumnNamePodId, kvItem.getPodId().getValue());
//    }
//
//    if(kvItem.getType() != null)
//    {
//      baseLength += ColumnNamePayloadType.length() + kvItem.getType().length();
//      
//      item.withString(ColumnNamePayloadType, kvItem.getType());
//    }
//    
//    if(kvItem.getPurgeDate() != null)
//    {
//      long ttl = kvItem.getPurgeDate().toEpochMilli() / 1000;
//      
//      baseLength += ColumnNameTTL.length() + String.valueOf(ttl).length();
//      
//      item.withLong(ColumnNameTTL,       ttl);
//    }
//    
//    items.add(item);
//    
//    int length = baseLength + ColumnNameDocument.length() + kvItem.getJson().length();
//    
//    if(length < payloadLimit)
//    {
//      item.withJSON(ColumnNameDocument, kvItem.getJson());
//      return false;
//    }
//    else
//    {
//      return true;
//    }
//  }

  private String getPartitionKey(IKvPartitionKeyProvider kvItem)
  {
    if(podPrivate_)
      return serviceId_ + Separator + kvItem.getPodId() + Separator + kvItem.getPartitionKey();
    else
      return serviceId_ + Separator + kvItem.getPartitionKey();
  }

  @Override
  public void start()
  {
  }

  @Override
  public void stop()
  {
    if(amazonDynamoDB_ != null)
      amazonDynamoDB_.shutdown();
  }

//  private DynamoDbTableAdmin createTableAdmin()
//  {
//    return new DynamoDbTableAdmin(nameFactory_, objectTable_, getAmazonDynamoDB(), stsManager_)
//    {
//    
//      @Override
//      protected CreateTableRequest createCreateTableRequest()
//      {
//        return new CreateTableRequest()
//
//            .withTableName(objectTable_.getTableName())
//            .withAttributeDefinitions(
//                new AttributeDefinition(ColumnNameHashKey, ScalarAttributeType.S),
//                new AttributeDefinition(ColumnNameSortKey, ScalarAttributeType.S)
//                )
//            .withKeySchema(new KeySchemaElement(ColumnNameHashKey, KeyType.HASH), new KeySchemaElement(ColumnNameSortKey, KeyType.RANGE))
//            ;
//      }
//    }
//    .withTtlColumnName(ColumnNameTTL);
//  }
  
  @Override
  public void createTable(boolean dryRun)
  {
    List<Tag> tags = new AwsTags(nameFactory_.getTags())
        .put(Fugue.TAG_FUGUE_SERVICE, serviceId_)
        .put(Fugue.TAG_FUGUE_ITEM, objectTableName_)
        .getDynamoTags();
    
    String tableArn;
    
    try
    {
      TableDescription tableInfo = amazonDynamoDB_.describeTable(objectTableName_).getTable();

      tableArn = tableInfo.getTableArn();

      log_.info("Table \"" + objectTableName_ + "\" already exists as " + tableArn);
      
      configureStream(streamSpecification_, tableInfo);
    }
    catch (ResourceNotFoundException e)
    {
      // Table does not exist, create it
      
      if(dryRun)
      {
        log_.info("Table \"" + objectTableName_ + "\" does not exist and would be created");
        return;
      }
      else
      {
        try
        {
          CreateTableRequest    request;
          CreateTableResult     result;
          
          request = new CreateTableRequest()
              .withTableName(objectTable_.getTableName())
              .withAttributeDefinitions(
                  new AttributeDefinition(ColumnNamePartitionKey, ScalarAttributeType.S),
                  new AttributeDefinition(ColumnNameSortKey, ScalarAttributeType.S)
                  )
              .withKeySchema(new KeySchemaElement(ColumnNamePartitionKey, KeyType.HASH), new KeySchemaElement(ColumnNameSortKey, KeyType.RANGE))
              .withBillingMode(BillingMode.PAY_PER_REQUEST)
              .withStreamSpecification(streamSpecification_)
              ;
          
          result = amazonDynamoDB_.createTable(request);
          tableArn = result.getTableDescription().getTableArn();
          
          log_.info("Table \"" + objectTableName_ + "\" created as " + tableArn);
        }
        catch (RuntimeException e2)
        {
          log_.error("Failed to create tables", e2);
          throw new ProgramFault(e2);
        }
              
        try
        {
          objectTable_.waitForActive();
        }
        catch (InterruptedException e2)
        {
          throw new ProgramFault(e2);
        }
      }
    }
    
    
//    configureAutoScale();
    
    try
    {
      DescribeTimeToLiveRequest describeTimeToLiveRequest = new DescribeTimeToLiveRequest().withTableName(objectTableName_);
      
      DescribeTimeToLiveResult ttlDescResult = amazonDynamoDB_.describeTimeToLive(describeTimeToLiveRequest);
      
      TimeToLiveDescription ttlDesc = ttlDescResult.getTimeToLiveDescription();
      
      if("ENABLED".equals(ttlDesc.getTimeToLiveStatus()))
      {
        log_.info("Table \"" + objectTableName_ + "\" already has TTL enabled.");
      }
      else
      {
        if(dryRun)
        {
          log_.info("Table \"" + objectTableName_ + "\" does not have TTL set and it would be set for column " + ColumnNameTTL);
        }
        else
        {
          //table created now enabling TTL
          UpdateTimeToLiveRequest req = new UpdateTimeToLiveRequest();
          req.setTableName(objectTableName_);
           
          TimeToLiveSpecification ttlSpec = new TimeToLiveSpecification();
          ttlSpec.setAttributeName(ColumnNameTTL);
          ttlSpec.setEnabled(true);
           
          req.withTimeToLiveSpecification(ttlSpec);
           
          UpdateTimeToLiveResult result2 = amazonDynamoDB_.updateTimeToLive(req);
          log_.info("Table \"" + objectTableName_ + "\" TTL updated " + result2);
        }
      }
    }
    catch (RuntimeException e)
    {
      log_.info("Failed to update TTL for table \"" + objectTableName_ + "\"", e);
      throw new ProgramFault(e);
    }
    
    try
    {
      amazonDynamoDB_.tagResource(new TagResourceRequest()
          .withResourceArn(tableArn)
          .withTags(tags)
          );
      log_.info("Table \"" + objectTableName_ + "\" tagged");
    }
    catch (RuntimeException e)
    {
      log_.error("Failed to add tags", e);
      throw new ProgramFault(e);
    }
    
    try
    {
      objectTable_.waitForActive();
    }
    catch (InterruptedException e)
    {
      throw new ProgramFault(e);
    }
  }

//  private void configureAutoScale()
//  {
//    boolean updateTable = false;
//    UpdateTableRequest  updateRequest = new UpdateTableRequest()
//        .withTableName(objectTableName_);
//    
//    TableDescription tableInfo = amazonDynamoDB_.describeTable(objectTableName_).getTable();
//    
//    if(tableInfo.getBillingModeSummary() != null && BillingMode.PAY_PER_REQUEST.toString().equals(tableInfo.getBillingModeSummary().getBillingMode()))
//    {
//      log_.info("Table is set to on-demand - no change made.");
//    }
//    else
//    {
//      log_.info("Updating table to on-demand mode");
//      updateRequest.withBillingMode(BillingMode.PAY_PER_REQUEST);
//      updateTable=true;
//    }
//    
//    if(updateTable)
//    {
//      try
//      {
//        amazonDynamoDB_.updateTable(updateRequest);
//      }
//      catch(AmazonDynamoDBException e)
//      {
//        log_.error("Unable to update table throughput.", e);
//      }
//    }
//  }
  
  private void configureStream(StreamSpecification streamSpecification, TableDescription tableInfo)
  {
    String streamArn = tableInfo.getLatestStreamArn();
    
    if(streamSpecification == null || !streamSpecification.isStreamEnabled())
    {
      if(streamArn != null || tableInfo.getStreamSpecification().isStreamEnabled())
      {
        log_.info("Table has streams enabled, disabling....");
        streamSpecification = new StreamSpecification().withStreamEnabled(false);
      }
      else
      {
        log_.info("Table does not have streams enabled, nothing to do here.");
        return;
      }
    }
    else
    {
      if(streamArn == null || !tableInfo.getStreamSpecification().isStreamEnabled())
      {
        log_.info("Enabling streams for table....");
      }
      else
      {
        log_.info("Table has streams enabled, nothing to do here.");
        return;
      }
    }
    
    
    UpdateTableRequest  updateTableRequest = new UpdateTableRequest()
        .withTableName(objectTable_.getTableName())
        .withStreamSpecification(streamSpecification)
        ;
    
    amazonDynamoDB_.updateTable(updateTableRequest);
    
    log_.info("Stream settings updated.");
  }

  @Override
  public void deleteTable(boolean dryRun)
  {
    try
    {
      TableDescription tableInfo = amazonDynamoDB_.describeTable(objectTableName_).getTable();

      String tableArn = tableInfo.getTableArn();
      
      if(dryRun)
      {
        log_.info("Table \"" + objectTableName_ + "\" with arn " + tableArn + " would be deleted (dry run).");
      }
      else
      {
        log_.info("Deleting table \"" + objectTableName_ + "\" with arn " + tableArn + "...");

        amazonDynamoDB_.deleteTable(new DeleteTableRequest()
            .withTableName(objectTableName_));
      }
    }
    catch (ResourceNotFoundException e)
    {
      log_.info("Table \"" + objectTableName_ + "\" Does not exist.");
    }
  }

  @Override
  public String fetchPartitionObjects(IKvPartitionKeyProvider partitionKey, boolean scanForwards, Integer limit, String after,
      Consumer<String> consumer, ITraceContext trace)
  {
    return doFetchPartitionObjects(partitionKey, scanForwards, limit, after, new PartitionConsumer(consumer), trace);
  }

  private String doFetchPartitionObjects(IKvPartitionKeyProvider partitionKey, boolean scanForwards, Integer limit, String after,
      AbstractItemConsumer consumer, ITraceContext trace)
  {
    return doDynamoQueryTask(() ->
    {
      QuerySpec spec = new QuerySpec()
          .withKeyConditionExpression(ColumnNamePartitionKey + " = :v_partition")
          .withValueMap(new ValueMap()
              .withString(":v_partition", getPartitionKey(partitionKey)))
          .withScanIndexForward(scanForwards)
          ;
      
      if(limit != null)
      {
        spec.withMaxResultSize(limit);
      }
      
      if(after != null)
      {
        spec.withExclusiveStartKey(
            new KeyAttribute(ColumnNamePartitionKey, getPartitionKey(partitionKey)),
            new KeyAttribute(ColumnNameSortKey,  after)
            );
      }
    
      Map<String, AttributeValue> lastEvaluatedKey = null;
      ItemCollection<QueryOutcome> items = objectTable_.query(spec);
      
      for(Page<Item, QueryOutcome> page : items.pages())
      {
        Iterator<Item> it = page.iterator();
        
        while(it.hasNext())
        {
          Item item = it.next();
          
          consumer.consume(item, trace);
        }
      }
      
      lastEvaluatedKey = items.getLastLowLevelResult().getQueryResult().getLastEvaluatedKey();
      
      if(lastEvaluatedKey != null)
      {
        AttributeValue sequenceKeyAttr = lastEvaluatedKey.get(ColumnNameSortKey);
        
        return sequenceKeyAttr.getS();
      }
      
      return null;
    });
  }

  abstract class AbstractItemConsumer
  {
    abstract void consume(Item item, ITraceContext trace);
  }
  
  class PartitionConsumer extends AbstractItemConsumer
  {
    Consumer<String> consumer_;
    
    PartitionConsumer(Consumer<String> consumer)
    {
      consumer_ = consumer;
    }

    @Override
    void consume(Item item, ITraceContext trace)
    {
      String payloadString = item.getString(ColumnNameDocument);
      
      if(payloadString == null)
      {
        String hashString = item.getString(ColumnNameAbsoluteHash);
        Hash absoluteHash = Hash.newInstance(hashString);
        
        try
        {
          payloadString = fetchFromSecondaryStorage(absoluteHash, trace);
        }
        catch (NoSuchObjectException e)
        {
          throw new IllegalStateException("Unable to read known object from S3", e);
        }
      }
      
      consumer_.accept(payloadString);
    }
  }

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractDynamoDbKvTable<B>> extends AbstractKvTable.AbstractBuilder<T,B>
  {
    protected final AmazonDynamoDBClientBuilder amazonDynamoDBClientBuilder_;

    protected String              region_;
    protected int                 payloadLimit_           = MAX_RECORD_SIZE;
    protected boolean             validate_               = true;
    protected boolean             enableSecondaryStorage_ = false;

    protected StreamSpecification streamSpecification_    = new StreamSpecification().withStreamEnabled(false);
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
      
      amazonDynamoDBClientBuilder_ = AmazonDynamoDBClientBuilder.standard();
    }
    
    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(region_,      "region");
    }

    public T withValidate(boolean validate)
    {
      validate_ = validate;
      
      return self();
    }
    
    public T withStreamSpecification(StreamSpecification streamSpecification)
    {
      streamSpecification_ = streamSpecification;
      
      return self();
    }

    public T withEnableSecondaryStorage(boolean enableSecondaryStorage)
    {
      enableSecondaryStorage_ = enableSecondaryStorage;
      
      return self();
    }

    public T withRegion(String region)
    {
      region_ = region;
      
      return self();
    }

    public T withRegion(int payloadLimit)
    {
      payloadLimit_ = Math.min(payloadLimit, MAX_RECORD_SIZE);
      
      return self();
    }

    public T withCredentials(AWSCredentialsProvider credentials)
    {
      amazonDynamoDBClientBuilder_.withCredentials(credentials);
      
      return self();
    }
  }
}
