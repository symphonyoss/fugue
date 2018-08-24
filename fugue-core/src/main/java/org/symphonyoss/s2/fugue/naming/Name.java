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

package org.symphonyoss.s2.fugue.naming;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.common.fault.CodingFault;

public class Name
{
  public static final String SEPARATOR = "-";
  
  private final String name_;
  private final String sha1Name_;
  private final String md5Name_;

  /**
   * Base class for Names.
   * 
   * The name may not be <code>null</code>. Any optional additional non-null suffix components will be
   * appended to the final name each with the standard separator.
   * 
   * @param name        The name
   * @param additional  Zero or more optional suffix elements.
   */
  public Name(@Nonnull String name, String ...additional)
  {
    if(name == null)
      throw new NullPointerException("name may not be null");
    
    StringBuilder b = new StringBuilder(name);
    
    for(String s : additional)
    {
      if(s != null)
      {
        b.append(SEPARATOR);
        b.append(s);
      }
    }
    
    name_ = b.toString();
    
    try
    {
      byte[] hash = MessageDigest.getInstance("SHA-1").digest(name_.getBytes(StandardCharsets.UTF_8));
      
      sha1Name_ = Base62.encodeToString(hash);
      
      hash = MessageDigest.getInstance("MD5").digest(name_.getBytes(StandardCharsets.UTF_8));
      
      md5Name_ = Base62.encodeToString(hash);
    }
    catch(NoSuchAlgorithmException e)
    {
      throw new CodingFault(e); // "Can't happen"
    }
  }

  @Override
  public String toString()
  {
    return name_;
  }

  /**
   * Returns a short name of at most the given length.
   * 
   * If the full name is short enough it is returned. Otherwise if maxLen is >= 27 the
   * Base62 encoded SHA-1 hash of the name is returned, otherwise if maxLen is >= 22 the
   * Base62 encoded MD5 hash of the name is returned.
   * 
   * Base62 encoding uses only [a-zA-Z0-9], names are constructed from a series of elements
   * separated by hyphens so as long as a long name has more than one element, the Base62
   * encoded short versions cannot clash with unencoded names.
   * 
   * @param maxLen The maximum length of the name.
   * 
   * @return the short name.
   */
  public String getShortName(int maxLen)
  {
    if(name_.length() <= maxLen)
      return name_;
    
    if(sha1Name_.length() <= maxLen)
      return sha1Name_;
    
    if(md5Name_.length() <= maxLen)
      return md5Name_;
    
    throw new IllegalArgumentException(md5Name_.length() + " is the shortest abbreviated name.");
  }
}
