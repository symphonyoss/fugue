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

package org.symphonyoss.s2.fugue.google.pubsub;

import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

public class GoogleSubscriber implements MessageReceiver
{
  private final GoogleSubscriberManager manager_;
  private final ITraceContextFactory                             traceFactory_;
  private final IThreadSafeRetryableConsumer<ImmutableByteArray> consumer_;

  public GoogleSubscriber(GoogleSubscriberManager manager, ITraceContextFactory traceFactory, IThreadSafeRetryableConsumer<ImmutableByteArray> consumer)
  {
    manager_ = manager;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
  }

  @Override
  public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer)
  {
    try
    {
      ITraceContext trace = traceFactory_.createTransaction(PubsubMessage.class.getName(), message.getMessageId());

      ByteString bytes = message.getData();
      String string = bytes.toStringUtf8();
      
      ImmutableByteArray byteArray = ImmutableByteArray.newInstance(message.getData());
      
      long retryTime = manager_.handleMessage(consumer_, byteArray, trace);
      
      if(retryTime < 0)
      {
        trace.trace("ABOUT_TO_ACK");
        consumer.ack();
      }
      else
      {
        // TODO: do we need to do this or is it better to do nothing so the ack timeout exceeds,
        // given that the async library does extension of ack deadlines it's unclear
        consumer.nack();
      }
      
      trace.finished();
    }
    catch (RuntimeException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
