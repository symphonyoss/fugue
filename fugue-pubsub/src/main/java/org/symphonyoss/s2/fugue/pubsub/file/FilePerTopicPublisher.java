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

package org.symphonyoss.s2.fugue.pubsub.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

public class FilePerTopicPublisher implements IPublisher<String>, AutoCloseable
{
  static final int MAX_MESSAGE_SIZE = 100*1000*1000; // an arbitrary large number

  private static final Logger  log_ = LoggerFactory.getLogger(FilePerTopicPublisher.class);

  private static final String NOT_OPEN = "Not open.";
  
  private final File rootDir_;
  private FileOutputStream out_;

  public FilePerTopicPublisher(File rootDir)
  {
    rootDir_ = rootDir;
  }
  
  public void startByName(String topicName)
  {
    if(out_ != null)
      throw new IllegalStateException("Publisher is running");
    
    try
    {
      out_ = new FileOutputStream(new File(rootDir_, topicName));
    }
    catch (FileNotFoundException e)
    {
      throw new ProgramFault(e);
    }
  }

  @Override
  public synchronized void consume(String item, ITraceContext trace)
  {
    if(out_ == null)
      throw new IllegalStateException("Publisher is closed");
    
    try
    {
      out_.write(item.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e)
    {
      throw new TransactionFault("Unable to write message", e);
    }
  }
  
  public void flush()
  {
    if(out_ != null)
    {
      try
      {
        out_.flush();
      }
      catch (IOException e)
      {
        log_.error("Unable to flush stream", e);
      }
    }
    else
    {
      throw new IllegalStateException(NOT_OPEN);
    }
  }

  @Override
  public void close()
  {
    if(out_ != null)
    {
      try
      {
        out_.close();
      }
      catch (IOException e)
      {
        log_.error("Failed to close file", e);
      }
      finally
      {
        out_ = null;
      }
    }
    else
    {
      throw new IllegalStateException(NOT_OPEN);
    }
  }

  @Override
  public int getMaximumMessageSize()
  {
    return MAX_MESSAGE_SIZE;
  }
}
