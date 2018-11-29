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
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.IStringProvider;
import org.symphonyoss.s2.common.dom.json.IJsonArray;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonObject;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.common.dom.json.JsonObject;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.aws.config.S3Helper;
import org.symphonyoss.s2.fugue.aws.secret.AwsSecretManager;
import org.symphonyoss.s2.fugue.deploy.ConfigHelper;
import org.symphonyoss.s2.fugue.deploy.ConfigProvider;
import org.symphonyoss.s2.fugue.deploy.FugueDeploy;
import org.symphonyoss.s2.fugue.naming.CredentialName;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.Name;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.cloudwatchevents.model.EcsParameters;
import com.amazonaws.services.cloudwatchevents.model.LaunchType;
import com.amazonaws.services.cloudwatchevents.model.PutRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RuleState;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.AvailabilityZone;
import com.amazonaws.services.elasticloadbalancingv2.model.Certificate;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateRuleResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeRulesRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeRulesResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.Rule;
import com.amazonaws.services.elasticloadbalancingv2.model.RuleCondition;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;
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
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.UpdateAssumeRolePolicyRequest;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.CreateHostedZoneRequest;
import com.amazonaws.services.route53.model.CreateHostedZoneResult;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesByNameRequest;
import com.amazonaws.services.route53.model.ListHostedZonesByNameResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.PriorRequestNotCompleteException;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
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
  private static final String            CLUSTER_NAME                  = "ecsCluster";
  private static final String            VPC_ID                        = "vpcId";
  private static final String            LOAD_BALANCER_CERTIFICATE_ARN = "loadBalancerCertificateArn";
  private static final String            LOAD_BALANCER_SECURITY_GROUPS = "loadBalancerSecurityGroups";
  private static final String            LOAD_BALANCER_SUBNETS         = "loadBalancerSubnets";
//  private static final String            IALB_ARN                      = "ialbArn";
//  private static final String            IALB_DNS                      = "ialbDns";
//  private static final String            R53_ZONE                      = "r53Zone";
  
  private static final String            POLICY                         = "policy";
  private static final String            GROUP                          = "group";
  public static final String            ROLE                           = "role";
  private static final String            USER                           = "user";
  private static final String            ROOT                           = "root";
  private static final String            ADMIN                          = "admin";
  private static final String            SUPPORT                        = "support";
  private static final String            CICD                           = "cicd";
  private static final String            CONFIG                         = "config";
  @Deprecated
  public static final String            DYNAMO_AUTOSCALE               = "dynamo-autoscale";

  private static final ObjectMapper      MAPPER                        = new ObjectMapper();

  private static final String AWS_CONFIG_BUCKET = "awsConfigBucket";
  private static final String AWS_ACCOUNT_ID    = "awsAccountId";

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
  
  private static final String TRUST_EVENTS_DOCUMENT = "{\n" + 
      "  \"Version\": \"2012-10-17\",\n" + 
      "  \"Statement\": [\n" + 
      "    {\n" + 
      "      \"Sid\": \"\",\n" + 
      "      \"Effect\": \"Allow\",\n" + 
      "      \"Principal\": {\n" + 
      "        \"Service\": \"events.amazonaws.com\"\n" + 
      "      },\n" + 
      "      \"Action\": \"sts:AssumeRole\"\n" + 
      "    }\n" + 
      "  ]\n" + 
      "}";

  private static final String HOST_HEADER = "host-header";

  private static final String PATH_PATERN = "path-pattern";

  private static final String DEFAULT = "default";


  private final AmazonIdentityManagement iam_                          = AmazonIdentityManagementClientBuilder
      .defaultClient();
  private final AWSSecurityTokenService  sts_                          = AWSSecurityTokenServiceClientBuilder
      .defaultClient();
  private final AwsSecretManager         secretManager_                = new AwsSecretManager("us-east-1");

  private String                         awsAccountId_;
//  private User                           awsUser_;
  private String                         awsRegion_;
  private String                         awsClientRegion_ = "us-east-1"; // used to create client instances
  private String                         awsVpcId_;
  private String                         awsLoadBalancerCertArn_;
  private List<String>                   awsLoadBalancerSecurityGroups_ = new LinkedList<>();
  private List<String>                   awsLoadBalancerSubnets_        = new LinkedList<>();
//  private String                         awsIalbArn_;
//  private String                         awsIalbDns_;
//  private String                         awsR53Zone_;

  private List<String>                   environmentTypeRegions_       = new LinkedList<>();
  private Map<String, String>            environmentTypeConfigBuckets_ = new HashMap<>();
  private String                         configBucket_;
  private String                         callerRefPrefix_              = UUID.randomUUID().toString() + "-";

  private AmazonElasticLoadBalancing     elbClient_;
  private AmazonRoute53                  r53Clinet_;
  private AmazonIdentityManagement       iamClient_;
  private AmazonCloudWatch               cwClient_;
  private AmazonCloudWatchEvents         cweClient_;

  private AmazonECS ecsClient_;

  private String clusterName_;
//
//  private String clusterArn_;

  private AWSLogs logsClient_;




  
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

  @Override
  protected DeploymentContext createContext(String tenantId, INameFactory nameFactory)
  {
    return new AwsDeploymentContext(tenantId, nameFactory);
  }

  private String getAwsRegion()
  {
    return require("AWS Region", awsRegion_);
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
  
  private void abort(String message, Throwable cause)
  {
    log_.error(message, cause);
    
    throw new IllegalStateException(message, cause);
  }
  
  
  
  // Need to figure out how to do templates for this...
//  private void registerTaskDef(String name, int port, String healthCheckPath, String tenantId)
//  {
//    Name logGroupName = new Name(getEnvironmentType(), getEnvironment(), getRealm(), tenantId, getService());
//    
//    createLogGroupIfNecessary(logGroupName.toString());
//    
//    //TODO: allow per service override of template
//    String taskDef = loadTemplateFromResource("ecs/taskDefinition.json");
//    
//    RegisterTaskDefinitionResult registerResult = ecsClient_.registerTaskDefinition(new RegisterTaskDefinitionRequest()
//        .withFamily("family")
//        );
//  }
//
//  private void createLogGroupIfNecessary(String logGroupName)
//  {
//    DescribeLogGroupsResult describeLogsResult = logsClient_.describeLogGroups(new DescribeLogGroupsRequest()
//        .withLimit(1)
//        .withLogGroupNamePrefix(logGroupName.toString())
//        );
//    
//    for(LogGroup logGroup : describeLogsResult.getLogGroups())
//    {
//      if(logGroupName.equals(logGroup.getLogGroupName()))
//      {
//        log_.info("LogGroup " + logGroupName + " already exists.");
//        return;
//      }
//    }
//    
//    CreateLogGroupResult createResult = logsClient_.createLogGroup(new CreateLogGroupRequest()
//        .withLogGroupName(logGroupName)
//        );
//    
//    log_.info("LogGroup " + logGroupName + " created.");
//  }

  private String getServiceHostName(String tenantId)
  {
//    return new Name(getEnvironmentType(), getEnvironment(), "any", tenantId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
    if(tenantId == null)
      return getService().toLowerCase() + "." + getDnsSuffix();
    else
      return (tenantId + "-" + getService()).toString().toLowerCase() + "." + getDnsSuffix();
  }

  private void getOrCreateCluster()
  {
    // We are using pre-created EC2 clusters for now...
//    clusterName_ = new Name(getEnvironmentType(), getEnvironment(),getRealm(), getRegion());
//    
//    DescribeClustersResult describeResult = ecsClient_.describeClusters(new DescribeClustersRequest()
//        .withClusters(clusterName_.toString())
//        );
//    
//    for(Cluster cluster : describeResult.getClusters())
//    {
//      if(clusterName_.toString().equals(cluster.getClusterName()))
//      {
//        clusterArn_ = cluster.getClusterArn();
//        
//        break;
//      }
//    }
//    
//    if(clusterArn_ == null)
//    {
//      log_.info("Cluster does not exist, creating...");
//      
//      CreateClusterResult createResult = ecsClient_.createCluster(new CreateClusterRequest()
//          .withClusterName(clusterName_.toString())
//          );
//      
//      clusterArn_ = createResult.getCluster().getClusterArn();
//      
//      log_.info("Cluster " + clusterArn_ + " created.");
//    }
//    else
//    {
//      log_.info("Cluster " + clusterArn_ + " aready exists.");
//    }
  }

  

//  private void createDnsZones()
//  {
//    String name       = getDnsSuffix();
//    
//    if(baseZoneId_ == null)
//      baseZoneId_ = createOrGetHostedZone(name, false);
//    
////    name = getEnvironmentType() + "." + name;
////    
//////    if(environmentTypeZoneId_ == null)
//////      environmentTypeZoneId_ = createOrGetHostedZone(name);
////    
////    name = getEnvironment() + "." + name;
////    
//////    if(environmentZoneId_ == null)
//////      environmentZoneId_ = createOrGetHostedZone(name);
////    
////    String regionalName = getRegion() + "." + name;
////    
//////    if(regionZoneId_ == null)
//////      regionZoneId_ = createOrGetHostedZone(regionalName);
////    
////    name = getService() + "." + name;
////    
////    if(serviceZoneId_ == null)
////      serviceZoneId_ = createOrGetHostedZone(name);
////    
////    regionalName = getService() + "." + regionalName;
////    
////    if(regionalServiceZoneId_ == null)
////      regionalServiceZoneId_ = createOrGetHostedZone(regionalName);
////    
//////    if(getTenant() != null)
//////    {
//////      name = getTenant() + "." + name;
//////      
//////      if(tenantZoneId_ == null)
//////        tenantZoneId_ = createOrGetHostedZone(name);
//////      
//////      regionalName = getTenant() + "." + regionalName;
//////      
//////      if(regionalTenantZoneId_ == null)
//////        regionalTenantZoneId_ = createOrGetHostedZone(regionalName);
//////    }
//  }
  
  private String createOrGetHostedZone(String name, boolean create)
  {
    String dnsName = name.toLowerCase();
    
    ListHostedZonesByNameResult listResult = r53Clinet_.listHostedZonesByName(new ListHostedZonesByNameRequest()
        .withDNSName(dnsName)
        .withMaxItems("1")
        );
    
    List<HostedZone> zoneList = listResult.getHostedZones();
    String zoneName = dnsName + ".";
    
    if(zoneList.size()>0 && zoneName.equals(zoneList.get(0).getName()))
    {
      log_.info("Zone " + dnsName + " exists as " + zoneList.get(0).getId());
      
      return zoneList.get(0).getId();
    }
    else
    {
      if(create)
      {
        log_.info("Creating zone " + dnsName + "...");
        
        CreateHostedZoneResult createResult = r53Clinet_.createHostedZone(new CreateHostedZoneRequest()
            .withName(dnsName)
            .withCallerReference(callerRefPrefix_ + dnsName)
            );
        
        log_.info("Zone " + dnsName + " created as " + createResult.getHostedZone().getId());
        
        return createResult.getHostedZone().getId();
      }
      else
      {
        throw new IllegalStateException("Zone " + dnsName + " not found.");
      }
    }
  }

  

  @Override
  protected void validateAccount(IJsonObject<?> config)
  {
    IJsonDomNode node = config.get(AMAZON);
    
    if(node instanceof IJsonObject)
    {
      IJsonObject<?> amazon = ((IJsonObject<?>)node);
      
      awsAccountId_           = amazon.getRequiredString(ACCOUNT_ID);
      awsRegion_              = amazon.getString(REGION, null);
      awsVpcId_               = amazon.getRequiredString(VPC_ID);
      clusterName_            = amazon.getRequiredString(CLUSTER_NAME);
      awsLoadBalancerCertArn_ = amazon.getRequiredString(LOAD_BALANCER_CERTIFICATE_ARN);

      if(awsRegion_ != null)
        awsClientRegion_ = awsRegion_;
      
//        awsIalbArn_   = amazon.getRequiredString(IALB_ARN);
//        awsIalbDns_   = amazon.getRequiredString(IALB_DNS);
//        awsR53Zone_   = amazon.getRequiredString(R53_ZONE);
      
      
      GetCallerIdentityResult callerIdentity = sts_.getCallerIdentity(new GetCallerIdentityRequest());
      
      log_.info("Connected as user " + callerIdentity.getArn());
      
      String actualAccountId = callerIdentity.getAccount();
      
      if(!actualAccountId.equals(awsAccountId_))
      {
        throw new IllegalStateException("AWS Account ID is " + awsAccountId_ + " but our credentials are for account " + actualAccountId);
      }
      
      //awsUser_ = iam_.getUser().getUser();
      
      getStringArray(amazon, LOAD_BALANCER_SUBNETS, awsLoadBalancerSubnets_);
      getStringArray(amazon, LOAD_BALANCER_SECURITY_GROUPS, awsLoadBalancerSecurityGroups_);

      IJsonDomNode regionsNode = amazon.get(REGIONS);
      if(regionsNode instanceof IJsonObject)
      {
        INameFactory nameFactory = createNameFactory(getEnvironmentType(), null, null, null, null, null);
        IJsonObject<?> regionsObject = (IJsonObject<?>)regionsNode;
        
        Iterator<String> it = regionsObject.getNameIterator();
        
        while(it.hasNext())
        {
          String name = it.next();
          
          environmentTypeRegions_.add(name);
          
          IJsonObject<?> regionObject = regionsObject.getRequiredObject(name);
          
          String bucketName = regionObject.getString(AWS_CONFIG_BUCKET,
              nameFactory.getConfigBucketName(name).toString());
              //FUGUE_PREFIX + getEnvironmentType() + Name.SEPARATOR + name + CONFIG_SUFFIX);
//              getGlobalNamePrefix() + FUGUE_PREFIX + getEnvironmentType() + Name.SEPARATOR + name + CONFIG_SUFFIX);
          environmentTypeConfigBuckets_.put(name, bucketName);
          
          if(awsRegion_ != null && name.equals(awsRegion_))
          {
            configBucket_ = bucketName;
          }
        }
      }
      else
      {
        if(regionsNode == null)
          throw new IllegalStateException("A top level configuration object called \"/" + AMAZON + "/" + REGIONS + "\" is required.");
        
        throw new IllegalStateException("The top level configuration object called \"/" + AMAZON + "/" + REGIONS + "\" must be an object not a " + node.getClass().getSimpleName());
      }

      elbClient_ = AmazonElasticLoadBalancingClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      r53Clinet_ = AmazonRoute53ClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      iamClient_ = AmazonIdentityManagementClientBuilder.standard()
        .withRegion(awsClientRegion_)
        .build();
      
      ecsClient_ = AmazonECSClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      logsClient_ = AWSLogsClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      cwClient_ =
          AmazonCloudWatchClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      cweClient_ =
          AmazonCloudWatchEventsClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
    }
    else
    {
      if(node == null)
        throw new IllegalStateException("A top level configuration object called \"" + AMAZON + "\" is required.");
      
      throw new IllegalStateException("The top level configuration object called \"" + AMAZON + "\" must be an object not a " + node.getClass().getSimpleName());
    }
  }
  
  private void getStringArray(IJsonObject<?> amazon, String nodeName, List<String> list)
  {
    IJsonDomNode sgNode = amazon.get(nodeName);
    if(sgNode instanceof IJsonArray)
    {
      IJsonArray<?> securityGroups = (IJsonArray<?>)sgNode;
      
      for(IJsonDomNode n : securityGroups)
      {
        if(n instanceof IStringProvider)
        {
          list.add(((IStringProvider)n).asString());
        }
        else
        {
          throw new IllegalStateException("The top level configuration object called \"/" + AMAZON + "/" + nodeName + "\" must be an array of strings, but it contains a " + n.getClass().getSimpleName());
        }
      }
    }
    else
    {
      if(sgNode == null)
        throw new IllegalStateException("A top level configuration object called \"/" + AMAZON + "/" + nodeName + "\" is required.");
      
      throw new IllegalStateException("The top level configuration object called \"/" + AMAZON + "/" + nodeName + "\" must be an array of strings not a " + sgNode.getClass().getSimpleName());
    }
  }

  
  private void createBucketIfNecessary(String region, String name)
  {
    AmazonS3 s3 = AmazonS3ClientBuilder
        .standard()
        .withRegion(region)
        .build();

    S3Helper.createBucketIfNecessary(s3, name, false);
  }
  
  
  // returns an access key if one was created.
  private void createUser(Name name, String groupName, List<String> keys)
  {
    String  userName      = name.append(USER).toString();
    String  accessKeyJson = null;
    
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
          log_.debug("User \"" + userName + "\" is already a member of group \"" + groupName + "\"");
          return;
        }
      }
    }
    catch(NoSuchEntityException e)
    {
      log_.info("User \"" + userName + "\" does not exist, creating...");
      
      iam_.createUser(new CreateUserRequest()
          .withUserName(userName)).getUser();
      
      log_.debug("Created user \"" + userName + "\"");
      
      AccessKey accessKey = iam_.createAccessKey(new CreateAccessKeyRequest()
          .withUserName(userName)).getAccessKey();
        
//        secret.println("#######################################################");
//        secret.println("# SAVE THIS ACCESS KEY IN ~/.aws/credentials");
//        secret.println("#######################################################");
//        secret.format("[%s]%n", userName);
//        secret.format("aws_access_key_id = %s%n", accessKey.getAccessKeyId());
//        secret.format("aws_secret_access_key = %s%n", accessKey.getSecretAccessKey());
//        secret.println("#######################################################");
        


      accessKeyJson = "  \"" + name + "\": {\n" +
        "    \"accessKeyId\": \"" + accessKey.getAccessKeyId() + "\",\n" +
        "    \"secretAccessKey\": \"" + accessKey.getSecretAccessKey() + "\"\n" +
        "  }";
      
      keys.add(accessKeyJson);
    }
    
    log_.debug("Adding user \"" + userName + "\" to group \"" + groupName + "\"");
    
    iam_.addUserToGroup(new AddUserToGroupRequest()
        .withUserName(userName)
        .withGroupName(groupName));
  }

  private String createGroup(Name name, String policyArn)
  {
    String groupName       = name.append(GROUP).toString();
    
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
  
  private String createRole(Name name, String trustDocument, String ...policyArnList)
  {
    String roleName       = name.append(ROLE).toString();
    Role role;
    
    try
    {
      role = iam_.getRole(new GetRoleRequest()
        .withRoleName(roleName))
        .getRole();
      
      List<AttachedPolicy> policies = iam_.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest()
          .withRoleName(roleName)).getAttachedPolicies();
      
      Set<String> attachedPolicyArns = new HashSet<>();
      
      for(AttachedPolicy policy : policies)
      {
        attachedPolicyArns.add(policy.getPolicyArn());
      }
      
      for(String policyArn : policyArnList)
      {
        if(attachedPolicyArns.contains(policyArn))
        {
          log_.debug("Role " + roleName + " already has policy " + policyArn + " attached.");
        }
        else
        {
          log_.info("Attaching policy " + policyArn + " to existing role " + roleName + "...");
          
          iam_.attachRolePolicy(new AttachRolePolicyRequest()
              .withPolicyArn(policyArn)
              .withRoleName(roleName));
        }
      }
      
      if(trustDocument != null)
      {
        log_.info("Updating trust policy for existing role " + roleName + "...");
        
        iam_.updateAssumeRolePolicy(new UpdateAssumeRolePolicyRequest()
          .withPolicyDocument(trustDocument)
          .withRoleName(roleName.toString())
          );
      }
      
      return role.getArn();
    }
    catch(NoSuchEntityException e)
    {
      log_.info("Role " + roleName + " does not exist, creating...");
      
      CreateRoleRequest request = new CreateRoleRequest()
          .withRoleName(roleName);
      
      if(trustDocument != null)
        request.withAssumeRolePolicyDocument(trustDocument);
      
      role = iam_.createRole(request
          ).getRole();
      
      log_.debug("Created role " + roleName);
    }
   
    for(String policyArn : policyArnList)
    {
      iam_.attachRolePolicy(new AttachRolePolicyRequest()
        .withPolicyArn(policyArn)
        .withRoleName(roleName));
    }
    
    return role.getArn();
  }
  
  

  private String createPolicy(Name name, String templateOutput)
  {
    String policyName       = name.append(POLICY).toString();
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


  
  
  
  
  
  
  
  
  
  
  

  protected class AwsDeploymentContext extends DeploymentContext
  {
    private LoadBalancer loadBalancer_;
  
    private String defaultTargetGroupArn_;
  
    private String listenerArn_;
    
    protected AwsDeploymentContext(String tenantId, INameFactory nameFactory)
    {
      super(tenantId, nameFactory);
    }

    @Override
    protected void createEnvironment()
    {
      List<String>  keys      = new LinkedList<>();
      Name          baseName  = getNameFactory().getName();
      //getEnvironmentType() + Name.SEPARATOR + getEnvironment();
      
      createEnvironmentAdminUser(baseName, keys);
      
      if(keys.isEmpty())
      {
        log_.info("No key created, secret unchanged.");
      }
      else
      {
        CredentialName  name    = getNameFactory().getFugueCredentialName("root");

        updateSecret(name, keys);
      }
    }
    
    private void updateSecret(CredentialName name, List<String> keys)
    {
      StringBuilder builder = new StringBuilder("{\n");
      
      for(int i=0 ; i<keys.size() ; i++)
      {
        builder.append(keys.get(i));
        
        if(i<keys.size()-1)
          builder.append(",\n");
        else
          builder.append("\n}");
      }
      
      String secret = builder.toString();
    
      secretManager_.putSecret(name, secret);
      
      log_.info("Created secret " + name);
    }

    private void createEnvironmentAdminUser(Name baseName, List<String> keys)
    {
      Name name = baseName.append(ADMIN);
      
      String policyArn = createPolicyFromResource(name, "policy/environmentAdmin.json");
//      String groupName = createGroup(name, policyArn);
//      String result    = createUser(name, groupName, keys);
      
      createRole(name, TRUST_ECS_DOCUMENT, policyArn);
    }

    @Override
    protected void createEnvironmentType()
    {
      
      Name baseName = getNameFactory().getFugueName(); 
          
          //FUGUE_PREFIX + getEnvironmentType();
      List<String>  keys = new LinkedList<>();

      createEnvironmentTypeAdminUser(baseName, keys);
      createEnvironmentTypeCicdUser(baseName, keys);
      createEnvironmentTypeSupportUser(baseName, keys);
      
      createEnvironmentTypeRoles(baseName);

      if(keys.isEmpty())
      {
        log_.info("No key created, secret unchanged.");
      }
      else
      {
//        CredentialName  name = 
//            getNameFactory().getCredentialName(tenantId, owner)
//            new CredentialName("fugue-" + getEnvironmentType(),
//          null, // environment
//          null, // realm
//          null, // tenant
//          "root");
        
        CredentialName  name    = getNameFactory().getFugueCredentialName("root");
      
        updateSecret(name, keys);
      }
      
      for(String region : environmentTypeRegions_)
      {
        createBucketIfNecessary(region, environmentTypeConfigBuckets_.get(region));
      }
    }

    private void createEnvironmentTypeRoles(Name baseName)
    {
      createAssumedRole(baseName.append(DYNAMO_AUTOSCALE), "dynamoDbAutoscale");
    }
    
    private void createAssumedRole(Name name, String templateName)
    {
      String policyArn        = createPolicyFromResource(name, "policy/" + templateName + ".json");
      String assumeRolePolicy = loadTemplateFromResource("policy/" + templateName + "Trust.json");
      
      createRole(name, assumeRolePolicy, policyArn);
    }

    private void createEnvironmentTypeAdminUser(Name baseName, List<String> keys)
    {
      Name name = baseName.append(ADMIN);
      
      String policyArn = createPolicyFromResource(name, "policy/environmentTypeAdmin.json");
//      String groupName = createGroup(name, policyArn);
//      String result    = createUser(name, groupName, keys);
      
      createRole(name, TRUST_ECS_DOCUMENT, policyArn);
    }
    
    private void createEnvironmentTypeSupportUser(Name baseName, List<String> keys)
    {
      Name name = baseName.append(SUPPORT);
      
      String infraPolicyArn = createPolicyFromResource(baseName.append("infra-list-all"), "policy/environmentTypeInfraListAll.json");
      String appPolicyArn = createPolicyFromResource(baseName.append("app-list-all"), "policy/environmentTypeAppListAll.json");
      String fuguePolicyArn = createPolicyFromResource(name, "policy/environmentTypeSupport.json");
//      String groupName = createGroup(name, policyArn);
//      String result    = createUser(name, groupName, keys);
      

      String assumeRolePolicy = loadTemplateFromResource("policy/environmentTypeSupportTrust.json");
      
      createRole(name, assumeRolePolicy, infraPolicyArn, appPolicyArn, fuguePolicyArn);
    }
    
    private void createEnvironmentTypeCicdUser(Name baseName, List<String> keys)
    {
      Name name = baseName.append(CICD);
      
      String policyArn = createPolicyFromResource(name, "policy/environmentTypeCicd.json");
      String groupName = createGroup(name, policyArn);
      createUser(name, groupName, keys);
      
//      createRole(name, policyArn);
    }
    @Override
    protected void populateTemplateVariables(ImmutableJsonObject config, Map<String, String> templateVariables)
    {
      if(configBucket_ != null)
      {
        templateVariables.put(AWS_CONFIG_BUCKET, configBucket_);
      }
      
      templateVariables.put(AWS_ACCOUNT_ID, awsAccountId_);
      
      super.populateTemplateVariables(config, templateVariables);
    }
    
    @Override
    protected void processRole(String roleName, String roleSpec, String trustSpec)
    {
      Name name = getNameFactory().getServiceItemName(roleName);
//          Name(getEnvironmentType(), getEnvironment(), getTenantId(), getService(), roleName).toString();
      
      String policyArn = createPolicy(name, roleSpec);
      
      createRole(name, trustSpec==null ? 
          TRUST_ECS_DOCUMENT : 
            trustSpec, 
            policyArn);
    }

    @Override
    protected void saveConfig()
    {
      String              name        = getNameFactory().getServiceName().toString();
      String              bucketName  = environmentTypeConfigBuckets_.get(getAwsRegion());
      String              key         = CONFIG + "/" + name + DOT_JSON;
      ImmutableByteArray  dom         = getConfigDom().serialize();
      
      
      log_.info("Saving config to region: " + getAwsRegion() + " bucket: " + bucketName + " key: " + key);
      
      AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
          .withRegion(getAwsRegion())
          .build();
    
      try
      {
        ObjectMetadata      metaData  = s3Client.getObjectMetadata(bucketName, key);
        
        if(APPLICATION_JSON.equals(metaData.getContentType()) && metaData.getContentLength() == dom.length())
        {
          S3Object existingContent = s3Client.getObject(bucketName, key);
          int i;
          
          for(i=0 ; i<metaData.getContentLength() ; i++)
            if(existingContent.getObjectContent().read() != dom.byteAt(i))
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
      metadata.setContentLength(dom.length());
      
      PutObjectRequest request = new PutObjectRequest(bucketName, key, dom.getInputStream(), metadata);
      
      s3Client.putObject(request);
    }
    
    private String createPolicyFromResource(Name name, String fileName)
    {
      return createPolicy(name, loadTemplateFromResource(fileName));
    }
    
    @Override
    protected void configureServiceNetwork()
    {
      try
      {
        String  tenantId        = getTenantId();
//        Name    targetGroupName = getNameFactory().getServiceItemName(name);
        //new Name(getEnvironmentType(), getEnvironment(), tenantId, getService(), name);
        
//        String targetGroupArn = createTargetGroup(targetGroupName, healthCheckPath, port);
        
        String hostName = isPrimaryEnvironment() ? getServiceHostName(tenantId) : null;
        
        String regionalHostName = getNameFactory()
            .getRegionalServiceName().toString().toLowerCase() + "." + getDnsSuffix();
        
//        String wildCardHostName = getNameFactory()
//            .withRegionId("*")
//            .getRegionalServiceName().toString().toLowerCase() + "." + getDnsSuffix();
        
        //new Name(getEnvironmentType(), getEnvironment(), "*", tenantId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
        
//        configureNetworkRule(targetGroupArn, wildCardHostName, name, port, paths, healthCheckPath);
        
        createR53RecordSet(hostName, regionalHostName, loadBalancer_);
        
        getOrCreateCluster();
        
      }
      catch(RuntimeException e)
      {
        e.printStackTrace();
        
        throw e;
      }
    }

    @Override
    protected void deployServiceContainer(String name, int port, Collection<String> paths, String healthCheckPath, int instances)
    {
      try
      {
//        String  tenantId        = getTenantId();
        Name    targetGroupName = getNameFactory().getServiceItemName(name);
//        //new Name(getEnvironmentType(), getEnvironment(), tenantId, getService(), name);
//        
        String targetGroupArn = createTargetGroup(targetGroupName, healthCheckPath, port);
//        
//        String hostName = isPrimaryEnvironment() ? getServiceHostName(tenantId) : null;
//        
        String regionalHostName = getNameFactory()
            .getRegionalServiceName().toString().toLowerCase() + "." + getDnsSuffix();
        
        String wildCardHostName = getNameFactory()
            .withRegionId("*")
            .getRegionalServiceName().toString().toLowerCase() + "." + getDnsSuffix();
        
//        //new Name(getEnvironmentType(), getEnvironment(), "*", tenantId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
//        
        configureNetworkRule(targetGroupArn, wildCardHostName, name, port, paths, healthCheckPath);
//        
//        createR53RecordSet(hostName, regionalHostName, loadBalancer_);
        
        getOrCreateCluster();
        
  //      registerTaskDef(name, port, healthCheckPath, tenantId);
        
        createService(regionalHostName, targetGroupArn, name, port, instances, paths);
      }
      catch(RuntimeException e)
      {
        e.printStackTrace();
        
        throw e;
      }
    }
    
    
    @Override
    protected void deployScheduledTaskContainer(String name, int port, Collection<String> paths, String schedule)
    {
      Name serviceName  = getNameFactory().getServiceItemName(name);
      Name baseName     = serviceName.append("schedule");
      Name ruleName     = baseName.append("rule");

      String policyArn =  createPolicyFromResource(baseName, "policy/eventsInvokeEcsTask.json");
      String roleArn =   createRole(baseName, TRUST_EVENTS_DOCUMENT, policyArn);
      
      cweClient_.putRule(new PutRuleRequest()
          .withName(ruleName.toString())
          .withScheduleExpression("cron(" + schedule + ")")
          .withState(RuleState.ENABLED)
          );

      PutTargetsRequest request = new PutTargetsRequest()
          .withTargets(new Target()
              .withArn(getClusterArn())
              .withRoleArn(roleArn)
              .withEcsParameters(new EcsParameters()
                  .withTaskCount(1)
                  .withTaskDefinitionArn(getTaskDefinitionArn(serviceName.toString()))
                  .withLaunchType(LaunchType.EC2)
                  .withGroup(name)
                  )
              .withId(name)
              )
          .withRule(ruleName.toString()
          );

      cweClient_.putTargets(request);
    }

    private String getClusterArn()
    {
      return "arn:aws:ecs:" + awsRegion_ + ":" + awsAccountId_ + ":cluster/" + clusterName_;
    }

    private String getTaskDefinitionArn(String name)
    {
      return "arn:aws:ecs:" + awsRegion_ + ":" + awsAccountId_ + ":task-definition/" + name;
    }

    private void createR53RecordSet(String host, String regionalHost, LoadBalancer loadBalancer)
    {
      if(host != null)
        createR53RecordSet(host, regionalHost, true);
      
      createR53RecordSet(regionalHost, loadBalancer.getDNSName(), false);
    }
    
    private void createR53RecordSet(String source, String target, boolean multiValue)
    {
      String zoneId = createOrGetHostedZone(source.substring(source.indexOf('.') + 1), false);
      
      if(zoneId.startsWith("/hostedzone/"))
        zoneId = zoneId.substring(12);
      
      String sourceDomain = source + ".";
      String setIdentifier = multiValue ? getNameFactory().getRegionName().toString().toLowerCase() : null;
      
      
      ListResourceRecordSetsResult result = r53Clinet_.listResourceRecordSets(new ListResourceRecordSetsRequest()
          .withHostedZoneId(zoneId)
          .withStartRecordName(source)
          );
      
      List<ResourceRecordSet> recordSetList = result.getResourceRecordSets();
      boolean                 exists        = false;
      boolean                 ok            = false;
      
      for(ResourceRecordSet recordSet : recordSetList)
      {
        if(sourceDomain.equals(recordSet.getName()) 
            && strEquals(recordSet.getSetIdentifier(), setIdentifier)
            )
        {
          log_.info("R53 record set exists for " + source);
          exists = true;
          
          for(ResourceRecord record : recordSet.getResourceRecords())
          {
            if(target.equals(record.getValue()))
            {
                ok = true;
                break;
            }
          }
        }
        else
        {
          // records come back in order...
          break;
        }
      }
      if(ok)
      {
        log_.info("R53 record set for " + source + " to " + target + " exists, nothing to do here.");
      }
      else
      {
        log_.info("Creating R53 record set for " + source + " to " + target + "...");
        
        ResourceRecordSet resourceRecordSet = new ResourceRecordSet()
            .withName(source)
            .withType(RRType.CNAME)
            .withTTL(300L)
            .withResourceRecords(new ResourceRecord()
                .withValue(target)
                )
            ;
        
        if(multiValue)
        {
          resourceRecordSet
            .withWeight(1L)
            .withSetIdentifier(setIdentifier)
            ;
        }
        
        RuntimeException savedException = new RuntimeException("No saved exception");
        
        for(int i=0 ; i<10 ; i++)
        {
          try
          {
            ChangeResourceRecordSetsResult rresult = r53Clinet_.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
              .withHostedZoneId(zoneId)
              .withChangeBatch(new ChangeBatch()
                  .withChanges(new Change()
                      .withAction(ChangeAction.UPSERT)
                      .withResourceRecordSet(resourceRecordSet)
                      )
                  )
              );
            
            return;
          }
          catch(PriorRequestNotCompleteException e)
          {
            savedException = e;
            log_.info("Route53 request still in progress", e);
            
            try
            {
              Thread.sleep(5000);
            }
            catch (InterruptedException e1)
            {
              log_.error("Interrupted", e1);
              
              throw e;
            }
          }
        }
        log_.error("Failed to create resource sets", savedException);
        throw savedException;
      }
    }

    private boolean strEquals(String a, String b)
    {
     if(a == null)
       return b == null;
     
      return a.equals(b);
    }

    private String createTargetGroup(Name name, String healthCheckPath, int port)
    {
      String shortName = name.getShortName(32);
      
      try
      {
        DescribeTargetGroupsResult desc = elbClient_.describeTargetGroups(new DescribeTargetGroupsRequest().withNames(shortName));
        
        List<TargetGroup> groups = desc.getTargetGroups();
        
        if(groups.size() != 1)
            throw new IllegalStateException("Describe target group by name returns " + groups.size() + " results!");
        
        log_.info("Target group " + name + " (" + shortName + ") already exists.");
        return elbTag(groups.get(0).getTargetGroupArn());
      }
      catch(TargetGroupNotFoundException e)
      {
        log_.info("Target group " + name + " (" + shortName + ") does not exist, will create it...");
      }
      
      CreateTargetGroupResult result = elbClient_.createTargetGroup(new CreateTargetGroupRequest()
          .withName(shortName)
          .withHealthCheckPath(healthCheckPath)
          .withHealthCheckProtocol(ProtocolEnum.HTTP)
          .withProtocol(ProtocolEnum.HTTP)
          .withVpcId(awsVpcId_)
          .withPort(port)
          );
      
      return elbTag(result.getTargetGroups().get(0).getTargetGroupArn());
    }


    private String elbTag(String arn)
    {
      List<Tag> tags = new LinkedList<>();
      
      for(Entry<String, String> entry : getTags().entrySet())
      {
        tags.add(new Tag().withKey(entry.getKey()).withValue(entry.getValue()));
      }
      
      tagIfNotNull(tags, "FUGUE_TENANT", getTenantId());
      
      if(!tags.isEmpty())
      {
        elbClient_.addTags(new AddTagsRequest()
            .withResourceArns(arn)
            .withTags(tags)
            );
      }
    
      return arn;
    }
    
    private void tagIfNotNull(List<Tag> tags, String name, String value)
    {
      if(value != null)
      {
        tags.add(new Tag().withKey(name).withValue(value));
      }
    }

    private void configureNetworkRule(String targetGroupArn, String host, String name, int port, Collection<String> paths, String healthCheckPath)
    {
      
      List<String> remainingPaths = new ArrayList<>();
      
      remainingPaths.addAll(paths);
      
      DescribeRulesResult ruleDescription = elbClient_.describeRules(new DescribeRulesRequest()
          .withListenerArn(listenerArn_)
          );
      
      List<Rule> ruleList = ruleDescription.getRules();
      int        priority = 1000;
      
      for(Rule rule : ruleList)
      {
        String conditionHost = null;
        String conditionPath = null;
        
        for(RuleCondition c : rule.getConditions())
        {
          if(c.getField().equals(HOST_HEADER))
          {
            if(c.getValues().size() > 0)
              conditionHost = c.getValues().get(0);
          }
          else if(c.getField().equals(PATH_PATERN))
          {
            if(c.getValues().size() > 0)
              conditionPath = c.getValues().get(0);
          }
        }
        
        String actionTargetArn = null;
        
        // since there is only one action I can't see how there will not always be exactly one of these but....
        for(Action action : rule.getActions())
        {
          actionTargetArn = action.getTargetGroupArn();
        }
        
        if(host.equals(conditionHost))
        {
          // remove old host rules
          
          log_.info("Deleting rule " + rule.getRuleArn() + " for host " + conditionHost + " for path " + conditionPath);
          
          elbClient_.deleteRule(new DeleteRuleRequest()
              .withRuleArn(rule.getRuleArn())
              );
        }
        else
        {
          if(conditionHost == null)
          {
            if(remainingPaths.remove(conditionPath))
            {
              if(targetGroupArn.equals(actionTargetArn))
              {
                log_.debug("Rule " + rule.getRuleArn() + " for path " + conditionPath + " is OK, nothing to do");
              }
              else
              {
                log_.info("Updating rule " + rule.getRuleArn() + " for path " + conditionPath);
                // the rule is there but it's wrong
                elbClient_.modifyRule(new ModifyRuleRequest()
                    .withRuleArn(rule.getRuleArn())
                    .withActions(new Action()
                        .withTargetGroupArn(targetGroupArn)
                        .withType(ActionTypeEnum.Forward)
                        )
                    );
              }
            }
            else
            {
              // this rule is for a path which we don't have, maybe it was removed from the service
              
              if(!"default".equals(rule.getPriority()))
              {
                if(targetGroupArn.equals(actionTargetArn))
                {
                  log_.info("Deleting rule " + rule.getRuleArn() + " for non-existant path " + conditionPath);
                  
                  elbClient_.deleteRule(new DeleteRuleRequest()
                      .withRuleArn(rule.getRuleArn())
                      );
                }
              }
            }
          }
          
          if(!"default".equals(rule.getPriority()))
          {
            try
            {
              int p = Integer.parseInt(rule.getPriority());
              
              if(p >= priority)
                priority = p + 1;
            }
            catch(NumberFormatException e)
            {
              log_.warn("Rule has non-integer priority: " + rule);
            }
          }
        }
      }
      
      for(String path : remainingPaths)
      {
        log_.info("Creating rule for host " + host + " for non-existant path " + path + "...");
        
        CreateRuleResult createRuleResult = elbClient_.createRule(new CreateRuleRequest()
            .withListenerArn(listenerArn_)
            .withConditions(
//                new RuleCondition()
//                  .withField(HOST_HEADER)
//                  .withValues(host),
                new RuleCondition()
                  .withField(PATH_PATERN)
                  .withValues(path)
                )
            .withActions(new Action()
                .withTargetGroupArn(targetGroupArn)
                .withType(ActionTypeEnum.Forward)
                )
            .withPriority(priority)
            );
        
        log_.info("Created rule " + createRuleResult.getRules().get(0).getRuleArn() + " for host " + host + " for non-existant path " + path);
      }
    }
    
    @Override
    protected void deployInitContainer(String name, int port, Collection<String> paths, String healthCheckPath)
    {
      // TODO move from groovy land
      
      createTaskDef(name, port, paths, healthCheckPath);
    }

    @Override
    protected void deployService()
    {
      // createDnsZones();
      
      if(!getServiceContainerMap().isEmpty())
      {
        loadBalancer_ = createLoadBalancer();
        
        Name targetGroupName = getNameFactory().getServiceItemName(DEFAULT);
            //new Name(getEnvironmentType(), getEnvironment(), getTenantId(), getService(), DEFAULT);
        
        defaultTargetGroupArn_ = createTargetGroup(targetGroupName, "/HealthCheck", 80);
        
        listenerArn_ = createLoadBalancerListener(loadBalancer_, defaultTargetGroupArn_);
      }
    }
    
    private String createLoadBalancerListener(LoadBalancer loadBalancer, String defaultTargetGroupArn)
    {
      DescribeListenersResult describeResponse = elbClient_.describeListeners(new DescribeListenersRequest()
          .withLoadBalancerArn(loadBalancer.getLoadBalancerArn())
          );
      
      List<Listener> listeners = describeResponse.getListeners();
      
      if(!listeners.isEmpty())
      {
        String listenerArn = listeners.get(0).getListenerArn();
        log_.info("Listener " + listenerArn + " already exists.");
        return listenerArn;
      }
//      for(Listener listener : listeners)
//      {
//        if(ProtocolEnum.HTTPS.equals(listener.getProtocol()))
//        {
//          for(Certificate cert : listener.getCertificates())
//          {
//            cert.getCertificateArn()
//          }
//        }
//      }
      
      
      log_.info("Creating listener...");
      
//      elbClient_.Cer
//      GetServerCertificateResult certificateResult = iamClient_.getServerCertificate(new GetServerCertificateRequest()
//          .withServerCertificateName("NAME")
//          );
//      
//      Certificate certificate = certificateResult.getServerCertificate();
      
//      elbClient_.addListenerCertificates(new AddListenerCertificatesRequest()
//          .withCertificates(certificates)
//          );
      
      CreateListenerResult createResult = elbClient_.createListener(new CreateListenerRequest()
          .withCertificates(new Certificate()
            .withCertificateArn(awsLoadBalancerCertArn_)
          )
          .withLoadBalancerArn(loadBalancer.getLoadBalancerArn())
          .withProtocol(ProtocolEnum.HTTPS)
          .withPort(443)
          .withDefaultActions(new Action()
              .withType(ActionTypeEnum.Forward)
              .withTargetGroupArn(defaultTargetGroupArn)
              )
          );
      
      listeners = createResult.getListeners();
      
      String listenerArn = listeners.get(0).getListenerArn();
      log_.info("Listener " + listenerArn + " created.");
      return listenerArn;
    }
    

    
    private void createTaskDef(String name, int port, Collection<String> paths, String healthCheckPath)
    {
//      Name    serviceName = getNameFactory().getServiceItemName(name); //new Name(getEnvironmentType(), getEnvironment(), getRealm(), getRegion(), tenantId, name);
//      boolean create      = true;
//      
//      ContainerDefinition containerDefinition = new ContainerDefinition()
//          .withName(serviceName.toString())
//          .withImage(image)
//          ;
//      ecsClient_.registerTaskDefinition(new RegisterTaskDefinitionRequest()
//          .withContainerDefinitions(containerDefinition)
//          );
    }

    private void createService(String regionalHostName, String targetGroupArn, String name, int port, int desiredCnt, Collection<String> paths)
    {
      log_.info("Cluster name is " + clusterName_);
      
      Name    serviceName = getNameFactory().getServiceItemName(name); //new Name(getEnvironmentType(), getEnvironment(), getRealm(), getRegion(), tenantId, name);
      boolean create      = true;
      
      DescribeServicesResult describeResult = ecsClient_.describeServices(new DescribeServicesRequest()
          .withCluster(clusterName_)
          .withServices(serviceName.toString())
          );
      
      for(Service service : describeResult.getServices())
      {
        if(serviceName.toString().equals(service.getServiceName()))
        {
          log_.info("Service " + serviceName + " exists with status " + service.getStatus());
          
          switch(service.getStatus())
          {
            case "INACTIVE":
            case "DRAINING":
              create = true;
              break;
              
            default:
              create = false;
          }
        }
      }
      
      if(create)
      {
        log_.info("Creating service " + serviceName + "...");
        
        CreateServiceRequest request = new CreateServiceRequest()
            .withCluster(clusterName_)
            .withServiceName(serviceName.toString())
            .withTaskDefinition(serviceName.toString())
            .withDesiredCount(desiredCnt)
    //        .withDeploymentConfiguration(new DeploymentConfiguration()
    //            .withMaximumPercent(maximumPercent)
    //            .withMinimumHealthyPercent(minimumHealthyPercent)
    //            )
            ;
        
        if(!paths.isEmpty())
        {
          request
            .withLoadBalancers(new com.amazonaws.services.ecs.model.LoadBalancer()
              .withContainerName(serviceName.toString()) // TODO: change to just "name" once we get task def working from Java
              .withContainerPort(port)
              .withTargetGroupArn(targetGroupArn)
            );
        }
        CreateServiceResult createServiceResult = ecsClient_.createService(request);
        
        log_.info("Created service " + serviceName + "as" + createServiceResult.getService().getServiceArn() + " with status " + createServiceResult.getService().getStatus() + ".");
      }
      else
      {
        log_.info("Updating service " + serviceName + "...");
        
        UpdateServiceResult updateResult = ecsClient_.updateService(new UpdateServiceRequest()
            .withCluster(clusterName_)
            .withService(serviceName.toString())
            .withTaskDefinition(serviceName.toString())
            .withDesiredCount(desiredCnt)
//            .withForceNewDeployment(true)
            );
        
        log_.info("Updated service " + serviceName + "as" + updateResult.getService().getServiceArn() + " with status " + updateResult.getService().getStatus() + ".");
      }
    }

    private LoadBalancer createLoadBalancer()
    {
      String name = getNameFactory().getServiceName().getShortName(32);
//          new Name(getEnvironmentType(), getEnvironment(), tenant, getService()).getShortName(32);
      
      try
      {
        DescribeLoadBalancersResult describeResult = elbClient_.describeLoadBalancers(new DescribeLoadBalancersRequest()
            .withNames(name)
            );
        
        List<LoadBalancer> loadBalancerList = describeResult.getLoadBalancers();
        
        if(loadBalancerList.size() > 0 && name.equals(loadBalancerList.get(0).getLoadBalancerName()))
        {
          LoadBalancer loadBalancer = loadBalancerList.get(0);
    
          log_.info("Load balancer exists as " + loadBalancer.getLoadBalancerArn() + " at " + loadBalancer.getDNSName());
          
          boolean ok = true;
          
//          // So the LB exists, check that it has the correct security groups and subnets
//          int     cnt = awsLoadBalancerSecurityGroups_.size();
//          
//          for(String sg : loadBalancer.getSecurityGroups())
//          {
//            if(awsLoadBalancerSecurityGroups_.contains(sg))
//            {
//              cnt--;
//            }
//            else
//            {
//              ok = false;
//              break;
//            }
//          }
//          
//          if(cnt > 0)
//            ok = false;
//          
//          if(ok)
//          {
//            cnt = awsLoadBalancerSubnets_.size();
//            for(AvailabilityZone az : loadBalancer.getAvailabilityZones())
//            {
//              if(awsLoadBalancerSubnets_.contains(az.getSubnetId()))
//              {
//                cnt--;
//              }
//              else
//              {
//                ok = false;
//                break;
//              }
//            }
//            
//            if(cnt > 0)
//              ok = false;
//          }
          
          if(ok)
          {
            log_.info("Load balancer " + loadBalancer.getLoadBalancerArn() + " is good, no more to do");
            elbTag(loadBalancer.getLoadBalancerArn());
            return loadBalancer;
          }
          else
          {
            log_.info("Load balancer " + loadBalancer.getLoadBalancerArn() + " needs to be updated...");
            
            // To fix this we ned to get all the rules, delete the LB, create a new one, and add all the rules back
            throw new IllegalStateException("Loadbalancer needs to be updated");
          }
        }
      }
      catch(LoadBalancerNotFoundException e)
      {
        log_.info("Load balancer " + name + " does not exist, creating...");
      }

      CreateLoadBalancerResult createResponse = elbClient_.createLoadBalancer(new CreateLoadBalancerRequest()
          .withName(name)
          .withSecurityGroups(awsLoadBalancerSecurityGroups_)
          .withSubnets(awsLoadBalancerSubnets_)
          );
      
      LoadBalancer loadBalancer = createResponse.getLoadBalancers().get(0);
      
      log_.info("Load balancer " + loadBalancer.getLoadBalancerArn() + " created.");
      
      elbTag(loadBalancer.getLoadBalancerArn());
      
      return loadBalancer;
    }
  }

}
