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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;

public class HttpServerBuilder implements IServletContainer
{
  private Map<String, Servlet> servletMap_       = new HashMap<>();
  private List<Handler>        resourceHandlers_ = new ArrayList<>();
  private List<Filter>         filters_          = new ArrayList<>();
  private int                  httpPort_         = -1;
  private int                  httpsPort_        = -1;
  private String               keyStorePath_;
  private String               keyStorePassword_;
  
  
  @Override
  public HttpServerBuilder addServlet(String path, Servlet servlet)
  {
    if(servletMap_.containsKey(path))
      throw new ProgramFault("Path \"" + path + "\" is already mapped.");
    
    servletMap_.put(path, servlet);
    
    return this;
  }
  
  public HttpServerBuilder withResources(IResourceProvider provider)
  {
    resourceHandlers_.add(new S2ResourceHandler(provider));
    
    return this;
  }
  
  @Override
  public HttpServerBuilder addServlet(IUrlPathServlet servlet)
  {
    if(servletMap_.containsKey(servlet.getUrlPath()))
      throw new ProgramFault("Path \"" + servlet.getUrlPath() + "\" is already mapped.");
    
    servletMap_.put(servlet.getUrlPath(), servlet);
    
    return this;
  }
  
  public HttpServerBuilder setHttpPort(int httpPort)
  {
    httpPort_ = httpPort;
    
    return this;
  }
  
  public HttpServerBuilder setHttpsPort(int httpsPort)
  {
    httpsPort_ = httpsPort;
    
    return this;
  }
  
  public HttpServerBuilder setKeyStorePath(String keyStorePath)
  {
    keyStorePath_ = keyStorePath;
    
    return this;
  }

  public HttpServerBuilder setKeyStorePassword(String keyStorePassword)
  {
    keyStorePassword_ = keyStorePassword;
    
    return this;
  }

  public HttpServer build()
  {
    if(httpPort_ == -1 && httpsPort_ == -1)
      throw new ProgramFault("One of httpPort and httpsPort must be set");
    
    //S2HttpServer server = httpPort_ == -1 ? new S2HttpServer() : new S2HttpServer(httpPort_);
    HttpServer server = new HttpServer();

    
    // HTTP Configuration
    // HttpConfiguration is a collection of configuration information
    // appropriate for http and https. The default scheme for http is
    // <code>http</code> of course, as the default for secured http is
    // <code>https</code> but we show setting the scheme to show it can be
    // done. The port for secured communication is also set here.
    HttpConfiguration http_config = new HttpConfiguration();
    http_config.setSecureScheme("https");
//    http_config.setSecurePort(httpsPort_);
    http_config.setOutputBufferSize(32768);

    http_config.setSendServerVersion( false );

    // HTTP connector
    // The first server connector we create is the one for http, passing in
    // the http configuration we configured above so it can get things like
    // the output buffer size, etc. We also set the port (8080) and
    // configure an idle timeout.
    ServerConnector http = null;
    
    if(httpPort_ != -1)
    {
      http = new ServerConnector(server.getJettyServer(),
          new HttpConnectionFactory(http_config));
      http.setPort(httpPort_);
      http.setIdleTimeout(30000);
    }
    
    ServerConnector https = null;
    
    if(httpsPort_ != -1)
    {
      // SSL Context Factory for HTTPS
      // SSL requires a certificate so we configure a factory for ssl contents
      // with information pointing to what keystore the ssl connection needs
      // to know about. Much more configuration is available the ssl context,
      // including things like choosing the particular certificate out of a
      // keystore to be used.
      SslContextFactory sslContextFactory = new SslContextFactory();
      
      if(keyStorePath_ == null || keyStorePassword_ == null)
        throw new ProgramFault("When httpsPort is set keyStorePath and keyStorePassword must also be set.");
      
      sslContextFactory.setKeyStorePath(keyStorePath_);
      sslContextFactory.setKeyStorePassword(keyStorePassword_);
  
      // FIXME: we need the X509TrustManager
      sslContextFactory.setTrustAll(true);
  
      // HTTPS Configuration
      // A new HttpConfiguration object is needed for the next connector and
      // you can pass the old one as an argument to effectively clone the
      // contents. On this HttpConfiguration object we add a
      // SecureRequestCustomizer which is how a new connector is able to
      // resolve the https connection before handing control over to the Jetty
      // Server.
      HttpConfiguration https_config = new HttpConfiguration(http_config);
      SecureRequestCustomizer src = new SecureRequestCustomizer();
  //          src.setStsMaxAge(2000);
  //          src.setStsIncludeSubDomains(true);
      https_config.addCustomizer(src);
  
      // HTTPS connector
      // We create a second ServerConnector, passing in the http configuration
      // we just made along with the previously created ssl context factory.
      // Next we set the port and a longer idle timeout.
      https = new ServerConnector(server.getJettyServer(),
          new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
          new HttpConnectionFactory(https_config));
      https.setPort(httpsPort_);
      https.setIdleTimeout(300000);
    }
    
    // Set the connectors
    if(http == null)
    {
      server.getJettyServer().setConnectors(new Connector[] {https});
    }
    else if(https == null)
    {
      server.getJettyServer().setConnectors(new Connector[] {http});
    }
    else
    {
      server.getJettyServer().setConnectors(new Connector[] {https, http});
    }
    
    // Create a ContextHandlerCollection and set the context handlers to it.
    // This will let jetty process urls against the declared contexts in
    // order to match up content.
    ServletContextHandler sch = server.getServletContextHandler();
    int servletCnt = 0;

    for (Entry<String, Servlet> entry : servletMap_.entrySet())
    {
      sch.addServlet(new ServletHolder(entry.getValue()),entry.getKey());
      servletCnt++;
    }
    
    EnumSet<DispatcherType> filterDispatches = EnumSet.of(DispatcherType.REQUEST);
    
    HandlerList handlers = new HandlerList();
    
        
    for(Handler handler : resourceHandlers_)
    {
      handlers.addHandler(handler);
    }
    
    if(servletCnt>0)
    {
      for(Filter filter : filters_)
        sch.addFilter(new FilterHolder(filter), "/*", filterDispatches);
      
      handlers.addHandler(sch);
    }

    server.getJettyServer().setHandler(handlers);

    return server;
  }

  public void addFilter(Filter filter)
  {
    filters_.add(filter);
  }
}
