/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberAdmin;
import org.symphonyoss.s2.fugue.pubsub.ITopicSubscriptionAdmin;

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
public class GoogleSubscriberAdmin extends AbstractSubscriberAdmin<GoogleSubscriberAdmin>
{
  private static final Logger          log_            = LoggerFactory.getLogger(GoogleSubscriberAdmin.class);
  
  private  String                 projectId_;

  private SubscriptionAdminClient subscriptionAdminClient_;
  
  private GoogleSubscriberAdmin(Builder builder)
  {
    super(GoogleSubscriberAdmin.class, builder);
    
    projectId_ = builder.projectId_;
    
    try
    {
      subscriptionAdminClient_ = SubscriptionAdminClient.create();
    }
    catch (IOException e)
    {
      throw new IllegalStateException("Unable to create SubscriptionAdminClient", e);
    }
  }
  
  /**
   * Builder for GoogleSubscriberAdmin.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractSubscriberAdmin.Builder<Builder, GoogleSubscriberAdmin>
  {
    private String                 projectId_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }
    
    /**
     * Set the Google project ID.
     * 
     * @param projectId The ID of the Google project in which to operate.
     * 
     * @return this (fluent method)
     */
    public Builder withProjectId(String projectId)
    {
      projectId_  = projectId;
      
      return self();
    }

    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(projectId_, "projectId");
    }

    @Override
    protected GoogleSubscriberAdmin construct()
    {
      return new GoogleSubscriberAdmin(this);
    }
  }

  @Override
  public synchronized void stop()
  {
    subscriptionAdminClient_.close();
    
    super.stop();
  }

  @Override
  protected void createSubcription(SubscriptionName subscriptionName, ITopicSubscriptionAdmin subscription, boolean dryRun)
  {
    if(subscription.getFilterPropertyName() != null)
      throw new IllegalArgumentException("Google pubsub does not support filtering.");
    
    TopicName topicName = subscriptionName.getTopicName();
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
  protected void deleteSubcription(SubscriptionName subscriptionName, boolean dryRun)
  {
    TopicName topicName = subscriptionName.getTopicName();
    log_.info("About to delete subscription " + subscriptionName + " on topic " + topicName);
    
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
        log_.info("About to delete subscription with name " + projectSubscriptionName);
        
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
