/*
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

package org.symphonyoss.s2.fugue.inmemory.pubsub;

import java.util.Map;

import org.junit.Test;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContext;
import org.symphonyoss.s2.fugue.inmemory.pubsub.InMemoryPublisherManager;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.NameFactory;
import org.symphonyoss.s2.fugue.pubsub.IPubSubMessage;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

/**
 * Unit test for InMemoryPubSub
 * 
 * @author Bruce Skingle
 *
 */
public class TestInMemoryPubSub
{
  private static final String TEST_TOPIC = "TestTopic";
  
  private INameFactory nameFactory_ = new NameFactory("sym-s2-", "test", "s2test1", "realm", "us-east-1", "testTenant", "testService");

  /**
   * Test publishing.
   */
  @Test
  public void testPublish()
  {
    InMemoryPublisherManager publisherManager = new InMemoryPublisherManager.Builder()
        .withNameFactory(nameFactory_)
        .withTopic(TEST_TOPIC)
        .build();
    
    IPublisher publisher = publisherManager.getPublisherByName(TEST_TOPIC);
    
    ITraceContext trace = NoOpTraceContext.INSTANCE;
    
    IPubSubMessage item = new IPubSubMessage()
    {
      @Override
      public String getPayload()
      {
        return "Payload";
      }

      @Override
      public Map<String, String> getAttributes()
      {
        return null;
      }

      @Override
      public ITraceContext getTraceContext()
      {
        return trace;
      }
    };
    
    publisher.consume(item);
  }
}

  
