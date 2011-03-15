package bdm;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * That class implements a reusable spider
 * 
 * @author Jeff Heaton(http://www.jeffheaton.com)
 * @version 1.0
 */
public class Spider {

	/**
	 * A collection of URLs that resulted in an error
	 */
	Collection<URL> deadLinksProcessed = new HashSet<URL>();

	/**
	 * A collection of URLs that are waiting to be processed
	 */
	Collection<URL> activeLinkQueue = new HashSet<URL>();
	
	/**
	 * A collection of URLs that were processed
	 */
	Collection<URL> goodInternalLinksProcessed = new HashSet<URL>();

	
	Collection<URL> goodExternalLinksProcessed = new HashSet<URL>();
	/**
	 * The class that the spider should report its URLs to
	 */
	protected ISpiderReportable report;

	/**
	 * A flag that indicates whether this process
	 * should be canceled
	 */
	protected boolean pause = false;

	/**
	 * The constructor
	 * 
	 * @param report A class that implements the ISpiderReportable
	 * interface, that will receive information that the
	 * spider finds.
	 */
	public Spider(ISpiderReportable report)
	{
		this.report = report;
	}

	/**
	 * Get the URLs that resulted in an error.
	 * 
	 * @return A collection of URL's.
	 */
	public Collection<URL> getDeadLinksProcessed()
	{
		return deadLinksProcessed;
	}

	/**
	 * Get the URLs that were waiting to be processed.
	 * You should add one URL to this collection to
	 * begin the spider.
	 * 
	 * @return A collection of URLs.
	 */
	public Collection<URL> getActiveLinkQueue()
	{
		return activeLinkQueue;
	}

	/**
	 * Get the URLs that were processed by this spider.
	 * 
	 * @return A collection of URLs.
	 */
	public Collection<URL> getGoodInternalLinksProcessed()
	{
		return goodInternalLinksProcessed;
	}    
	
	public Collection<URL> getGoodExternalLinksProcessed() {
		return goodExternalLinksProcessed;
	}
	/**
	 * Clear all of the workloads.
	 */
	public void clear()
	{
		getDeadLinksProcessed().clear();
		getActiveLinkQueue().clear();
    	getGoodInternalLinksProcessed().clear();
    	getGoodExternalLinksProcessed().clear();
	}
	
	/**
	 * Set a flag that will cause the begin
	 * method to return before it is done.
  	 */
	public void pause()
	{
		pause = true;
	}

	/**
	 * Add a URL for processing.
	 * 
	 * @param url
	 */
	public void addURL(URL url)
	{
		if ( getActiveLinkQueue().contains(url) )
		  return;
		if ( getDeadLinksProcessed().contains(url) )
		  return;
		if ( getGoodInternalLinksProcessed().contains(url) )
		  return;
		if ( getGoodExternalLinksProcessed().contains(url) )
		  return;
		log("Adding to workload: " + url );
		getActiveLinkQueue().add(url);
	}

	/**
	 * Called internally to process a URL
   	* 
   	* @param url The URL to be processed.
   	*/
	public void processURL(URL url)
	{
		try 
		{
			log("Processing: " + url );
			// get the URL's contents
			
			URLConnection connection = url.openConnection();
//			connection.setRequestProperty(, value)
			if ( (connection.getContentType()!=null) && !connection.getContentType().toLowerCase().startsWith("text/") ) 
			{
				getActiveLinkQueue().remove(url);
				getGoodInternalLinksProcessed().add(url);
				log("Not processing because content type is: " +
						connection.getContentType() );
				return;
			}
  
			// read the URL
			InputStream is = connection.getInputStream();
			Reader r = new InputStreamReader(is);
			// parse the URL
			HTMLEditorKit.Parser parser = new HTMLParser().getParser();
			parser.parse(r,new Parser(url),true);
			
			// mark URL as complete
			getActiveLinkQueue().remove(url);
			getGoodInternalLinksProcessed().add(url);
			log("Complete: " + url );
		} catch ( IOException e ) 
		{
			getActiveLinkQueue().remove(url);
			getDeadLinksProcessed().add(url);
			log("Error: " + url );
			report.spiderURLError(url);
		}
	}

	public void resume(){
		start();
	}
	/**
	 * Called to start the spider
	 */
	public void start()
	{
		pause = false;
		while ( !getActiveLinkQueue().isEmpty() && !pause ) 
		{
			Object list[] = getActiveLinkQueue().toArray();
			for ( int i=0;(i<list.length)&&!pause;i++ )
				processURL((URL)list[i]);
		}
	}

	/**
	 * A HTML parser callback used by this class to detect links
	 * 
	 * @author Jeff Heaton
	 * @version 1.0
	 */
	class Parser extends HTMLEditorKit.ParserCallback 
	{
		URL base;
		
		public Parser(URL base)
		{
		  this.base = base;
		}
		
		public void handleSimpleTag(HTML.Tag tag,MutableAttributeSet attributes,int pos)
		{
			String href = (String)attributes.getAttribute(HTML.Attribute.HREF);
		  
			if( (href==null) && (tag==HTML.Tag.FRAME) )
				href = (String)attributes.getAttribute(HTML.Attribute.SRC);
		    
			if ( href==null )
				return;
		
			int i = href.indexOf('#');
			if ( i!=-1 )
				href = href.substring(0,i);
		
			if ( href.toLowerCase().startsWith("mailto:") ) 
			{
				report.spiderFoundEMail(href);
				return;
			}
		
			handleLink(base,href);
		}
		
		public void handleStartTag(HTML.Tag t,MutableAttributeSet a,int pos)
		{
			handleSimpleTag(t,a,pos);    // handle the same way
		}
		
		protected void handleLink(URL base,String str)
		{
			try {
				URL url = new URL(base,str);
				if ( report.spiderFoundURL(base,url) )
					addURL(url);
			} catch ( MalformedURLException e ) {
				log("Found malformed URL: " + str );
			}
		}
	}

	/**
	 * Called internally to log information
	 * This basic method just writes the log
	 * out to the stdout.
	 * 
	 * @param entry The information to be written to the log.
	 */
	public void log(String entry)
	{
		System.out.println( (new Date()) + ":" + entry );
	}
}