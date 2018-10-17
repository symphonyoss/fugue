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

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

/**
 * A subscription.
 * 
 * @author Bruce Skingle
 *
 * @param <P> The type of message produced by this subscription.
 */
public class SubscriptionImpl<P>
{
  private final IThreadSafeRetryableConsumer<P> consumer_;
  private final Collection<TopicName>           topicNames_;
  @Deprecated
  private final Collection<TopicName>           obsoleteTopicNames_;
  private final String                          subscriptionId_;
  @Deprecated
  private final String                          obsoleteSubscriptionId_;

  /**
   * Constructor.
   * 
   * @param topicNames        One or more topics on which to subscribe.
   * @param subscriptionName  The simple subscription name.
   * @param consumer          A consumer for received messages.
   */
  public SubscriptionImpl(Collection<TopicName> topicNames, String subscriptionName, @Nullable IThreadSafeRetryableConsumer<P> consumer)
  {
    consumer_ = consumer;
    topicNames_ = topicNames;
    obsoleteTopicNames_ = new ArrayList<>();
    obsoleteSubscriptionId_ = null;
    subscriptionId_ = subscriptionName;
  }

  @Deprecated
  public SubscriptionImpl(Collection<TopicName> topicNames, Collection<TopicName> obsoleteTopicNames,
      String subscriptionId, String obsoleteSubscriptionId, @Nullable IThreadSafeRetryableConsumer<P> consumer)
  {
    consumer_ = consumer;
    topicNames_ = topicNames;
    obsoleteTopicNames_ = obsoleteTopicNames;
    subscriptionId_ = subscriptionId;
    obsoleteSubscriptionId_ = obsoleteSubscriptionId;
  }

  /**
   * 
   * @return The list of topic names.
   */
  public Collection<TopicName> getTopicNames()
  {
    return topicNames_;
  }

  @Deprecated
  public Collection<TopicName> getObsoleteTopicNames()
  {
    return obsoleteTopicNames_;
  }

  /**
   * 
   * @return The simple subscription name.
   */
  public String getSubscriptionId()
  {
    return subscriptionId_;
  }

  @Deprecated
  public String getObsoleteSubscriptionId()
  {
    return obsoleteSubscriptionId_ == null ? subscriptionId_ : obsoleteSubscriptionId_;
  }

  /**
   * 
   * @return The consumer for received messages.
   */
  public IThreadSafeRetryableConsumer<P> getConsumer()
  {
    return consumer_;
  }
}
