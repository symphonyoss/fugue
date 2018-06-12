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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;
import org.symphonyoss.s2.fugue.pubsub.IPublisher;

public class FilePerTopicPublisherManager extends AbstractPublisherManager<String, FilePerTopicPublisherManager>
{
  private final File                         rootDir_;
  private final INameFactory                 nameFactory_;

  private Map<String, FilePerTopicPublisher> publisherNameMap_   = new HashMap<>();
  private List<FilePerTopicPublisher>        publishers_         = new ArrayList<>();
  
  public FilePerTopicPublisherManager(File rootDir, INameFactory nameFactory)
  {
    super(FilePerTopicPublisherManager.class);
    
    rootDir_ = rootDir;
    nameFactory_ = nameFactory;
  }

  @Override
  public void start()
  {
    for(Entry<String, FilePerTopicPublisher> entry : publisherNameMap_.entrySet())
    {
      entry.getValue().startByName(nameFactory_.getTopicName(entry.getKey()).toString());
      publishers_.add(entry.getValue());
    }
  }

  @Override
  public void stop()
  {
    for(FilePerTopicPublisher publisher : publishers_)
    {
      publisher.close();
    }
  }
  
  public void flush()
  {
    for(FilePerTopicPublisher publisher : publishers_)
    {
      publisher.flush();
    }
  }

  @Override
  public synchronized IPublisher<String> getPublisherByName(String topicName)
  {
    assertConfigurable();
    
    FilePerTopicPublisher publisher = publisherNameMap_.get(topicName);
    
    if(publisher == null)
    {
      publisher = new FilePerTopicPublisher(rootDir_);
      publisherNameMap_.put(topicName, publisher);
    }
    
    return publisher;
  }

  @Override
  public int getMaximumMessageSize()
  {
    return FilePerTopicPublisher.MAX_MESSAGE_SIZE;
  }
}
