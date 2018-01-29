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

package org.symphonyoss.s2.fugue.example.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.http.IServletContainer;
import org.symphonyoss.s2.common.http.IServletProvider;
import org.symphonyoss.s2.fugue.core.FugueServer;
import org.symphonyoss.s2.fugue.di.IComponent;
import org.symphonyoss.s2.fugue.di.IDIContext;
import org.symphonyoss.s2.fugue.di.impl.ComponentDescriptor;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.pubsub.v1.TopicName;

public class PubSubServer extends FugueServer implements IComponent, IServletProvider
{
  private static final String TEST_TOPIC = "Test-Topic";
  
  private static final Logger log_ = LoggerFactory.getLogger(PubSubServer.class);

  public PubSubServer(IDIContext diContext, String name)
  {
    super(diContext, name, 8080);
  }

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return super.getComponentDescriptor()
        .addProvidedInterface(IServletProvider.class)
//        .addStart(() -> openBrowser())
        .addStart(() -> startPubSub());
  }

  private void startPubSub()
  {
    createTopic();
  }

  private void createTopic()
  {
    log_.info("About to create topic");
    
    // Your Google Cloud Platform project ID
    String projectId = ServiceOptions.getDefaultProjectId();

    // Your topic ID, eg. "my-topic"
    String topicId = TEST_TOPIC;

    // Create a new topic
    TopicName topic = TopicName.of(projectId, topicId);
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create())
    {
      topicAdminClient.createTopic(topic);
    }
    catch (ApiException e)
    {
      // example : code = ALREADY_EXISTS(409) implies topic already exists
      log_.error("Failed to create topic, HTTP {} retryable {}", e.getStatusCode().getCode(), 
          e.isRetryable(), e);
    }
    catch (Exception e)
    {
      log_.error("Failed to create topic", e);
    }
    

    log_.info("Topic {}:{} created.", topic.getProject(), topic.getTopic());
  }

  @Override
  public void registerServlets(IServletContainer servletContainer)
  {
    servletContainer.addServlet("/", new HelloWorldServlet());
  }

}
