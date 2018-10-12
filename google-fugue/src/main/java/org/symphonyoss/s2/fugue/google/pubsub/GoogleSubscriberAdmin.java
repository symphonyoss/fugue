/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberAdmin;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.NotFoundException;
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
public class GoogleSubscriberAdmin extends AbstractSubscriberAdmin<ImmutableByteArray, GoogleSubscriberAdmin>
{
  private static final Logger          log_            = LoggerFactory.getLogger(GoogleSubscriberAdmin.class);
  
  private final String                 projectId_;

  private SubscriptionAdminClient subscriptionAdminClient_;
  
  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param projectId                       The Google project ID for the pubsub service.
   * @param traceFactory                    A trace context factory.
   */
  public GoogleSubscriberAdmin(INameFactory nameFactory, String projectId,
      ITraceContextFactory traceFactory)
  {
    super(nameFactory, GoogleSubscriberAdmin.class);
    
    projectId_ = projectId;
  }

  @Override
  public synchronized void start()
  {
    super.start();
    
    try
    {
      subscriptionAdminClient_ = SubscriptionAdminClient.create();
    }
    catch (IOException e)
    {
      throw new IllegalStateException("Unable to create SubscriptionAdminClient", e);
    }
  }

  @Override
  public synchronized void stop()
  {
    subscriptionAdminClient_.close();
    
    super.stop();
  }

  @Override
  protected void createSubcription(TopicName topicName, SubscriptionName subscriptionName, boolean dryRun)
  {
    try
    {
      ProjectTopicName projectTopicName = ProjectTopicName.of(projectId_, topicName.toString());
      ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());
      
      try
      {
        subscriptionAdminClient_.getSubscription(projectSubscriptionName);
        
        log_.info("Subscription " + subscriptionName + " on topic " + topicName + " already exists.");
      }
      catch(NotFoundException e)
      {
        if(dryRun)
        {
          log_.info("Subscription " + subscriptionName + " on topic " + topicName + " would be created (dry run).");
        }
        else
        {
          // create a pull subscription with default acknowledgement deadline
          subscriptionAdminClient_.createSubscription(projectSubscriptionName, projectTopicName,
              PushConfig.getDefaultInstance(), 0);
          
          log_.info("Subscription " + subscriptionName + " on topic " + topicName + " created.");
        }
      }
    }
    catch (AlreadyExistsException e)
    {
      log_.info("Subscription " + subscriptionName + " on topic " + topicName + " already exists.");
    }
  }

  @Override
  protected void deleteSubcription(TopicName topicName, SubscriptionName subscriptionName, boolean dryRun)
  {
    log_.debug("About to delete subscription " + subscriptionName + " on topic " + topicName);
    
    try
    {
      ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());
      
      subscriptionAdminClient_.getSubscription(projectSubscriptionName);
      
      if(dryRun)
      {
        log_.info("Subscription " + subscriptionName + " would be deleted (dry run).");
      }
      else
      {
          subscriptionAdminClient_.deleteSubscription(projectSubscriptionName);
          
          log_.info("Subscription " + subscriptionName + " deleted.");
      }
    }
    catch (NotFoundException e)
    {
      log_.info("Subscription " + subscriptionName + " does not exist.");
    }
  }
}
