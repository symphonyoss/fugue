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

import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.TopicName;

/**
 * A topic as represented in the Fugue API.
 * 
 * @author Bruce Skingle
 *
 */
public class Topic
{
  private String             id_;
  private String             obsoleteId_;
  private String             serviceId_;
  
  /**
   * Constructor.
   * 
   * @param id  The topicId (simple name)
   */
  public Topic(String id)
  {
    id_ = id;
  }

  /**
   * Set the obsolete ID for this topic.
   * 
   * If there are no subscriptions to the topic with the obsolete id then it will be deleted.
   * 
   * @param obsoleteId  The obsolete topicId.
   * 
   * @return This (fluent method)
   */
  public Topic withObsoleteId(String obsoleteId)
  {
    obsoleteId_ = obsoleteId;
    
    return this;
  }

  /**
   * Set the service ID for the owner of this topic.
   * 
   * If no serviceId is specified the current service is assumed.
   * The topic owner is responsible for creation and deletion of the topic.
   * 
   * @param serviceId  The id of the service which owns the topic.
   * 
   * @return This (fluent method)
   */
  public Topic withServiceId(String serviceId)
  {
    serviceId_ = serviceId;
    
    return this;
  }

  /**
   * 
   * @return The topic ID (simple name).
   */
  public String getId()
  {
    return id_;
  }

  /**
   * 
   * @return The obsolete topic ID, if any.
   */
  public @Nullable String getObsoleteId()
  {
    return obsoleteId_;
  }

  /**
   * 
   * @return the id of the owning service. 
   */
  public @Nullable String getServiceId()
  {
    return serviceId_;
  }

  /**
   * Create a TopicName for this topic using the given NameFactory.
   * 
   * @param nameFactory The name factory to use (which knows the current environment, service etc)
   * 
   * @return The fully qualified name for the topic.
   */
  public TopicName createTopicName(INameFactory nameFactory)
  {
    if(serviceId_ == null)
      return nameFactory.getTopicName(id_);
    
    return nameFactory.getTopicName(serviceId_, id_);
  }
}
