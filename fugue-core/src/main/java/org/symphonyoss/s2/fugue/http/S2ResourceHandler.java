/*
 *
 *
 * Copyright 2017-2018 Symphony Communication Services, LLC.
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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.symphonyoss.s2.common.fault.CodingFault;

public class S2ResourceHandler extends ResourceHandler
{
  private final IResourceProvider resourceProvider_;
  
  private Map<String, Resource>  resourceMap_ = new HashMap<>();

  public S2ResourceHandler(IResourceProvider resourceProvider)
  {
    resourceProvider_ = resourceProvider;
  }

  @Override
  public Resource getResource(String path)
  {
    if(resourceMap_.containsKey(path))
    {
      return resourceMap_.get(path);
    }
    
    URL url = resourceProvider_.getResourceUrl(path);
    
    if(url == null)
    {
      resourceMap_.put(path, null);
      
      return null;
    }
    
    try
    {
      Resource  result = Resource.newResource(url.toURI());
      
      resourceMap_.put(path, result);
      
      return result;
    }
    catch (URISyntaxException | MalformedURLException e)
    {
      throw new CodingFault("Impossible error", e);
    }
  }
}
