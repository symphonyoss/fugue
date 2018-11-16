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
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.SubscriptionImpl;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.Subscriber.Builder;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.common.util.concurrent.MoreExecutors;
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
              "subscriberThreadPoolSize": 40
            }
          }
        }
      }
    }
  }
 * @author Bruce Skingle
 *
 */
public class GoogleAsyncSubscriberManager extends AbstractSubscriberManager<ImmutableByteArray, GoogleAsyncSubscriberManager>
{
  private static final Logger          log_            = LoggerFactory.getLogger(GoogleAsyncSubscriberManager.class);

  /* package */ final String                 projectId_;
  /* package */ final boolean                startSubscriptions_;

  /* package */ List<Subscriber>             subscriberList_ = new LinkedList<>();
  List<GoogleAsyncSubscriber>                receiverList_  = new LinkedList<>();
  /* package */ int                          subscriptionErrorCnt_;
  private final IConfiguration pubSubConfig_;

  private ICounter counter_;


  /**
   * Constructor.
   * 
   * @param config                          Configuration
   * @param nameFactory                     A NameFactory.
   * @param projectId                       The Google project ID for the pubsub service.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public GoogleAsyncSubscriberManager(IConfiguration config, INameFactory nameFactory, String projectId,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeErrorConsumer<ImmutableByteArray> unprocessableMessageConsumer)
  {
    super(nameFactory, GoogleAsyncSubscriberManager.class, traceFactory, unprocessableMessageConsumer);
    
    projectId_ = projectId;
    startSubscriptions_ = true;
    
    pubSubConfig_ = config.getConfiguration(GoogleConstants.CONFIG_PATH);
  }
  
  public GoogleAsyncSubscriberManager withCounter(ICounter counter)
  {
    counter_ = counter;
    
    return this;
  }

  @Override
  protected void initSubscription(SubscriptionImpl<ImmutableByteArray> subscription)
  { 
    for(TopicName topicName : subscription.getTopicNames())
    {
      log_.info("Validating topic " + topicName + "...");
      
      SubscriptionName        subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());

      validateSubcription(topicName, subscriptionName);
      
    }
    
    if(startSubscriptions_)
    {
      if(subscriptionErrorCnt_>0)
      {
        throw new IllegalStateException("There are " + subscriptionErrorCnt_ + " subscription errors.");
      }
      
      long threadsPerSubscription = pubSubConfig_.getLong("subscriberThreadsPerSubscription", 10L);
      int subscriberThreadPoolSize = pubSubConfig_.getInt("subscriberThreadPoolSize", 4);
      long maxOutstandingElementCount = pubSubConfig_.getLong("maxOutstandingElementCount", threadsPerSubscription);
      
      threadsPerSubscription = 1L;
      subscriberThreadPoolSize = 1;
      maxOutstandingElementCount = 1L;
      
      log_.info("Starting subscriptions threadsPerSubscription=" + threadsPerSubscription +
          " subscriberThreadPoolSize=" + subscriberThreadPoolSize +
          " maxOutstandingElementCount=" + maxOutstandingElementCount +
          " ...");

      
      for(TopicName topicName : subscription.getTopicNames())
      {
        log_.info("Subscribing to topic " + topicName + " ...");
        
        SubscriptionName        subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionId());

        validateSubcription(topicName, subscriptionName);
        
        GoogleAsyncSubscriber   receiver                = new GoogleAsyncSubscriber(this, getTraceFactory(), subscription.getConsumer(), subscriptionName, counter_);
        ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, subscriptionName.toString());      
        Builder                 builder                 = Subscriber.newBuilder(projectSubscriptionName, receiver);
        
//        ExecutorProvider executorProvider =
//            InstantiatingExecutorProvider.newBuilder()
//              .setExecutorThreadCount(subscriberThreadPoolSize)
//              .build();
//        builder.setExecutorProvider(executorProvider);
//        
//        
//        
//        builder.setFlowControlSettings(FlowControlSettings.newBuilder()
//            .setMaxOutstandingElementCount(maxOutstandingElementCount).build());
        Subscriber              subscriber              = builder.build();
        
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
          receiverList_.add(receiver);
        }
        
        
      }
    }
  }
  
  @Override
  protected void startSubscriptions()
  {
    synchronized (subscriberList_)
    {
      for(Subscriber subscriber : subscriberList_)
      {
        subscriber.startAsync();
        log_.info("Subscribing to " + subscriber.getSubscriptionNameString() + "...");
      }
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
        log_.error("Subscription " + subscriptionName + " on topic " + topicName + " DOES NOT EXIST.");
        subscriptionErrorCnt_++;
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
  protected void stopSubscriptions()
  {
    synchronized (subscriberList_)
    {
      for(Subscriber subscriber : subscriberList_)
      {
        try
        {
          log_.info("Stopping subscriber " + subscriber.getSubscriptionNameString() + "...");
          
          subscriber.stopAsync().awaitTerminated();
          
          log_.info("Stopped subscriber " + subscriber.getSubscriptionNameString());
        }
        catch(RuntimeException e)
        {
          log_.error("Failed to stop subscriber " + subscriber.getSubscriptionNameString(), e);
        }
      }
    }
  }
}
