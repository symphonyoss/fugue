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

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.symphonyoss.s2.common.exception.NotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestBaseConfigurationProvider
{
  private static final String ABSENT_PROPERTY_NAME = "ABSENT_PROPERTY_NAME";
  private static final String TEST_PROPERTY_NAME = "TEST_PROPERTY_NAME";
  private static final String TEST_PROPERTY_VALUE = "TEST_PROPERTY_VALUE";
  
  private static final String NEST_ONE = "NEST_ONE";
  private static final String ABSENT_NEST_ONE_PROPERTY_NAME = NEST_ONE + "/" + ABSENT_PROPERTY_NAME;
  private static final String TEST_NEST_ONE_PROPERTY_NAME = NEST_ONE + "/" + TEST_PROPERTY_NAME;
  private static final String TEST_NEST_ONE_PROPERTY_VALUE = NEST_ONE + "_" + TEST_PROPERTY_VALUE;
  

  private static final String NEST_TWO = "NEST_TWO";
  private static final String ABSENT_NEST_TWO_PROPERTY_NAME = NEST_ONE + "/" + NEST_TWO + "/" + ABSENT_PROPERTY_NAME;
  private static final String TEST_NEST_TWO_PROPERTY_NAME = NEST_ONE + "/" + NEST_TWO + "/" + TEST_PROPERTY_NAME;
  private static final String TEST_NEST_TWO_PROPERTY_VALUE = NEST_TWO + "_" + TEST_PROPERTY_VALUE;
  
  private JsonNode json_;
  
  public TestBaseConfigurationProvider() throws IOException
  {
    InputStream in = getClass().getResourceAsStream("/config.json");
    json_ = new ObjectMapper().readTree(in);
  }
  
  @Test
  public void testTop() throws NotFoundException
  {
    BaseConfigurationProvider provider = new BaseConfigurationProvider(json_);
    
    assertEquals("Test propety is not valid", TEST_PROPERTY_VALUE, provider.getString(TEST_PROPERTY_NAME));
  }
  
  @Test(expected=NotFoundException.class)
  public void testMissingTop() throws NotFoundException
  {
    BaseConfigurationProvider provider = new BaseConfigurationProvider(json_);
    
    provider.getString(ABSENT_PROPERTY_NAME);
  }

  @Test
  public void testNestOne() throws NotFoundException
  {
    BaseConfigurationProvider provider = new BaseConfigurationProvider(json_);
    
    assertEquals("Test propety is not valid", TEST_NEST_ONE_PROPERTY_VALUE, provider.getString(TEST_NEST_ONE_PROPERTY_NAME));
  }
  
  @Test(expected=NotFoundException.class)
  public void testMissingNestOne() throws NotFoundException
  {
    BaseConfigurationProvider provider = new BaseConfigurationProvider(json_);
    
    provider.getString(ABSENT_NEST_ONE_PROPERTY_NAME);
  }

  @Test
  public void testNestTwo() throws NotFoundException
  {
    BaseConfigurationProvider provider = new BaseConfigurationProvider(json_);
    
    assertEquals("Test propety is not valid", TEST_NEST_TWO_PROPERTY_VALUE, provider.getString(TEST_NEST_TWO_PROPERTY_NAME));
  }
  
  @Test(expected=NotFoundException.class)
  public void testMissingNestTwo() throws NotFoundException
  {
    BaseConfigurationProvider provider = new BaseConfigurationProvider(json_);
    
    provider.getString(ABSENT_NEST_TWO_PROPERTY_NAME);
  }
}