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

package org.symphonyoss.s2.fugue.aws.deploy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import org.symphonyoss.s2.fugue.naming.Name;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.AttachGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListAttachedGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

public class CreateUserForPolicy
{

  /**
   * Main.
   * 
   * @param args  Command line args.
   * @throws IOException If there is an exceptional condition with the IO. 
   */
  public static void main(String[] args) throws IOException
  {
    new CreateUserForPolicy().run();
  }

  private final AmazonIdentityManagement iam_                          = AmazonIdentityManagementClientBuilder
      .defaultClient();
  private final AWSSecurityTokenService  sts_                          = AWSSecurityTokenServiceClientBuilder
      .defaultClient();

  private BufferedReader in_;
  private String baseName_;
  private String userName_;
  private String policyName_;
  private String accountId_;
  private String policyArn_;
  private File credentialsFile_;
  private String groupName_;

  private void run() throws IOException
  {
    in_ = new BufferedReader(new InputStreamReader(System.in));
    
    System.out.print("Enter policy name: ");
    System.out.flush();
    
    baseName_ = in_.readLine().trim();
    
    if(baseName_.endsWith("-policy"))
      baseName_ = baseName_.substring(0, baseName_.length()-7);
    else if(baseName_.endsWith("-user"))
      baseName_ = baseName_.substring(0, baseName_.length()-5);
    else while(baseName_.endsWith("-"))
      baseName_ = baseName_.substring(0, baseName_.length()-1);
    
    System.out.println("name is " + baseName_);
 
    userName_      = baseName_ + "-user";
    policyName_    = baseName_ + "-policy";
    groupName_    = baseName_ + "-group";
    
    GetCallerIdentityResult callerIdentity = sts_.getCallerIdentity(new GetCallerIdentityRequest());
    
    System.out.println("Connected as user " + callerIdentity.getArn());
    
    accountId_ = callerIdentity.getAccount();
    
    policyArn_     = "arn:aws:iam::" + accountId_ + ":policy/" + policyName_;
    
    String currentUsersHomeDir = System.getProperty("user.home");
    credentialsFile_ = new File(currentUsersHomeDir + File.separator + ".aws"  + File.separator + "credentials");
    
    createGroup(groupName_, policyArn_);
    createUser();
    
    
  }
  
  private void createUser() throws IOException
  {
    try
    {
      iam_.getUser(new GetUserRequest()
        .withUserName(userName_))
        .getUser();
      
      List<Group> groups = iam_.listGroupsForUser(new ListGroupsForUserRequest()
          .withUserName(userName_)).getGroups();
      
      for(Group group : groups)
      {
        if(group.getGroupName().equals(groupName_))
        {
          System.out.println("User \"" + userName_ + "\" is already a member of group \"" + groupName_ + "\"");
          return;
        }
      }
    }
    catch(NoSuchEntityException e)
    {
      System.out.println("User \"" + userName_ + "\" does not exist, creating...");
      
      iam_.createUser(new CreateUserRequest()
          .withUserName(userName_)).getUser();
      
      System.out.println("Created user \"" + userName_ + "\"");
      
      AccessKey accessKey = iam_.createAccessKey(new CreateAccessKeyRequest()
          .withUserName(userName_)).getAccessKey();
        
        System.out.println("#######################################################");
        System.out.println("# SAVE THIS ACCESS KEY IN ~/.aws/credentials");
        System.out.println("#######################################################");
        System.out.format("[%s]%n", userName_);
        System.out.format("aws_access_key_id = %s%n", accessKey.getAccessKeyId());
        System.out.format("aws_secret_access_key = %s%n", accessKey.getSecretAccessKey());
        System.out.println("#######################################################");
        
        System.out.println("credentials is " + credentialsFile_.getAbsolutePath());
        
        try(PrintWriter out = new PrintWriter(new FileWriter(credentialsFile_, true)))
        {
          out.format("%n[%s]%n", userName_);
          out.format("aws_access_key_id = %s%n", accessKey.getAccessKeyId());
          out.format("aws_secret_access_key = %s%n", accessKey.getSecretAccessKey());
        }
    }
    
//    System.out.println("Adding policy \"" + policyName_ + "\" to user \"" + userName_ + "\"");
//    
//    iam_.attachUserPolicy(new AttachUserPolicyRequest()
//        .withUserName(userName_)
//        .withPolicyArn(policyArn_));
    
    System.out.println("Adding user \"" + userName_ + "\" to group \"" + groupName_ + "\"");
    
    iam_.addUserToGroup(new AddUserToGroupRequest()
        .withUserName(userName_)
        .withGroupName(groupName_));
  }

  private String createGroup(String groupName, String policyArn)
  { 
    try
    {
      iam_.getGroup(new GetGroupRequest()
        .withGroupName(groupName))
        .getGroup();
      
      List<AttachedPolicy> policies = iam_.listAttachedGroupPolicies(new ListAttachedGroupPoliciesRequest()
          .withGroupName(groupName)).getAttachedPolicies();
      
      for(AttachedPolicy policy : policies)
      {
        if(policy.getPolicyArn().equals(policyArn))
        {
          System.out.println("Group already has policy attached.");
          return groupName;
        }
      }
      
      System.out.println("Attaching policy to existing group...");
    }
    catch(NoSuchEntityException e)
    {
      System.out.println("Fugue environment group does not exist, creating...");
      
      iam_.createGroup(new CreateGroupRequest()
          .withGroupName(groupName)).getGroup();
      
      System.out.println("Created group " + groupName);
    }
    
    iam_.attachGroupPolicy(new AttachGroupPolicyRequest()
        .withPolicyArn(policyArn)
        .withGroupName(groupName));
    
    return groupName;
  }
}
