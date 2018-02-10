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
package org.symphonyoss.s2.fugue;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;
import org.symphonyoss.s2.common.exception.NotFoundException;

public class TestConfigurationProvider
{
  private static final String ABSENT_PROPERTY_NAME = "ABSENT_PROPERTY_NAME";
  private static final String TEST_PROPERTY_NAME = "TEST_PROPERTY_NAME";
  private static final String TEST_PROPERTY_VALUE = "TEST_PROPERTY_VALUE";

  @Test
  public void testInline() throws URISyntaxException, NotFoundException
  {
    File file = Paths.get(getClass().getResource("/inlineConfig.json").toURI()).toFile();
    
    ConfigurationProvider provider = new ConfigurationProvider(file.getAbsolutePath());
    
    assertEquals("Test propety is not valid", TEST_PROPERTY_VALUE, provider.getProperty(TEST_PROPERTY_NAME));
  }
  
  @Test(expected=NotFoundException.class)
  public void testMissingInline() throws URISyntaxException, NotFoundException
  {
    File file = Paths.get(getClass().getResource("/inlineConfig.json").toURI()).toFile();
    
    ConfigurationProvider provider = new ConfigurationProvider(file.getAbsolutePath());
    
    provider.getProperty(ABSENT_PROPERTY_NAME);
  }

}