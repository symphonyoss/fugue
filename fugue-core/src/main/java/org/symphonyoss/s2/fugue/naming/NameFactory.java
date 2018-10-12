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

import javax.annotation.Nonnull;

public abstract class NameFactory implements INameFactory
{
  protected final String CONFIG = "config";
  protected final String FUGUE  = "fugue";
  
  protected Name createName(@Nonnull String name, String ...additional)
  {
    return new Name(name, additional);
  }

  protected ServiceName createServiceName(String serviceId, String tenantId, @Nonnull String name, String ...additional)
  {
    return new ServiceName(serviceId, tenantId, name, additional);
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

  protected CredentialName createCredentialName(String prefix, String environmentTypeId, String environmentId, String tenantId, String owner, String suffix)
  {
    return new CredentialName(prefix, environmentTypeId, environmentId, tenantId, owner, suffix);
  }
}
