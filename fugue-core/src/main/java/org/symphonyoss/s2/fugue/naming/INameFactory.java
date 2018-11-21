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

import com.google.common.collect.ImmutableMap;

public interface INameFactory
{

  TableName getTableName(String table);

  CredentialName getEnvironmentCredentialName(String owner);

  @Deprecated
  TopicName getObsoleteTopicName(String topicId);
  
  @Deprecated
  Collection<TopicName> getObsoleteTopicNameCollection(String topicId, String ...additionalTopicIds);
  
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
  
  @Deprecated
  SubscriptionName  getObsoleteSubscriptionName(TopicName topicName, String subscriptionId);
  
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

  CredentialName getCredentialName(String tenantId, String owner);

  /**
   * 
   * @return the service name with no tenant element even if we are in a tenant specific context.
   */
  ServiceName getMultiTenantServiceName();
  ServiceName getServiceName();
  ServiceName getRegionalServiceName();

  CredentialName getFugueCredentialName(String owner);

  Name getServiceItemName(String name);

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
   * Create a new INameFactory with the given tenantId and inheriting all other attributes from the current factory.
   * 
   * @param tenantId  The tenantId for the new Name Factory.
   * 
   * @return a new INameFactory with the given tenantId and inheriting all other attributes from the current factory.
   */
  INameFactory withTenantId(String tenantId);
  
  String getEnvironmentType();
  
  String getEnvironmentId();

  String getTenantId();
  
  String getServiceId();

  String getRealmId();
  
  String getRegionId();
  
  String getRequiredEnvironmentType();
  
  String getRequiredEnvironmentId();

  String getRequiredTenantId();
  
  String getRequiredServiceId();

  String getRequiredRealmId();
  
  String getRequiredRegionId();

  ImmutableMap<String, String> getTags();

  Name getFugueEnvironmentTypeName();

  TopicName getTenantTopicName(String topicId);
}
