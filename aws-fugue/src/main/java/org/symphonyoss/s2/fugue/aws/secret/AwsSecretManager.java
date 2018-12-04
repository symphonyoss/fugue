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

package org.symphonyoss.s2.fugue.aws.secret;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.json.IImmutableJsonDomNode;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.exception.NoSuchObjectException;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.naming.CredentialName;
import org.symphonyoss.s2.fugue.secret.ISecretManager;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.PutSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceExistsException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AWS implementation of Secret Manager.
 * 
 * @author Bruce Skingle
 *
 */
public class AwsSecretManager implements ISecretManager
{
  private static final Logger       log_   = LoggerFactory.getLogger(AwsSecretManager.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private AWSSecretsManager         secretClient_;

  private AwsSecretManager(Builder builder)
  {
    secretClient_ = builder.secretClientBuilder_.build();
  }
  
  
  /**
   * Builder for AwsSecretManager.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends BaseAbstractBuilder<Builder>
  {
    protected final AWSSecretsManagerClientBuilder  secretClientBuilder_;

    protected String                            region_;
    
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
      
      secretClientBuilder_ = AWSSecretsManagerClientBuilder.standard();
    }

    /**
     * Build from the current settings.
     * 
     * @return A new AwsSecretManager.
     */
    public AwsSecretManager build()
    {
      validate();
      return new AwsSecretManager(this);
    }
    
    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(region_,      "region");
   
      secretClientBuilder_
        .withRegion(region_)
        ;
    }

    public Builder withRegion(String region)
    {
      region_ = region;
      
      return self();
    }

    public Builder withCredentials(AWSCredentialsProvider credentials)
    {
      secretClientBuilder_.withCredentials(credentials);
      
      return self();
    }
  }
  
  @Override
  public IImmutableJsonDomNode getSecret(CredentialName name) throws NoSuchObjectException
  {
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(name.toString());
    
    try
    {
      GetSecretValueResult getSecretValueResponse = secretClient_.getSecretValue(getSecretValueRequest);
      String secret = getSecretValueResponse.getSecretString();
      
      if(getSecretValueResponse.getSecretString() == null) 
        throw new IllegalStateException("Returned value is not a string");
          
      return JacksonAdaptor.adapt(MAPPER.readTree(secret)).immutify();
    }
    catch (InvalidParameterException e)
    {
      throw new IllegalArgumentException(e);
    }
    catch (InvalidRequestException e)
    {
      throw new CodingFault(e);
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
    catch(ResourceNotFoundException e)
    {
      throw new NoSuchObjectException("Unable to find secret " + name, e);
    }
}
  
  @Override
  public void putSecret(CredentialName name, IImmutableJsonDomNode secret)
  {
    putSecret(name, secret.toString());
  }
  
  @Override
  public void putSecret(CredentialName name, String secret)
  {
    CreateSecretRequest createSecretRequest = new CreateSecretRequest()
        .withName(name.toString())
        .withSecretString(secret.toString());
    
    try
    {
      CreateSecretResult result = secretClient_.createSecret(createSecretRequest);
      
      log_.info("Created secret " + name + " as " + result.getARN());
    }
    catch(ResourceExistsException e)
    {
      log_.info("Secret " + name + " already exists, attempting put...");
      
      PutSecretValueRequest putSecretRequest = new PutSecretValueRequest()
          .withSecretId(name.toString())
          .withSecretString(secret.toString());
      
      PutSecretValueResult result = secretClient_.putSecretValue(putSecretRequest);
      
      log_.info("Put secret " + name + " as " + result.getARN());
    }
    catch (InvalidParameterException | ResourceNotFoundException e)
    {
      throw new IllegalArgumentException(e);
    }
    catch (InvalidRequestException e)
    {
      throw new CodingFault(e);
    }
  }
}
