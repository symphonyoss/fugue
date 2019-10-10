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
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JsonLambdaResponse extends LambdaResponse
{
  private static final ObjectMapper        mapper_    = new ObjectMapper();
  
  private final ObjectNode  json_ = mapper_.createObjectNode();

  protected void put(String name, String value)
  {
    json_.put(name, value);
  }
  
  protected void put(String name, int value)
  {
    json_.put(name, value);
  }
  
  protected void put(String name, boolean value)
  {
    json_.put(name, value);
  }
  
  protected ObjectNode putObject(String name)
  {
    return json_.putObject(name);
  }

  @Override
  public void write(OutputStream outputStream) throws IOException
  {
    mapper_.writeValue(outputStream, json_);
  }
}
