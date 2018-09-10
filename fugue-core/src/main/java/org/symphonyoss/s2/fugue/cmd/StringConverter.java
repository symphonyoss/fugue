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

package org.symphonyoss.s2.fugue.cmd;

import java.util.HashMap;
import java.util.Map;

public class StringConverter
{
  private static Map<Class<?>, IStringConverter<?>> typeMap_ = new HashMap<>();
  
  static
  {
    register(String.class, (s) -> s);
    register(Integer.class, (s) -> Integer.parseInt(s));
    register(Long.class, (s) -> Long.parseLong(s));
    register(Double.class, (s) -> Double.parseDouble(s));
    register(Float.class, (s) -> Float.parseFloat(s));
    register(Boolean.class, (s) -> Boolean.parseBoolean(s));
  }
  
  public static <T> void register(Class<T> type, IStringConverter<T> converter)
  {
    typeMap_.put(type, converter);
  }
  
  @SuppressWarnings("unchecked")
  public static <T> IStringConverter<T> get(Class<T> type)
  {
    return (IStringConverter<T>) typeMap_.get(type);
  }
}
