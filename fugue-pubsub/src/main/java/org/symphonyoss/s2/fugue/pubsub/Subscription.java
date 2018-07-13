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

import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

/**
 * A subscription.
 * 
 * @author Bruce Skingle
 *
 * @param <P> The type of message produced by this subscription.
 */
public class Subscription<P>
{
  private final IThreadSafeRetryableConsumer<P> consumer_;
  private final List<String>                    topicNames_;
  private final String                          subscriptionName_;

  /**
   * Constructor.
   * 
   * @param topicNames        One or more topics on which to subscribe.
   * @param subscriptionName  The simple subscription name.
   * @param consumer          A consumer for received messages.
   */
  public Subscription(List<String> topicNames, String subscriptionName, @Nullable IThreadSafeRetryableConsumer<P> consumer)
  {
    consumer_ = consumer;
    topicNames_ = topicNames;
    subscriptionName_ = subscriptionName;
  }

  /**
   * 
   * @return The list of topic names.
   */
  public Collection<String> getTopicNames()
  {
    return topicNames_;
  }

  /**
   * 
   * @return The simple subscription name.
   */
  public String getSubscriptionName()
  {
    return subscriptionName_;
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
