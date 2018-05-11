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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.fugue.BaseConfigurationProvider;
import org.symphonyoss.s2.fugue.Fugue;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class S3ConfigurationProvider extends BaseConfigurationProvider
{
  private static final Logger log_ = LoggerFactory.getLogger(S3ConfigurationProvider.class);

  private static final String AWS_COM = ".amazonaws.com";
  
  public S3ConfigurationProvider()
  {
    String fugueConfig = Fugue.getRequiredProperty(Fugue.FUGUE_CONFIG);
   
    try
    {
      URL configUrl = new URL(fugueConfig);
      
      log_.info("Loading config from {}", configUrl);
      
      String host = configUrl.getHost();
      
      if(host.endsWith(AWS_COM))
      {
        String region = host.substring(0, host.length() - AWS_COM.length());
        
        if(region.startsWith("s3-"))
          region = region.substring(3);
        
        String path = configUrl.getPath();
        int i = path.indexOf('/', 1);
        
        String bucket = path.substring(0, i);
        String key = path.substring(i+1);
        
        loadConfig(region, bucket, key);
      }
      else
      {
        throw new ProgramFault("FUGUE_CONFIG is " + configUrl + " a " + AWS_COM + " hostname is expected.");
      }
    }
    catch (MalformedURLException e)
    {
      File file = new File(fugueConfig);
      
      if(file.exists())
      {
        try(InputStream in = new FileInputStream(file))
        {
          log_.info("Loading config from file " + file.getAbsolutePath());
          
          loadConfig(in);
        }
        catch (FileNotFoundException e1)
        {
          throw new ProgramFault("FUGUE_CONFIG is " + fugueConfig + " but this file does not exist.", e);
        }
        catch (IOException e1)
        {
          log_.warn("Failed to close config input", e);
        }
      }
      else
      {
        throw new ProgramFault("FUGUE_CONFIG is " + fugueConfig + " but this file does not exist.");
      }
    }
  }


  private void loadConfig(String region, String bucket, String key)
  {
    log_.info("Loading config from region: " + region + " bucket: " + bucket + " key: " + key);
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(region)
//        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
  
    S3Object s3object = s3Client.getObject(new GetObjectRequest(bucket, key));
    
    try(S3ObjectInputStream in = s3object.getObjectContent())
    {
      loadConfig(in);
    }
    catch (IOException e)
    {
      log_.warn("Failed to close config input", e);
    }
  }


  private void loadConfig(InputStream in)
  {
    ObjectMapper mapper = new ObjectMapper();
    
    try
    {
      JsonNode configSpec = mapper.readTree(in);
      
      setTree(configSpec);
    }
    catch (IOException e1)
    {
      throw new ProgramFault("Cannot parse config.", e1);
    }
  }
}
