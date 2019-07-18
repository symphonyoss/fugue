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

/**
 * Base lambda function implementation.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class AwsLambda implements RequestStreamHandler
{
  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
  {
    LambdaResponse  response = doHandle(inputStream);

    response.write(outputStream);
  }

  private LambdaResponse doHandle(InputStream inputStream)
  {
    try
    {
      AwsLambdaRequest request = new AwsLambdaRequest(inputStream);
      
      return handle(request);
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
}
