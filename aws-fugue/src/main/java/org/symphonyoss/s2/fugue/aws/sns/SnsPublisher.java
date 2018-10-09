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

package org.symphonyoss.s2.fugue.aws.sns;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;


class SnsPublisher implements IPublisher<String>
{
  private final String              topicArn_;
  private final SnsPublisherManager manager_;

  SnsPublisher(String topicArn, SnsPublisherManager manager)
  {
    topicArn_ = topicArn;
    manager_ = manager;
  }

  @Override
  public synchronized void consume(String item, ITraceContext trace)
  {
    manager_.send(topicArn_, item);
  }

  @Override
  public void close()
  {
  }

  @Override
  public int getMaximumMessageSize()
  {
    return SnsPublisherManager.MAX_MESSAGE_SIZE;
  }
}
