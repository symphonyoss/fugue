/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.io.IOException;

import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.ISubscriberAdmin;
import org.symphonyoss.s2.fugue.pubsub.Subscription;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;

/**
 * Admin variant of GooglePublisherManager.
 * 
 * @author Bruce Skingle
 *
 */
public class GoogleSubscriberAdmin extends GoogleAbstractSubscriberManager<GoogleSubscriberAdmin> implements ISubscriberAdmin<ImmutableByteArray, GoogleSubscriberAdmin>
{
  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param config                          The configuration to use.
   * @param traceFactory                    A trace context factory.
   */
  public GoogleSubscriberAdmin(INameFactory nameFactory, IConfigurationProvider config,
      ITraceContextFactory traceFactory)
  {
    super(GoogleSubscriberAdmin.class, nameFactory, config, traceFactory);
  }

  @Override
  public void createSubscriptions()
  {
    for(Subscription<?> subscription : getSubscribers())
    {
      for(String topic : subscription.getTopicNames())
      {
        TopicName topicName = nameFactory_.getTopicName(topic);
        
        SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionName());
        
        createSubcription(topicName, subscriptionName);
      }
    }
  }

  private void createSubcription(TopicName topicName, SubscriptionName subscriptionName)
  {
    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create())
    {
      ProjectTopicName projectTopicName = ProjectTopicName.of(projectId_, topicName.toString());
      ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());
      
      // create a pull subscription with default acknowledgement deadline
      subscriptionAdminClient.createSubscription(projectSubscriptionName, projectTopicName,
          PushConfig.getDefaultInstance(), 0);
    }
    catch (IOException e)
    {
      throw new TransactionFault(e);
    }
  }
}
