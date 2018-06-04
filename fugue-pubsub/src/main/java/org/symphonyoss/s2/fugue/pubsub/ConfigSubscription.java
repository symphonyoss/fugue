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

import java.util.Collection;
import java.util.List;

import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

/**
 * A subscriber whose parameters are defined in configuration.
 * 
 * @author Bruce Skingle
 *
 * @param <P> The type of the payload.
 */
public class ConfigSubscription<P> extends  Subscription<P>
{
  private String topicListConfigName_;
  private String subscriptionConfigName_;
  private List<String> topicNames_;
  private String subscriptionName_;
  
  /**
   * Construct a subscriber whose parameters are defined in configuration.
   * 
   * @param topicListConfigName     The name of a key in the given configuration which contains the name of one or more topics
   * @param subscriptionConfigName  The name of a key in the given configuration which contains the name of the subscription
   * @param consumer                A consumer for received messages.
   */
  public ConfigSubscription(
      String topicListConfigName, String subscriptionConfigName,
      IThreadSafeRetryableConsumer<P> consumer)
  {
    super(consumer);
    topicListConfigName_ = topicListConfigName;
    subscriptionConfigName_ = subscriptionConfigName;
  }

  @Override
  public void resolve(IConfigurationProvider config)
  {
    topicNames_ = config.getRequiredStringArray(topicListConfigName_);
    subscriptionName_ = config.getRequiredString(subscriptionConfigName_);
  }

  @Override
  public Collection<String> getTopicNames()
  {
    return topicNames_;
  }

  @Override
  public String getSubscriptionName()
  {
    return subscriptionName_;
  }

}
