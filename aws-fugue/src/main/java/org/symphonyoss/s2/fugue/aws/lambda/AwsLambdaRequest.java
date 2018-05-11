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

package org.symphonyoss.s2.fugue.aws.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.lambda.JsonLambdaRequest;

import com.fasterxml.jackson.databind.JsonNode;

public class AwsLambdaRequest extends JsonLambdaRequest
{
  private final Map<String, String> queryParams_;
  private final Map<String, String> pathParams_;
  private final Map<String, String> requestHeaders_;
  private final ImmutableByteArray  body_;
  private Map<String, String> stageVariables_;
  
  public AwsLambdaRequest(InputStream inputStream)
  {
    super(inputStream);
    
    queryParams_ = mapAdaptor(getJson().get("queryStringParameters"));
    pathParams_ = mapAdaptor(getJson().get("pathParameters"));
    requestHeaders_ = mapAdaptor(getJson().get("headers"));
    stageVariables_ = mapAdaptor(getJson().get("stageVariables"));
    
    String body = getString("body");
    
    if(body == null)
    {
      body_ = ImmutableByteArray.EMPTY;
    }
    else
    {
      JsonNode isEncoded = getJson().get("isBase64Encoded");
      
      if(isEncoded != null && isEncoded.asBoolean())
      {
        body_ = ImmutableByteArray.newInstance(Base64.decodeBase64(body));
      }
      else
      { 
        body_ = ImmutableByteArray.newInstance(body);
      }
    }
  }

  @Override
  public Map<String, String> getQueryParams()
  {
    return queryParams_;
  }

  @Override
  public Map<String, String> getPathParams()
  {
    return pathParams_;
  }

  @Override
  public Map<String, String> getRequestHeaders()
  {
    return requestHeaders_;
  }

  @Override
  public Map<String, String> getStageVariables()
  {
    return stageVariables_;
  }

  @Override
  public ImmutableByteArray getBody()
  {
    return body_;
  }
}
