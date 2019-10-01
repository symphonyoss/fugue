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

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A queue manager.
 * 
 * @author Bruce Skingle
 */
public interface IQueueManager
{
  /**
   * Delete the given queue.
   * 
   * @param queueName The name of the queue to be deleted.
   * @param dryRun    If true then log actions which would be performed but don't actually do anything.
   */
  void deleteQueue(String queueName, boolean dryRun);

  /**
   * Create a queue.
   * 
   * @param queueName The name of the queue to be created.
   * @param tags      Tags to be applied to the queue.
   * @param dryRun    If true then log actions which would be performed but don't actually do anything.
   * 
   * @return The queue event source name.
   */
  String createQueue(String queueName, @Nullable Map<String, String> tags, boolean dryRun);

  /**
   * Return a sender for the given queue.
   * 
   * @param queueName The name of the queue to which messages will be sent.
   * 
   * @return A sender for the given queue.
   */
  IQueueSender getSender(String queueName);

  /**
   * Return a receiver for the given queue.
   * 
   * @param queueName The name of the queue from which messages will be received.
   * 
   * @return A receiver for the given queue.
   */
  IQueueReceiver getReceiver(String queueName);

  /**
   * Return the maximum allowed size of a message in bytes.
   * 
   * @return The maximum allowed size of a message in bytes.
   */
  int getMaximumMessageSize();
}
