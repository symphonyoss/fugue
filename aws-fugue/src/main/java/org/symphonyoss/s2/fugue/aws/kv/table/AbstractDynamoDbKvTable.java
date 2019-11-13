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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.fault.TransientTransactionFault;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.aws.AwsTags;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPartitionKey;
import org.symphonyoss.s2.fugue.kv.IKvPartitionSortKey;
import org.symphonyoss.s2.fugue.kv.KvPartitionSortKey;
import org.symphonyoss.s2.fugue.kv.table.AbstractKvTable;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.BatchWriteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.Tag;
import com.amazonaws.services.dynamodbv2.model.TagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveDescription;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
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
public class AbstractDynamoDbKvTable<T extends AbstractDynamoDbKvTable<T>> extends AbstractKvTable<T>
{
  private static final Logger   log_                   = LoggerFactory.getLogger(AbstractDynamoDbKvTable.class);

  protected static final String ColumnNamePartitionKey = "pk";
  protected static final String ColumnNameSortKey      = "sk";
  protected static final String ColumnNameDocument     = "d";
  protected static final String ColumnNamePodId        = "p";
  protected static final String ColumnNamePayloadType  = "pt";
  protected static final String ColumnNameTTL          = "t";
  protected static final String ColumnNameCreatedDate  = "c";

  protected static final int    MAX_RECORD_SIZE        = 400 * 1024;

  protected final String        region_;

  protected AmazonDynamoDB      amazonDynamoDB_;
  protected DynamoDB            dynamoDB_;
  protected Table               objectTable_;

  protected final String        objectTableName_;
  protected final int           payloadLimit_;
  protected final boolean       validate_;
  
  protected AbstractDynamoDbKvTable(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    region_ = builder.region_;
    payloadLimit_ = builder.payloadLimit_;
    validate_ = builder.validate_;
  
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
  public String fetch(IKvPartitionSortKey partitionSortKey, ITraceContext trace) throws NoSuchObjectException
  {
    return doDynamoReadTask(() ->
    {
      GetItemSpec spec = new GetItemSpec().withPrimaryKey(ColumnNamePartitionKey, getPartitionKey(partitionSortKey), ColumnNameSortKey, partitionSortKey.getSortKey());

      Item item = objectTable_.getItem(spec);
      
      String payloadString = item.getJSON(ColumnNameDocument);
      
      if(payloadString == null)
      {
        payloadString = fetchFromSecondaryStorage(partitionSortKey, trace);
      }
      
      return payloadString;
    });
  }

  @Override
  public String fetchFirst(IKvPartitionKey partitionKey, ITraceContext trace) throws NoSuchObjectException
  {
    return fetchOne(partitionKey, true, trace);
  }

  @Override
  public String fetchLast(IKvPartitionKey partitionKey, ITraceContext trace) throws NoSuchObjectException
  {
    return fetchOne(partitionKey, false, trace);
  }

  private String fetchOne(IKvPartitionKey partitionKey, boolean scanForwards, ITraceContext trace) throws NoSuchObjectException
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
        
        String payloadString = item.getJSON(ColumnNameDocument);
            
        if(payloadString == null)
        {
          payloadString = fetchFromSecondaryStorage(new KvPartitionSortKey(partitionKey, item.getString(ColumnNameSortKey)), trace);
        }
        
        trace.trace("DONE_FETCH_ONE");
        return payloadString;
      }
      
      throw new NoSuchObjectException(partitionKey + " not found");
    });
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
    List<Item>          items           = new ArrayList<>(kvItems.size());
   
    for(IKvItem kvItem : kvItems)
    {
      String partitionKey = getPartitionKey(kvItem);
      String sortKey = kvItem.getSortKey();
      
      boolean saveToS3 = createPutItem(items, kvItem, partitionKey, sortKey, payloadLimit_);
      
      if(saveToS3)
        storeToSecondaryStorage(kvItem, trace);
    }
    
    write(items, trace);
  }

  @Override
  public void store(IKvItem kvItem, ITraceContext trace)
  {
    List<Item>          items           = new ArrayList<>(1);

    String partitionKey = getPartitionKey(kvItem);
    String sortKey = kvItem.getSortKey();
    
    boolean saveToS3 = createPutItem(items, kvItem, partitionKey, sortKey, payloadLimit_);
    
    if(saveToS3)
      storeToSecondaryStorage(kvItem, trace);
    
    write(items, trace);
  }

  /**
   * Fetch the given item from secondary storage.
   * 
   * @param partitionSortKey  Partition and sort key of the required object.
   * @param trace             Trace context.
   * 
   * @return The required object.
   * 
   * @throws NoSuchObjectException If the required object does not exist.
   */
  protected @Nonnull String fetchFromSecondaryStorage(IKvPartitionSortKey partitionSortKey, ITraceContext trace) throws NoSuchObjectException
  {
    throw new NoSuchObjectException("This table does not support large objects.");
  }
  
  /**
   * Store the given item to secondary storage.
   * 
   * @param kvItem  An item to be stored.
   * @param trace   A trace context.
   */
  protected void storeToSecondaryStorage(IKvItem kvItem, ITraceContext trace)
  {
    throw new IllegalArgumentException("Object is too large.");
  }

  protected void write(List<Item> items, ITraceContext trace)
  {
    doDynamoWriteTask(() -> 
    {
      dynamoBatchWrite(items, null);
      trace.trace("WRITTEN-DYNAMODB");
      
      return null;
     }
    , trace);
  }

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

  protected void dynamoBatchWrite(Collection<Item> itemsToPut, Collection<PrimaryKey> primaryKeysToDelete)
  {
    TableWriteItems tableWriteItems = new TableWriteItems(objectTable_.getTableName())
        .withItemsToPut(itemsToPut)
        ;
    
    if(primaryKeysToDelete != null)
      tableWriteItems = tableWriteItems.withPrimaryKeysToDelete(primaryKeysToDelete.toArray(new PrimaryKey[primaryKeysToDelete.size()]));
    
    BatchWriteItemOutcome outcome = dynamoDB_.batchWriteItem(tableWriteItems);
    int requestItems = itemsToPut.size();
    long  delay = 4;
    do
    {
        Map<String, List<WriteRequest>> unprocessedItems = outcome.getUnprocessedItems();

        if (outcome.getUnprocessedItems().size() > 0)
        {
          requestItems = 0;
          
          for(List<WriteRequest> ui : unprocessedItems.values())
          {
            requestItems += ui.size();
          }
    
          log_.info("Retry " + requestItems + " of " + requestItems + " items after " + delay + "ms.");
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

  protected boolean createPutItem(List<Item> items, IKvItem kvItem, String partitionKey, String sortKey, int payloadLimit)
  {
    Item item = new Item()
        .withPrimaryKey(ColumnNamePartitionKey, 
            partitionKey, 
            ColumnNameSortKey, sortKey);
    
    int baseLength = ColumnNamePartitionKey.length() + partitionKey.length() + 
        ColumnNameSortKey.length() + sortKey.length();
    
    if(kvItem.getPodId() != null)
    {
      baseLength += ColumnNamePodId.length() + kvItem.getPodId().toString().length();
      
      item.withInt(ColumnNamePodId, kvItem.getPodId().getValue());
    }
    
    if(kvItem.getType() != null)
    {
      baseLength += ColumnNamePayloadType.length() + kvItem.getType().length();
      
      item.withString(ColumnNamePayloadType, kvItem.getType());
    }
    
    if(kvItem.getPurgeDate() != null)
    {
      long ttl = kvItem.getPurgeDate().toEpochMilli() / 1000;
      
      baseLength += ColumnNameTTL.length() + String.valueOf(ttl).length();
      
      item.withLong(ColumnNameTTL,       ttl);
    }
    
    items.add(item);
    
    int length = baseLength + ColumnNameDocument.length() + kvItem.getJson().length();
    
    if(length < payloadLimit)
    {
      item.withJSON(ColumnNameDocument, kvItem.getJson());
      return false;
    }
    else
    {
      return true;
    }
  }

  private String getPartitionKey(IKvPartitionKey kvItem)
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

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractDynamoDbKvTable<B>> extends AbstractKvTable.AbstractBuilder<T,B>
  {
    protected final AmazonDynamoDBClientBuilder amazonDynamoDBClientBuilder_;

    protected String                            region_;
    protected int                               payloadLimit_  = MAX_RECORD_SIZE;
    protected boolean                           validate_ = true;
    
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
