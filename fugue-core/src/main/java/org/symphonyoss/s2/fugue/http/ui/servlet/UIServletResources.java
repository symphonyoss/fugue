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

package org.symphonyoss.s2.fugue.http.ui.servlet;

import java.io.File;
import java.nio.file.Path;

import org.symphonyoss.s2.fugue.http.IResourceProvider;

public class UIServletResources
{
  private static final Path html_ = new File("/html").toPath();
  private final String headerHtml_;
  private final String bannerHtml_;
  private final String footerHtml_;
  private final String leftNavHeaderHtml_;
  private final String leftNavFooterHtml_;

  public UIServletResources(IResourceProvider provider)
  {
    headerHtml_ = provider.loadResourceAsString(html_.resolve("header.html"));
    bannerHtml_ = provider.loadResourceAsString(html_.resolve("banner.html"));
    footerHtml_ = provider.loadResourceAsString(html_.resolve("footer.html"));
    leftNavHeaderHtml_ = provider.loadResourceAsString(html_.resolve("leftNavHeader.html"));
    leftNavFooterHtml_ = provider.loadResourceAsString(html_.resolve("leftNavFooter.html"));
  }

  public String getHeaderHtml()
  {
    return headerHtml_;
  }

  public String getBannerHtml()
  {
    return bannerHtml_;
  }

  public String getFooterHtml()
  {
    return footerHtml_;
  }

  public String getLeftNavHeaderHtml()
  {
    return leftNavHeaderHtml_;
  }

  public String getLeftNavFooterHtml()
  {
    return leftNavFooterHtml_;
  }
}
