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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.FluentBuilder;
import org.symphonyoss.s2.common.fluent.IFluent;
import org.symphonyoss.s2.common.fluent.IFluentBuilder;
import org.symphonyoss.s2.fugue.FugueLifecycleComponent;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;

/**
 * Base class for publisher managers.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public abstract class AbstractPublisherManager<T extends AbstractPublisherManager<T>>
  extends FugueLifecycleComponent<T>
  implements IPublisherManager
{
  protected final INameFactory   nameFactory_;
  
  protected AbstractPublisherManager(Builder<?,T> builder)
  {
    super(builder.getBuiltType());
    
    nameFactory_  = builder.nameFactory_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   * @param <T>   The concrete type returned by fluent methods.
   * @param <B>   The concrete type of the built object.
   */
  public static abstract class Builder<T extends IFluentBuilder<T,B>, B extends IFluent<B>> extends FluentBuilder<T,B>
  {
    private final List<String>              topicIds_        = new LinkedList<>();
    private final Map<String, List<String>> serviceTopicIds_ = new HashMap<>();

    private INameFactory                    nameFactory_;

    protected Builder(Class<T> type, Class<B> builtType)
    {
      super(type, builtType);
      
    }
    
    public T withNameFactory(INameFactory nameFactory)
    {
      nameFactory_ = nameFactory;
      
      return self();
    }
    
    public abstract T withTopic(TopicName topicName);

    public T withTopics(TopicName ...topics)
    {
      for(TopicName name : topics)
        withTopic(name);
      
      return self();
    }
    
    public T withTopics(String ...topics)
    {
      for(String name : topics)
        topicIds_.add(name);
      
      return self();
    }
    
    public T withTopic(String name)
    {
      topicIds_.add(name);
      
      return self();
    }

    public synchronized T withTopic(String serviceId, String topicId)
    {
      List<String> list = serviceTopicIds_.get(serviceId);
      
      if(list == null)
      {
        list = new LinkedList<>();
        serviceTopicIds_.put(serviceId, list);
      }
      
      list.add(topicId);
      
      return self();
    }

    @Override
    public synchronized void validate(FaultAccumulator faultAccumulator)
    {
      faultAccumulator.checkNotNull(nameFactory_, "nameFactory");
      
      for(String topicId : topicIds_)
        withTopic(nameFactory_.getTopicName(topicId));
      
      for(Entry<String, List<String>> entry : serviceTopicIds_.entrySet())
      {
        for(String topicId : entry.getValue())
        {
          withTopic(nameFactory_.getTopicName(entry.getKey(), topicId));
        }
      }
    }
  }
  
  @Override
  public IPublisher getPublisherByName(String topicId)
  {
    return getPublisherByName(nameFactory_.getTopicName(topicId));
  }

  @Override
  public IPublisher getPublisherByName(String serviceId, String topicId)
  {
    return getPublisherByName(nameFactory_.getTopicName(serviceId, topicId));
  }
}
