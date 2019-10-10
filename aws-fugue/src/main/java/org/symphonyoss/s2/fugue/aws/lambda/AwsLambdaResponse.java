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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.symphonyoss.s2.fugue.lambda.JsonLambdaResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AwsLambdaResponse extends JsonLambdaResponse
{
  private static final String RESPONSE_STATUS = "statusCode";
  private static final String RESPONSE_EXCEPTION = "exception";
  private static final String RESPONSE_MESSAGE = "message";
  private static final String RESPONSE_BODY = "body";
  private static final String RESPONSE_HEADERS = "headers";
  private static final String RESPONSE_BASE64 = "isBase64Encoded";
  

  private StringWriter           stringWriter_ = new StringWriter();
  private PrintWriter            writer_ = new PrintWriter(stringWriter_);
  private ByteArrayOutputStream  outputStream_;
  private ObjectNode headers_;

  public AwsLambdaResponse(int statusCode, String message)
  {
    setStatus(statusCode);
    setMessage(message);
}
  
  public AwsLambdaResponse(Throwable cause)
  {
    this(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause);
  }
  
  public AwsLambdaResponse(int statusCode, Throwable cause)
  {
    setStatus(statusCode);
    setMessage(cause.getLocalizedMessage());
  }

  @Override
  public void write(OutputStream outputStream) throws IOException
  {
    if(outputStream_ != null)
    {
      String body = Base64.encodeBase64String(outputStream_.toByteArray());
      
      if(body != null && body.length()>0)
      {
        put(RESPONSE_BODY, body);
        put(RESPONSE_BASE64, true);
      }
    }
    else if(stringWriter_ != null)
    {
      String body = stringWriter_.toString();
      
      if(body != null && body.length()>0)
      {
        put(RESPONSE_BODY, body);
      }
    }
    
    super.write(outputStream);
  }

  public synchronized PrintWriter getWriter() throws IOException
  {
    if(outputStream_ != null)
      throw new IOException("getOutputStream() has already been called.");

    if(writer_ == null)
    {
      stringWriter_ = new StringWriter();
      writer_ = new PrintWriter(stringWriter_);
    }
    
    return writer_;
  }
  
  public synchronized OutputStream getOutputStream() throws IOException
  {
    if(writer_ != null)
      throw new IOException("getWriter() has already been called.");
    
    if(outputStream_ == null)
      outputStream_ = new ByteArrayOutputStream();
    
    return outputStream_;
  }

  public void setStatus(int statusCode)
  {
    put(RESPONSE_STATUS, statusCode);
  }
  
  public void setMessage(String message)
  {
    //put(RESPONSE_MESSAGE, message);
  }
  
  public void setContentType(String contentType)
  {
    setHeader("Content-Type", contentType);
  }
  
  public synchronized void setHeader(String name, String value)
  {
    if(headers_ == null)
    {
      headers_ = putObject(RESPONSE_HEADERS);
    }
    
    headers_.put(name, value);
  }
}
