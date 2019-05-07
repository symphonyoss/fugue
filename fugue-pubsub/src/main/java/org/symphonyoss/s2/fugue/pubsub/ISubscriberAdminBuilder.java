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

package org.symphonyoss.s2.fugue.pubsub;

import org.symphonyoss.s2.common.fluent.IBuilder;

/**
 * A builder for a subscriber admin controller.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete builder, needed for fluent methods.
 * @param <B> Type of concrete manager (built object), needed for fluent methods.
 */
public interface ISubscriberAdminBuilder<T extends ISubscriberAdminBuilder<T,B>, B extends ISubscriberAdmin<B>> extends IBuilder<T,B>
{
  /**
   * Subscribe to the given subscription on the given topics.
   * 
   * @param subscription The required subscription details.
   * 
   * @return this (fluent method)
   */
  T withSubscription(ITopicSubscriptionAdmin subscription);
  
  /**
   * Delete the given subscription on the given topics.
   * 
   * This method allows for the deletion of subscriptions which are no longer required. It causes the subscription
   * to be deleted during both deploy and undeploy operations. If currently running instances of the service are
   * actively consuming from this subscription then they will experience errors. For this reason, subscriptions
   * should normally be phased out in two stages (assuming a no downtime deployment is required). In the first
   * stage the service would remove the withSubscription() call, once that version is deployed a subsequent
   * deployment can be made including a call to this method, which will cause the subscription to be deleted.
   * 
   * This does mean that messages will back up on the subscription during the period between the two releases
   * which may cause montioring systems to alert.
   * 
   * @param subscription    The subscription to be deleted.
   * 
   * @return  this (fluent method)
   */
  T withObsoleteSubscription(ITopicSubscriptionAdmin subscription);
}
