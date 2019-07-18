/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.aws.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.FugueComponentContainer;
import org.symphonyoss.s2.fugue.IFugueAssemblyBuilder;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.counter.BusyCounter;
import org.symphonyoss.s2.fugue.counter.IBusyCounter;
import org.symphonyoss.s2.fugue.counter.ITopicBusyCounterFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * A lambda function implementation based on a Fugue Assembly.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class AwsAssemblyLambda implements RequestStreamHandler
{
  private static final Logger log_ = LoggerFactory.getLogger(AwsAssemblyLambda.class);

  protected abstract IFugueAssemblyBuilder<?,?> createBuilder();

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
  {
    try
    {
      FugueComponentContainer container = new FugueComponentContainer();
      
      IFugueAssemblyBuilder<?, ?> builder = createBuilder();
      
      builder
          .withContainer(container)
          .withBusyCounterFactory(new LambdaBusyCounterFactory(container,
              builder.getConfiguration().getConfiguration("com/symphony/s2/legacy/message/forwarder/AwsForwarderComponent"))) // TODO: refactor this into Symphony code
          .build();
            
      container.start();
      
      try
      {
        int timeout = context.getRemainingTimeInMillis() - 60000;
        
        log_.info("context.getRemainingTimeInMillis()=" + context.getRemainingTimeInMillis() + ", timeout=" + timeout);
        
        container.mainLoop(timeout);
      }
      finally
      {
        log_.info("Quiescing...");
        container.quiesce();
        log_.info("Stopping...");
        container.stop();
        log_.info("Stopping...Done.");
      }
      
      new AwsLambdaResponse(200, "OK").write(outputStream);
      
      
    }
    catch(IOException | InterruptedException e)
    {
      log_.error("Failed to process", e);
      try
      {
        new AwsLambdaResponse(500, e.toString()).write(outputStream);
      }
      catch (IOException e1)
      {
        log_.error("Failed to write error", e1);
      }
    }
  }
  
  class LambdaBusyCounterFactory implements ITopicBusyCounterFactory
  {
    private final FugueComponentContainer container_;
    private final IConfiguration config_;

    public LambdaBusyCounterFactory(FugueComponentContainer container, IConfiguration config)
    {
      container_ = container;
      config_ = config;
    }

    @Override
    public IBusyCounter create(String topicId)
    {
      return new LambdaBusyCounter(container_, config_);
    }
    
  }
  
  class LambdaBusyCounter extends BusyCounter
  {
    private FugueComponentContainer container_;

    public LambdaBusyCounter(FugueComponentContainer container, IConfiguration config)
    {
      super(config);
      container_ = container;
    }

    @Override
    protected boolean scaleDown()
    {
      log_.info("Received SCALE DOWN so terminating main loop...");
      container_.setRunning(false);
      
      return true;
    }
  }
}
