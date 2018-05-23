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

package org.symphonyoss.s2.fugue.aws.snssqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;


class SNSPublisher implements IPublisher<String>
{
  private static final Logger          log_ = LoggerFactory.getLogger(SNSPublisher.class);

  private final SNSPublisherManager manager_;
  private String                       topicName_;

  SNSPublisher(SNSPublisherManager manager)
  {
    manager_ = manager;
  }

  @Override
  public synchronized void consume(String item, ITraceContext trace)
  {
    if(topicName_ == null)
      throw new IllegalStateException("Publisher is not started");
    
    try
    {
      manager_.send(topicName_, item);
    }
    catch (Exception e)
    {
      throw new TransactionFault(e);
    }
  }

  

  @Override
  public void close()
  {
  }

  void startByName(String topicName)
  {
    log_.info("Starting publisher for topic " + topicName + "...");
    topicName_ = topicName;
  }

  @Override
  public int getMaximumMessageSize()
  {
    return SNSPublisherManager.MAX_MESSAGE_SIZE;
  }
}
