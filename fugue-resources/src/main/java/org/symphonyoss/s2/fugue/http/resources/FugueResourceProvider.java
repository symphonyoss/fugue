package org.symphonyoss.s2.fugue.http.resources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.http.IResourceProvider;
import org.slf4j.Logger;

public class FugueResourceProvider implements IResourceProvider
{
  private static final Logger   log_ = LoggerFactory.getLogger(FugueResourceProvider.class);
  private static final String[] ALLOWED_PATHS = new String[] 
      {
        "/html/", "/css/", "/images/"  
      };

  @Override
  public String loadResourceAsString(Path path)
  {
    try
    {
      try(Reader in = new InputStreamReader(getResourceUrl(path).openStream()))
      {
        StringBuffer  s       = new StringBuffer();
        char[]        buf     = new char[1024];
        int           nbytes;
        
        while((nbytes = in.read(buf))>0)
        {
          s.append(buf, 0, nbytes);
        }
        
        return s.toString();
      }
    }
    catch(IOException e)
    {
      log_.error("Exception closing InputStream", e);
      return e.toString();
    }
  }

  @Override
  public URL getResourceUrl(String path)
  {
    if(pathIsDisallowed(path))
      return null;
    
//    while(path.startsWith("/"))
//      path = path.substring(1);
    
    return getClass().getResource(path);
  }
  
  private boolean pathIsDisallowed(String path)
  {
    if(path.endsWith("/"))
      return true;
    
    for(String prefix : ALLOWED_PATHS)
      if(path.startsWith(prefix))
        return false;
    
    return true;
  }

  @Override
  public URL getResourceUrl(Path path)
  {
    return getResourceUrl(path.toString());
  }
}
