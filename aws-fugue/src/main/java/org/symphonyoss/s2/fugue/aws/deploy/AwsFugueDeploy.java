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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.IStringProvider;
import org.symphonyoss.s2.common.dom.json.IJsonArray;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonObject;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonDom;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.fugue.aws.config.S3Helper;
import org.symphonyoss.s2.fugue.deploy.ConfigHelper;
import org.symphonyoss.s2.fugue.deploy.ConfigProvider;
import org.symphonyoss.s2.fugue.deploy.FugueDeploy;
import org.symphonyoss.s2.fugue.naming.Name;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
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
  private static final String            VPC_ID                        = "vpcId";
  private static final String            LOAD_BALANCER_CERTIFICATE_ARN = "loadBalancerCertificateArn";
  private static final String            LOAD_BALANCER_SECURITY_GROUPS = "loadBalancerSecurityGroups";
  private static final String            LOAD_BALANCER_SUBNETS         = "loadBalancerSubnets";
//  private static final String            IALB_ARN                      = "ialbArn";
//  private static final String            IALB_DNS                      = "ialbDns";
//  private static final String            R53_ZONE                      = "r53Zone";
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

  private static final String HOST_HEADER = "host-header";

  private static final String PATH_PATERN = "path-pattern";

  private static final String DEFAULT = "default";


  private final AmazonIdentityManagement iam_                          = AmazonIdentityManagementClientBuilder
      .defaultClient();
  private final AWSSecurityTokenService  sts_                          = AWSSecurityTokenServiceClientBuilder
      .defaultClient();

  private String                         awsAccountId_;
//  private User                           awsUser_;
  private String                         awsRegion_;
  private String                         awsVpcId_;
  private String                         awsLoadBalancerCertArn_;
  private List<String>                   awsLoadBalancerSecurityGroups_ = new LinkedList<>();
  private List<String>                   awsLoadBalancerSubnets_        = new LinkedList<>();
//  private String                         awsIalbArn_;
//  private String                         awsIalbDns_;
//  private String                         awsR53Zone_;

  private List<String>                   environmentTypeRegions_       = new LinkedList<>();
  private Map<String, String>            environmentTypeConfigBuckets_ = new HashMap<>();
  private String                         callerRefPrefix_              = UUID.randomUUID().toString() + "-";

  private AmazonElasticLoadBalancing     elbClient_;
  private AmazonRoute53                  r53Clinet_;
  private AmazonIdentityManagement       iamClient_;

  private String baseZoneId_;
//  private String s2ZoneId_;
//  private String environmentTypeZoneId_;
//  private String environmentZoneId_;
//  private String regionZoneId_;
//  private String regionalServiceZoneId_;
//  private String serviceZoneId_;
//  private String tenantZoneId_;
//  private String regionalTenantZoneId_;
  private LoadBalancer singleTenantLoadBalancer_;
  private LoadBalancer multiTenantLoadBalancer_;

  private String singleTenantDefaultTargetGroupArn_;
  private String multiTenantDefaultTargetGroupArn_;

  private String singleTenantListenerArn_;

  private String multiTenantListenerArn_;

  private AmazonECS ecsClient_;

  private Name clusterName_;

  private String clusterArn_;

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
  
  private void abort(String message, Throwable cause)
  {
    log_.error(message, cause);
    
    throw new IllegalStateException(message, cause);
  }
  
  @Override
  protected void deployServiceContainer(String name, int port, Collection<String> paths, String healthCheckPath, String tenantId)
  {
    try
    {
    Name targetGroupName = new Name(getEnvironmentType(), getEnvironment(), tenantId, getService(), name);
    
    String targetGroupArn = createTargetGroup(targetGroupName, healthCheckPath, port);
    
    String hostName = getServiceHostName(tenantId);
    String regionalHostName = new Name(getEnvironmentType(), getEnvironment(), getRegion(), tenantId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
    String wildCardHostName = new Name(getEnvironmentType(), getEnvironment(), "*", tenantId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
    
    configureNetworkRule(targetGroupArn, wildCardHostName, name, port, paths, healthCheckPath, tenantId);
    
    createR53RecordSet(hostName, regionalHostName, tenantId == null ? multiTenantLoadBalancer_ : singleTenantLoadBalancer_);
    
    getOrCreateCluster();
    
//    registerTaskDef(name, port, healthCheckPath, tenantId);
    
    createService(regionalHostName, targetGroupArn, name, port, tenantId);
    
    System.err.println("All done!");
    }
    catch(RuntimeException e)
    {
      e.printStackTrace();
      
      throw e;
    }
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

  public String getServiceHostName(String tenantId)
  {
    return new Name(getEnvironmentType(), getEnvironment(), "any", tenantId, getService()).toString().toLowerCase() + "." + getDnsSuffix();
  }

  private void getOrCreateCluster()
  {
    clusterName_ = new Name(getEnvironmentType(), getEnvironment(),getRealm(), getRegion());
    
    DescribeClustersResult describeResult = ecsClient_.describeClusters(new DescribeClustersRequest()
        .withClusters(clusterName_.toString())
        );
    
    for(Cluster cluster : describeResult.getClusters())
    {
      if(clusterName_.toString().equals(cluster.getClusterName()))
      {
        clusterArn_ = cluster.getClusterArn();
        
        break;
      }
    }
    
    if(clusterArn_ == null)
    {
      log_.info("Cluster does not exist, creating...");
      
      CreateClusterResult createResult = ecsClient_.createCluster(new CreateClusterRequest()
          .withClusterName(clusterName_.toString())
          );
      
      clusterArn_ = createResult.getCluster().getClusterArn();
      
      log_.info("Cluster " + clusterArn_ + " created.");
    }
    else
    {
      log_.info("Cluster " + clusterArn_ + " aready exists.");
    }
  }

  private void createService(String regionalHostName, String targetGroupArn, String name, int port, String tenantId)
  {
    // TODO: FIXME:
    String clusterName = "sym-ms-devb-ause1";
    
    if("qa".equals(getEnvironmentType()))
        clusterName = "sym-ms-qa-ause1";
    
    log_.info("Cluster name is " + clusterName);
    
    Name    serviceName = new Name(getEnvironmentType(), getEnvironment(), getRealm(), getRegion(), tenantId, name);
    boolean create      = true;
    
    DescribeServicesResult describeResult = ecsClient_.describeServices(new DescribeServicesRequest()
        .withCluster(clusterName)
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
            
          default:
            create = false;
        }
      }
    }
    
    if(create)
    {
      log_.info("Creating service " + serviceName + "...");
      
      CreateServiceResult createServiceResult = ecsClient_.createService(new CreateServiceRequest()
          .withCluster(clusterName)
          .withServiceName(serviceName.toString())
          .withTaskDefinition(serviceName.toString())
          .withDesiredCount(1)
  //        .withDeploymentConfiguration(new DeploymentConfiguration()
  //            .withMaximumPercent(maximumPercent)
  //            .withMinimumHealthyPercent(minimumHealthyPercent)
  //            )
          .withLoadBalancers(new com.amazonaws.services.ecs.model.LoadBalancer()
              .withContainerName(serviceName.toString()) // TODO: change to just "name" once we get task def working from Java
              .withContainerPort(port)
              .withTargetGroupArn(targetGroupArn)
              )
          );
      
      System.err.println(createServiceResult);
      
      System.err.println(createServiceResult.getService());
      
      log_.info("Created service " + serviceName + "as" + createServiceResult.getService().getServiceArn() + " with status " + createServiceResult.getService().getStatus() + ".");
    }
    else
    {
      log_.info("Updating service " + serviceName + "...");
      
      UpdateServiceResult updateResult = ecsClient_.updateService(new UpdateServiceRequest()
          .withCluster(clusterName)
          .withService(serviceName.toString())
          .withTaskDefinition(serviceName.toString())
          .withDesiredCount(1)
//          .withForceNewDeployment(true)
          );
      
      System.err.println(updateResult);
      
      log_.info("Updated service " + serviceName + "as" + updateResult.getService().getServiceArn() + " with status " + updateResult.getService().getStatus() + ".");
    }
  }

  @Override
  protected void deployService(boolean hasSingleTenantContainer, boolean hasMultiTenantContainer)
  {
    createDnsZones();
    
    createLoadBalancer(hasSingleTenantContainer, hasMultiTenantContainer);
  }
  
  private void createLoadBalancer(boolean hasSingleTenantContainer, boolean hasMultiTenantContainer)
  {
    if(hasSingleTenantContainer && getTenant() != null)
    {
      singleTenantLoadBalancer_ = createLoadBalancer(getTenant());
      
      if(singleTenantDefaultTargetGroupArn_ == null)
      {
        Name name = new Name(getEnvironmentType(), getEnvironment(), getTenant(), getService(), DEFAULT);
        
        singleTenantDefaultTargetGroupArn_ = createTargetGroup(name, "/HealthCheck", 80);
      }
      
      singleTenantListenerArn_ = createLoadBalancerListener(singleTenantLoadBalancer_, singleTenantDefaultTargetGroupArn_);
    }
    
    if(hasMultiTenantContainer)
    {
      multiTenantLoadBalancer_ = createLoadBalancer(null);
      
      if(multiTenantDefaultTargetGroupArn_ == null){
        Name name = new Name(getEnvironmentType(), getEnvironment(), getService(), DEFAULT);

        multiTenantDefaultTargetGroupArn_ = createTargetGroup(name, "/HealthCheck", 80);
      }

      multiTenantListenerArn_ = createLoadBalancerListener(multiTenantLoadBalancer_, multiTenantDefaultTargetGroupArn_);
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
//    for(Listener listener : listeners)
//    {
//      if(ProtocolEnum.HTTPS.equals(listener.getProtocol()))
//      {
//        for(Certificate cert : listener.getCertificates())
//        {
//          cert.getCertificateArn()
//        }
//      }
//    }
    
    
    log_.info("Creating listener...");
    
//    elbClient_.Cer
//    GetServerCertificateResult certificateResult = iamClient_.getServerCertificate(new GetServerCertificateRequest()
//        .withServerCertificateName("NAME")
//        );
//    
//    Certificate certificate = certificateResult.getServerCertificate();
    
//    elbClient_.addListenerCertificates(new AddListenerCertificatesRequest()
//        .withCertificates(certificates)
//        );
    
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

  private LoadBalancer createLoadBalancer(String tenant)
  {
    String name = new Name(getEnvironmentType(), getEnvironment(), tenant, getService()).getShortName(32);
    
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
        
        // So the LB exists, check that ithas the correct security groups and subnets
        int     cnt = awsLoadBalancerSecurityGroups_.size();
        
        for(String sg : loadBalancer.getSecurityGroups())
        {
          if(awsLoadBalancerSecurityGroups_.contains(sg))
          {
            cnt--;
          }
          else
          {
            ok = false;
            break;
          }
        }
        
        if(cnt > 0)
          ok = false;
        
        if(ok)
        {
          cnt = awsLoadBalancerSubnets_.size();
          for(AvailabilityZone az : loadBalancer.getAvailabilityZones())
          {
            if(awsLoadBalancerSubnets_.contains(az.getSubnetId()))
            {
              cnt--;
            }
            else
            {
              ok = false;
              break;
            }
          }
          
          if(cnt > 0)
            ok = false;
        }
        
        if(ok)
        {
          log_.info("Load balancer " + loadBalancer.getLoadBalancerArn() + " is good, no more to do");
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
    
    return loadBalancer;
  }

  private void createDnsZones()
  {
    String name       = getDnsSuffix();
    
    if(baseZoneId_ == null)
      baseZoneId_ = createOrGetHostedZone(name, false);
    
//    name = getEnvironmentType() + "." + name;
//    
////    if(environmentTypeZoneId_ == null)
////      environmentTypeZoneId_ = createOrGetHostedZone(name);
//    
//    name = getEnvironment() + "." + name;
//    
////    if(environmentZoneId_ == null)
////      environmentZoneId_ = createOrGetHostedZone(name);
//    
//    String regionalName = getRegion() + "." + name;
//    
////    if(regionZoneId_ == null)
////      regionZoneId_ = createOrGetHostedZone(regionalName);
//    
//    name = getService() + "." + name;
//    
//    if(serviceZoneId_ == null)
//      serviceZoneId_ = createOrGetHostedZone(name);
//    
//    regionalName = getService() + "." + regionalName;
//    
//    if(regionalServiceZoneId_ == null)
//      regionalServiceZoneId_ = createOrGetHostedZone(regionalName);
//    
////    if(getTenant() != null)
////    {
////      name = getTenant() + "." + name;
////      
////      if(tenantZoneId_ == null)
////        tenantZoneId_ = createOrGetHostedZone(name);
////      
////      regionalName = getTenant() + "." + regionalName;
////      
////      if(regionalTenantZoneId_ == null)
////        regionalTenantZoneId_ = createOrGetHostedZone(regionalName);
////    }
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
        throw new IllegalStateException("\"Zone " + dnsName + " not found.");
      }
    }
}
  
  private void createR53RecordSet(String host, String regionalHost, LoadBalancer loadBalancer)
  {
    createR53RecordSet(host, regionalHost, true);
    createR53RecordSet(regionalHost, loadBalancer.getDNSName(), false);
  }
  
  private void createR53RecordSet(String source, String target, boolean multiValue)
  {
    String zoneId = createOrGetHostedZone(source.substring(source.indexOf('.') + 1), false);
    
    String sourceDomain = source + ".";
    
    ListResourceRecordSetsResult result = r53Clinet_.listResourceRecordSets(new ListResourceRecordSetsRequest()
        .withHostedZoneId(zoneId)
        .withStartRecordName(source)
        );
    
    List<ResourceRecordSet> recordSetList = result.getResourceRecordSets();
    boolean                 ok            = false;
    
    for(ResourceRecordSet recordSet : recordSetList)
    {
      if(sourceDomain.equals(recordSet.getName()))
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
          .withSetIdentifier(getRegion().toLowerCase())
          ;
      }
      
      ChangeResourceRecordSetsResult rresult = r53Clinet_.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
          .withHostedZoneId(zoneId)
          .withChangeBatch(new ChangeBatch()
              .withChanges(new Change()
                  .withAction(ChangeAction.CREATE)
                  .withResourceRecordSet(resourceRecordSet)
                  )
              )
          );
        
      System.out.println(rresult);
    }
    
/*

    
    
    
    
    if(!R53RecordSetExist(environmentType, environment, tenant)) {
        def rs_template_args = ['SERVICE_NAME':servicename,
                                'TENANT_ID':tenant,
                                'DNS_SUFFIX':ECSClusterMaps.env_cluster[environment]['dns_suffix'],
                                'ALB_DNS':ECSClusterMaps.env_cluster[environment]['ialb_dns']
        ]
        def rs_def = (new org.apache.commons.lang3.text.StrSubstitutor(rs_template_args)).replace(record_set_template)
        def rs_def_file = 'r53-rs-'+environment+'-'+tenant+'-'+servicename+'.json'
        steps.writeFile file:rs_def_file, text:rs_def
        //steps.sh 'ls -alh'
        steps.withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'sym-aws-'+environmentType, secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            steps.sh 'aws --region us-east-1 route53 change-resource-record-sets --hosted-zone-id '+ECSClusterMaps.env_cluster[environment]['r53_zone']+' --change-batch file://'+rs_def_file+' > r53-rs-create-out-'+environment+'-'+tenant+'-'+servicename+'.json'
        
        }
    }
    */
  }

  private void configureNetworkRule(String targetGroupArn, String host, String name, int port, Collection<String> paths, String healthCheckPath,
      String tenantId)
  {
    
    List<String> remainingPaths = new ArrayList<>();
    
    remainingPaths.addAll(paths);
    
    DescribeRulesResult ruleDescription = elbClient_.describeRules(new DescribeRulesRequest()
        .withListenerArn(tenantId == null ? multiTenantListenerArn_ : singleTenantListenerArn_)
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
      
      // since there is only one action I cant see how there will not always be exactly one of these but....
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
            log_.info("Deleting rule " + rule.getRuleArn() + " for non-existant path " + conditionPath);
            
            elbClient_.deleteRule(new DeleteRuleRequest()
                .withRuleArn(rule.getRuleArn())
                );
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
    
    for(String path : remainingPaths)
    {
      log_.info("Creating rule for host " + host + " for non-existant path " + path + "...");
      
      CreateRuleResult createRuleResult = elbClient_.createRule(new CreateRuleRequest()
          .withListenerArn(tenantId == null ? multiTenantListenerArn_ : singleTenantListenerArn_)
          .withConditions(
//              new RuleCondition()
//                .withField(HOST_HEADER)
//                .withValues(host),
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
      return groups.get(0).getTargetGroupArn();
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
    
    System.out.println("result=" + result);
    
    return result.getTargetGroups().get(0).getTargetGroupArn();
  }


  @Override
  protected void validateAccount(MutableJsonObject config)
  {
    IJsonDomNode node = config.get(AMAZON);
    
    if(node instanceof IJsonObject)
    {
      IJsonObject<?> amazon = ((IJsonObject<?>)node);
      
      awsAccountId_           = amazon.getRequiredString(ACCOUNT_ID);
      awsRegion_              = amazon.getRequiredString(REGION);
      awsVpcId_               = amazon.getRequiredString(VPC_ID);
      awsLoadBalancerCertArn_ = amazon.getRequiredString(LOAD_BALANCER_CERTIFICATE_ARN);
      
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

      elbClient_ = AmazonElasticLoadBalancingClientBuilder.standard()
          .withRegion(awsRegion_)
          .build();
      
      r53Clinet_ = AmazonRoute53ClientBuilder.standard()
          .withRegion(awsRegion_)
          .build();
      
      iamClient_ = AmazonIdentityManagementClientBuilder.standard()
        .withRegion(awsRegion_)
        .build();
      
      ecsClient_ = AmazonECSClientBuilder.standard()
          .withRegion(awsRegion_)
          .build();
      
      logsClient_ = AWSLogsClientBuilder.standard()
          .withRegion(awsRegion_)
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
    
    createRole(name, policyArn);
  }

  @Override
  protected void createEnvironmentType()
  {
    String baseName = FUGUE_PREFIX + getEnvironmentType();
    
    createEnvironmentTypeAdminUser(baseName);
    createEnvironmentTypeCicdUser(baseName);
    
    for(String region : environmentTypeRegions_)
    {
      createBucketIfNecessary(region, environmentTypeConfigBuckets_.get(region));
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
    
    createRole(name, policyArn);
  }
  
  private void createBucketIfNecessary(String region, String name)
  {
    AmazonS3 s3 = AmazonS3ClientBuilder
        .standard()
        .withRegion(region)
        .build();

    S3Helper.createBucketIfNecessary(s3, name);
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
