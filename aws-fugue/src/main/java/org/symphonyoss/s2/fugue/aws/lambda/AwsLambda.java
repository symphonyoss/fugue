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
import java.io.OutputStream;

import org.symphonyoss.s2.fugue.lambda.LambdaResponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public abstract class AwsLambda implements RequestStreamHandler
{


  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
  {
    LambdaResponse  response = doHandle(inputStream);

    try
    {
      response.write(outputStream);
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private LambdaResponse doHandle(InputStream inputStream)
  {
    try
    {
      AwsLambdaRequest request = new AwsLambdaRequest(inputStream);
      
      return handle(request);
      
//      String body = getBody(event);
      
      
      
      

//      if (event.get("body") != null)
//      {
//        JSONObject body = (JSONObject) parser.parse((String) event.get("body"));
//        if (body.get("time") != null)
//        {
//          time = (String) body.get("time");
//        }
//      }
//
//      String greeting = "Good " + time + ", " + name + " of " + city + ". ";
//      if (day != null && day != "")
//        greeting += "Happy " + day + "!";
//
//      JSONObject responseBody = new JSONObject();
//      responseBody.put("input", event.toJSONString());
//      responseBody.put("message", greeting);
//
//      JSONObject headerJson = new JSONObject();
//      headerJson.put("x-custom-header", "my custom header value");
//
//      responseJson.put("isBase64Encoded", false);
//      responseJson.put("statusCode", responseCode);
//      responseJson.put("headers", headerJson);
//      responseJson.put("body", responseBody.toString());
    }
    catch (IllegalArgumentException e)
    {
      return new AwsLambdaResponse(400, e);
    }
    catch (RuntimeException e)
    {
      return new AwsLambdaResponse(500, e);
    }
  }

  protected abstract LambdaResponse handle(AwsLambdaRequest request);

  

//  private String getBody(ObjectNode event)
//  {
//    JsonNode node = event.get("body");
//    
//    if(node == null)
//      return "";
//    
//    String body = node.asText();
//    
//    node = event.get("isBase64Encoded");
//    
//    if(node != null && node.asBoolean())
//    {
//      body = Base64.decodeBase64(body);
//    }
//  }

  

}
