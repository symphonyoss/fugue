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

package org.symphonyoss.s2.fugue.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.symphonyoss.s2.common.dom.json.jackson.ReadOnlyMapAdaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JsonLambdaRequest extends LambdaRequest
{
  private static final Map<String, String> EMPTY_MAP = new HashMap<>();
  private static final ObjectMapper        MAPPER    = new ObjectMapper();
  
  private final ObjectNode json_;
  
  public JsonLambdaRequest(InputStream inputStream)
  {
    try
    {
      json_ = getJsonObject(MAPPER.readTree(inputStream), null);
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  public ObjectNode getJson()
  {
    return json_;
  }
  
  public String getString(String name)
  {
    return getString(json_, name);
  }

  public String getString(ObjectNode json, String name)
  {
    JsonNode node = json.get(name);
    
    if(node == null)
      return null;
    
    if(node.isValueNode())
      return node.asText();
    
    try
    {
      return MAPPER.writeValueAsString(node);
    }
    catch (JsonProcessingException e)
    {
      throw new IllegalStateException("Invalid JSON exception", e);
    }
  }

  protected Map<String, String> mapAdaptor(JsonNode jsonNode)
  {
    if(jsonNode != null && jsonNode.isObject())
      return new ReadOnlyMapAdaptor((ObjectNode) jsonNode);
    
    return EMPTY_MAP;
  }

  private @Nullable ObjectNode getJsonObject(JsonNode tree, @Nullable String name)
  {
    JsonNode node = name == null ? tree : tree.get(name);
    
    if(node == null)
      return null;
    
    if(node.isObject())
      return (ObjectNode) node;
    
    if(name == null)
      throw new IllegalArgumentException("JSON object expected");
    
    throw new IllegalArgumentException("\"" + name + "\": JSON object expected");
  }
}
