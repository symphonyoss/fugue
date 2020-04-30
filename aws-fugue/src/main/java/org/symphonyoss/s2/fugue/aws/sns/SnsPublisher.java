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
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.IPubSubMessage;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;


class SnsPublisher implements IPublisher
{
  private final String              topicName_;
  private final String              topicArn_;
  private final SnsPublisherBase<?> manager_;

  SnsPublisher(TopicName topicName, String topicArn, SnsPublisherBase<?> manager)
  {
    topicName_ = topicName.toString();
    topicArn_ = topicArn;
    manager_ = manager;
  }

  @Override
  public void consume(IPubSubMessage item, ITraceContext trace)
  {
    manager_.send(topicName_, topicArn_, item, trace);
  }

  @Override
  public void consume(IPubSubMessage item)
  {
    manager_.send(topicName_, topicArn_, item, item.getTraceContext());
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

  @Override
  public int getBillableMessageSize()
  {
    return SnsPublisherManager.BILLABLE_MESSAGE_SIZE;
  }
}
