/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.IConfigurationProvider;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.google.config.GoogleConfigKey;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.SubscriptionName;
import org.symphonyoss.s2.fugue.naming.TopicName;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.ISubscriberManager;
import org.symphonyoss.s2.fugue.pubsub.Subscription;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectSubscriptionName;

/* package */ class GoogleAbstractSubscriberManager<T extends ISubscriberManager<ImmutableByteArray,T>> extends AbstractSubscriberManager<ImmutableByteArray, T>
{
  private static final Logger          log_            = LoggerFactory.getLogger(GoogleAbstractSubscriberManager.class);

  /* package */ final INameFactory           nameFactory_;
  /* package */ final IConfigurationProvider config_;
  /* package */ final boolean                startSubscriptions_;

  /* package */ List<Subscriber>             subscriberList_ = new LinkedList<>();
  /* package */ String projectId_;
  
  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param config                          The configuration to use.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   * @param initialize                      If true then create subscriptions (use PublisherManager to create topics)
   */
  /* package */ GoogleAbstractSubscriberManager(Class<T> type, INameFactory nameFactory, IConfigurationProvider config,
      ITraceContextFactory traceFactory,
      IThreadSafeConsumer<ImmutableByteArray> unprocessableMessageConsumer)
  {
    super(type, config, traceFactory, unprocessableMessageConsumer);
    
    nameFactory_ = nameFactory;
    config_ = config;
    startSubscriptions_ = true;
  }
  
  /**
   * Construct a subscriber manager without and subscription processing.
   * 
   * @param nameFactory                     A NameFactory.
   * @param config                          The configuration to use.
   * @param traceFactory                    A trace context factory.
   * @param initialize                      If true then create subscriptions (use PublisherManager to create topics)
   */
  /* package */ GoogleAbstractSubscriberManager(Class<T> type, INameFactory nameFactory, IConfigurationProvider config,
      ITraceContextFactory traceFactory)
  {
    super(type, config, traceFactory, null);
    
    nameFactory_ = nameFactory;
    config_ = config;
    startSubscriptions_ = false;
  }
  
  @Override
  public void start()
  {
    IConfigurationProvider googleConfig = config_.getConfiguration(GoogleConfigKey.GOOGLE);
    projectId_ = googleConfig.getRequiredString(GoogleConfigKey.PROJECT_ID);
  
    log_.info("Starting GoogleSubscriberManager in project " + projectId_ + "...");
    
    super.start();
  }
  
  @Override
  protected void startSubscription(Subscription<ImmutableByteArray> subscription)
  {
    
    
//    // Instantiate an asynchronous message receiver
//    MessageReceiver receiver = new MessageReceiver()
//    {
//      @Override
//      public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer)
//      {
//        try
//        {
//          ITraceContext trace = getTraceFactory().createTransaction(PubsubMessage.class.getName(), message.getMessageId());
//
//          ByteString bytes = message.getData();
//          String string = bytes.toStringUtf8();
//          
//          ImmutableByteArray byteArray = ImmutableByteArray.newInstance(message.getData());
//          
//          long retryTime = handleMessage(byteArray, trace);
//          
//          if(retryTime < 0)
//          {
//            trace.trace("ABOUT_TO_ACK");
//            consumer.ack();
//          }
//          else
//          {
//            // TODO: do we need to do this or is it better to do nothing so the ack timeout exceeds,
//            // given that the async library does extension of ack deadlines it's unclear
//            consumer.nack();
//          }
//          
//          trace.finished();
//        }
//        catch (RuntimeException e)
//        {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//        }
//      }
//    };
    
    if(startSubscriptions_)
    {
      for(String topic : subscription.getTopicNames())
      {
        log_.info("Subscribing to topic " + topic + "...");
        
        TopicName               topicName = nameFactory_.getTopicName(topic);
        
        SubscriptionName        subscriptionName = nameFactory_.getSubscriptionName(topicName, subscription.getSubscriptionName());

        GoogleSubscriber        receiver                = new GoogleSubscriber(this, getTraceFactory(), subscription.getConsumer());
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
    
    
    
    
//    IConfigurationProvider metaConfig = config_.getConfiguration(FugueConfigKey.META);
//    
//    String projectId = googleConfig.getRequiredString(GoogleConfigKey.PROJECT_ID);
//    
//    log_.info("Starting GoogleSubscriberManager in project " + projectId + "...");
//    
//    for(Entry<String, Set<String>> entry : subscriptionsByTopic.entrySet())
//    {
//      TopicName topicName = nameFactory_.getTopicName(metaConfig.getRequiredString(entry.getKey()));
//      
//      for(String subscription : entry.getValue())
//      {
//          SubscriptionName subscriptionName = nameFactory_.getSubscriptionName(topicName, metaConfig.getRequiredString(subscription));
//        
//        if(initialize_)
//          createSubcription(topicName, subscriptionName);
//        
//        ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId, subscriptionName.toString());      
//        Subscriber              subscriber              = Subscriber.newBuilder(projectSubscriptionName, receiver).build();
//        
//        subscriber.addListener(new Subscriber.Listener()
//        {
//          @Override
//          public void failed(Subscriber.State from, Throwable failure)
//          {
//            log_.error("Error for " + projectSubscriptionName + " from " + from, failure);
//          }
//        }, MoreExecutors.directExecutor());
//        
//        synchronized (subscriberList_)
//        {
//          subscriberList_.add(subscriber);
//        }
//        
//        subscriber.startAsync();
//        log_.info("Subscribing to " + projectSubscriptionName + "...");
//      }
//    }
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
