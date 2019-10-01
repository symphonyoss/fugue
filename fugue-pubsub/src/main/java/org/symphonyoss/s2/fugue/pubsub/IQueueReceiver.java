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

package org.symphonyoss.s2.fugue.pubsub;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * A component capable of sending messages to a queue.
 * 
 * @author Bruce Skingle
 *
 */
public interface IQueueReceiver
{
  /**
   * Pull messages from the queue.
   * 
   * @param maxMessages Max number of messages to receive.
   * @param ackMessages Receipt handles of messages to ack.
   * @param nakMessages Receipt handles of messages to nak
   * 
   * @return A collection of messages of size 0 - maxMessages.
   */
  @Nonnull Collection<IQueueMessage> receiveMessages(int maxMessages, Set<? extends IQueueMessageAck> ackMessages, Set<? extends IQueueMessageNak> nakMessages);
}
