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

package org.symphonyoss.s2.fugue.aws.sts;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

/**
 * Manager for the Secure Token Service.
 * 
 * @author Bruce Skingle
 *
 */
public class StsManager
{
  private final String                  region_;
  private final AWSSecurityTokenService stsClient_;
  private final String                  accountId_;
  private final GetCallerIdentityResult identityResult_;

  /**
   * Constructor.
   * 
   * @param region      The AWS region to use.
   */
  public StsManager(String region)
  {
    region_ = region;
    
    stsClient_ = AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(region_)
        .build();
    
    identityResult_ = stsClient_.getCallerIdentity(new GetCallerIdentityRequest());
    
    accountId_ = identityResult_.getAccount();
  }
  
  /**
   * Assume the given role.
   * 
   * @param assumeRole  A role to be assumed.
   * 
   * @return A credentials provider containing the assumed credentials.
   */
  public AWSCredentialsProvider assumeRole(String assumeRole)
  {
    if(assumeRole == null)
      return DefaultAWSCredentialsProviderChain.getInstance();
    
    try
    {
      AssumeRoleResult roleResult = stsClient_.assumeRole(new AssumeRoleRequest()
        .withRoleArn(roleArn(assumeRole))
        .withRoleSessionName(assumeRole)
        );
      
      Credentials creds = roleResult.getCredentials();
      
      BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
          creds.getAccessKeyId(),
          creds.getSecretAccessKey(),
          creds.getSessionToken());

       return new AWSStaticCredentialsProvider(sessionCredentials);
    }
    catch(RuntimeException e)
    {
      throw new IllegalStateException("Unable to assume role " + assumeRole, e);
    }
  }

  private String roleArn(String roleName)
  {
    return "arn:aws:iam::" + accountId_ + ":role/" + roleName;
  }

  /**
   * @return The AWS region used.
   */
  public String getRegion()
  {
    return region_;
  }

  /**
   * @return the account id of the current credentials.
   */
  public String getAccountId()
  {
    return accountId_;
  }
}
