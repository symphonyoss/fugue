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

import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

/**
 * A subscription on a Topic.
 * 
 * @author Bruce Skingle
 *
 * @param <P> The type of message produced by this subscription.
 */
public class TopicSubscription<P> implements ISubscription<P>
{
  private final INameFactory                    nameFactory_;
  private final IThreadSafeRetryableConsumer<P> consumer_;
  private final Collection<TopicName>           topicNames_;
  private final String                          subscriptionId_;

  /**
   * Constructor.
   * 
   * @param nameFactory       A name factory.
   * @param topicNames        One or more topics on which to subscribe.
   * @param subscriptionName  The simple subscription name.
   * @param consumer          A consumer for received messages.
   */
  public TopicSubscription(INameFactory nameFactory, Collection<TopicName> topicNames, String subscriptionName, @Nullable IThreadSafeRetryableConsumer<P> consumer)
  {
    nameFactory_ = nameFactory;
    consumer_ = consumer;
    topicNames_ = topicNames;
    subscriptionId_ = subscriptionName;
  }

  /**
   * 
   * @return The list of topic names.
   */
  public Collection<TopicName> getTopicNames()
  {
    return topicNames_;
  }

  /**
   * 
   * @return The simple subscription name.
   */
  public String getSubscriptionId()
  {
    return subscriptionId_;
  }

  @Override
  public IThreadSafeRetryableConsumer<P> getConsumer()
  {
    return consumer_;
  }
  
  @Override
  public List<String> getSubscriptionNames()
  {
    List<String> names = new ArrayList<>();
    
    for(TopicName topicName : getTopicNames())
    { 
      names.add(nameFactory_.getSubscriptionName(topicName, getSubscriptionId()).toString());
    }
    
    return names;
  }
}
