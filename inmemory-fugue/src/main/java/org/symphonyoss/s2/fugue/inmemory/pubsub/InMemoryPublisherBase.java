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

package org.symphonyoss.s2.fugue.inmemory.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

/**
 * In memory implementation of PublisherManager.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete manager, needed for fluent methods.
 *
 */
public abstract class InMemoryPublisherBase<T extends InMemoryPublisherBase<T>> extends AbstractPublisherManager<T>
{
  protected static final int                   MAX_MESSAGE_SIZE  = 256 * 1024; // 256K

  protected final Map<TopicName, InMemoryPublisher> publisherNameMap_ = new HashMap<>();
  protected final List<InMemoryPublisher>           publishers_       = new ArrayList<>();

  protected InMemoryPublisherBase(Class<T> type, Builder<?,T> builder)
  {
    super(type, builder);
   
    
    int errorCnt = 0;
    
    for(TopicName topicName : builder.topicNames_)
    {
      if(!validateTopic(topicName))
        errorCnt++;
    }
    
    if(errorCnt > 0)
    {
      throw new IllegalStateException("There are " + errorCnt + " topic validation errors");
    }
    
    for(TopicName topicName : builder.topicNames_)
    {
      publisherNameMap_.put(topicName, new InMemoryPublisher(topicName));
    }
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   * @param <T>   The concrete type returned by fluent methods.
   * @param <B>   The concrete type of the built object.
   */
  public static abstract class Builder<T extends Builder<T,B>, B extends AbstractPublisherManager<B>>
  extends AbstractPublisherManager.Builder<T,B>
  {
    protected final Set<TopicName>         topicNames_ = new HashSet<>();
    
    protected Builder(Class<T> type)
    {
      super(type);
    }
    
    @Override
    public T withTopic(TopicName name)
    {
      topicNames_.add(name);
      
      return self();
    }
  }

  @Override
  public void start()
  {
  }

  /**
   * Validate the given topic name.
   * 
   * @param topicName The name of a topic.
   * 
   * @return true if the topic is valid.
   */
  protected abstract boolean validateTopic(TopicName topicName);

  @Override
  public void stop()
  {
    for(InMemoryPublisher publisher : publishers_)
    {
      publisher.close();
    }
  }

  @Override
  public synchronized IPublisher getPublisherByName(TopicName topicName)
  {
    InMemoryPublisher publisher = publisherNameMap_.get(topicName);
    
    if(publisher == null)
    {
      throw new IllegalArgumentException("Unregistered topic \"" + topicName + "\"");
    }
    
    return publisher;
  }

  @Override
  public int getMaximumMessageSize()
  {
    return MAX_MESSAGE_SIZE;
  }
}
