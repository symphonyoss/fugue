/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pubsub.AbstractPullSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.SubscriptionImpl;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.pubsub.v1.ProjectSubscriptionName;

import io.grpc.StatusRuntimeException;

/**
 * Subscriber manager for Google pubsub.
 * 
 * The following configuration is supported:
 * 
  "org":
  {
    "symphonyoss":
    {
      "s2":
      {
        "fugue":
        {
          "google":
          {
            "pubsub":
            {
              "subscriberThreadPoolSize": 40,
              "handlerThreadPoolSize": 360
            }
          }
        }
      }
    }
  }
 * @author Bruce Skingle
 *
 */
public class GoogleSubscriberManager extends AbstractPullSubscriberManager<ImmutableByteArray, GoogleSubscriberManager>
{
  private static final Logger          log_            = LoggerFactory.getLogger(GoogleSubscriberManager.class);

  private final String                 projectId_;

  private List<GoogleSubscriber> subscribers_ = new LinkedList<>();
  
  private GoogleSubscriberManager(Builder builder)
  {
    super(GoogleSubscriberManager.class, builder);
    
    projectId_ = builder.projectId_;
  }
  
  /**
   * Builder for GoogleSubscriberManager.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractPullSubscriberManager.Builder<ImmutableByteArray, Builder, GoogleSubscriberManager>
  {
    private String                 projectId_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }
    
    @Override
    protected String getConfigPath()
    {
      return GoogleConstants.CONFIG_PATH;
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
    protected GoogleSubscriberManager construct()
    {
      return new GoogleSubscriberManager(this);
    }
  }

  @Override
  protected void initSubscription(SubscriptionImpl<ImmutableByteArray> subscription)
  { 
    for(TopicName topicName : subscription.getTopicNames())
    {
      log_.info("Validating topic " + topicName + "...");
      
      SubscriptionName        subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());

      validateSubcription(topicName, subscriptionName);

      GoogleSubscriber subscriber = new GoogleSubscriber(this, ProjectSubscriptionName.format(projectId_,  subscriptionName.toString()),
          getTraceFactory(), subscription.getConsumer(), getCounter(), getBusyCounter(), nameFactory_.getTenantId());

      subscribers_.add(subscriber);
      log_.info("Subscribing to " + subscriptionName + "...");  
    }
  }
  
  private void validateSubcription(TopicName topicName, SubscriptionName subscriptionName)
  {
    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create())
    {
      ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());
      
      try
      {
        com.google.pubsub.v1.Subscription existing = subscriptionAdminClient.getSubscription(projectSubscriptionName);
        
        log_.info("Subscription " + subscriptionName + " on topic " + topicName + " exists with ack deadline " + existing.getAckDeadlineSeconds() + " seconds.");
      }
      catch(NotFoundException e)
      {   
        throw new ProgramFault("Subscription " + subscriptionName + " on topic " + topicName + " DOES NOT EXIST.");
      }
      catch(StatusRuntimeException e)
      {
        log_.error("Subscription " + subscriptionName + " on topic " + topicName + " cannot be validated - lets hope....", e);
      }
    }
    catch (IOException e)
    {
      throw new TransactionFault(e);
    }
  }

  @Override
  protected void startSubscriptions()
  {
    for(GoogleSubscriber subscriber : subscribers_)
    {
      log_.info("Starting subscription to " + subscriber.getSubscriptionName() + "...");
      submit(subscriber, true);
    }
  }

  @Override
  protected void stopSubscriptions()
  {
     for(GoogleSubscriber subscriber : subscribers_)
        subscriber.stop();
      
     super.stopSubscriptions();
  }

//  @Override
//  protected void stopSubscriptions()
//  {
//    synchronized (subscriberList_)
//    {
////      for(GoogleSubscriber receiver : receiverList_)
////      {
////        receiver.stop();
////      }
//      
//      
//      for(Subscriber subscriber : subscriberList_)
//      {
//        try
//        {
//          log_.info("Stopping subscriber " + subscriber.getSubscriptionNameString() + "...");
//          
//          subscriber.stopAsync().awaitTerminated();
//          
//          log_.info("Stopped subscriber " + subscriber.getSubscriptionNameString());
//        }
//        catch(RuntimeException e)
//        {
//          log_.error("Failed to stop subscriber " + subscriber.getSubscriptionNameString(), e);
//        }
//      }
//    }
//  }
}
