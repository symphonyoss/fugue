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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

/**
 * A payload sent or received over a pub sub channel.
 * 
 * @author Bruce Skingle
 */
public class PubSubMessage implements IPubSubMessage
{
  private static final Map<String, String>  EMPTY_ATTRIBUTES = new HashMap<>();
  
  private final String              payload_;
  private final Map<String, String> attributes_;
  private final ITraceContext       traceContext_;
  
  /**
   * Constructor.
   * 
   * @param payload       The payload
   * @param traceContext  A trace context
   * @param payloadType   The payload type attribute
   */
  public PubSubMessage(String payload, ITraceContext traceContext, String payloadType)
  {
    payload_ = payload;
    traceContext_ = traceContext;
    attributes_ = new HashMap<>();
    
    attributes_.put(PAYLOAD_TYPE_ATTRIBUTE, payloadType);
  }
  
  /**
   * Constructor.
   * 
   * @param payload       The payload
   * @param traceContext  A trace context
   * @param payloadType   The payload type attribute
   * @param podId         The pod from which the message originates
   */
  public PubSubMessage(String payload, ITraceContext traceContext, String payloadType, String podId)
  {
    payload_ = payload;
    traceContext_ = traceContext;
    attributes_ = new HashMap<>();
    
    attributes_.put(PAYLOAD_TYPE_ATTRIBUTE, payloadType);
    attributes_.put(POD_ID_ATTRIBUTE, podId);
  }
  
  /**
   * Constructor.
   * 
   * @param payload       The payload
   * @param traceContext  A trace context
   * @param attributes    Message attributes
   */
  public PubSubMessage(String payload, ITraceContext traceContext, Map<String, String> attributes)
  {
    payload_ = payload;
    traceContext_ = traceContext;
    attributes_ = attributes == null ? EMPTY_ATTRIBUTES : attributes;
  }

  /**
   * 
   * @return The message payload
   */
  @Override
  public String getPayload()
  {
    return payload_;
  }

  /**
   * 
   * @return Any optional attributes. If the object was created with null attributes an empty map is returned.
   */
  @Override
  public @Nonnull Map<String, String> getAttributes()
  {
    return attributes_;
  }

  /**
   * 
   * @return The trace context.
   */
  @Override
  public ITraceContext getTraceContext()
  {
    return traceContext_;
  }
}
