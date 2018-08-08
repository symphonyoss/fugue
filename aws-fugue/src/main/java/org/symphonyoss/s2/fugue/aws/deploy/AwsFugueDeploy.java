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

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonObject;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.fugue.deploy.ConfigHelper;
import org.symphonyoss.s2.fugue.deploy.ConfigProvider;
import org.symphonyoss.s2.fugue.deploy.FugueDeploy;
import org.symphonyoss.s2.fugue.naming.Name;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.AttachGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyResult;
import com.amazonaws.services.identitymanagement.model.CreatePolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeletePolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetPolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListAttachedGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListPolicyVersionsRequest;
import com.amazonaws.services.identitymanagement.model.ListPolicyVersionsResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PolicyVersion;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AWS implementation of FugueDeploy.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class AwsFugueDeploy extends FugueDeploy
{
  private static final Logger            log_                          = LoggerFactory.getLogger(AwsFugueDeploy.class);

  private static final String            AMAZON                        = "amazon";
  private static final String            ACCOUNT_ID                    = "accountId";
  private static final String            REGION                        = "regionName";
  private static final String            REGIONS                       = "environmentTypeRegions";
  private static final String            POLICY_SUFFIX                 = "-policy";
  private static final String            GROUP_SUFFIX                  = "-group";
  private static final String            ROLE_SUFFIX                   = "-role";
  private static final String            USER_SUFFIX                   = "-user";
  private static final String            ADMIN_SUFFIX                  = "-admin";
  private static final String            CICD_SUFFIX                   = "-cicd";
  private static final String            CONFIG_SUFFIX                 = "-config";

  private static final ObjectMapper      MAPPER                        = new ObjectMapper();

  private static final String AWS_CONFIG_BUCKET = "awsConfigBucket";

  private static final String APPLICATION_JSON = "application/json";
  
  private static final String TRUST_ECS_DOCUMENT = "{\n" + 
      "  \"Version\": \"2012-10-17\",\n" + 
      "  \"Statement\": [\n" + 
      "    {\n" + 
      "      \"Sid\": \"\",\n" + 
      "      \"Effect\": \"Allow\",\n" + 
      "      \"Principal\": {\n" + 
      "        \"Service\": \"ecs-tasks.amazonaws.com\"\n" + 
      "      },\n" + 
      "      \"Action\": \"sts:AssumeRole\"\n" + 
      "    }\n" + 
      "  ]\n" + 
      "}";

  private final AmazonIdentityManagement iam_                          = AmazonIdentityManagementClientBuilder
      .defaultClient();
  private final AWSSecurityTokenService  sts_                          = AWSSecurityTokenServiceClientBuilder
      .defaultClient();

  private String                         awsAccountId_;
//  private User                           awsUser_;
  private String                         awsRegion_;

  private List<String>                   environmentTypeRegions_       = new LinkedList<>();
  private Map<String, String>            environmentTypeConfigBuckets_ = new HashMap<>();
  
  /**
   * Constructor.
   * 
   * @param provider              A config provider.
   * @param helpers               Zero or more config helpers.
   */
  public AwsFugueDeploy(ConfigProvider provider, ConfigHelper... helpers)
  {
    super(AMAZON, provider, helpers);
  }
  
  private String getPolicyArn(String policyName)
  {
    return getIamPolicy("policy", policyName);
  }
  
  private String getIamPolicy(String type, String name)
  {
    return getArn("iam", type, name);
  }

  private String getArn(String service, String type, String name)
  {
    return String.format("arn:aws:%s::%s:%s/%s", service, awsAccountId_, type, name);
  }

  @Override
  public void saveConfig(String target, ImmutableJsonDom multiTenantConfig, ImmutableJsonDom singleTenantConfig)
  {
    saveConfig(target, multiTenantConfig, getConfigName(null));
    
    if(singleTenantConfig != null)
      saveConfig(target, singleTenantConfig, getConfigName(getTenant()));
  }

  private void saveConfig(String target, ImmutableJsonDom dom, String name)
  {
    String bucketName = environmentTypeConfigBuckets_.get(awsRegion_);
    String key = CONFIG + "/" + name + DOT_JSON;
    
    log_.info("Saving config to region: " + awsRegion_ + " bucket: " + bucketName + " key: " + key);
    
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(awsRegion_)
        .build();
  
    try
    {
      ObjectMetadata metaData = s3Client.getObjectMetadata(bucketName, key);
      
      if(APPLICATION_JSON.equals(metaData.getContentType()) && metaData.getContentLength() == dom.serialize().length())
      {
        S3Object existingContent = s3Client.getObject(bucketName, key);
        int i;
        
        for(i=0 ; i<metaData.getContentLength() ; i++)
          if(existingContent.getObjectContent().read() != dom.serialize().byteAt(i))
            break;
        
        if(i == metaData.getContentLength())
        {
          log_.info("Configuration has not changed, no need to overwrite.");
          return;
        }
      }
      // else its not the right content so overwrite it.
    }
    catch(AmazonS3Exception e)
    {
      // Nothing here we will overwrite the object below...
    }
    catch (IOException e)
    {
      abort("Unexpected S3 error reading current value of config object " + bucketName + "/" + key, e);
    }
    
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(APPLICATION_JSON);
    metadata.setContentLength(dom.serialize().length());
    
    PutObjectRequest request = new PutObjectRequest(bucketName, key, dom.serialize().getInputStream(), metadata);
    
    s3Client.putObject(request);
  }


  @Override
  protected void validateAccount(MutableJsonObject config)
  {
    IJsonDomNode node = config.get(AMAZON);
    
    if(node instanceof IJsonObject)
    {
        awsAccountId_ = ((IJsonObject<?>)node).getRequiredString(ACCOUNT_ID);
        awsRegion_ = ((IJsonObject<?>)node).getRequiredString(REGION);
        
        GetCallerIdentityResult callerIdentity = sts_.getCallerIdentity(new GetCallerIdentityRequest());
        
        log_.info("Connected as user " + callerIdentity.getArn());
        
        String actualAccountId = callerIdentity.getAccount();
        
        if(!actualAccountId.equals(awsAccountId_))
        {
          throw new IllegalStateException("AWS Account ID is " + awsAccountId_ + " but our credentials are for account " + actualAccountId);
        }
        
        //awsUser_ = iam_.getUser().getUser();

        IJsonDomNode regionsNode = ((IJsonObject<?>)node).get(REGIONS);
        if(regionsNode instanceof IJsonObject)
        {
          IJsonObject<?> regionsObject = (IJsonObject<?>)regionsNode;
          
          Iterator<String> it = regionsObject.getNameIterator();
          
          while(it.hasNext())
          {
            String name = it.next();
            
            environmentTypeRegions_.add(name);
            
            IJsonObject<?> regionObject = regionsObject.getRequiredObject(name);
            
            String bucketName = regionObject.getString(AWS_CONFIG_BUCKET,
                FUGUE_PREFIX + getEnvironmentType() + Name.SEPARATOR + name + CONFIG_SUFFIX);
            
            environmentTypeConfigBuckets_.put(name, bucketName);
            
            if(name.equals(awsRegion_))
              getTemplateVariables().put(AWS_CONFIG_BUCKET, bucketName);
          }
        }
        else
        {
          if(regionsNode == null)
            throw new IllegalStateException("A top level configuration object called \"/" + AMAZON + "/" + REGIONS + "\" is required.");
          
          throw new IllegalStateException("The top level configuration object called \"/" + AMAZON + "/" + REGIONS + "\" must be an object not a " + node.getClass().getSimpleName());
        }

        
        
    }
    else
    {
      if(node == null)
        throw new IllegalStateException("A top level configuration object called \"" + AMAZON + "\" is required.");
      
      throw new IllegalStateException("The top level configuration object called \"" + AMAZON + "\" must be an object not a " + node.getClass().getSimpleName());
    }
  }
  
  @Override
  protected void createEnvironment()
  {
    String baseName = getEnvironmentType() + Name.SEPARATOR + getEnvironment();
    
    createEnvironmentAdminUser(baseName);
  }
  
  private void createEnvironmentAdminUser(String baseName)
  {
    String name = baseName + ADMIN_SUFFIX;
    
    String policyArn = createPolicyFromResource(name, "policy/environmentAdmin.json");
    String groupName = createGroup(name, policyArn);
    createUser(name, groupName, System.out);
  }

  @Override
  protected void createEnvironmentType()
  {
    String baseName = FUGUE_PREFIX + getEnvironmentType();
    
    createEnvironmentTypeAdminUser(baseName);
    createEnvironmentTypeCicdUser(baseName);
    
    for(String region : environmentTypeRegions_)
    {
      createConfigBucket(region, environmentTypeConfigBuckets_.get(region));
    }
  }
  
  private void createEnvironmentTypeAdminUser(String baseName)
  {
    String name = baseName + ADMIN_SUFFIX;
    
    String policyArn = createPolicyFromResource(name, "policy/environmentTypeAdmin.json");
    String groupName = createGroup(name, policyArn);
    createUser(name, groupName, System.out);
  }
  
  private void createEnvironmentTypeCicdUser(String baseName)
  {
    String name = baseName + CICD_SUFFIX;
    
    String policyArn = createPolicyFromResource(name, "policy/environmentTypeCicd.json");
    String groupName = createGroup(name, policyArn);
    createUser(name, groupName, System.out);
  }
  
  private void createConfigBucket(String region, String name)
  {
    AmazonS3 s3 = AmazonS3ClientBuilder
        .standard()
        .withRegion(region)
        .build();

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
          log_.info("Config bucket " + name + " does not exist, creating...");
          
          createBucket(s3, region, name);
          break;
          
        case "AuthorizationHeaderMalformed":
          abort("Config bucket " + name + ", appears to be in the wrong region.", e);
          break;
        
        case "AccessDenied":
          boolean denied = true;
          for(int i=0 ; i<5 && denied ; i++)
          {
            denied = bucketAccessDenied(s3, name);
          }
          
          String message = "Cannot access config bucket " + name;
          
          if(denied)
            abort(message + ", could not access 5 random bucket names either, check your policy permissions.");
          else
            abort(message + ", but we can access a random bucket name, "
                + "this bucket could belong to another AWS customer.\n"
                + "Configure a custome bucket name in the environmentType/region config.");
          break;
          
        default:
          abort("Unexpected S3 error looking for config bucket " + name, e);
      }
      
    }
    
    
//    try
//    {
//      ListObjectsV2Result list = s3.listObjectsV2(name, CONFIG);
//      
//      
//      log_.info("Got " + list.getKeyCount() + " keys");
//    }
//    catch(AmazonS3Exception e)
//    {
//      switch(e.getErrorCode())
//      {
//        case "Not Found":
//          log_.info("Config folder not found, creating it...");
//          s3.
//        
//        default:
//          abort("Unexpected S3 error looking for config folder " + name + "/" + CONFIG, e);
//      }
//    }
  }

  private void createBucket(AmazonS3 s3, String region, String name)
  {
    s3.createBucket(name);
    
    s3.setBucketEncryption(new SetBucketEncryptionRequest()
        .withBucketName(name)
        .withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
            .withRules(new ServerSideEncryptionRule()
                .withApplyServerSideEncryptionByDefault(new ServerSideEncryptionByDefault()
                    .withSSEAlgorithm(SSEAlgorithm.AES256)))));
  }

  private void abort(String message, Throwable cause)
  {
    log_.error(message, cause);
    
    throw new IllegalStateException(message, cause);
  }
  
  private void abort(String message)
  {
    log_.error(message);
    
    throw new IllegalStateException(message);
  }

  private boolean bucketAccessDenied(AmazonS3 s3, String name)
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

  private String createUser(String name, String groupName, PrintStream out)
  {
    String userName       = name + USER_SUFFIX;
    
    try
    {
      iam_.getUser(new GetUserRequest()
        .withUserName(userName))
        .getUser();
      
      List<Group> groups = iam_.listGroupsForUser(new ListGroupsForUserRequest()
          .withUserName(userName)).getGroups();
      
      for(Group group : groups)
      {
        if(group.getGroupName().equals(groupName))
        {
          log_.debug("User is already a member of group.");
          return userName;
        }
      }
    }
    catch(NoSuchEntityException e)
    {
      log_.info("Fugue environment user does not exist, creating...");
      
      iam_.createUser(new CreateUserRequest()
          .withUserName(userName)).getUser();
      
      log_.debug("Created user " + userName);
      
      if(out != null)
      {
        AccessKey accessKey = iam_.createAccessKey(new CreateAccessKeyRequest()
            .withUserName(userName)).getAccessKey();
        
        out.println("#######################################################");
        out.println("# SAVE THIS ACCESS KEY IN ~/.aws/credentials");
        out.println("#######################################################");
        out.format("[%s]%n", userName);
        out.format("aws_access_key_id = %s%n", accessKey.getAccessKeyId());
        out.format("aws_secret_access_key = %s%n", accessKey.getSecretAccessKey());
        out.println("#######################################################");
      }
    }
    
    iam_.addUserToGroup(new AddUserToGroupRequest()
        .withUserName(userName)
        .withGroupName(groupName));
    
    return userName;
  }

  private String createGroup(String name, String policyArn)
  {
    String groupName       = name + GROUP_SUFFIX;
    
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
          log_.debug("Group already has policy attached.");
          return groupName;
        }
      }
      
      log_.info("Attaching policy to existing group...");
    }
    catch(NoSuchEntityException e)
    {
      log_.info("Fugue environment group does not exist, creating...");
      
      iam_.createGroup(new CreateGroupRequest()
          .withGroupName(groupName)).getGroup();
      
      log_.debug("Created group " + groupName);
    }
    
    iam_.attachGroupPolicy(new AttachGroupPolicyRequest()
        .withPolicyArn(policyArn)
        .withGroupName(groupName));
    
    return groupName;
  }
  
  private String createRole(String name, String policyArn)
  {
    String roleName       = name + ROLE_SUFFIX;
    
    try
    {
      iam_.getRole(new GetRoleRequest()
        .withRoleName(roleName))
        .getRole();
      
      List<AttachedPolicy> policies = iam_.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest()
          .withRoleName(roleName)).getAttachedPolicies();
      
      for(AttachedPolicy policy : policies)
      {
        if(policy.getPolicyArn().equals(policyArn))
        {
          log_.debug("Role " + roleName + " already has policy " + policyArn + " attached.");
          return roleName;
        }
      }
      
      log_.info("Attaching policy " + policyArn + " to existing role " + roleName + "...");
    }
    catch(NoSuchEntityException e)
    {
      log_.info("Role " + roleName + " does not exist, creating...");
      
      iam_.createRole(new CreateRoleRequest()
          .withRoleName(roleName)
          .withAssumeRolePolicyDocument(TRUST_ECS_DOCUMENT)
          ).getRole();
      
      log_.debug("Created role " + roleName);
    }
    
    iam_.attachRolePolicy(new AttachRolePolicyRequest()
        .withPolicyArn(policyArn)
        .withRoleName(roleName));
    
    return roleName;
  }
  
  private String createPolicyFromResource(String name, String fileName)
  {
    return createPolicy(name, loadTemplateFromResource(fileName));
  }

  private String createPolicy(String name, String templateOutput)
  {
    String policyName       = name + POLICY_SUFFIX;
    String policyArn        = getPolicyArn(policyName);
    String policyDocument;
    
    try(StringReader tempIn = new StringReader(templateOutput))
    {
      // Canonicalise the new policy document.
      policyDocument = JacksonAdaptor.adapt(MAPPER.readTree(tempIn)).immutify().toString();
    }
    catch (IOException e)
    {
      throw new CodingFault("Impossible IO error on im-memory IO", e);
    }
    
    try
    {
      
      GetPolicyResult getResult = iam_.getPolicy(new GetPolicyRequest().withPolicyArn(policyArn));
      
      PolicyVersion currentVersion = iam_.getPolicyVersion(new GetPolicyVersionRequest()
          .withPolicyArn(policyArn)
          .withVersionId(getResult.getPolicy().getDefaultVersionId()))
          .getPolicyVersion();
      
      try(StringReader in = new StringReader(URLDecoder.decode(currentVersion.getDocument(), StandardCharsets.UTF_8.name())))
      {
     // Canonicalise the existing policy document.
        String existingPolicy = JacksonAdaptor.adapt(MAPPER.readTree(in)).immutify().toString();
        
        if(policyDocument.equals(existingPolicy))
        {
          log_.info("The existing policy " + policyArn + " is the same, nothing more to do.");
          return policyArn;
        }
      }
      catch (IOException e)
      {
        log_.error("Unable to parse existing policy version", e);
      }
      
      ListPolicyVersionsResult versions = iam_.listPolicyVersions(new ListPolicyVersionsRequest()
          .withPolicyArn(policyArn));
      
      PolicyVersion oldestVersion = null;
      
      if(versions.getVersions().size() > 3)
      {
        log_.debug("We have " + versions.getVersions().size() + " versions, checking to delete one...");
        
        for(PolicyVersion version : versions.getVersions())
        {
          if(version.getIsDefaultVersion())
          {
            log_.debug("Found existing default policy version " + version.getVersionId());
            
            
          }
          else
          {
            log_.debug("Found existing policy version " + version.getVersionId());
            
            if(oldestVersion == null || version.getCreateDate().before(oldestVersion.getCreateDate()))
            {
              oldestVersion  = version;
            }
          }
        }
        
        if(oldestVersion == null)
        {
          // This "can't happen"
          log_.error("There are " + versions.getVersions().size() + " versions but we found none we can delete!");
        }
        else
        {
          log_.info("Deleting policy " + policyArn + " version " + oldestVersion.getVersionId());
          iam_.deletePolicyVersion(new DeletePolicyVersionRequest()
              .withPolicyArn(policyArn)
              .withVersionId(oldestVersion.getVersionId()));
        }
      }
      
      log_.info("Creating new version of policy " + policyArn + "...");
      
      PolicyVersion newPolicy = iam_.createPolicyVersion(new CreatePolicyVersionRequest()
          .withPolicyArn(policyArn)
          .withPolicyDocument(policyDocument)
          .withSetAsDefault(Boolean.TRUE)).getPolicyVersion();
      
      log_.info("Created policy " + policyArn + " version " + newPolicy.getVersionId());
    }
    catch(NoSuchEntityException e)
    {
      log_.info("Policy " + policyArn + " does not exist, creating...");
      
      CreatePolicyResult result = iam_.createPolicy(new CreatePolicyRequest()
          .withDescription("Fugue environment type admin policy for \"" + getEnvironmentType() + "\"")
          .withPolicyDocument(policyDocument)
          .withPolicyName(policyName));
      
      log_.debug("Created policy " + result.getPolicy().getArn());
    }
    
    return policyArn;
  }


  @Override
  protected void processRole(String roleName, String roleSpec, String tenant)
  {
    String name = new Name(getEnvironmentType(), getEnvironment(), tenant, getService(), roleName).toString();
    
    String policyArn = createPolicy(name, roleSpec);
    createRole(name, policyArn);
  }
}
