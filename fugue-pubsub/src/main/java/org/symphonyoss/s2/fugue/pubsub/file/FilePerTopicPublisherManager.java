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

import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.core.strategy.naming.DefaultNamingStrategy;
import org.symphonyoss.s2.fugue.core.strategy.naming.INamingStrategy;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPublisherManager;

public class FilePerTopicPublisherManager extends AbstractPublisherManager<String, FilePerTopicPublisherManager>
{
  private final IConfigurationProvider       config_;
  private final File                         rootDir_;
  private final INamingStrategy              namingStrategy_;

  private Map<String, FilePerTopicPublisher> publisherNameMap_   = new HashMap<>();
  private Map<String, FilePerTopicPublisher> publisherConfigMap_ = new HashMap<>();
  private List<FilePerTopicPublisher>        publishers_ = new ArrayList<>();

  public FilePerTopicPublisherManager(IConfigurationProvider config, File rootDir)
  {
    this(config, rootDir, new DefaultNamingStrategy(null));
  }
  
  public FilePerTopicPublisherManager(IConfigurationProvider config, File rootDir, INamingStrategy namingStrategy)
  {
    super(FilePerTopicPublisherManager.class);
    
    config_ = config;
    rootDir_ = rootDir;
    namingStrategy_ = namingStrategy;
  }

  @Override
  public void start()
  {
    for(Entry<String, FilePerTopicPublisher> entry : publisherNameMap_.entrySet())
    {
      entry.getValue().startByName(namingStrategy_.getTopicName(entry.getKey()));
      publishers_.add(entry.getValue());
    }
    
    for(Entry<String, FilePerTopicPublisher> entry : publisherConfigMap_.entrySet())
    {
      entry.getValue().startByName(namingStrategy_.getTopicName(config_.getRequiredString(entry.getKey())));
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
  public synchronized IThreadSafeConsumer<String> getPublisherByName(String topicName)
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
  public synchronized IThreadSafeConsumer<String> getPublisherByConfig(String topicConfigId)
  {
    assertConfigurable();
    
    FilePerTopicPublisher publisher = publisherConfigMap_.get(topicConfigId);
    
    if(publisher == null)
    {
      publisher = new FilePerTopicPublisher(rootDir_);
      publisherConfigMap_.put(topicConfigId, publisher);
    }
    
    return publisher;
  }
}
