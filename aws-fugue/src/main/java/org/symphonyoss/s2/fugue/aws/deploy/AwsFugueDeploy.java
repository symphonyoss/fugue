/*
 *
 *
 * Copyright 2018-2019 Symphony Communication Services, LLC.
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
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.json.IJsonArray;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonObject;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.common.dom.json.JsonObject;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.common.type.provider.IStringProvider;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.aws.config.S3Helper;
import org.symphonyoss.s2.fugue.aws.secret.AwsSecretManager;
import org.symphonyoss.s2.fugue.deploy.ConfigHelper;
import org.symphonyoss.s2.fugue.deploy.ConfigProvider;
import org.symphonyoss.s2.fugue.deploy.FugueDeploy;
import org.symphonyoss.s2.fugue.deploy.FugueDeployAction;
import org.symphonyoss.s2.fugue.deploy.Subscription;
import org.symphonyoss.s2.fugue.naming.CredentialName;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.Name;

import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.ConnectionType;
import com.amazonaws.services.apigateway.model.CreateBasePathMappingRequest;
import com.amazonaws.services.apigateway.model.CreateBasePathMappingResult;
import com.amazonaws.services.apigateway.model.CreateDeploymentRequest;
import com.amazonaws.services.apigateway.model.CreateDeploymentResult;
import com.amazonaws.services.apigateway.model.CreateDomainNameRequest;
import com.amazonaws.services.apigateway.model.CreateDomainNameResult;
import com.amazonaws.services.apigateway.model.CreateResourceRequest;
import com.amazonaws.services.apigateway.model.CreateResourceResult;
import com.amazonaws.services.apigateway.model.CreateRestApiRequest;
import com.amazonaws.services.apigateway.model.CreateRestApiResult;
import com.amazonaws.services.apigateway.model.DeleteMethodRequest;
import com.amazonaws.services.apigateway.model.DeleteRestApiRequest;
import com.amazonaws.services.apigateway.model.EndpointConfiguration;
import com.amazonaws.services.apigateway.model.EndpointType;
import com.amazonaws.services.apigateway.model.GetBasePathMappingRequest;
import com.amazonaws.services.apigateway.model.GetBasePathMappingResult;
import com.amazonaws.services.apigateway.model.GetDomainNameRequest;
import com.amazonaws.services.apigateway.model.GetDomainNameResult;
import com.amazonaws.services.apigateway.model.GetMethodRequest;
import com.amazonaws.services.apigateway.model.GetMethodResult;
import com.amazonaws.services.apigateway.model.GetResourcesRequest;
import com.amazonaws.services.apigateway.model.GetResourcesResult;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.GetStageRequest;
import com.amazonaws.services.apigateway.model.GetStageResult;
import com.amazonaws.services.apigateway.model.IntegrationType;
import com.amazonaws.services.apigateway.model.Op;
import com.amazonaws.services.apigateway.model.PatchOperation;
import com.amazonaws.services.apigateway.model.PutIntegrationRequest;
import com.amazonaws.services.apigateway.model.PutIntegrationResult;
import com.amazonaws.services.apigateway.model.PutMethodRequest;
import com.amazonaws.services.apigateway.model.PutMethodResult;
import com.amazonaws.services.apigateway.model.Resource;
import com.amazonaws.services.apigateway.model.RestApi;
import com.amazonaws.services.apigateway.model.SecurityPolicy;
import com.amazonaws.services.apigateway.model.TagResourceRequest;
import com.amazonaws.services.apigateway.model.UpdateBasePathMappingRequest;
import com.amazonaws.services.apigateway.model.UpdateDomainNameRequest;
import com.amazonaws.services.apigateway.model.UpdateStageRequest;
import com.amazonaws.services.apigateway.model.UpdateStageResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.cloudwatchevents.model.EcsParameters;
import com.amazonaws.services.cloudwatchevents.model.LaunchType;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.ListTargetsByRuleResult;
import com.amazonaws.services.cloudwatchevents.model.PutRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RuleState;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.ContainerOverride;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DeregisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsRequest;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsResult;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.amazonaws.services.ecs.model.LogDriver;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RunTaskRequest;
import com.amazonaws.services.ecs.model.RunTaskResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskOverride;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
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
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeletePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeletePolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetPolicyVersionRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.ListAttachedGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListPolicyVersionsRequest;
import com.amazonaws.services.identitymanagement.model.ListPolicyVersionsResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PolicyVersion;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.UpdateAssumeRolePolicyRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.AddPermissionRequest;
import com.amazonaws.services.lambda.model.AddPermissionResult;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;
import com.amazonaws.services.lambda.model.DeleteProvisionedConcurrencyConfigRequest;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetAliasRequest;
import com.amazonaws.services.lambda.model.GetAliasResult;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.GetProvisionedConcurrencyConfigRequest;
import com.amazonaws.services.lambda.model.GetProvisionedConcurrencyConfigResult;
import com.amazonaws.services.lambda.model.ListVersionsByFunctionRequest;
import com.amazonaws.services.lambda.model.ListVersionsByFunctionResult;
import com.amazonaws.services.lambda.model.ProvisionedConcurrencyConfigNotFoundException;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.PutProvisionedConcurrencyConfigRequest;
import com.amazonaws.services.lambda.model.PutProvisionedConcurrencyConfigResult;
import com.amazonaws.services.lambda.model.RemovePermissionRequest;
import com.amazonaws.services.lambda.model.RemovePermissionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.logs.model.OutputLogEvent;
import com.amazonaws.services.logs.model.PutRetentionPolicyRequest;
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
  static final String LAMBDA_ALIAS_NAME = "live";
  
  private static final Logger            log_                          = LoggerFactory.getLogger(AwsFugueDeploy.class);

  private static final String            AMAZON                        = "amazon";
  private static final String            ACCOUNT_ID                    = "accountId";
  private static final String            REGION                        = "regionName";
  private static final String            REGIONS                       = "environmentTypeRegions";
  private static final String            CLUSTER_NAME                  = "ecsCluster";
  private static final String            LOAD_BALANCER_CERTIFICATE_ARN = "loadBalancerCertificateArn";
  private static final String            PUBLIC_CERTIFICATE_ARN        = "publicCertificateArn";
  
  private static final String            POLICY                         = "policy";
  private static final String            GROUP                          = "group";
  public static final String             ROLE                           = "role";
  private static final String            USER                           = "user";
  private static final String            ROOT                           = "root";
  private static final String            ADMIN                          = "admin";
  private static final String            SUPPORT                        = "support";
  private static final String            CICD                           = "cicd";
  private static final String            CONFIG                         = "config";
  private static final String            LAMBDA                         = "lambda";

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
  private final AwsSecretManager         secretManager_                = new AwsSecretManager.Builder().withRegion("us-east-1").build();

  private String                         awsAccountId_;
  private String                         awsRegion_;
  private String                         awsClientRegion_ = "us-east-1"; // used to create client instances
  private String                         awsLoadBalancerCertArn_;
  private String                         awsPublicCertArn_;

  private List<String>                   environmentTypeRegions_       = new LinkedList<>();
  private Map<String, String>            environmentTypeConfigBuckets_ = new HashMap<>();
  private String                         configBucket_;
  private String                         callerRefPrefix_              = UUID.randomUUID().toString() + "-";
  private String                         clusterName_;

  private AmazonRoute53                  r53Clinet_;
  private AmazonIdentityManagement       iamClient_;
  private AmazonCloudWatch               cwClient_;
  private AmazonCloudWatchEvents         cweClient_;
  private AWSLambda                      lambdaClient_;
  private AmazonECS                      ecsClient_;
  private AWSLogs                        logsClient_;
  private AmazonApiGateway               apiClient_;
  private AmazonDynamoDBStreams          dynamoStreamsClient_;

  private AmazonDynamoDB amazonDynamoDB_;

  private DynamoDB dynamoDB_;




  
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
  protected DeploymentContext createContext(String podName, INameFactory nameFactory)
  {
    return new AwsDeploymentContext(podName, nameFactory);
  }

  private String getAwsRegion()
  {
    return require("AWS Region", awsRegion_);
  }
  
  private String getPolicyArn(String policyName)
  {
    return getIamPolicy("policy", policyName);
  }
  
  private String getApiArn(String name)
  {
    // arn:aws:execute-api:region:account-id:api-id/stage-name/HTTP-VERB/resource-path-specifier
    // arn:aws:apigateway:region::resource-path-specifier.
    return getNoAccountArn("apigateway", "/restapis", name);
  }
  
  private String getFunctionInvocationArn(String name)
  {
    return String.format("arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/arn:aws:lambda:%s:%s:function:%s:%s/invocations",
        awsRegion_,
        awsRegion_,
        awsAccountId_,
        name,
        LAMBDA_ALIAS_NAME);
  }
  
  private String getQueueArn(String name)
  {
    return String.format("arn:aws:sqs:%s:%s:%s",
        awsRegion_,
        awsAccountId_,
        name);
  }
  
  private String getIamPolicy(String type, String name)
  {
    return getArn("iam", type, name);
  }

  private String getArn(String service, String type, String name)
  {
    return String.format("arn:aws:%s::%s:%s/%s", service, awsAccountId_, type, name);
  }

  private String getNoAccountArn(String service, String type, String name)
  {
    return String.format("arn:aws:%s:%s::%s/%s", service, awsRegion_, type, name);
  }

  private String getArn(String service, String type)
  {
    return String.format("arn:aws:%s::%s:%s", service, awsAccountId_, type);
  }
  
  private void abort(String message, Throwable cause)
  {
    log_.error(message, cause);
    
    throw new IllegalStateException(message, cause);
  }

  private String getServiceHostName(String podId)
  {
//    return new Name(getEnvironmentType(), getEnvironment(), "any", podId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
    if(podId == null)
      return getService().toLowerCase() + "." + getDnsSuffix();
    else
      return (podId + "-" + getService()).toString().toLowerCase() + "." + getDnsSuffix();
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
      clusterName_            = amazon.getRequiredString(CLUSTER_NAME);
      awsLoadBalancerCertArn_ = amazon.getRequiredString(LOAD_BALANCER_CERTIFICATE_ARN);
      awsPublicCertArn_       = amazon.getRequiredString(PUBLIC_CERTIFICATE_ARN);

      if(awsRegion_ != null)
        awsClientRegion_ = awsRegion_;
      
      GetCallerIdentityResult callerIdentity = sts_.getCallerIdentity(new GetCallerIdentityRequest());
      
      log_.info("Connected as user " + callerIdentity.getArn());
      
      String actualAccountId = callerIdentity.getAccount();
      
      if(!actualAccountId.equals(awsAccountId_))
      {
        throw new IllegalStateException("AWS Account ID is " + awsAccountId_ + " but our credentials are for account " + actualAccountId);
      }

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
      
      lambdaClient_ =
          AWSLambdaClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      apiClient_ = 
          AmazonApiGatewayClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      amazonDynamoDB_ = AmazonDynamoDBClientBuilder.standard()
          .withRegion(awsClientRegion_)
          .build();
      
      dynamoDB_               = new DynamoDB(amazonDynamoDB_);
      
      dynamoStreamsClient_ = AmazonDynamoDBStreamsClientBuilder.standard()
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
    
    Map<String, String> tags = new HashMap<>(getTags());
    
    tags.put(Fugue.TAG_FUGUE_ITEM, name);

    S3Helper.createBucketIfNecessary(s3, name, tags, false);
  }
  
  
  private void createUser(Name name, @Nullable String groupName, List<String> keys, List<Name> nonKeyUsers)
  {
    String  userName      = name.append(USER).toString();
    
    try
    {
      iam_.getUser(new GetUserRequest()
        .withUserName(userName))
        .getUser();
      
      checkAccessKey(name, userName, keys, nonKeyUsers, false);
      
      if(groupName == null)
      {
        return;
      }
      else
      {
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
    }
    catch(NoSuchEntityException e)
    {
      log_.info("User \"" + userName + "\" does not exist, creating...");
      
      iam_.createUser(new CreateUserRequest()
          .withUserName(userName)).getUser();
      
      log_.debug("Created user \"" + userName + "\"");
      
      createAccessKey(name, userName, keys);
    }
    
    if(groupName != null)
    {
      log_.debug("Adding user \"" + userName + "\" to group \"" + groupName + "\"");
      
      iam_.addUserToGroup(new AddUserToGroupRequest()
          .withUserName(userName)
          .withGroupName(groupName));
    }
  }

  private void createAccessKey(Name name, String userName, List<String> keys)
  {
    AccessKey accessKey = iam_.createAccessKey(new CreateAccessKeyRequest()
        .withUserName(userName)).getAccessKey();
      
//      secret.println("#######################################################");
//      secret.println("# SAVE THIS ACCESS KEY IN ~/.aws/credentials");
//      secret.println("#######################################################");
//      secret.format("[%s]%n", userName);
//      secret.format("aws_access_key_id = %s%n", accessKey.getAccessKeyId());
//      secret.format("aws_secret_access_key = %s%n", accessKey.getSecretAccessKey());
//      secret.println("#######################################################");
      


    String accessKeyJson = "  \"" + name + "\": {\n" +
      "    \"accessKeyId\": \"" + accessKey.getAccessKeyId() + "\",\n" +
      "    \"secretAccessKey\": \"" + accessKey.getSecretAccessKey() + "\"\n" +
      "  }";
    
    keys.add(accessKeyJson);
  }

  private void checkAccessKey(Name name, String userName, List<String> keys, List<Name> nonKeyUsers, boolean force)
  {
    AccessKeyMetadata       latestKey = null;
    List<AccessKeyMetadata> oldKeys = new ArrayList<>();
    ListAccessKeysResult    accessKeys = iam_.listAccessKeys(new ListAccessKeysRequest()
        .withUserName(userName)
        );
    
    for(AccessKeyMetadata meta : accessKeys.getAccessKeyMetadata())
    {
      if(latestKey == null)
      {
        latestKey = meta;
      }
      else if(latestKey.getCreateDate().before(meta.getCreateDate()))
      {
        oldKeys.add(latestKey);
        latestKey = meta;
      }
      else
      {
        oldKeys.add(meta);
      }
    }
    
    if(latestKey == null)
    {
      log_.info("We have no access keys");

      createAccessKey(name, userName, keys);
    }
    else
    {
      int age = (int)Math.ceil((System.currentTimeMillis() - latestKey.getCreateDate().getTime()) / (1000.0*60*60*24));
      
      log_.info("We have " + oldKeys.size() + " older access keys and the latest key is " + age + " days old");
      
      if(force || age > 30)
      {
        log_.info("Rolling access keys...");
        
        for(AccessKeyMetadata meta : oldKeys)
        {
          log_.info("Deleting access key " + meta.getAccessKeyId() + " created on " + meta.getCreateDate());
          
          try
          {
            iam_.deleteAccessKey(new DeleteAccessKeyRequest()
              .withAccessKeyId(meta.getAccessKeyId())
              .withUserName(userName)
              );
          }
          catch(RuntimeException e)
          {
            log_.error("Failed to delete access key " + meta.getAccessKeyId() + " created on " + meta.getCreateDate(), e);
          }
        }
        
        createAccessKey(name, userName, keys);
      }
      else
      {
        nonKeyUsers.add(name);
      }
    }
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
      
      log_.debug("Created role " + roleName + ", waiting to allow role to become active...");
      
      try
      {
        Thread.sleep(10000);
      }
      catch (InterruptedException e1)
      {
        log_.warn("Interrupted", e1);
      }
      log_.debug("Created role " + roleName + ", waiting to allow role to become active...OK carry on.");
    }
   
    for(String policyArn : policyArnList)
    {
      iam_.attachRolePolicy(new AttachRolePolicyRequest()
        .withPolicyArn(policyArn)
        .withRoleName(roleName));
    }
    
    return role.getArn();
  }
  
  

  private void deletePolicy(Name name)
  {
    String policyName       = name.append(POLICY).toString();
    String policyArn        = getPolicyArn(policyName);
    
    try
    {
      iam_.getPolicy(new GetPolicyRequest().withPolicyArn(policyArn));
      
      ListPolicyVersionsResult versions = iam_.listPolicyVersions(new ListPolicyVersionsRequest()
          .withPolicyArn(policyArn));
      
      for(PolicyVersion version : versions.getVersions())
      {
        if(version.getIsDefaultVersion())
        {
          log_.debug("Found existing default policy version " + version.getVersionId());
        }
        else
        {
          iam_.deletePolicyVersion(new DeletePolicyVersionRequest()
              .withPolicyArn(policyArn)
              .withVersionId(version.getVersionId()));
          
          log_.debug("Deleting policy version " + version.getVersionId());
        }
      }
      
      iam_.deletePolicy(new DeletePolicyRequest()
        .withPolicyArn(policyArn));
      
      log_.info("Deleted policy " + policyArn);
    }
    catch(NoSuchEntityException e)
    {
      log_.info("Policy " + policyArn + " does not exist.");
    }
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


  protected String getEnvironmentPrefix()
  {
    return isPrimaryEnvironment() ? "" : getEnvironment() + "-";
  }
  
  protected String getStageName()
  {
    return "master";
  }

  protected class AwsDeploymentContext extends DeploymentContext
  {
    private static final String API_GATEWAY_PATH = "(none)";

    private static final String ANY = "ANY";

    private static final String NONE = "NONE";

    private LoadBalancer loadBalancer_;
  
    // ARN and ID of the actual API gateway
    private String apiGatewayArn_;
    private String apiGatewayId_;
    
    // For master environments the public domain name mapping
    private String apiGatewayMasterDomainName_;
    private String apiGatewayMasterTargetDomain_;

    // For all environments the private domain name mapping
    private String apiGatewayPrivateDomainName_;
    private String apiGatewayPrivateTargetDomain_;
    
    private boolean deploy_;

    private boolean createStage_;

    
    protected AwsDeploymentContext(String podName, INameFactory nameFactory)
    {
      super(podName, nameFactory);
    }

    @Override
    protected Subscription newQueueSubscription(JsonObject<?> sn)
    {
      return new AwsTopicSubscription(sn, getNameFactory(), lambdaClient_,
          String.format("arn:aws:sqs:%s:%s:",
              awsRegion_,
              awsAccountId_)
          );
    }

    @Override
    protected Subscription newDbSubscription(JsonObject<?> sn)
    {
      return new AwsDbSubscription(sn, getNameFactory(), lambdaClient_, dynamoStreamsClient_, amazonDynamoDB_);
    }

    @Override
    protected void putFugueConfig(Map<String, String> environment)
    {
      String storedConfig = "https://s3." + getAwsRegion() + ".amazonaws.com/sym-s2-fugue-" + getNameFactory().getEnvironmentType() +
          "-" + getAwsRegion() + "-config/config/" +
          getNameFactory().getPhysicalServiceName() + ".json";
      
      if(configValue_ == null)
      {
        environment.put(FUGUE_CONFIG, storedConfig);
      }
      else if(configValue_.length() < 3000)
      {
        environment.put(FUGUE_CONFIG, configValue_);
        environment.put(FUGUE_STORED_CONFIG, storedConfig);
      }
      else
      {
        environment.put(FUGUE_CONFIG, storedConfig);
      }
    }

    @Override
    protected void createEnvironment()
    {
      List<String>  keys      = new LinkedList<>();
      Name          baseName  = getNameFactory().getName();
      
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
      
      createRole(name, TRUST_ECS_DOCUMENT, policyArn);
    }

    @Override
    protected void createEnvironmentType()
    {
      
      Name baseName = getNameFactory().getFugueName(); 
          
          //FUGUE_PREFIX + getEnvironmentType();
      List<String>  keys = new LinkedList<>();
      List<Name>    nonKeyUsers = new LinkedList<>();

      createEnvironmentTypeRootUser(baseName, keys, nonKeyUsers);
      createEnvironmentTypeAdminUser(baseName, keys, nonKeyUsers);
      createEnvironmentTypeCicdUser(baseName, keys, nonKeyUsers);
      createEnvironmentTypeSupportUser(baseName, keys, nonKeyUsers);

      if(keys.isEmpty())
      {
        log_.info("No key created, secret unchanged.");
      }
      else
      {
        for(Name name : nonKeyUsers)
        {
          String  userName      = name.append(USER).toString();
          
          checkAccessKey(name, userName, keys, null, true);
        }
        
        CredentialName  name    = getNameFactory().getFugueCredentialName("root");
      
        updateSecret(name, keys);
      }
      
      for(String region : environmentTypeRegions_)
      {
        createBucketIfNecessary(region, environmentTypeConfigBuckets_.get(region));
      }
    }
    
    private void createAssumedRole(Name name, String templateName)
    {
      String policyArn        = createPolicyFromResource(name, "policy/" + templateName + ".json");
      String assumeRolePolicy = loadTemplateFromResource("policy/" + templateName + "Trust.json");
      
      createRole(name, assumeRolePolicy, policyArn);
    }

    private void createEnvironmentTypeAdminUser(Name baseName, List<String> keys, List<Name> nonKeyUsers)
    {
      Name name = baseName.append(ADMIN);
      
      String policyArn = createPolicyFromResource(name, "policy/environmentTypeAdmin.json");
      
      createRole(name, TRUST_ECS_DOCUMENT, policyArn);
    }
    
    private void createEnvironmentTypeSupportUser(Name baseName, List<String> keys, List<Name> nonKeyUsers)
    {
      Name name = baseName.append(SUPPORT);
      
      String infraPolicyArn = createPolicyFromResource(baseName.append("infra-list-all"), "policy/environmentTypeInfraListAll.json");
      String appPolicyArn = createPolicyFromResource(baseName.append("app-list-all"), "policy/environmentTypeAppListAll.json");
      String fuguePolicyArn = createPolicyFromResource(name, "policy/environmentTypeSupport.json");
      

      String assumeRolePolicy = loadTemplateFromResource("policy/environmentTypeSupportTrust.json");
      
      createRole(name, assumeRolePolicy, infraPolicyArn, appPolicyArn, fuguePolicyArn);
    }
    
    private void createEnvironmentTypeRootUser(Name baseName, List<String> keys, List<Name> nonKeyUsers)
    {
      Name name = baseName.append(ROOT);
      
      createUser(name, null, keys, nonKeyUsers);
    }
    
    private void createEnvironmentTypeCicdUser(Name baseName, List<String> keys, List<Name> nonKeyUsers)
    {
      Name name = baseName.append(CICD);
      
      String policyArn = createPolicyFromResource(name, "policy/environmentTypeCicd.json");
      String groupName = createGroup(name, policyArn);
      createUser(name, groupName, keys, nonKeyUsers);
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
      Name name = getNameFactory().getLogicalServiceItemName(roleName);
      
      String policyArn = createPolicy(name, roleSpec);
      
      createRole(name, trustSpec==null ? 
          TRUST_ECS_DOCUMENT : 
            trustSpec, 
            policyArn);
      
      if(getNameFactory().getPodId() != null)
      {
        // delete obsolete name
        deleteRole(getNameFactory().getPhysicalServiceItemName(roleName));
      }
    }

    @Override
    protected void deleteRole(String roleName)
    {
      if(getNameFactory().getPodId() != null)
      {
        deleteRole(getNameFactory().getPhysicalServiceItemName(roleName));
      }
      
      deleteRole(getNameFactory().getLogicalServiceItemName(roleName));
    }
        
    private void deleteRole(Name name)
    {
      String roleName       = name.append(ROLE).toString();
      
      try
      {
        iam_.getRole(new GetRoleRequest()
          .withRoleName(roleName))
          .getRole();
        
        ListAttachedRolePoliciesResult list = iam_.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest()
            .withRoleName(roleName));
        
        for(AttachedPolicy policy : list.getAttachedPolicies())
        {
          iam_.detachRolePolicy(new DetachRolePolicyRequest()
              .withRoleName(roleName)
              .withPolicyArn(policy.getPolicyArn()));
        }
        
        DeleteRoleRequest request = new DeleteRoleRequest()
            .withRoleName(roleName);
        
        iam_.deleteRole(request
            );
        
        log_.debug("Deleted role " + roleName );
      }
      catch(NoSuchEntityException e)
      {
        log_.info("Role " + roleName + " does not exist.");
      }
    }

    @Override
    protected void saveConfig()
    {
      String              name        = getNameFactory().getPhysicalServiceName().toString();
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

    @Override
    protected void deleteConfig()
    {
      String              name        = getNameFactory().getPhysicalServiceName().toString();
      String              bucketName  = environmentTypeConfigBuckets_.get(getAwsRegion());
      String              key         = CONFIG + "/" + name + DOT_JSON;
      
      
      log_.info("Deleting config from region: " + getAwsRegion() + " bucket: " + bucketName + " key: " + key);
      
      AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
          .withRegion(getAwsRegion())
          .build();
    
      try
      {
        s3Client.deleteObject(bucketName, key);
      }
      catch(AmazonS3Exception e)
      {
        log_.error("Failed to delete config", e);
      }
    }
    
    private String createPolicyFromResource(Name name, String fileName)
    {
      return createPolicy(name, loadTemplateFromResource(fileName));
    }
    
    @Override
    protected void configureServiceNetwork()
    {
      String hostName;

      if(isPrimaryEnvironment())
      {
        if(getPodName() == null)
        {
          hostName = getServiceHostName(null);
        }
        else
        {
          String  podId        = "" + getConfig().getRequiredObject("id").getRequiredInteger("podId"); 

          hostName = getServiceHostName(podId);
        }
      }
      else
      {
        hostName = null;
      }
      
      createR53RecordSet(hostName);
      
      getOrCreateCluster();
    }

    @Override
    protected void deployServiceContainer(String name, int port, Collection<String> paths, String healthCheckPath,
        int instances, Name roleName, String imageName, int jvmHeap, int memory, boolean deleted)
    {
      if(!deleted && action_.isDeploy_)
      {
        deleteTaskDefinitions(name, 5);
        
        getOrCreateCluster();
        
  //      registerTaskDef(name, port, healthCheckPath, podId);
        
        createService(name, port, instances, paths, roleName, imageName, jvmHeap, memory);
      }
      else
      {
        deleteService(name);
      }
    }
    
    
    @Override
    protected void deployScheduledTaskContainer(String name, int port, Collection<String> paths, String schedule, Name roleName, String imageName, int jvmHeap, int memory, boolean deleted)
    {
      Name serviceName  = getNameFactory().getPhysicalServiceItemName(name);
      Name baseName     = serviceName.append("schedule");
      Name ruleName     = baseName.append("rule");

      if(!deleted && action_.isDeploy_)
      { 
        deleteTaskDefinitions(name, 5);
        
        registerTaskDefinition(serviceName, port, roleName, imageName, jvmHeap, memory);
        
        
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
      if(deleted || action_.isUndeploy_)
      {
        // undeploy
        try
        {
          ListTargetsByRuleResult listResponse = cweClient_.listTargetsByRule(new ListTargetsByRuleRequest()
              .withRule(ruleName.toString()));
          
          RemoveTargetsRequest  removeTargetsRequest  = new RemoveTargetsRequest().withRule(ruleName.toString());
          Set<String>           targetArns            = new HashSet<>();
          
          for(Target target : listResponse.getTargets())
          { 
  
            removeTargetsRequest.withIds(target.getId());
            
            targetArns.add(target.getEcsParameters().getTaskDefinitionArn());
          }
          
          deleteTaskDefinitions(targetArns, 0);
          
          
          cweClient_.removeTargets(removeTargetsRequest);
          
          String n = ruleName.toString();
          
          cweClient_.deleteRule(new com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest()
              .withName(ruleName.toString())
              );
        }
        catch(com.amazonaws.services.cloudwatchevents.model.ResourceNotFoundException e)
        {
          log_.info("Rule " + ruleName + " does not exist.");
        }
        
        
        deleteRole(baseName);
        deletePolicy(baseName);
      }
    }

    private synchronized void deleteTaskDefinitions(Iterable<String> targetArns, int remaining)
    {
      try
      {
        Set<String> taskDefFamilies = new HashSet<>();
        
        for(String targetArn : targetArns)
        {
          taskDefFamilies.add(stripAfter(stripBefore(targetArn, '/'), ':'));
        }
  
        for(String taskDefFamily : taskDefFamilies)
        {
          ListTaskDefinitionsResult list = ecsClient_.listTaskDefinitions(new ListTaskDefinitionsRequest()
              .withFamilyPrefix(taskDefFamily));
          
          int limit = (list.getTaskDefinitionArns().size() - remaining);
          
          if(action_.isDeploy_ && limit > 10)
            limit = 10;
          
          for(int i=0 ; i< limit ; i++)
          {
            String taskDefArn = list.getTaskDefinitionArns().get(i);
            
            log_.info("Deregistering task definition " + taskDefArn + "...");
            ecsClient_.deregisterTaskDefinition(new DeregisterTaskDefinitionRequest()
                .withTaskDefinition(taskDefArn));
            
            Thread.sleep(action_.isDeploy_ ? 1000 : 100);
          }
        }
      }
      catch(InterruptedException | AmazonECSException e)
      {
        log_.warn("Failed to delete task definition", e);
      }
    }

    @Override
    protected void postDeployLambdaContainer(String name, Collection<String> paths, Collection<Subscription> subscriptions)
    {
      String  functionName  = getNameFactory().getLogicalServiceItemName(name).toString();

      log_.info("postDeployLambdaContainer(" + name + ", " + paths + ");");
      
      if(action_.isDeploy_)
      {
        if(!paths.isEmpty())
          createApiGatewayPaths(getFunctionInvocationArn(functionName), paths);
        
        if(!subscriptions.isEmpty())
          createLambdaSubscriptions(functionName, subscriptions);
      }
    }

    private void createLambdaSubscriptions(String functionName, Collection<Subscription> subscriptions)
    {
      for(Subscription subscription : subscriptions)
      {
        subscription.create(functionName);
      }
    }

    @Override
    protected void deployLambdaContainer(String name, String imageName, 
        String roleId, String handler, int memorySize, int timeout, 
        int provisionedConcurrentExecutions, Map<String, String> variables, Collection<String> paths)
    {
      String  functionName  = getNameFactory().getLogicalServiceItemName(name).toString();
      Name    roleName      = getNameFactory().getLogicalServiceItemName(roleId).append(ROLE);
      String  bucketName    = environmentTypeConfigBuckets_.get(getAwsRegion());
      String  key           = LAMBDA + "/" + getNameFactory().getServiceId() + "/" +
                              imageName + "-" + getBuildId() + DOT_JAR;

      if(action_.isDeploy_)
      {
        String codeSha256;
        String revisionId;
        
        try
        {
          GetFunctionResult function = lambdaClient_.getFunction(new GetFunctionRequest()
              .withFunctionName(functionName)
              );
          
          log_.info("Lambda function " + functionName + " already exists, updating...");
          UpdateFunctionCodeResult updateCodeResult = lambdaClient_.updateFunctionCode(new UpdateFunctionCodeRequest()
              .withFunctionName(functionName)
              .withS3Bucket(bucketName)
              .withS3Key(key)
              );
          
          codeSha256 = updateCodeResult.getCodeSha256();
          
          UpdateFunctionConfigurationResult updateConfigResult = lambdaClient_.updateFunctionConfiguration(new UpdateFunctionConfigurationRequest()
              .withFunctionName(functionName)
              .withHandler(handler)
              .withMemorySize(memorySize)
              .withTimeout(timeout)
              .withEnvironment(new Environment()
                  .withVariables(variables))
              .withRole(getRoleArn(roleName))
              .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
              );
          
          revisionId = updateConfigResult.getRevisionId();
          
          lambdaClient_.tagResource(new com.amazonaws.services.lambda.model.TagResourceRequest()
              .withResource(function.getConfiguration().getFunctionArn())
              .withTags(getTags())
              );
        }
        catch(ResourceNotFoundException e)
        {
          log_.info("Lambda function " + functionName + " does not exist, creating...");
          
          CreateFunctionResult createFunctionResult = lambdaClient_.createFunction(new CreateFunctionRequest()
              .withFunctionName(functionName)
              .withHandler(handler)
              .withMemorySize(memorySize)
              .withTimeout(timeout)
              .withEnvironment(new Environment()
                  .withVariables(variables))
              .withRole(getRoleArn(roleName))
              .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
              .withTags(getTags())
              .withCode(new FunctionCode()
                  .withS3Bucket(bucketName)
                  .withS3Key(key)
                  )
              );
          
          codeSha256 = createFunctionResult.getCodeSha256();
          revisionId = createFunctionResult.getRevisionId();
        }
        
        ListVersionsByFunctionResult listVersionsResult = lambdaClient_.listVersionsByFunction(new ListVersionsByFunctionRequest()
            .withFunctionName(functionName));
        
        PublishVersionResult publishVersionResult = lambdaClient_.publishVersion(new PublishVersionRequest()
            .withCodeSha256(codeSha256)
            .withRevisionId(revisionId)
            .withFunctionName(functionName)
            );
        
        try
        {
          GetAliasResult getAliasResult = lambdaClient_.getAlias(new GetAliasRequest()
              .withFunctionName(functionName)
              .withName(LAMBDA_ALIAS_NAME)
              );

          log_.info("Lambda function " + functionName + " alias " + LAMBDA_ALIAS_NAME + " exists to revision " + getAliasResult.getRevisionId() + ", updating...");


          UpdateAliasResult updateAliasResult = lambdaClient_.updateAlias(new UpdateAliasRequest()
              .withFunctionName(functionName)
              .withFunctionVersion(publishVersionResult.getVersion())
              .withName(LAMBDA_ALIAS_NAME)
              );
        }
        catch(ResourceNotFoundException e)
        {

          log_.info("Lambda function " + functionName + " alias " + LAMBDA_ALIAS_NAME + " does not exist, creating...");
          

          CreateAliasResult createAliasResult = lambdaClient_.createAlias(new CreateAliasRequest()
              .withFunctionName(functionName)
              .withFunctionVersion(publishVersionResult.getVersion())
              .withName(LAMBDA_ALIAS_NAME)
              );
        }

        int versionLimit = Integer.parseInt(publishVersionResult.getVersion()) - 1;
        
        
        for(FunctionConfiguration version : listVersionsResult.getVersions())
        {
          try
          {
            int v = Integer.parseInt(version.getVersion());
            
            if(v < versionLimit)
            {
              log_.info("Deleting old version " + functionName + ":" + v + " (revision " + version.getRevisionId() + ")...");
              
              lambdaClient_.deleteFunction(new DeleteFunctionRequest()
                  .withFunctionName(functionName)
                  .withQualifier(version.getVersion())
                  );
            }
          }
          catch(NumberFormatException e)
          {
            // This is $LATEST
            log_.info("Leaving version " + functionName + ":" + version.getVersion() + " (revision " + version.getRevisionId() + ")");
          }
        }
                
        try
        {
          GetProvisionedConcurrencyConfigResult getProvisionedConcurrencyConfigResult = lambdaClient_.getProvisionedConcurrencyConfig(new GetProvisionedConcurrencyConfigRequest()
              .withFunctionName(functionName)
              .withQualifier(LAMBDA_ALIAS_NAME)
              );
          
          if(provisionedConcurrentExecutions <= 0)
          {
            log_.info("Lambda function " + functionName + " provisioned capacity is set to " + getProvisionedConcurrencyConfigResult.getRequestedProvisionedConcurrentExecutions() + ", deleting...");
            
            lambdaClient_.deleteProvisionedConcurrencyConfig(new DeleteProvisionedConcurrencyConfigRequest()
                .withFunctionName(functionName)
                .withQualifier(LAMBDA_ALIAS_NAME)
                );
          }
          else
          {
            if(getProvisionedConcurrencyConfigResult.getRequestedProvisionedConcurrentExecutions().equals(provisionedConcurrentExecutions))
            {
              log_.info("Lambda function " + functionName + " provisioned capacity is set to " + getProvisionedConcurrencyConfigResult.getRequestedProvisionedConcurrentExecutions());
            }
            else
            {
              log_.info("Lambda function " + functionName + " provisioned capacity is set to " + getProvisionedConcurrencyConfigResult.getRequestedProvisionedConcurrentExecutions() + ", updating...");
              
  
              PutProvisionedConcurrencyConfigResult putProvisionedConcurrencyConfigResult = lambdaClient_.putProvisionedConcurrencyConfig(new PutProvisionedConcurrencyConfigRequest()
                  .withFunctionName(functionName)
                  .withProvisionedConcurrentExecutions(provisionedConcurrentExecutions)
                  .withQualifier(LAMBDA_ALIAS_NAME)
                  );
              
              log_.info("Lambda function " + functionName + " provisioned capacity has been set to " + putProvisionedConcurrencyConfigResult.getRequestedProvisionedConcurrentExecutions());
            }
          }       
        }
        catch(ProvisionedConcurrencyConfigNotFoundException e)
        {
          if(provisionedConcurrentExecutions > 0)
          {
            log_.info("Lambda function " + functionName + " provisioned capacity does not exist, creating...");
            
            PutProvisionedConcurrencyConfigResult putProvisionedConcurrencyConfigResult = lambdaClient_.putProvisionedConcurrencyConfig(new PutProvisionedConcurrencyConfigRequest()
                .withFunctionName(functionName)
                .withProvisionedConcurrentExecutions(provisionedConcurrentExecutions)
                .withQualifier(LAMBDA_ALIAS_NAME)
                );
  
            log_.info("Lambda function " + functionName + " provisioned capacity has been set to " + putProvisionedConcurrencyConfigResult.getRequestedProvisionedConcurrentExecutions());
          }
        }
        
        if(!paths.isEmpty())
        {
          setLambdaApiGatewayPolicy(functionName);
        }
      }
      if(action_.isUndeploy_)
      {
        try
        {
          lambdaClient_.getFunction(new GetFunctionRequest()
              .withFunctionName(functionName)
              );
          
          log_.info("Deleting lambda function " + functionName + "...");
          lambdaClient_.deleteFunction(new DeleteFunctionRequest()
              .withFunctionName(functionName));
        }
        catch(ResourceNotFoundException e)
        {
          log_.info("Lambda function " + functionName + " does not exist.");
        }
      }
      if(!name.equals(imageName))
      {
        // delete the obsolete function named by the image name if it exists.
        deleteObsoleteFunction(getNameFactory().getPhysicalServiceItemName(imageName).toString());
      }
      
      if(getNameFactory().getPodId() != null)
      {
        deleteObsoleteFunction(getNameFactory().getPhysicalServiceItemName(name).toString());
      }
    }

    private void setLambdaApiGatewayPolicy(String functionName)
    {
      String apiGatewaySourceArn_ = String.format("arn:aws:execute-api:%s:%s:%s/*",
          awsRegion_,
          awsAccountId_,
          apiGatewayId_);

      try
      {
        RemovePermissionResult removeResult = lambdaClient_.removePermission(new RemovePermissionRequest()
            .withFunctionName(functionName)
            .withStatementId("apiGatewayInvoke")
            );
        
        log_.info("Removed existing lambda permission " + removeResult);
      }
      catch(com.amazonaws.services.lambda.model.ResourceNotFoundException e)
      {
        // does not exist anyway which is fine.
      }
      AddPermissionResult permissionResult = lambdaClient_.addPermission(new AddPermissionRequest()
          .withFunctionName(functionName)
          .withAction("lambda:InvokeFunction")
          .withPrincipal("apigateway.amazonaws.com")
          .withStatementId("apiGatewayInvoke")
          .withSourceArn(apiGatewaySourceArn_)
          );
    
      log_.info("Added lambda permission " + permissionResult);      
    }

    private void createApiGatewayPaths(String functionInvokeArn, Collection<String> paths)
    {
      Map<String, String> pathIdMap = new HashMap<>();
      Set<String> remainingPaths = new HashSet<>();
      Set<String> remainingMethods = new HashSet<>();
      
      for(String path : paths)
      {
        remainingPaths.add(path.replace("*", "{proxy+}"));
      }
      
      String rootResourceId = null;
      

      GetResourcesResult resources = apiClient_.getResources(new GetResourcesRequest()
        .withRestApiId(apiGatewayId_)
        );
      
      for(Resource resource : resources.getItems())
      {
        pathIdMap.put(resource.getPath(), resource.getId());
        
        if(resource.getPath().equals("/"))
          rootResourceId = resource.getId();
        
        if(remainingPaths.remove(resource.getPath()))
        {
          log_.info("Resource " + resource.getPath() + " exists.");
        
          // Are they correct?
          
          
          
          if(resource.getResourceMethods() == null)
          {
            log_.info("Resource " + resource.getPath() + " has no methods, will add...");
            remainingMethods.add(resource.getId());
          }
          else
          {
            for(String methodName : resource.getResourceMethods().keySet())
            {
              log_.info("Existing method " + methodName);
              
              if(ANY.equals(methodName))
              {
                GetMethodResult method = apiClient_.getMethod(new GetMethodRequest()
                  .withHttpMethod(ANY)
                  .withResourceId(resource.getId())
                  .withRestApiId(apiGatewayId_)
                  );
                
                if(NONE.equals(method.getAuthorizationType())
                    && IntegrationType.AWS_PROXY.toString().equals(method.getMethodIntegration().getType())
                    && functionInvokeArn.equals(method.getMethodIntegration().getUri())
                    )
                {
                  log_.info("Existing method is good.");
                }
                else
                {
                  log_.info("Existing method needs to be updated.");
                  deleteMethod(method.getHttpMethod(), resource.getId());
                  remainingMethods.add(resource.getId());
                }
              }
              else
              {
                deleteMethod(methodName, resource.getId());
              }
            }
          }
        }
      }
      
      if(rootResourceId == null)
        throw new IllegalStateException("Unable to locste root resource for api gateway " + apiGatewayId_);
      
      for(String path : remainingPaths)
      {
        log_.info("Creating path " + path);
        
        createApiGatewayPath(pathIdMap, functionInvokeArn, rootResourceId, path.split("/"), 1, "");
        deploy_=true;
      }
      
      for(String resourceId : remainingMethods)
      {
        log_.info("Creating method for resource " + resourceId);
        
        createMethod(functionInvokeArn, resourceId);
        deploy_=true;
      }
      
      try
      {
        GetStageResult stage = apiClient_.getStage(new GetStageRequest()
          .withRestApiId(apiGatewayId_)
          .withStageName(getStageName())
          );
        
        log_.info("Stage exists as " + stage);
      }
      catch(com.amazonaws.services.apigateway.model.NotFoundException e)
      {
        log_.info("Stage does not exist.");
        createStage_ = true;
        deploy_=true;
      }
      
      
    }

    @Override
    protected void postDeployContainers()
    {
      log_.info("postDeployContainers deploy_ = " + deploy_ + ":" + apiGatewayId_ + ", createStage_ = " + createStage_ + ":" + getStageName());
      if(deploy_)
      {
        checkStage();
        CreateDeploymentResult deployment = apiClient_.createDeployment(new CreateDeploymentRequest()
            .withRestApiId(apiGatewayId_)
            .withStageName(getStageName())
            );
        checkStage();
        if(!createStage_)
        {
          UpdateStageResult stage = apiClient_.updateStage(new UpdateStageRequest()
              .withRestApiId(apiGatewayId_)
              .withStageName(getStageName())
              .withPatchOperations(new PatchOperation()
                  .withOp(Op.Replace)
                  .withPath("/deploymentId")
                  .withValue(deployment.getId())
                  )
              );
          
          log_.info("Updated stage " + stage);
        }
      }
      createApiGatewayBasePath();
    }

    private void checkStage()
    {
      log_.info("Check Stage " + getStageName());
      try
      {
        GetStageResult stage = apiClient_.getStage(new GetStageRequest()
          .withRestApiId(apiGatewayId_)
          .withStageName(getStageName())
          );
        
        log_.info("Stage exists as " + stage);
      }
      catch(com.amazonaws.services.apigateway.model.NotFoundException e)
      {
        log_.info("Stage does not exist.");
        createStage_ = true;
        deploy_=true;
      }
    }

    private void deleteMethod(String httpMethod, String resourceId)
    {
      log_.info("Deleting method: " + httpMethod);
      
      try
      {
        apiClient_.deleteMethod(new DeleteMethodRequest()
            .withHttpMethod(httpMethod)
            .withResourceId(resourceId)
            .withRestApiId(apiGatewayId_)
            );
      }
      catch(RuntimeException e)
      {
        log_.warn("Failed to delete method", e);
      }
    }

    private void createApiGatewayPath(Map<String, String> pathIdMap, String functionInvokeArn, String parentResourceId, String[] pathElements, int cnt, String currentPath)
    {
      currentPath = currentPath + "/" + pathElements[cnt];
      
      String resourceId = pathIdMap.get(currentPath);
      
      if(resourceId == null)
      {
        CreateResourceResult resource = apiClient_.createResource(new CreateResourceRequest()
            .withRestApiId(apiGatewayId_)
            .withParentId(parentResourceId)
            .withPathPart(pathElements[cnt])
            .withRestApiId(apiGatewayId_)
            );
        
        resourceId = resource.getId();
        pathIdMap.put(currentPath, resourceId);
      }
      
      if(pathElements.length > ++cnt)
      {
        createApiGatewayPath(pathIdMap, functionInvokeArn, resourceId, pathElements, cnt, currentPath);
      }
      else
      {
        createMethod(functionInvokeArn, resourceId);
      }
    }

    private void createMethod(String functionInvokeArn, String resourceId)
    {
      PutMethodResult method = apiClient_.putMethod(new PutMethodRequest()
          .withHttpMethod(ANY)
          .withResourceId(resourceId)
          .withRestApiId(apiGatewayId_)
          .withAuthorizationType(NONE)
          );
      
      PutIntegrationResult integration = apiClient_.putIntegration(new PutIntegrationRequest()
          .withResourceId(resourceId)
          .withRestApiId(apiGatewayId_)
          .withHttpMethod(ANY)
          .withUri(functionInvokeArn)
          .withIntegrationHttpMethod("POST")
          .withType(IntegrationType.AWS_PROXY)
          .withConnectionType(ConnectionType.INTERNET)
          );
      
      log_.info("Created method " + method.getHttpMethod() + " with integration " + integration.getType() + " to " + integration.getUri());
    }

    private void createApiGatewayBasePath()
    {
      if(apiGatewayId_ != null)
      {
        if(apiGatewayMasterDomainName_ != null)
        {
          apiGatewayMasterTargetDomain_ = createApiGatewayBasePath(apiGatewayMasterDomainName_, awsPublicCertArn_);
          doCreateR53RecordSet(apiGatewayMasterDomainName_, apiGatewayMasterTargetDomain_, true, false, 
              apiGatewayMasterDomainName_.length() - getPublicDnsSuffix().length(), true);
        }
        
        apiGatewayPrivateTargetDomain_ = createApiGatewayBasePath(apiGatewayPrivateDomainName_, awsLoadBalancerCertArn_);
        doCreateR53RecordSet(apiGatewayPrivateDomainName_, apiGatewayPrivateTargetDomain_, true, false);
      }
    }

    private String createApiGatewayBasePath(String apiGatewayDomainName_, String apiGatewayCertArn_)
    {
      String apiGatewayTargetDomain_;
      
      try
      {
        GetDomainNameResult existingDomain = apiClient_.getDomainName(new GetDomainNameRequest()
            .withDomainName(apiGatewayDomainName_)
            );
        
        if(apiGatewayCertArn_.equals(existingDomain.getRegionalCertificateArn()) &&
            existingDomain.getSecurityPolicy().equals(SecurityPolicy.TLS_1_2.toString()))
        {
          apiGatewayTargetDomain_ = existingDomain.getRegionalDomainName();
          log_.info("API Gateway custom domain " + apiGatewayDomainName_ + " exists with correct certificate.");
        }
        else
        {
          log_.info("API Gateway custom domain " + apiGatewayDomainName_ + " exists, but the certificate is wrong.");
          
          apiClient_.updateDomainName(new UpdateDomainNameRequest()
              .withDomainName(apiGatewayDomainName_)
              .withPatchOperations(new PatchOperation()
                  .withOp(Op.Replace)
                  .withPath("/regionalCertificateArn")
                  .withValue(apiGatewayCertArn_)
                  ,
                  new PatchOperation()
                  .withOp(Op.Replace)
                  .withPath("/securityPolicy")
                  .withValue(SecurityPolicy.TLS_1_2.toString())
                  )
              );
          
          apiGatewayTargetDomain_ = existingDomain.getRegionalDomainName();
          log_.info("API Gateway custom domain " + apiGatewayDomainName_ + " certificate updated to " + apiGatewayCertArn_);
        }
      }
      catch(com.amazonaws.services.apigateway.model.NotFoundException e)
      {
        CreateDomainNameResult newDomain = apiClient_.createDomainName(new CreateDomainNameRequest()
            .withDomainName(apiGatewayDomainName_)
            .withRegionalCertificateArn(apiGatewayCertArn_)
            .withSecurityPolicy(SecurityPolicy.TLS_1_2)
            .withEndpointConfiguration(new EndpointConfiguration()
                .withTypes(EndpointType.REGIONAL)
                )
            );
        
        apiGatewayTargetDomain_ = newDomain.getRegionalDomainName();
        
        log_.info("Created API Gateway domain in hosted zone " + newDomain.getRegionalHostedZoneId());
      }

      // whats the ARN format for this?
//        try
//        {
//          apiClient_.tagResource(new TagResourceRequest()
//            .withResourceArn(getNoAccountArn("apigateway", "/domainname", apiGatewayDomainName_)));
//        }
//        catch(RuntimeException e)
//        {
//          log_.info("Failed to tag domain", e);
//        }
      try
      {
        GetBasePathMappingResult existsingPath = apiClient_.getBasePathMapping(new GetBasePathMappingRequest()
          .withDomainName(apiGatewayDomainName_)
          .withBasePath(API_GATEWAY_PATH)
          );
        
        if(existsingPath.getRestApiId().equals(apiGatewayId_) && getStageName().equals(existsingPath.getStage()))
        {
          log_.info("API Gateway path exists");
        }
        else
        {
          log_.info("API Gateway path exists, but to wrong API Gateway and/ stage");
          
          apiClient_.updateBasePathMapping(new UpdateBasePathMappingRequest()
              .withDomainName(apiGatewayDomainName_)
              .withBasePath(API_GATEWAY_PATH)
              .withPatchOperations(new PatchOperation()
                  .withOp(Op.Replace)
                  .withPath("/restapiId")
                  .withValue(apiGatewayId_)
                  ,
                  new PatchOperation()
                  .withOp(Op.Replace)
                  .withPath("/stage")
                  .withValue(getStageName())
                  )
              );
          
          log_.info("Updated API Gateway path exists to " + apiGatewayId_);
        }
      }
      catch(com.amazonaws.services.apigateway.model.NotFoundException e)
      {
        log_.info("Creating API Gateway path...");
        CreateBasePathMappingResult newPath = apiClient_.createBasePathMapping(new CreateBasePathMappingRequest()
            .withBasePath("")
            .withDomainName(apiGatewayDomainName_)
            .withRestApiId(apiGatewayId_)
            .withStage(getStageName())
            );
        
        log_.info("API Gateway path created to API " + newPath.getRestApiId());
      }
      
      return apiGatewayTargetDomain_;
    }

    private void deleteObsoleteFunction(String obsoleteFunctionName)
    {
      try
      {
        lambdaClient_.getFunction(new GetFunctionRequest()
            .withFunctionName(obsoleteFunctionName)
            );
        
        log_.info("Obsolete lambda function " + obsoleteFunctionName + " exists, deleting...");
        lambdaClient_.deleteFunction(new DeleteFunctionRequest()
            .withFunctionName(obsoleteFunctionName)
            );
      }
      catch(ResourceNotFoundException e)
      {
        log_.info("Obsolete lambda function " + obsoleteFunctionName + " does not exist.");
      }
    }

    private String getClusterArn()
    {
      return "arn:aws:ecs:" + awsRegion_ + ":" + awsAccountId_ + ":cluster/" + clusterName_;
    }

    private String getTaskDefinitionArn(String name)
    {
      return "arn:aws:ecs:" + awsRegion_ + ":" + awsAccountId_ + ":task-definition/" + name;
    }

    private String getRoleArn(Name name)
    {
      return "arn:aws:iam::" + awsAccountId_ + ":role/" + name;
    }

    private void createR53RecordSet(String host)
    {
      String regionalHostName = getNameFactory()
          .getRegionalServiceName().toString().toLowerCase() + "." + getDnsSuffix();
      
      if(host != null)
        doCreateR53RecordSet(host, regionalHostName, false, true);
      
      if(loadBalancer_ != null)
        doCreateR53RecordSet(regionalHostName, loadBalancer_.getDNSName(), false, false);
    }
    
    private void doCreateR53RecordSet(String source, String target, boolean create, boolean multiValue)
    {
      doCreateR53RecordSet(source, target, create, multiValue, source.indexOf('.') + 1, false);
    }
    
    private void doCreateR53RecordSet(String source, String target, boolean create, boolean multiValue, int pos, boolean doNotDelete)
    {
      String zoneId = createOrGetHostedZone(source.substring(pos), create);
      
      if(zoneId.startsWith("/hostedzone/"))
        zoneId = zoneId.substring(12);
      
      String sourceDomain = source + ".";
      String setIdentifier = multiValue ? getNameFactory().getRegionName().toString().toLowerCase() : null;
      
      
      ListResourceRecordSetsResult result = r53Clinet_.listResourceRecordSets(new ListResourceRecordSetsRequest()
          .withHostedZoneId(zoneId)
          .withStartRecordName(source)
          );
      
      List<ResourceRecordSet> recordSetList = result.getResourceRecordSets();
      boolean                 ok            = false;
      
      for(ResourceRecordSet recordSet : recordSetList)
      {
        if(sourceDomain.equals(recordSet.getName()) 
            && strEquals(recordSet.getSetIdentifier(), setIdentifier)
            )
        {
          log_.info("R53 record set exists for " + source);
          
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
      
      
      if(action_ == FugueDeployAction.Deploy)
      {
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
      else
      {
        if(ok && !doNotDelete)
        {
          log_.info("Deleting R53 record set for " + source + " to " + target + "...");
          
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
                        .withAction(ChangeAction.DELETE)
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
          log_.error("Failed to delete resource sets", savedException);
          throw savedException;
        }
        else
        {
          log_.info("R53 record set for " + source + " to " + target + " does not exist.");
          
        }
      }
    }

    private boolean strEquals(String a, String b)
    {
     if(a == null)
       return b == null;
     
      return a.equals(b);
    }

//    private String createTargetGroup(Name name, String healthCheckPath, int port)
//    {
//      String shortName = name.getShortName(32);
//      
//      try
//      {
//        DescribeTargetGroupsResult desc = elbClient_.describeTargetGroups(new DescribeTargetGroupsRequest().withNames(shortName));
//        
//        List<TargetGroup> groups = desc.getTargetGroups();
//        
//        if(groups.size() != 1)
//            throw new IllegalStateException("Describe target group by name returns " + groups.size() + " results!");
//        
//        log_.info("Target group " + name + " (" + shortName + ") already exists.");
//        return elbTag(groups.get(0).getTargetGroupArn());
//      }
//      catch(TargetGroupNotFoundException e)
//      {
//        log_.info("Target group " + name + " (" + shortName + ") does not exist, will create it...");
//      }
//      
//      CreateTargetGroupResult result = elbClient_.createTargetGroup(new CreateTargetGroupRequest()
//          .withName(shortName)
//          .withHealthCheckPath(healthCheckPath)
//          .withHealthCheckProtocol(ProtocolEnum.HTTP)
//          .withProtocol(ProtocolEnum.HTTP)
//          .withVpcId(awsVpcId_)
//          .withPort(port)
//          );
//      
//      return elbTag(result.getTargetGroups().get(0).getTargetGroupArn());
//    }
//
//
//    private String elbTag(String arn)
//    {
//      List<Tag> tags = new LinkedList<>();
//      
//      for(Entry<String, String> entry : getTags().entrySet())
//      {
//        tags.add(new Tag().withKey(entry.getKey()).withValue(entry.getValue()));
//      }
//      
//      tagIfNotNull(tags, "FUGUE_TENANT", getPodName());
//      
//      if(!tags.isEmpty())
//      {
//        elbClient_.addTags(new AddTagsRequest()
//            .withResourceArns(arn)
//            .withTags(tags)
//            );
//      }
//    
//      return arn;
//    }
    
    private void tagIfNotNull(List<Tag> tags, String name, String value)
    {
      if(value != null)
      {
        tags.add(new Tag().withKey(name).withValue(value));
      }
    }
    
    @Override
    protected void deployInitContainer(String name, int port, Collection<String> paths, String healthCheckPath, Name roleName, String imageName, int jvmHeap, int memory)
    {
      Name taskName = getNameFactory().getPhysicalServiceItemName(name);

      registerTaskDefinition(taskName, port, roleName, imageName, jvmHeap, memory);
      
      ContainerOverride containerOverrides = new ContainerOverride()
          .withName(taskName.toString())
          .withEnvironment(new KeyValuePair().withName("FUGUE_ACTION").withValue(String.valueOf(action_)))
          .withEnvironment(new KeyValuePair().withName("FUGUE_DRY_RUN").withValue(String.valueOf(dryRun_)))
          ;
      
      for(Entry<String, String> entry : getTags().entrySet())
        containerOverrides.withEnvironment(new KeyValuePair().withName("FUGUE_TAG_" + entry.getKey()).withValue(entry.getValue()));
      
      RunTaskResult run = ecsClient_.runTask(new RunTaskRequest()
          .withCluster(clusterName_)
          .withCount(1)
          .withTaskDefinition(taskName.toString())
          .withOverrides(new TaskOverride()
              .withTaskRoleArn(getRoleArn(roleName))
              .withContainerOverrides(containerOverrides))
          );
      
      String taskArn = run.getTasks().get(0).getTaskArn();
      
      waitTaskComplete(taskName, taskArn, TimeUnit.SECONDS, 600);
      
    }
    
    private void waitTaskComplete(Name taskName, String taskArn, TimeUnit timeUnit, long timeout)
    {
      long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
      String logGroupName = getNameFactory().getPhysicalServiceName().toString();
      String logStreamName = getService() + '/' + taskName + '/' + taskArn.substring(taskArn.lastIndexOf('/') + 1);
      String nextToken = null;
      
      log_.info("Waiting for task " + taskArn + " logGroup=" + logGroupName + " logStream=" + logStreamName + " ...");
      
      boolean notDone = true;
      boolean stopped = false;
      boolean taskFailed = false;
      int     logEvents  = 0;
      int     logWait  = 3;
      
      while(notDone)
      {
        notDone = !(stopped || System.currentTimeMillis() > deadline);
        
        DescribeTasksResult taskDesc = ecsClient_.describeTasks(new DescribeTasksRequest()
            .withTasks(taskArn)
            .withCluster(clusterName_)
            );
        
        if(taskDesc.getTasks().size() == 1)
        {
          Task myTask = taskDesc.getTasks().get(0);
          
          String status = myTask.getLastStatus();
          
          
          // read log
          try
          {
            GetLogEventsResult events = logsClient_.getLogEvents(new GetLogEventsRequest()
                .withLogGroupName(logGroupName)
                .withLogStreamName(logStreamName)
                .withNextToken(nextToken)
                );
            
            nextToken = events.getNextForwardToken();
            
            for(OutputLogEvent event : events.getEvents())
            {
              log_.info("| " + event.getMessage());
              notDone = true; // go around at least one more time to get more log data
              logEvents++;
            }
          }
          catch(com.amazonaws.services.logs.model.ResourceNotFoundException e)
          {
            // No logs yet...
          }
          
          if("STOPPED".equals(status))
          {
            stopped = true;
            
            for(Container container : myTask.getContainers())
            {
              if(container.getExitCode() == null)
              {
                taskFailed = true;
                
                log_.error("Container failed to launch: " + container);
              }
              else if(container.getExitCode() != 0)
              {
                taskFailed = true;
                
                log_.error("Container exited with an error code: " + container);
              }
            }
          }
        }
        else
        {
          log_.info("Got " + taskDesc.getTasks().size() + " tasks");
        }
        
        if(logEvents == 0 && logWait-- > 0)
        {
          notDone = true; // go around at least one more time to get more log data
        }
        
        if(notDone)
        {
          try
          {
            Thread.sleep(10000);
          }
          catch (InterruptedException e)
          {
            throw new IllegalStateException("Interrupted.", e);
          }
        }
      }
      
      if(taskFailed)
        throw new IllegalStateException("TASK FAILED");

      if(stopped)
        return;
      
      throw new IllegalStateException("Timed out.");
    }

    @Override
    protected void undeployInitContainer(String name, int port, Collection<String> paths, String healthCheckPath)
    {
      Name taskName = getNameFactory().getPhysicalServiceItemName(name);
      // TODO move from groovy land
      
      deleteTaskDef(taskName, port, paths, healthCheckPath);
    }

    @Override
    protected void deployService()
    {
      // createDnsZones();
      
      if(action_.isDeploy_)
      {
        getApiGateway(getPathCnt() > 0);
      }
      
      if(action_.isUndeploy_)
      {
        getApiGateway(false);
      }
    }

    @Override
    protected void undeployService()
    {
      // createDnsZones();
      
      if(hasDockerContainers())
      {
        deleteApiGateway();
      }
    }
    
    private void deleteTaskDef(Name name, int port, Collection<String> paths, String healthCheckPath)
    {
//      Name    serviceName = getNameFactory().getServiceItemName(name); //new Name(getEnvironmentType(), getEnvironment(), getRealm(), getRegion(), podId, name);
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

    private void deleteService(String name)
    {
      deleteTaskDefinitions(name, 0);
      Name    serviceName = getNameFactory().getPhysicalServiceItemName(name);
      
      log_.info("Deleting service " + serviceName + "...");
      
      try
      {
      
        ecsClient_.deleteService(new DeleteServiceRequest()
            .withCluster(clusterName_)
            .withForce(true)
            .withService(serviceName.toString()));
        
        log_.info("Deleted service " + serviceName + ".");
      }
      catch(ServiceNotFoundException e)
      {
        log_.info("Service " + serviceName + " did not exist anyway.");
      }
    }

    private void deleteTaskDefinitions(String name, int remaining)
    {
      Name    serviceName = getNameFactory().getPhysicalServiceItemName(name);
      
      DescribeServicesResult services = ecsClient_.describeServices(new DescribeServicesRequest()
          .withCluster(clusterName_)
          .withServices(serviceName.toString())
          );
      
      Set<String> targetArns = new HashSet<>();
      
      for(Service service : services.getServices())
      {
        targetArns.add(service.getTaskDefinition());
      }
      
      deleteTaskDefinitions(targetArns, remaining);
    }

    private void registerTaskDefinition(Name serviceName, int port, Name roleName, String imageName, int jvmHeap, int memory)
    {
      String roleArn = getRoleArn(roleName);
      
      Map<String, String> logOptions = new HashMap<>();

      logOptions.put("awslogs-group", createLogGroupIfNecessary());
      logOptions.put("awslogs-region", awsRegion_);
      logOptions.put("awslogs-stream-prefix", getNameFactory().getServiceId());
      
      RegisterTaskDefinitionRequest registerTaskDefinitionRequest =  new RegisterTaskDefinitionRequest()
          .withTaskRoleArn(roleArn)
          .withFamily(serviceName.toString())
          .withContainerDefinitions(new ContainerDefinition()
              .withName(serviceName.toString())
              .withImage(awsAccountId_ + ".dkr.ecr.us-east-1.amazonaws.com/" + imageName)
              .withPortMappings(new PortMapping()
                  .withContainerPort(port)
                  .withHostPort(0)
                  .withProtocol(TransportProtocol.Tcp)
                  )
              .withEssential(true)
              .withLogConfiguration(new LogConfiguration()
                  .withLogDriver(LogDriver.Awslogs)
                  .withOptions(logOptions)
                  )
              .withEnvironment(getTaskEnvironment(jvmHeap))
              .withMemory(memory)
              )
          ;
      

      ecsClient_.registerTaskDefinition(registerTaskDefinitionRequest);
    }

    private Collection<KeyValuePair> getTaskEnvironment(int jvmHeap)
    {
      List<KeyValuePair>  env = new LinkedList<>();
      INameFactory nf = getNameFactory();

      env.add(new KeyValuePair().withName("FUGUE_ENVIRONMENT_TYPE")   .withValue(nf.getEnvironmentType()));
      env.add(new KeyValuePair().withName("FUGUE_ENVIRONMENT")        .withValue(nf.getEnvironmentId()));
      env.add(new KeyValuePair().withName("FUGUE_REGION")             .withValue(awsRegion_));
      env.add(new KeyValuePair().withName("FUGUE_CONFIG")             .withValue(
          "https://s3." + awsRegion_ + ".amazonaws.com/" + nf.getConfigBucketName(awsRegion_) +
            "/" + CONFIG + "/" + nf.getPhysicalServiceName() + ".json"
          ));
      env.add(new KeyValuePair().withName("FUGUE_SERVICE")            .withValue(nf.getServiceId()));
//      env.add(new KeyValuePair().withName("FUGUE_PRIMARY_ENVIRONMENT").withValue(nf.getEnvironmentType()));
//      env.add(new KeyValuePair().withName("FUGUE_PRIMARY_REGION")     .withValue(nf.getEnvironmentType()));
      env.add(new KeyValuePair().withName("FUGUE_JAVA_ARGS")          .withValue("-Xms" + jvmHeap + "m -Xmx" + jvmHeap + "m"));
      
      return env;
    }

    private String createLogGroupIfNecessary()
    {
      boolean create = true;
      
      String name = getNameFactory().getPhysicalServiceName().toString();
      
      DescribeLogGroupsResult result = logsClient_.describeLogGroups(new DescribeLogGroupsRequest()
          .withLogGroupNamePrefix(name)
          );
      
      for(LogGroup group : result.getLogGroups())
      {
        if(group.getLogGroupName().equals(name))
        {
          create = false;
          break;
        }
      }
      
      if(create)
      {
        logsClient_.createLogGroup(new CreateLogGroupRequest()
            .withLogGroupName(name)
            .withTags(getTags())
            );
        
        logsClient_.putRetentionPolicy(new PutRetentionPolicyRequest()
            .withLogGroupName(name)
            .withRetentionInDays(14));
      }
      
      return name;
    }

    private void createService(String name, int port, int desiredCnt,
        Collection<String> paths, Name roleName, String imageName, int jvmHeap, int memory)
    {
      log_.info("Cluster name is " + clusterName_);
      
      Name    serviceName = getNameFactory().getPhysicalServiceItemName(name); //new Name(getEnvironmentType(), getEnvironment(), getRealm(), getRegion(), podId, name);
      
      registerTaskDefinition(serviceName, port, roleName, imageName, jvmHeap, memory);
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
        
//        if(isPrimaryEnvironment() && !paths.isEmpty())
//        {
//          request
//            .withLoadBalancers(new com.amazonaws.services.ecs.model.LoadBalancer()
//              .withContainerName(serviceName.toString()) // TODO: change to just "name" once we get task def working from Java
//              .withContainerPort(port)
//              .withTargetGroupArn(targetGroupArn)
//            );
//        }
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

    private void getApiGateway(boolean createIfNecessary)
    {
      String name = getNameFactory().getEnvironmentName(null).toString();
          //.getLogicalServiceName().toString();
          //getEnvironmentPrefix() + getNameFactory().getServiceImageName();
      
      try
      {
        String pos = null;
        
        do
        {
          GetRestApisResult getResult = apiClient_.getRestApis(new GetRestApisRequest().withPosition(pos));
          
          for(RestApi api : getResult.getItems())
          {
            if(api.getName().equals(name))
            {
              apiGatewayId_ = api.getId();
              log_.info("ApiGateway " + name + " exists as Api ID " + apiGatewayId_);
              break;
            }
          }
          pos = getResult.getPosition();
        }while(apiGatewayId_==null && pos!=null);
      }
      catch(com.amazonaws.services.apigateway.model.NotFoundException e)
      {}
      
      if(createIfNecessary)
      {
        if(apiGatewayId_ == null)
        {
          CreateRestApiResult createResult = apiClient_.createRestApi(new CreateRestApiRequest()
              .withName(name)
              .withDescription(name + (getPodName() == null ? " Service" : " Pod " + getPodName()) + " API")
              .withEndpointConfiguration(new EndpointConfiguration()
                  .withTypes(EndpointType.REGIONAL)
                  )
              );
          
          apiGatewayId_ = createResult.getId();
          log_.info("ApiGateway " + name + " created as Api ID " + apiGatewayId_);
        }
        
        setApiGatewayArn();
        
        apiClient_.tagResource(new TagResourceRequest()
            .withResourceArn(apiGatewayArn_)
            .withTags(getTags())
            );
      }
      else if(apiGatewayId_ != null)
        setApiGatewayArn();
    }
    
    private void setApiGatewayArn()
    {
      apiGatewayArn_ = getApiArn(apiGatewayId_);
      
      
      String envTypePrefix = "prod".equals(getEnvironmentType()) ? "" : getEnvironmentType() + ".";
      
      if("master".equals(getEnvironment()))
      {
        apiGatewayMasterDomainName_= envTypePrefix + "api." + getPublicDnsSuffix();
        //apiGatewayMasterCertArn_ = awsPublicCertArn_;
      }
      
      apiGatewayPrivateDomainName_ = getEnvironment() + "-" + "api." + getDnsSuffix();
//      apiGatewayCertArn_ = awsLoadBalancerCertArn_;
    }
    
    private void deleteApiGateway()
    {
      if(apiGatewayArn_ != null)
      {
//        apiClient_.deleteRestApi(new DeleteRestApiRequest()
//            .withRestApiId(apiGatewayId_)
//            );
      }
    }
  }
}
