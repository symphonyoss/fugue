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

import java.util.Collection;

import javax.annotation.Nullable;

import org.symphonyoss.s2.common.hash.Hash;

import com.google.common.collect.ImmutableMap;

/**
 * A factory for producing Name instances.
 * 
 * @author Bruce Skingle
 *
 */
public interface INameFactory
{
  /**
   * Return a table name with the given local ID owned by the current service.
   * 
   * @param table The table ID.
   * 
   * @return A TableName.
   */
  TableName getTableName(String table);

  
  /**
   * Return a TopicName for the given topic owned by the current service.
   * 
   * @param topicId     The topic ID.
   * 
   * @return A TopicName.
   */
  TopicName getTopicName(String topicId);
  
  /**
   * Return a TopicName for the given topic owned by the current service.
   * 
   * @param topicId               A topic ID 
   * @param additionalTopicIds    Zero or more additional topic IDs
   * 
   * @return A collection of TopicNames.
   */
  Collection<TopicName> getTopicNameCollection(String topicId, String ...additionalTopicIds);
  
  /**
   * Return a TopicName for the given topic owned by the given service.
   * 
   * @param serviceId   The service which owns the topic.
   * @param topicId     The topic ID.
   * 
   * @return A TopicName.
   */
  TopicName getTopicName(String serviceId, String topicId);
  
  /**
   * Return a Subscription Name for the given topic and optional subscriptionId.
   * 
   * SubscriptionIds are only needed where a service needs more than one subscription to the same topic.
   * 
   * @param topicName       The name of the topic
   * @param subscriptionId  The subscriptionId (simple name) to distinguish this subscription from others on the same topic.
   * 
   * @return A SubscriptionName
   */
  SubscriptionName  getSubscriptionName(TopicName topicName, @Nullable String subscriptionId);

  /**
   * 
   * @return the service name with no tenant element even if we are in a tenant specific context.
   */
  ServiceName getMultiTenantServiceName();

  /**
   * 
   * @return the service name including the pod name element.
   */
  ServiceName getPhysicalServiceName();

  /**
   * 
   * @return the service name including the pod ID element.
   */
  ServiceName getLogicalServiceName();

  /**
   * 
   * @return the service name including the pod name and region elements.
   */
  ServiceName getRegionalServiceName();

  /**
   * Return the service name including the pod name and the given name element.
   * 
   * @param name An additional name element.
   * 
   * @return the service name including the pod name and the given name element.
   */
  Name getPhysicalServiceItemName(String name);

  /**
   * Return the service name including the pod ID and the given name element.
   * 
   * @param name An additional name element.
   * 
   * @return the service name including the pod ID and the given name element.
   */
  Name getLogicalServiceItemName(String name);

  Name getConfigBucketName(String regionId);

  Name getRegionalName(String name);

  Name getRegionName();

  Name getName();
  Name getFugueName();

  /**
   * Create a new INameFactory with the given regionId and inheriting all other attributes from the current factory.
   * 
   * @param regionId  The regionId for the new Name Factory.
   * 
   * @return a new INameFactory with the given regionId and inheriting all other attributes from the current factory.
   */
  INameFactory withRegionId(String regionId);


  /**
   * Create a new INameFactory with the given serviceId and inheriting all other attributes from the current factory.
   * 
   * @param serviceId  The serviceId for the new Name Factory.
   * 
   * @return a new INameFactory with the given serviceId and inheriting all other attributes from the current factory.
   */
  INameFactory withServiceId(String serviceId);

  /**
   * Create a new INameFactory with the given tenantId and inheriting all other attributes from the current factory.
   * 
   * @param podName The podName for the new Name Factory.
   * @param podId   The podId for the new Name Factory.
   * 
   * @return a new INameFactory with the given tenantId and inheriting all other attributes from the current factory.
   */
  INameFactory withPod(String podName, Integer podId);
  
  String getEnvironmentType();
  
  String getEnvironmentId();

  Integer getPodId();
  String getPodName();
  
  String getServiceId();
  
  String getRegionId();
  
  String getRequiredEnvironmentType();
  
  String getRequiredEnvironmentId();
  
  String getRequiredServiceId();
  
  String getRequiredRegionId();

  ImmutableMap<String, String> getTags();

  Name getFugueEnvironmentTypeName();

  TopicName getTenantTopicName(String topicId);

  String getGlobalNamePrefix();

  INameFactory withGlobalNamePrefix(String globalNamePrefix);

  CredentialName getFugueCredentialName(String owner);

  CredentialName getCredentialName(Integer podId, String owner);

  CredentialName getCredentialName(String owner);

  CredentialName getCredentialName(Integer podId, Hash principalBaseHash);

  CredentialName getEnvironmentCredentialName(String owner);


  ServiceName getServiceImageName();


  Name getEnvironmentName(String name);
}
