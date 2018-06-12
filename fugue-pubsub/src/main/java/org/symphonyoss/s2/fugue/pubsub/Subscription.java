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
import java.util.List;

import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

public class Subscription<P>
{
  private final IThreadSafeRetryableConsumer<P> consumer_;
  private final List<String>                    topicNames_;
  private final String                          subscriptionName_;

  public Subscription(List<String> topicNames, String subscriptionName, IThreadSafeRetryableConsumer<P> consumer)
  {
    consumer_ = consumer;
    topicNames_ = topicNames;
    subscriptionName_ = subscriptionName;
  }
  
  public Subscription(String topicName, String subscriptionName, IThreadSafeRetryableConsumer<P> consumer)
  {
    consumer_ = consumer;
    topicNames_ = new ArrayList<>(1);
    topicNames_.add(topicName);
    subscriptionName_ = subscriptionName;
  }

  public Collection<String> getTopicNames()
  {
    return topicNames_;
  }

  public String getSubscriptionName()
  {
    return subscriptionName_;
  }

  public IThreadSafeRetryableConsumer<P> getConsumer()
  {
    return consumer_;
  }
}
