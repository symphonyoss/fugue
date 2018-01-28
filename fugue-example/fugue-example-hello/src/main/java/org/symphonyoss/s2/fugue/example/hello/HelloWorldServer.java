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

package org.symphonyoss.s2.fugue.example.hello;

import org.symphonyoss.s2.common.http.IServletContainer;
import org.symphonyoss.s2.common.http.IServletProvider;
import org.symphonyoss.s2.fugue.core.FugueServer;
import org.symphonyoss.s2.fugue.di.IComponent;
import org.symphonyoss.s2.fugue.di.IDIContext;
import org.symphonyoss.s2.fugue.di.impl.ComponentDescriptor;

public class HelloWorldServer extends FugueServer implements IComponent, IServletProvider
{

  public HelloWorldServer(IDIContext diContext, String name)
  {
    super(diContext, name, 8080);
  }

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return super.getComponentDescriptor()
        .addProvidedInterface(IServletProvider.class)
        .addStart(() -> openBrowser());
  }

  @Override
  public void registerServlets(IServletContainer servletContainer)
  {
    servletContainer.addServlet("/", new HelloWorldServlet());
  }

}
