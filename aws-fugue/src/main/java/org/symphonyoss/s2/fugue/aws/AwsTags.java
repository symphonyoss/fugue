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

package org.symphonyoss.s2.fugue.aws;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A set of tags.
 * 
 * @author Bruce Skingle
 *
 */
public class AwsTags
{
  private Map<String, String>                                           tags_       = new HashMap<>();
  private List<com.amazonaws.services.dynamodbv2.model.Tag>             dynamoTags_ = new LinkedList<>();
  private List<com.amazonaws.services.elasticloadbalancingv2.model.Tag> elbTags_    = new LinkedList<>();
  
  /**
   * Constructor.
   * 
   * @param tags Initial tags.
   */
  public AwsTags(Map<String, String> tags)
  {
    tags_.putAll(tags);
    
    for(Entry<String, String> entry : tags.entrySet())
    {
      dynamoTags_.add(new com.amazonaws.services.dynamodbv2.model.Tag()             .withKey(entry.getKey()).withValue(entry.getValue()));
      elbTags_.add(   new com.amazonaws.services.elasticloadbalancingv2.model.Tag() .withKey(entry.getKey()).withValue(entry.getValue()));
    }
  }

  /**
   * Put the given additional tag.
   * 
   * @param name    Tag name
   * @param value   Tag value
   * 
   * @return this(fluent method)
   */
  public AwsTags  put(String name, String value)
  {
    if(value != null)
    {
      tags_.put(name, value);
      dynamoTags_.add(new com.amazonaws.services.dynamodbv2.model.Tag()             .withKey(name).withValue(value));
      elbTags_.add(   new com.amazonaws.services.elasticloadbalancingv2.model.Tag() .withKey(name).withValue(value));
    }
    
    return this;
  }

  /**
   * 
   * @return The tags as needed by DynamoDb
   */
  public List<com.amazonaws.services.dynamodbv2.model.Tag> getDynamoTags()
  {
    return dynamoTags_;
  }

  /**
   * 
   * @return The tags as needed by ELB
   */
  public List<com.amazonaws.services.elasticloadbalancingv2.model.Tag> getElbTags()
  {
    return elbTags_;
  }

  /**
   * 
   * @return The tags as a Map of Strings.
   */
  public Map<String, String> getTags()
  {
    return tags_;
  }
}
