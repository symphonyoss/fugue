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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.SubscriptionName;

public class SubServlet extends AbstractPubSubServlet
{
  private static final long serialVersionUID = 1L;
  private static final Logger log_ = LoggerFactory.getLogger(SubServlet.class);

  public SubServlet(PubSubServer server)
  {
    super(server, "Subscriber");
  }

  @Override
  public void handleGet(PrintWriter out)
  {
    try
    {
      SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().build();
      try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriptionAdminSettings))
      {
        String      projectId         = ServiceOptions.getDefaultProjectId();
        String      subscriptionId    = PubSubServer.SUBSCRIPTION_NAME;
        int         numOfMessages     = 1; // max number of messages to be pulled
        String      subscriptionName  = SubscriptionName.of(projectId, subscriptionId).toString();
        PullRequest pullRequest = PullRequest.newBuilder()
            .setMaxMessages(numOfMessages)
            .setReturnImmediately(true) // return immediately if messages are not available
            .setSubscription(subscriptionName)
            .build();
  
        // use pullCallable().futureCall to asynchronously perform this operation
        PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);
        List<String> ackIds = new ArrayList<>();
        for (ReceivedMessage message : pullResponse.getReceivedMessagesList())
        {
          out.println("Received message<pre>");
          out.println(new String(message.getMessage().getData().toByteArray()));
          out.println("</pre>");
          ackIds.add(message.getAckId());
        }
        if(ackIds.isEmpty())
        {
          out.println("No message available");
        }
        else
        {
          // acknowledge received messages
          AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder().setSubscription(subscriptionName)
              .addAllAckIds(ackIds).build();
          // use acknowledgeCallable().futureCall to asynchronously perform this
          // operation
          subscriber.acknowledgeCallable().call(acknowledgeRequest);
        }
      }
    }
    catch(Exception e) //SubscriberStub.close() throws Exception (!)
    {
      log_.error("Failed to subscribe", e);
      error("Failed to subscribe (%s)", e);
    }
  }

}
