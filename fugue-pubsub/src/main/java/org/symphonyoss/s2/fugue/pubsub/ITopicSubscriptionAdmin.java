/*
 *
 *
 * Copyright 2019 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.pubsub;

import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.naming.SubscriptionName;

import com.google.common.collect.ImmutableSet;

/**
 * A subscription.
 * 
 * @author Bruce Skingle
 */
public interface ITopicSubscriptionAdmin extends ISubscriptionAdmin
{
  /**
   * Return the set of subscription names.
   * 
   * @return The subscription names for this subscription.
   */
  @Override
  ImmutableSet<SubscriptionName> getSubscriptionNames();

  /**
   * Return the name of the property to be used for filtering.
   * 
   * @return The subscription names for this subscription.
   */
  @Nullable String getFilterPropertyName();

  /**
   * Return true iff filtering is exclusive, otherwise it is inclusive.
   * 
   * @return true iff filtering is exclusive, otherwise it is inclusive.
   */
  boolean isFilterExclude();

  /**
   * Return the set of values to filter.
   * 
   * @return The set of values to filter.
   */
  ImmutableSet<String> getFilterPropertyValues();

  /**
   * Return the name of a lambda function to be triggered to process messages on this subscription.
   * 
   * @return The name of a lambda function to be triggered to process messages on this subscription.
   */
  String getLambdaConsumer();

}
