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

/**
 * A pub/sub publisher manager.
 * 
 * @author Bruce Skingle
 *
 * @param <P> The type of the payload published.
 */
public interface IPublisherManager<P>
{
  /**
   * Get the IPublisher for the given named topic.
   * 
   * @param topicId The actual name of a topic.
   * 
   * @return The publisher for the required topic.
   */
  IPublisher<P> getPublisherByName(String topicId);

  /**
   * Get the IPublisher for the given named topic.
   * 
   * @param serviceId The ID of the service which owns the topic.
   * @param topicId   The actual name of a topic.
   * 
   * @return The publisher for the required topic.
   */
  IPublisher<P> getPublisherByName(String serviceId, String topicId);

  /**
   * Return the publisher for trace events.
   * 
   * @return the publisher for trace events.
   */
  IPublisher<P> getTracePublisher();
  
  /**
   * Return the maximum allowed size of a message in bytes.
   * 
   * @return The maximum allowed size of a message in bytes.
   */
  int getMaximumMessageSize();
}
