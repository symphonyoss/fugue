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

package org.symphonyoss.s2.fugue.naming;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.config.IGlobalConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * A factory for producing Name instances.
 * 
 * @author Bruce Skingle
 *
 */
public class NameFactory implements INameFactory
{
  protected final String                     CONFIG = "config";
  protected final String                     FUGUE  = "fugue";

  private final String                       globalNamePrefix_;
  private final String                       environmentType_;
  private final String                       environmentId_;
  private final String                       regionId_;
  private final String                       podName_;
  private final Integer                      podId_;
  private final String                       serviceId_;
  private final ImmutableMap<String, String> tags_;
  
  /**
   * Constructor.
   * 
   * @param config Configuration.
   */
  public NameFactory(IGlobalConfiguration config)
  {
    this(config.getGlobalNamePrefix(), config.getEnvironmentType(), config.getEnvironmentId(), config.getRegionId(), config.getPodName(), config.getPodId(), config.getServiceId());
  }

  /**
   * Constructor.
   * 
   * @param globalNamePrefix  Global prefix added to all Names.
   * @param environmentType   The environmentType (e.g. dev)
   * @param environmentId     The environment ID (e.g. s2dev1)
   * @param regionId          The cloud service provider region ID (e.g. us-east-1)
   * @param podName           The pod name (i.e. the physical deployment name)
   * @param podId             The pod ID (1.e. the logical tenant ID)
   * @param serviceId         The service ID
   */
  public NameFactory(String globalNamePrefix, String environmentType, String environmentId, String regionId, String podName, Integer podId, String serviceId)
  {
    globalNamePrefix_ = globalNamePrefix;
    environmentType_ = environmentType;
    environmentId_ = environmentId;
    regionId_ = regionId;
    podName_ = podName;
    podId_ = podId;
    serviceId_ = serviceId;
    tags_ = createTags();
  }
  
  protected NameFactory(INameFactory nameFactory)
  {
    globalNamePrefix_ = nameFactory.getGlobalNamePrefix();
    environmentType_ = nameFactory.getEnvironmentType();
    environmentId_ = nameFactory.getEnvironmentId();
    regionId_ = nameFactory.getRegionId();
    podName_ = nameFactory.getPodName();
    podId_ = nameFactory.getPodId();
    serviceId_ = nameFactory.getServiceId();
    tags_ = createTags();
  }
  
  @Override
  public INameFactory withRegionId(String regionId)
  {
    return new NameFactory(globalNamePrefix_, environmentType_, environmentId_, regionId, podName_, podId_, serviceId_);
  }
  
  @Override
  public INameFactory withServiceId(String serviceId)
  {
    return new NameFactory(globalNamePrefix_, environmentType_, environmentId_, regionId_, podName_, podId_, serviceId);
  }
  
  @Override
  public INameFactory withPod(String podName, Integer podId)
  {
    return new NameFactory(globalNamePrefix_, environmentType_, environmentId_, regionId_, podName, podId, serviceId_);
  }
  
  @Override
  public INameFactory withGlobalNamePrefix(String globalNamePrefix)
  {
    return new NameFactory(globalNamePrefix, environmentType_, environmentId_, regionId_, podName_, podId_, serviceId_);
  }

  private ImmutableMap<String, String> createTags()
  {
    Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
    
    putIfNotNull(builder, Fugue.TAG_FUGUE_ENVIRONMENT_TYPE, environmentType_);
    putIfNotNull(builder, Fugue.TAG_FUGUE_ENVIRONMENT,      environmentId_);
    putIfNotNull(builder, Fugue.TAG_FUGUE_REGION,           regionId_);
    putIfNotNull(builder, Fugue.TAG_FUGUE_POD,              podId_);
    
    return builder.build();
  }

  private void putIfNotNull(Builder<String, String> builder, String name, String value)
  {
      if(value != null)
        builder.put(name, value);
  }

  private void putIfNotNull(Builder<String, String> builder, String name, Integer value)
  {
      if(value != null)
        builder.put(name, value.toString());
  }

  @Override
  public String getGlobalNamePrefix()
  {
    return globalNamePrefix_;
  }

  @Override
  public String getEnvironmentType()
  {
    return environmentType_;
  }

  @Override
  public String getEnvironmentId()
  {
    return environmentId_;
  }
  
  @Override
  public String getRegionId()
  {
    return regionId_;
  }
  
  @Override
  public String getPodName()
  {
    return podName_;
  }

  @Override
  public Integer getPodId()
  {
    return podId_;
  }

  @Override
  public String getServiceId()
  {
    return serviceId_;
  }

  private @Nonnull <T> T require(T value, String name)
  {
    if(value == null)
      throw new IllegalStateException(name + " is not present.");
    
    return value;
  }
  
  @Override
  public String getRequiredEnvironmentType()
  {
    return require(environmentType_, "environmentType");
  }

  @Override
  public String getRequiredEnvironmentId()
  {
    return require(environmentId_, "environmentId");
  }
  
  @Override
  public String getRequiredRegionId()
  {
    return require(regionId_, "regionId");
  }
  
  @Override
  public String getRequiredServiceId()
  {
    return require(serviceId_, "serviceId");
  }

  @Override
  public ImmutableMap<String, String> getTags()
  {
    return tags_;
  }

//  @Override
//  public Name getName(String ...names)
//  {
//    return new Name(getGlobalNamePrefix(), names);
//  }
  
  @Override
  public ServiceName  getPhysicalServiceName()
  {
    return createServiceName(serviceId_, podName_, podId_, 
        getGlobalNamePrefix(), environmentType_, environmentId_, podName_, getRequiredServiceId());
  }
  
  @Override
  public ServiceName  getServiceImageName()
  {
    return createServiceName(serviceId_, null, null, 
        getGlobalNamePrefix(), getRequiredServiceId());
  }
  
  @Override
  public ServiceName  getLogicalServiceName()
  {
    return createServiceName(serviceId_, podName_, podId_, 
        getGlobalNamePrefix(), environmentType_, environmentId_, podId_, getRequiredServiceId());
  }
  
  @Override
  public ServiceName  getRegionalServiceName()
  {
    return createServiceName(serviceId_, podName_, podId_, 
        getGlobalNamePrefix(), environmentType_, environmentId_, regionId_, podName_, getRequiredServiceId());
  }
  
  @Override
  public ServiceName  getMultiTenantServiceName()
  {
    return createServiceName(serviceId_, null, null,
        getGlobalNamePrefix(), environmentType_, environmentId_, null, getRequiredServiceId());
  }
  
  @Override
  public ServiceName  getPhysicalServiceItemName(String name)
  {
    return createServiceName(serviceId_, podName_, podId_, 
        getGlobalNamePrefix(), environmentType_, environmentId_, podName_, getRequiredServiceId(), name);
  }
  
  @Override
  public ServiceName  getLogicalServiceItemName(String name)
  {
    return createServiceName(serviceId_, podName_, podId_, 
        getGlobalNamePrefix(), environmentType_, environmentId_, podId_, getRequiredServiceId(), name);
  }
  
  @Override
  public TableName  getTableName(String tableId)
  {
    return createTableName(serviceId_, tableId, 
        getGlobalNamePrefix(), environmentType_, environmentId_, serviceId_, tableId);
  }

  @Override
  public TopicName getTopicName(String topicId)
  {
    return createTopicName(getServiceId(), true, topicId, 
        getGlobalNamePrefix(), environmentType_, environmentId_, getServiceId(), topicId);
  }

  @Override
  public TopicName getTenantTopicName(String topicId)
  {
    return createTopicName(getServiceId(), true, topicId, 
        getGlobalNamePrefix(), environmentType_, environmentId_,
        getServiceId(), getPodName(),
        topicId);
  }

  @Override
  public Collection<TopicName> getTopicNameCollection(String topicId, String... additionalTopicIds)
  {
    ArrayList<TopicName> result = new ArrayList<>(additionalTopicIds==null ? 1 : 1 + additionalTopicIds.length);
    
    result.add(getTopicName(topicId));
    
    if(additionalTopicIds != null)
    {
      for(String id : additionalTopicIds)
      {
        result.add(getTopicName(id));
      }
    }
    return result;
  }

  @Override
  public TopicName getTopicName(String serviceId, String topicId)
  {
    return createTopicName(serviceId, getServiceId().equals(serviceId), topicId,
        getGlobalNamePrefix(), environmentType_, environmentId_, serviceId, topicId);
  }

  @Override
  public Name getConfigBucketName(String regionId)
  {
    return createName(getGlobalNamePrefix(), FUGUE, getRequiredEnvironmentType(), regionId, CONFIG);
  }
  
  @Override
  public Name getFugueName()
  {
    return createName(getGlobalNamePrefix(), FUGUE, environmentType_, environmentId_, regionId_, serviceId_);
  }
  
  @Override
  public Name getFugueEnvironmentTypeName()
  {
    return createName(getGlobalNamePrefix(), FUGUE, environmentType_);
  }
  
  @Override
  public Name getName()
  {
    return createName(getGlobalNamePrefix(), environmentType_, environmentId_, regionId_, serviceId_);
  }

  @Override
  public Name getRegionName()
  {
    return createName(getGlobalNamePrefix(), environmentType_, environmentId_, regionId_);
  }
  
  @Override
  public Name getRegionalName(String name)
  {
    return createName(getGlobalNamePrefix(), environmentType_, environmentId_, regionId_, name);
  }
  
  @Override
  public Name getEnvironmentName(String name)
  {
    return createName(getGlobalNamePrefix(), environmentType_, environmentId_, name);
  }

  @Override
  public CredentialName getFugueCredentialName(String owner)
  {
    return createCredentialName(getGlobalNamePrefix(), FUGUE + "-" + environmentType_, environmentId_, null, owner, CredentialName.SUFFIX);
  }

  @Override
  public CredentialName getEnvironmentCredentialName(String owner)
  {
    return createCredentialName(getGlobalNamePrefix(), environmentType_, environmentId_, null, owner, CredentialName.SUFFIX);
}
  
  @Override
  public CredentialName getCredentialName(String owner)
  {
    return getCredentialName(podId_, owner);
  }
  
  @Override
  public CredentialName getCredentialName(Integer podId, String owner)
  {
    return createCredentialName(getGlobalNamePrefix(), environmentType_, environmentId_, "pod-" + podId, owner, CredentialName.SUFFIX);
  }
  
  @Override
  public CredentialName getCredentialName(Integer podId, Hash principalBaseHash)
  {
    return createCredentialName(getGlobalNamePrefix(), environmentType_, environmentId_, "pod-" + podId, "principal-" + principalBaseHash, CredentialName.SUFFIX);
  }
  
  @Override
  public SubscriptionName getSubscriptionName(TopicName topicName, String subscriptionId)
  {
    return createSubscriptionName(topicName, getServiceId(), subscriptionId,
        getGlobalNamePrefix(), environmentType_, environmentId_, podName_, getServiceId(), subscriptionId, topicName.getTopicId(), topicName.getServiceId());
  }
  
  protected Name createName(@Nonnull String name, String ...additional)
  {
    return new Name(name, (Object[])additional);
  }

  protected ServiceName createServiceName(String serviceId, String podName, Integer podId, @Nonnull String name, Object ...additional)
  {
    return new ServiceName(serviceId, podName, podId, name, additional);
  }

  protected TableName createTableName(String serviceId, String tableId, @Nonnull String name, String ...additional)
  {
    return new TableName(serviceId, tableId, name, additional);
  }

  protected TopicName createTopicName(String serviceId, boolean isLocal, String topicId, @Nonnull String name, String ...additional)
  {
    return new TopicName(serviceId, isLocal, topicId, name, additional);
  }

  protected SubscriptionName createSubscriptionName(TopicName topicName, String serviceId, String subscriptionId, @Nonnull String name, String ...additional)
  {
    return new SubscriptionName(topicName, serviceId, subscriptionId, name, additional);
  }

  protected CredentialName createCredentialName(String prefix, String environmentTypeId, String environmentId, String podId, String owner, String suffix)
  {
    return new CredentialName(prefix, environmentTypeId, environmentId, podId, owner, suffix);
  }
}
