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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.symphonyoss.s2.fugue.http.ui.servlet.ISetter;

/**
 * Parser for command line flags and arguments.
 * 
 * @author Bruce Skingle
 *
 */
public class CommandLineHandler
{
  private Map<Character, AbstractFlag> shortMap_     = new HashMap<>();
  private Map<String, AbstractFlag>    longMap_      = new HashMap<>();
  private Map<String, AbstractFlag>    envMap_      = new HashMap<>();
  private Set<AbstractFlag>            doneSet_      = new HashSet<>();
  private Set<AbstractFlag>            requiredSet_  = new HashSet<>();
  private List<ISetter<String>>        paramSetters_ = new ArrayList<>();
  private int                          paramCnt_;
  private int                          errors_;
  
  /**
   * Add a flag to the list of acceptable parameters.
   * 
   * @param <T>         Type of the value set by this flag.
   * @param shortFlag   A single character flag which can be specified with -
   * @param longFlag    A string flag which can be specified with --
   * @param envName     An environment variable or system property name which may contain the flag value
   * @param type        Type of the value set by this flag.
   * @param multiple    If true then multiple values may be set.
   * @param required    If true then a value must be provided.
   * @param setter      A lambda which will be called with the value of this flag.
   * 
   * @return This (fluent method)
   */
  public <T> CommandLineHandler withFlag(Character shortFlag, String longFlag, String envName, Class<T> type, boolean multiple, boolean required, ISetter<T> setter)
  {
    return withFlag(new Flag<T>(shortFlag, longFlag, envName, type, multiple, required, setter));
  }
  
  /**
   * Add a flag to the list of acceptable parameters.
   * 
   * @param flag  The flag.
   * 
   * @return This (fluent method)
   */
  public CommandLineHandler withFlag(AbstractFlag flag)
  {
    if(flag.getShortFlag() != null)
    {
      if(shortMap_.put(flag.getShortFlag(), flag) != null)
      {
        throw new IllegalArgumentException("Duplicate short flag \"" + flag.getShortFlag() + "\"");
      }
    }
    
    if(flag.getLongFlag() != null)
    {
      if(longMap_.put(flag.getLongFlag(), flag) != null)
      {
        throw new IllegalArgumentException("Duplicate long flag \"" + flag.getLongFlag() + "\"");
      }
    }
    
    if(flag.getEnvName() != null)
    {
      if(envMap_.put(flag.getEnvName(), flag) != null)
      {
        throw new IllegalArgumentException("Duplicate environment variable name \"" + flag.getEnvName() + "\"");
      }
    }
    
    if(flag.isRequired())
      requiredSet_.add(flag);
    
    return this;
  }
  
  /**
   * Add the given setter to handle non-flag parameters.
   * 
   * @param setter  A setter which will be called with the value of each non-flag parameter.
   * 
   * @return This (fluent method)
   */
  public CommandLineHandler withParam(ISetter<String> setter)
  {
    paramSetters_.add(setter);
    
    return this;
  }
  
  /**
   * Process the given command line arguments.
   * 
   * @param args Command line arguments.
   * 
   * @return This (fluent method)
   * 
   * @throws IllegalArgumentException if required flags are missing.
   */
  public CommandLineHandler process(String[] args)
  {
    ArrayIterator it = new ArrayIterator(args);
    
    while(it.hasNext())
    {
      String arg = it.next();
      
      if(arg.startsWith("--"))
      {
        processFlag(arg, longMap_.get(arg.substring(2)), it, true);
      }
      else if(arg.startsWith("++"))
      {
        processFlag(arg, longMap_.get(arg.substring(2)), it, false);
      }
      else if(arg.startsWith("—")) // that's an em-dash
      {
        processFlag(arg, longMap_.get(arg.substring(1)), it, true);
      }

      else if(arg.startsWith("-"))
      {
        processFlag(arg, shortMap_.get(arg.charAt(1)), it, true);
      }
      else if(arg.startsWith("+"))
      {
        processFlag(arg, shortMap_.get(arg.charAt(1)), it, false);
      }
      else
      {
        if(paramCnt_ >= paramSetters_.size())
          System.err.println("Unknown flag \"" + arg + "\" ignored.");
        else
        {
          paramSetters_.get(paramCnt_).set(arg);
          
          if(paramCnt_ < paramSetters_.size() - 1)
            paramCnt_++;
        }
      }
    }
    
    for(Entry<String, AbstractFlag> entry : envMap_.entrySet())
    {
      AbstractFlag flag = entry.getValue();
      
      if(doneSet_.contains(flag) == false || flag.isMultiple())
      {
        String flagStr = entry.getKey();
        String value = System.getProperty(flagStr);
        
        if(value == null)
          value = System.getenv(flagStr);
        
        if(value != null)
        {
          requiredSet_.remove(flag);
          doneSet_.add(flag);
          
          flag.process(value);
        }
      }
    }
    
    for(AbstractFlag missing : requiredSet_)
    {
      System.err.println("Flag " + missing + " is required.");
      errors_++;
    }
    
    if(errors_ > 0)
      throw new IllegalArgumentException(errors_ + " command line errors");
    
    return this;
  }

  private void processFlag(String string, AbstractFlag flag, ArrayIterator it, boolean boolVal)
  {
    if(flag == null)
    {
      System.err.println("Unknown flag \"" + string + "\" ignored.");
    }
    else
    {
      requiredSet_.remove(flag);
      
      if(!flag.isMultiple())
      {
        if(!doneSet_.add(flag))
          throw new IllegalArgumentException("Flag " + flag + " is specified more than once");
      }
      
      flag.process(it, boolVal);
    }
  }
  
  class ArrayIterator implements Iterator<String>
  {
    private int i_;
    private String[] args_;
    
    public ArrayIterator(String[] args)
    {
      args_ = args;
    }

    @Override
    public boolean hasNext()
    {
      return args_.length > i_;
    }

    @Override
    public String next()
    {
      return args_[i_++];
    }
  }
}
