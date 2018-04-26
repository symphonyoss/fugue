/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.strategy.naming.INamingStrategy;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractSubscriberManager;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

public class GoogleSubscriberManager extends AbstractSubscriberManager<GoogleSubscriberManager>
{
  private static final Logger   log_            = LoggerFactory.getLogger(GoogleSubscriberManager.class);

  private final String          projectId_;
  private final INamingStrategy namingStrategy_;

  private List<Subscriber>      subscriberList_ = new LinkedList<>();
  
  public GoogleSubscriberManager(String projectId, INamingStrategy namingStrategy, ITraceContextFactory traceFactory,
      IThreadSafeRetryableConsumer<ImmutableByteArray> consumer)
  {
    super(GoogleSubscriberManager.class, namingStrategy, traceFactory, consumer, new UnprocessableMessageConsumer());
    
    projectId_ = projectId;
    namingStrategy_ = namingStrategy;
  }
  
  @Override
  protected void startSubscriptions(Map<String, Set<String>> subscriptionsByTopic,
      Map<String, Set<String>> topicsBySubscription)
  {
    // Instantiate an asynchronous message receiver
    MessageReceiver receiver = new MessageReceiver()
    {
      @Override
      public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer)
      {
        try
        {
          ITraceContext trace = getTraceFactory().createTransaction(PubsubMessage.class.getName(), message.getMessageId());

          long retryTime = handleMessage(ImmutableByteArray.newInstance(message.getData()), trace);
          
          if(retryTime < 0)
          {
            trace.trace("ABOUT_TO_ACK");
            consumer.ack();
          }
          else
          {
            // TODO: do we need to do this or is it better to do nothing so the ack timeout exceeds,
            // given that the async library does extension of ack deadlines it's unclear
            consumer.nack();
          }
          
          trace.finished();
        }
        catch (RuntimeException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    };
    
    for(Entry<String, Set<String>> entry : subscriptionsByTopic.entrySet())
    {
      for(String subscriptionName : entry.getValue())
      {
        ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId_, namingStrategy_.getSubscriptionName(entry.getKey(), subscriptionName));      
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

  static class UnprocessableMessageConsumer implements IThreadSafeConsumer<ImmutableByteArray>
  {

    @Override
    public void consume(ImmutableByteArray item, ITraceContext trace)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void close()
    {
      // TODO Auto-generated method stub
      
    }
    
  }
    
}
