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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.aws.config.S3Helper;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.kv.IKvItem;
import org.symphonyoss.s2.fugue.kv.IKvPartitionSortKeyProvider;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * S3/DynamoDb implementation of IKvTable.
 * 
 * @author Bruce Skingle
 *
 */
public class S3DynamoDbKvTable extends AbstractDynamoDbKvTable<S3DynamoDbKvTable>
{
  private static final char SEPARATOR = '/';
  
  protected final String   objectBucketName_;
  protected final AmazonS3 s3Client_;
  
  protected S3DynamoDbKvTable(S3DynamoDbKvTable.AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    objectBucketName_       = config_.getConfiguration(serviceId_).getString("objectsBucketName", objectTableName_.toString());
    
    s3Client_ = builder.s3ClientBuilder_
        .withPathStyleAccessEnabled(true)
      .build();
  }

  @Override
  protected String fetchFromSecondaryStorage(Hash absoluteHash, ITraceContext trace)
      throws NoSuchObjectException
  {
    try
    {
      S3Object object = s3Client_.getObject(new GetObjectRequest(objectBucketName_, s3Key(absoluteHash)));
      
      if(object.getObjectMetadata().getContentLength() > Integer.MAX_VALUE)
        throw new IllegalStateException("Blob is too big");
      
      try(
          InputStream is = object.getObjectContent();
          InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
        )
      {
        int contentLength = (int)object.getObjectMetadata().getContentLength();
        StringBuilder buf = new StringBuilder(contentLength);
        
        char[] cbuf = new char[1024];
        int nbytes;
        
        while((nbytes = in.read(cbuf)) > 0)
        {
          buf.append(cbuf, 0, nbytes);
        }
        
        return buf.toString();
      }
    }
    catch(AmazonS3Exception | IOException e)
    {
      throw new NoSuchObjectException("Failed to read object from S3", e);
    }
    // we only call for objects which we know exist and are not in dynamo
  }
  
  @Override
  protected void storeToSecondaryStorage(IKvItem kvItem, boolean payloadNotStored, ITraceContext trace)
  {
    byte[] bytes = kvItem.getJson().getBytes(StandardCharsets.UTF_8);
    
    try(InputStream in = new ByteArrayInputStream(bytes))
    {
      s3Client_.putObject(new PutObjectRequest(objectBucketName_, s3Key(kvItem.getAbsoluteHash()), in, getS3MetaData(kvItem.getAbsoluteHash(), bytes.length)));
    }
    catch (IOException e)
    {
      throw new CodingFault("In memory I/O - can't happen", e);
    }
    
    trace.trace("WRITTEN-S3");
  }
  
  private ObjectMetadata getS3MetaData(
      Hash absoluteHash, 
      long contentLength)
  {
    ObjectMetadata metaData = new ObjectMetadata();
        
    metaData.setContentLength(contentLength);
    metaData.setContentDisposition("attachment; filename=" + absoluteHash.toStringUrlSafeBase64() + ".json");
    metaData.setContentType("application/json");
    
    return metaData;
  }
  
  protected String s3Key(Hash absoluteHash)
  {
    // Return the S3 key used to store an object, we break the partition key into several directories to prevent a single directory
    // from getting too large.
    
    StringBuilder s = new StringBuilder();
    
    String partitionKey = absoluteHash.toStringUrlSafeBase64();
    
    return s.append(partitionKey.substring(0, 4))
        .append(SEPARATOR)
        .append(partitionKey.substring(4, 8))
        .append(SEPARATOR)
        .append(partitionKey.substring(8))
        .toString();
  }

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractDynamoDbKvTable<B>> extends AbstractDynamoDbKvTable.AbstractBuilder<T,B>
  {
    protected final AmazonS3ClientBuilder       s3ClientBuilder_;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
      
      s3ClientBuilder_ = AmazonS3ClientBuilder.standard();
    }
    
    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
//      IConfiguration s3Config = config_.getConfiguration("org/symphonyoss/s2/fugue/aws/s3");
//      
//      ClientConfiguration clientConfig = new ClientConfiguration()
//          .withMaxConnections(s3Config.getInt("maxConnections", ClientConfiguration.DEFAULT_MAX_CONNECTIONS))
//          .withClientExecutionTimeout(s3Config.getInt("clientExecutionTimeout", ClientConfiguration.DEFAULT_CLIENT_EXECUTION_TIMEOUT))
//          .withConnectionMaxIdleMillis(s3Config.getLong("connectionMaxIdleMillis", ClientConfiguration.DEFAULT_CONNECTION_MAX_IDLE_MILLIS))
//          .withConnectionTimeout(s3Config.getInt("connectionTimeout", ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT))
//          ;
//      
//      log_.info("Starting S3 object store client in " + region_ + " with " + clientConfig.getMaxConnections() + " max connections...");
//
//      
      s3ClientBuilder_
        .withRegion(region_)
//        .withClientConfiguration(clientConfig)
        ;
    }

    @Override
    public T withCredentials(AWSCredentialsProvider credentials)
    {
      s3ClientBuilder_.withCredentials(credentials);
      
      return super.withCredentials(credentials);
    }
  }
  
  @Override
  public void createTable(boolean dryRun)
  {
    super.createTable(dryRun);
    
    Map<String, String> tags = new HashMap<>(nameFactory_.getTags());
    
    tags.put(Fugue.TAG_FUGUE_SERVICE, serviceId_);
    tags.put(Fugue.TAG_FUGUE_ITEM, objectBucketName_);
    
    S3Helper.createBucketIfNecessary(s3Client_, objectBucketName_, tags, dryRun);
  }

  @Override
  public void deleteTable(boolean dryRun)
  {
    S3Helper.deleteBucket(s3Client_, objectBucketName_, dryRun);
    
    super.deleteTable(dryRun);
  }

  /**
   * Builder for S3DynamoDbKvTable.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, S3DynamoDbKvTable>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected S3DynamoDbKvTable construct()
    {
      return new S3DynamoDbKvTable(this);
    }
  }
}
