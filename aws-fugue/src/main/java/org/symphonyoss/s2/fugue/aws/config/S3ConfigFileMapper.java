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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class S3ConfigFileMapper
{
  private static final Logger log_ = LoggerFactory.getLogger(S3ConfigFileMapper.class);

  private static final String AWS_COM = ".amazonaws.com";
  
  public S3ConfigFileMapper(String variableName, URL s3Url)
  {
    if(s3Url != null)
    {
      String host = s3Url.getHost();
      
      if(host.endsWith(AWS_COM))
      {
        String region = host.substring(0, host.length() - AWS_COM.length());
        
        if(region.startsWith("s3-"))
          region = region.substring(3);
        
        String path = s3Url.getPath();
        int i = path.indexOf('/', 1);
        
        String bucket = path.substring(0, i);
        String key = path.substring(i+1);
        
        String target = System.getenv(variableName);
        
        try(FileOutputStream out = new FileOutputStream(target))
        {
          loadConfig(out, region, bucket, key);
        }
        catch (IOException e)
        {
          throw new ProgramFault("Failed to load config file from " + s3Url + " to " + target);
        }
      }
      else
      {
        throw new ProgramFault("s3Url is " + s3Url + " a " + AWS_COM + " hostname is expected.");
      }
    }
  }
  
  private void loadConfig(FileOutputStream out, String region, String bucket, String key)
  {
    log_.info("Loading config from region: " + region + " bucket: " + bucket + " key: " + key);
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .build();
  
    S3Object s3object = s3Client.getObject(new GetObjectRequest(bucket, key));
    
    try(S3ObjectInputStream in = s3object.getObjectContent())
    {
      int nbytes;
      byte[] buf = new byte[1024];
      
      while((nbytes = in.read(buf))>0)
      {
        out.write(buf, 0, nbytes);
      }
    }
    catch (IOException e)
    {
      log_.warn("Failed to close config input", e);
    }
  }
}
