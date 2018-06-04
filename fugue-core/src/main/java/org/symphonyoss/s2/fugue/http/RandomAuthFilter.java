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

package org.symphonyoss.s2.fugue.http;

import org.eclipse.jetty.servlet.Source;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RandomAuthFilter implements Filter
{
  public static final String LOGIN_TOKEN = "LOGIN_TOKEN";
  public static final String SESSION_TOKEN = "SESSION_TOKEN";
  
  private String authToken_;
  private String sessionToken_;

  public RandomAuthFilter()
  {
    authToken_ = UUID.randomUUID().toString();
    sessionToken_ = UUID.randomUUID().toString();
  }
  
  
  public String getAuthToken()
  {
    return authToken_;
  }


  @Override
  public void init(FilterConfig filterConfig) throws ServletException
  {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    HttpServletRequest  req = (HttpServletRequest) request;
    HttpServletResponse  resp = (HttpServletResponse) response;
    
    Cookie[] cookies = req.getCookies();
    
    if(cookies != null)
    {
      for(Cookie cookie : cookies)
      {
        if(SESSION_TOKEN.equals(cookie.getName()))
        {
          if(sessionToken_.equals(cookie.getValue()))
          {
            chain.doFilter(request, response);
            
            return;
          }
        }
      }
    }
    
    if(authToken_ != null)
    {
      String loginToken = request.getParameter(LOGIN_TOKEN);
      
      if(authToken_.equals(loginToken))
      {
        authToken_ = null;
        
        Cookie cookie = new Cookie(SESSION_TOKEN, sessionToken_);
        resp.addCookie(cookie);
        chain.doFilter(request, response);
      }
    }
  
    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
  }

  @Override
  public void destroy()
  {
  }

}
