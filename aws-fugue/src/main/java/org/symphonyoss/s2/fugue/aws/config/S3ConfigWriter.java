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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonDom;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.request.S3HandlerContextKeys;

/**
 * 
 * @author Bruce Skingle
 *
 */
public class S3ConfigWriter
{
  private static final Logger log_ = LoggerFactory.getLogger(S3ConfigWriter.class);

  private static final String AWS_COM = ".amazonaws.com";
  
  public boolean writeConfig(String targetDir, String targetName, ImmutableJsonDom dom) throws IOException
  {
    try
    {
      URL configUrl = new URL(targetDir);
      
      log_.info("Writing config to {}", configUrl);
      
      String host = configUrl.getHost();
      
      if(host.endsWith(AWS_COM))
      {
        String region = host.substring(0, host.length() - AWS_COM.length());
        
        if(region.startsWith("s3-"))
          region = region.substring(3);
        
        String path = configUrl.getPath();
        
        saveConfig(region, path, targetName, dom);
        
        return true;
      }
      else
      {
        return false;
      }
    }
    catch (MalformedURLException e)
    {
      File dir = new File(targetDir);
      
      if(dir.exists() && dir.isDirectory() && dir.canWrite())
      {
        File targetFile = new File(dir, targetName);
        
        try(OutputStream out = new FileOutputStream(targetFile))
        {
          log_.info("Writing config to file " + targetFile.getAbsolutePath());
          
          out.write(dom.serialize().toByteArray());
          
          return true;
        }
        catch (IOException e1)
        {
          throw new IOException("Failed to write config to " + targetFile.getAbsolutePath(), e1);
        }
      }
      else
      {
        throw new IOException(dir.getAbsolutePath() + " is not a writable directory.");
      }
    }
  }


  public void saveConfig(String region, String bucket, String key, ImmutableJsonDom json)
  {
    log_.info("Saving config to region: " + region + " bucket: " + bucket + " key: " + key);
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(region)
//        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
  
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType("application/json");
    metadata.setContentLength(json.serialize().length());
    
    PutObjectRequest request = new PutObjectRequest(bucket, key, json.serialize().getInputStream(), metadata);
    
    s3Client.putObject(request);
  }
}
