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

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.lambda.JsonLambdaRequest;

import com.fasterxml.jackson.databind.JsonNode;

/*
{
  "body":"{\"name\":\"value\"}",
  "headers":{
    "{"headerName"":"\"headerValue\"}"
  },
  "httpMethod":"POST",
  "isBase64Encoded":false,
  "multiValueHeaders":{
    "{"headerName"":[
      "\"headerValue\"}"
    ]
  },
  "multiValueQueryStringParameters":{
    "bruce":[
      "skingle"
    ],
    "matt":[
      "harper",
      "skingle"
    ]
  },
  "path":"/hello",
  "queryStringParameters":{
    "bruce":"skingle",
    "matt":"skingle"
  },
  "requestContext":{
    "accountId":"189141687483",
    "apiId":"q0tgk0oqxa",
    "domainName":"testPrefix.testDomainName",
    "domainPrefix":"testPrefix",
    "extendedRequestId":"cgfXZE6TIAMFsQA=",
    "httpMethod":"POST",
    "identity":{
      "accessKey":"ASIASYCNZDS554BDU77T",
      "accountId":"189141687483",
      "apiKey":"test-invoke-api-key",
      "apiKeyId":"test-invoke-api-key-id",
      "caller":"AROASYCNZDS5VH5M63V3D:bruce.skingle@symphony.com",
      "sourceIp":"test-invoke-source-ip",
      "user":"AROASYCNZDS5VH5M63V3D:bruce.skingle@symphony.com",
      "userAgent":"aws-internal/3 aws-sdk-java/1.11.563 Linux/4.9.137-0.1.ac.218.74.329.metal1.x86_64 OpenJDK_64-Bit_Server_VM/25.212-b03 java/1.8.0_212 vendor/Oracle_Corporation",
      "userArn":"arn:aws:sts::189141687483:assumed-role/Sym-SSO-DUO-Dev-Standard-Role/bruce.skingle@symphony.com"
    },
    "path":"/hello",
    "requestId":"bf5dc7b2-a184-11e9-a61c-e90ba41b088c",
    "resourceId":"va69eo",
    "resourcePath":"/hello",
    "stage":"test-invoke-stage"
  },
  "resource":"/hello"
}
*/


public class AwsLambdaRequest extends JsonLambdaRequest
{
  private final Map<String, String> queryParams_;
  private final Map<String, String> pathParams_;
  private final Map<String, String> requestHeaders_;
  private final ImmutableByteArray  body_;
  private final Map<String, String>       stageVariables_;
  private String                    awsRequestId_ = "UNKNOWN-" + UUID.randomUUID().toString();
  private long                      awsRequestEpoch_;
  private final String                    httpMethod_;
  private final String                    path_;
  
  public AwsLambdaRequest(InputStream inputStream)
  {
    super(inputStream);
    
    queryParams_ = mapAdaptor(getJson().get("queryStringParameters"));
    pathParams_ = mapAdaptor(getJson().get("pathParameters"));
    requestHeaders_ = mapAdaptor(getJson().get("headers"));
    stageVariables_ = mapAdaptor(getJson().get("stageVariables"));
    
    JsonNode context = getJson().get("requestContext");
    
    if(context != null)
    {
      JsonNode requestTime = context.get("requestTimeEpoch");
      
      if(requestTime != null && requestTime.canConvertToLong())
        awsRequestEpoch_ = requestTime.asLong();
      
      JsonNode requestId = context.get("requestId");
      
      if(requestId != null)
        awsRequestId_ = requestId.asText();
    }
    
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
    
    httpMethod_ = getString("httpMethod");
    
    JsonNode pathParameters = getJson().get("pathParameters");
    
    if(pathParameters != null)
    {
      JsonNode proxy = pathParameters.get("proxy");
      
      if(proxy != null)
        path_ = "/" + proxy.asText();
      else
        path_ = getString("path");
    }
    else
    {
      path_ = getString("path");
    }
  }

  public String getAwsRequestId()
  {
    return awsRequestId_;
  }

  public long getAwsRequestEpoch()
  {
    return awsRequestEpoch_;
  }

  @Override
  public String getParameter(String name)
  {
    return queryParams_.get(name);
  }

  @Override
  public Map<String, String> getPathParams()
  {
    return pathParams_;
  }

  @Override
  public String getHeader(String name)
  {
    return requestHeaders_.get(name);
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

  public String getHttpMethod()
  {
    return httpMethod_;
  }

  public BufferedReader getReader()
  {
    return new BufferedReader(body_.getReader());
  }

  public String getPath()
  {
    return path_;
  }
}
