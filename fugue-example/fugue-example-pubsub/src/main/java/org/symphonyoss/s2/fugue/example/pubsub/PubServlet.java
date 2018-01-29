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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

public class PubServlet extends AbstractPubSubServlet
{
  private static final long serialVersionUID = 1L;
  private static final Logger log_ = LoggerFactory.getLogger(PubServlet.class);

  public PubServlet(PubSubServer server)
  {
    super(server, "Publisher");
  }
  
  
  @Override
  public void handleGet(PrintWriter out)
  {
    out.println("<h2>Enter Message</h2>");
    out.println("<form method=\"POST\">");
    out.println(  "<textArea name=message rows=10 cols=60>");
    out.println(    "Enter message here");
    out.println(  "</textArea><br/>");
    out.println(  "<button type=\"submit\">Publish</button>");
    out.println("</form>");
  }


  @Override
  public void handlePost(PrintWriter out, HttpServletRequest req)
  {
    String message = req.getParameter("message");
    TopicName topicName = TopicName.of(ServiceOptions.getDefaultProjectId(), PubSubServer.TOPIC_NAME);
    Publisher publisher = null;
    List<ApiFuture<String>> messageIdFutures = new ArrayList<>();

    try
    {
      // Create a publisher instance with default settings bound to the topic
      publisher = Publisher.newBuilder(topicName).build();

      ByteString data = ByteString.copyFromUtf8(message);
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

      // Once published, returns a server-assigned message id (unique within
      // the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      messageIdFutures.add(messageIdFuture);
    }
    catch (IOException e)
    {
      log_.error("Failed to publish", e);
      error("Failed to publish (%s)", e);
    }
    finally
    {
      try
      {
        // wait on any pending publish requests.
        List<String> messageIds = ApiFutures.allAsList(messageIdFutures).get();
  
        for (String messageId : messageIds)
        {
          out.println("Published with message ID: " + messageId);
        }
  
        if (publisher != null)
        {
          // When finished with the publisher, shutdown to free up resources.
          publisher.shutdown();
        }
      }
      catch(Exception e) // publisher.shutdown() throws Exception
      {
        log_.error("Failed to shutdown publisher", e);
        error("Failed to shutdown publisher (%s)", e);
      }
    }
  }
}
