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

package org.symphonyoss.s2.fugue.core.trace.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.symphonyoss.s2.fugue.IFugueComponent;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;

/**
 * An ITraceContextFactory which emits messages to the log.
 * 
 * @author Bruce Skingle
 *
 */
public class FileTraceContextTransactionFactory implements ITraceContextTransactionFactory, IFugueComponent
{
  private Map<String, Integer>  counterMap_ = new HashMap<>();

  private PrintWriter out_;

  private File file_;
  
  /**
   * Constructor.
   */
  public FileTraceContextTransactionFactory()
  {
    try
    {
      file_ = File.createTempFile("FileTrace", ".txt");
      out_ = new PrintWriter(new FileWriter(file_));
      
      System.out.println("Trace data in " + file_.getAbsolutePath());
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
    
  }
  
  @Override
  public ITraceContextTransaction createTransaction(String subjectType, String subjectId)
  {
    increment(subjectType);
    return new FileTraceContextTransaction(this, null, subjectType, subjectId, Instant.now());
  }

  @Override
  public ITraceContextTransaction createTransaction(String subjectType, String subjectId, Instant startTime)
  {
    increment(subjectType);
    return new FileTraceContextTransaction(this, null, subjectType, subjectId, startTime);
  }

  synchronized void increment(String subjectType)
  {
    Integer count = counterMap_.get(subjectType);
    
    if(count == null)
      counterMap_.put(subjectType, 1);
    else
      counterMap_.put(subjectType, count + 1);
  }

  @Override
  public void start()
  {
  }

  @Override
  public void stop()
  {
    for(Entry<String, Integer> entry : counterMap_.entrySet())
    {
      printf("%5d %s%n", entry.getValue(), entry.getKey());
      System.err.printf("%5d %s%n", entry.getValue(), entry.getKey());
    }
    out_.close();
    System.err.println("Trace data in " + file_.getAbsolutePath());
  }

  PrintWriter printf(String format, Object... args)
  {
    return out_.printf(format, args);
  }
  
}
