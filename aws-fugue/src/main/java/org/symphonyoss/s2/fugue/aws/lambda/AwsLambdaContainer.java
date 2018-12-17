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
import org.symphonyoss.s2.fugue.IFugueAssembly;
import org.symphonyoss.s2.fugue.IFugueAssemblyBuilder;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.counter.BusyCounter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class AwsLambdaContainer implements RequestStreamHandler
{
  private static final Logger log_ = LoggerFactory.getLogger(AwsLambdaContainer.class);
  private IFugueAssemblyBuilder<?, ?> builder_;



  public AwsLambdaContainer(IFugueAssemblyBuilder<?,?> builder)
  {
    builder_ = builder;
  }



  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
  {
    try
    {
//      ByteArrayOutputStream bout = new ByteArrayOutputStream();
//      byte[] buf = new byte[1024];
//      int nbytes;
//      
//      while((nbytes = inputStream.read(buf))>0)
//      {
//        bout.write(buf, 0, nbytes);
//      }
//      
//      ImmutableByteArray  bytes = ImmutableByteArray.newInstance(bout.toByteArray());
//      
//      IForwarderRequest request = ForwarderRequest.FACTORY.newInstance(JacksonAdaptor.parseObject(bytes).immutify());
//      
//      log_.info("Started, request=" + request);
//      
//      final String[] topics = new String[request.getTopics().size()];
//      
//      for(int i=0 ; i<topics.length ; i++)
//        topics[i] = request.getTopics().get(i);
//      
//      System.setProperty(Fugue.FUGUE_CONFIG, request.getFugueConfig());
      
      FugueComponentContainer container = new FugueComponentContainer();
      
      IFugueAssembly assembly = builder_
          .withContainer(container)
          .withBusyCounter(new LambdaBusyCounter(container,
              builder_.getConfiguration().getConfiguration("com/symphony/s2/legacy/message/forwarder/AwsForwarderComponent")))
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
