/*
 *
 *
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

package org.symphonyoss.s2.fugue.aws.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.concurrent.NamedThreadFactory;
import org.symphonyoss.s2.fugue.deploy.ExecutorBatch;
import org.symphonyoss.s2.fugue.deploy.IBatch;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.TagSet;

public class S3Helper
{
  private static final Logger            log_                          = LoggerFactory.getLogger(S3Helper.class);
  private static final int BATCH_SIZE = 1000;

  public static void deleteBucket(AmazonS3 s3, String name, boolean dryRun)
  {
    ExecutorService         executor           = Executors.newFixedThreadPool(5,
        new NamedThreadFactory("Batch", true));
    
    IBatch<Runnable> batch = new ExecutorBatch<Runnable>(executor);
    
    try
    {
      String location = s3.getBucketLocation(name);
      
      log_.info("Bucket location is " + location);
      
      if(dryRun)
      {
        log_.info("Bucket " + name + " would be deleted (dry run).");
      }
      else
      {
        String continuationToken = null;
        do
        {
          ListObjectsV2Result list = s3.listObjectsV2(new ListObjectsV2Request()
              .withBucketName(name)
              .withContinuationToken(continuationToken)
              );
          
          List<KeyVersion> keys = new ArrayList<>(BATCH_SIZE);
          
          
          for(S3ObjectSummary item : list.getObjectSummaries())
          {
            keys.add(new KeyVersion(item.getKey()));
            
            if(keys.size() >= BATCH_SIZE)
            {
              delete(s3, name, batch, keys);
              
              keys = new ArrayList<>(BATCH_SIZE);
            }
          }
          
          if(!keys.isEmpty())
            delete(s3, name, batch, keys);
          
          
          continuationToken = list.getNextContinuationToken();
          
          
        }while(continuationToken != null);
        
        log_.info("Waiting for object delete for " + name);
        
        batch.waitForAllTasks();
        
        log_.info("Bucket " + name + " emptied.");
      }
    }
    catch(AmazonS3Exception e)
    {
      if(e.getErrorCode() == null)
      {
        abort("Unexpected S3 error for bucket " + name, e);
      }
      else
      {
        switch(e.getErrorCode())
        {
          case "NoSuchBucket":
            log_.info("Bucket " + name + " does not exist.");
            break;
            
          case "AuthorizationHeaderMalformed":
            abort("Bucket " + name + ", appears to be in the wrong region.", e);
            break;
                   
          default:
            abort("Unexpected S3 error for bucket " + name, e);
        }
      }
    }
  }
  
  private static void delete(AmazonS3 s3, String name, IBatch<Runnable> batch, List<KeyVersion> keys)
  {

    batch.submit(() ->
    {
      for(int retries=0 ; retries<100 ; retries++)
      {
        try
        {
          log_.info("Deleting " + keys.size() + " objects from bucket " + name);
          s3.deleteObjects(new DeleteObjectsRequest(name).withKeys(keys));
          
          return;
        }
        catch(SdkClientException e)
        {
          log_.error("Failed to delete objects, retrying....", e);
          
          try
          {
            Thread.sleep(900);
          }
          catch (InterruptedException e1)
          {
            log_.error("Failed to delete objects, retrying....", e1);
          }
        }
      }
    });
  }

  public static void createBucketIfNecessary(AmazonS3 s3, String name, Map<String, String> tags, boolean dryRun)
  {
    try
    {
      String location = s3.getBucketLocation(name);
      
      log_.info("Bucket location is " + location);
    }
    catch(AmazonS3Exception e)
    {
      switch(e.getErrorCode())
      {
        case "NoSuchBucket":
          if(dryRun)
          {
            log_.info("Bucket " + name + " does not exist, and would be created (dry run).");
          }
          else
          {
            log_.info("Bucket " + name + " does not exist, creating...");
            
            createBucket(s3, name);
          }
          break;
          
        case "AuthorizationHeaderMalformed":
          abort("Bucket " + name + ", appears to be in the wrong region.", e);
          break;
        
        case "AccessDenied":
          boolean denied = true;
          for(int i=0 ; i<5 && denied ; i++)
          {
            denied = bucketAccessDenied(s3, name);
          }
          
          String message = "Cannot access bucket " + name;
          
          if(denied)
            abort(message + ", could not access 5 random bucket names either, check your policy permissions.");
          else
            abort(message + ", but we can access a random bucket name, "
                + "this bucket could belong to another AWS customer.\n"
                + "Configure a custome bucket name in the environmentType/region config.");
          break;
          
        default:
          abort("Unexpected S3 error looking for bucket " + name, e);
      }
    }
    
    s3.setBucketTaggingConfiguration(name, new BucketTaggingConfiguration()
            .withTagSets(new TagSet(tags)
            )
        );
  }

  private static void createBucket(AmazonS3 s3, String name)
  {
    s3.createBucket(name);
    
    s3.setBucketEncryption(new SetBucketEncryptionRequest()
        .withBucketName(name)
        .withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
            .withRules(new ServerSideEncryptionRule()
                .withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault()
                    .withSSEAlgorithm(SSEAlgorithm.AES256)))));
  }

  private static void abort(String message, Throwable cause)
  {
    log_.error(message, cause);
    
    throw new IllegalStateException(message, cause);
  }
  
  private static void abort(String message)
  {
    log_.error(message);
    
    throw new IllegalStateException(message);
  }

  private static boolean bucketAccessDenied(AmazonS3 s3, String name)
  {
    try
    {
      s3.getBucketLocation(name + UUID.randomUUID());
      
      return false;
    }
    catch(AmazonS3Exception e)
    {
      return e.getErrorCode().equals("AccessDenied");
    }
  }
}
