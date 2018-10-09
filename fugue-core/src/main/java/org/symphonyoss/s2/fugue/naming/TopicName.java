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

package org.symphonyoss.s2.fugue.naming;

/**
 * The name of a topic.
 * 
 * All topics are owned by some service which is responsible for creating and deleting the topic.
 * 
 * @author Bruce Skingle
 *
 */
public class TopicName extends Name
{
  private final String serviceId_;
  private final String topicId_;
  private final boolean isLocal_;

  /**
   * Constructor.
   * 
   * It is intended that names are constructed by a name factory, if you are calling this from application code
   * you should probably be using a name factory instead.
   * 
   * @param serviceId   ID of service which owns this topic.
   * @param isLocal     True if the topic is owned by the current service. Controls creation and deletion.
   * @param topicId     The topic ID (simple name)
   * @param name        The first element of the full topic name.
   * @param additional  Zero or more additional name elements.
   */
  public TopicName(String serviceId, boolean isLocal, String topicId, String name, String ...additional)
  {
    super(name, additional);
    
    serviceId_ = serviceId;
    isLocal_ = isLocal;
    topicId_ = topicId;
  }

//  /**
//   * Constructor for SBE topics.
//   * 
//   * @param tenantId  Actually a pod name.
//   * @param topicId   Topic ID (SBE name).
//   */
//  public TopicName(String tenantId, String topicId)
//  {
//    super(tenantId, topicId);
//    
//    serviceId_ = "sbe";
//    isLocal_ = false;
//    topicId_ = topicId;
//  }

  /**
   * 
   * @return The topicId (simple name).
   */
  public String getTopicId()
  {
    return topicId_;
  }

  /**
   * 
   * @return The ID of the service which owns the topic.
   */
  public String getServiceId()
  {
    return serviceId_;
  }

  /**
   * 
   * @return True iff the topic is owned by the current service.
   */
  public boolean isLocal()
  {
    return isLocal_;
  }
}
