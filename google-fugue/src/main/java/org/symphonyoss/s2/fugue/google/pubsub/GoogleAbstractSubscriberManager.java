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
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.ISubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.Subscription;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;

/* package */ class GoogleAbstractSubscriberManager<T extends ISubscriberManager<ImmutableByteArray,T>> extends AbstractSubscriberManager<ImmutableByteArray, T>
{
  private static final Logger          log_            = LoggerFactory.getLogger(GoogleAbstractSubscriberManager.class);

  /* package */ final INameFactory           nameFactory_;
  /* package */ final String                 projectId_;
  /* package */ final boolean                startSubscriptions_;

  /* package */ List<Subscriber>             subscriberList_ = new LinkedList<>();
  /* package */ 
  
  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param projectId                       The Google project ID for the pubsub service.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  /* package */ GoogleAbstractSubscriberManager(Class<T> type, INameFactory nameFactory, String projectId,
      ITraceContextFactory traceFactory,
      IThreadSafeErrorConsumer<ImmutableByteArray> unprocessableMessageConsumer)
  {
    super(type, traceFactory, unprocessableMessageConsumer);
    
    nameFactory_ = nameFactory;
    projectId_ = projectId;
    startSubscriptions_ = true;
  }
  
  /**
   * Construct a subscriber manager without and subscription processing.
   * 
   * @param nameFactory                     A NameFactory.
   * @param projectId                       The Google project ID for the pubsub service.
   * @param traceFactory                    A trace context factory.
   */
  /* package */ GoogleAbstractSubscriberManager(Class<T> type, INameFactory nameFactory, String projectId,
      ITraceContextFactory traceFactory)
  {
    super(type, traceFactory, null);
    
    nameFactory_ = nameFactory;
    projectId_ = projectId;
    startSubscriptions_ = false;

    log_.info("Starting GoogleSubscriberManager in project " + projectId_ + "...");
  }
  
  @Override
  protected void startSubscription(Subscription<ImmutableByteArray> subscription)
  { 
    for(String topic : subscription.getTopicNames())
    {
      log_.info("Validating to topic " + topic + "...");
      
      TopicName               topicName = nameFactory_.getTopicName(topic);
      
      SubscriptionName        subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionName());

      validateSubcription(topicName, subscriptionName);
      
    }
    
    if(startSubscriptions_)
    {
      for(String topic : subscription.getTopicNames())
      {
        log_.info("Subscribing to topic " + topic + "...");
        
        TopicName               topicName = nameFactory_.getTopicName(topic);
        
        SubscriptionName        subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionName());

        validateSubcription(topicName, subscriptionName);
        
        GoogleSubscriber        receiver                = new GoogleSubscriber(this, getTraceFactory(), subscription.getConsumer(), subscriptionName);
        ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());      
        Subscriber              subscriber              = Subscriber.newBuilder(projectSubscriptionName, receiver).build();
        
        subscriber.addListener(new Subscriber.Listener()
        {
          @Override
          public void failed(Subscriber.State from, Throwable failure)
          {
            log_.error("Error for " + projectSubscriptionName + " from " + from, failure);
          }
        }, MoreExecutors.directExecutor());
        
        synchronized (subscriberList_)
        {
          subscriberList_.add(subscriber);
        }
        
        subscriber.startAsync();
        log_.info("Subscribing to " + projectSubscriptionName + "...");
      }
    }
  }
  
  private void validateSubcription(TopicName topicName, SubscriptionName subscriptionName)
  {
    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create())
    {
      ProjectTopicName projectTopicName = ProjectTopicName.of(projectId_, topicName.toString());
      ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());
      
      try
      {
        com.google.pubsub.v1.Subscription existing = subscriptionAdminClient.getSubscription(projectSubscriptionName);
        
        log_.info("Subscription " + subscriptionName + " on topic " + topicName + " exists.");
      }
      catch(NotFoundException e)
      {   
        log_.error("Subscription " + subscriptionName + " on topic " + topicName + " DOES NOT EXIST.");
      }
    }
    catch (IOException e)
    {
      throw new TransactionFault(e);
    }
  }

  @Override
  protected void stopSubscriptions()
  {
    for(Subscriber subscriber : subscriberList_)
    {
      try
      {
        subscriber.stopAsync();
        
        log_.info("Stopped subscriber " + subscriber.getSubscriptionNameString());
      }
      catch(RuntimeException e)
      {
        log_.error("Failed to stop subscriber " + subscriber.getSubscriptionNameString(), e);
      }
    }
  }
}
