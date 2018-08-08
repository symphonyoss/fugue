/*
 *
 *
 * Copyright 2017 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The SSF licenses this file
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

package org.symphonyoss.s2.fugue.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

public class TestHttpServer
{
  private static final String HELLO_WORLD = "Hello World!";

  @Test
  public void testEmptyServer() throws IOException
  {
    HttpServerBuilder builder = new HttpServerBuilder()
        .setHttpPort(0)
        .addServlet(new HelloServlet());
    
    HttpServer server = builder.build();
    
    server.start();
    
    int port = server.getLocalPort();
    
    URL url = new URL("http://localhost.symphony.com:"+ port + "/hello");
    
    try(InputStream in = url.openStream())
    {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      byte[]  buf = new byte[1024];
      int     nbytes;
      
      while((nbytes = in.read(buf))>0)
        bout.write(buf, 0, nbytes);
      
      Assert.assertEquals(HELLO_WORLD, bout.toString());
    }
    finally
    {
      server.stop();
    }
  }


  class HelloServlet extends HttpServlet implements IUrlPathServlet
  {
    private static final long serialVersionUID = 1L;
  
    @Override
    public String getUrlPath()
    {
      return "/hello";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
      resp.getWriter().print(HELLO_WORLD);
    }
    
  }
}