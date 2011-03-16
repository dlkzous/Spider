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
	
	private static final Map< String, String> REQUEST_PROPERTIES = new HashMap<String, String>();
	static {
		REQUEST_PROPERTIES.put("User-Agent", "BDM_University_of_Sheffield/1.0");
		REQUEST_PROPERTIES.put("Accept-Language", "en");
		REQUEST_PROPERTIES.put("Content-Type", "application/xwww-form-urlencoded");
	
	}
	/*
	 * A string value for the user agent field
	 */	
	public static final String USER_AGENT_FIELD = "User-Agent";
	
	/*
	 * A string value for the user agent value
	 */
	public static final String USER_AGENT_VALUE = "BDM_University_of_Sheffield/1.0";

	/**
	 * A collection of URLs that resulted in an error
	 */
	private Collection<URL> deadLinksProcessed = new HashSet<URL>();

	/**
	 * A collection of URLs that are waiting to be processed
	 */
	private Collection<URL> activeLinkQueue = new HashSet<URL>();
	
	/**
	 * A collection of URLs that were processed
	 */
	private Collection<URL> goodInternalLinksProcessed = new HashSet<URL>();

	
	private Collection<URL> goodExternalLinksProcessed = new HashSet<URL>();
	/**
	 * The class that the spider should report its URLs to
	 */
	ISpiderReportable report;
	
	private Thread processingThread;

	/**
	 * A flag that indicates whether this process
	 * should be canceled
	 */
	private volatile boolean running = false;

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
		return this.deadLinksProcessed;
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
		return this.activeLinkQueue;
	}

	/**
	 * Get the URLs that were processed by this spider.
	 * 
	 * @return A collection of URLs.
	 */
	public Collection<URL> getGoodInternalLinksProcessed()
	{
		return this.goodInternalLinksProcessed;
	}    
	
	public Collection<URL> getGoodExternalLinksProcessed() {
		return this.goodExternalLinksProcessed;
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
			this.report.spiderURLError(url);
		}
	}
	/**
	 * Called to start the spider with a base url
	 */
	public void start(URL base)
	{
		this.processingThread = Thread.currentThread();
		getActiveLinkQueue().add(base);
		processActiveQueue();
		
	}

	/**
	 * Stops the spider permanently.
	 */
	public void stop(){
		if (this.processingThread != null){
			this.processingThread.interrupt();
		}
	}
	
	

	/**
	 * Resumes processing active links
	 */
	public void resume(){
		processActiveQueue();
	}
	/**
	 * Pauses the spider
	 */
	public void pause()
	{
		this.running = false;
	}

	public void processActiveQueue(){
		if (this.running){
			return;
		}
		
		this.running = true;
		for (URL currUrl : getActiveLinkQueue()){
			if (!this.running){
				break;
			}
			processURL(currUrl);
		}
	}

	/**
	 * A HTML parser callback used by this class to detect links
	 * 
	 * @author Jeff Heaton
	 * @version 1.0
	 */
	private class Parser extends HTMLEditorKit.ParserCallback 
	{
		private URL base;
		
		public Parser(URL base)
		{
		  this.base = base;
		}
		
		@Override
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
				Spider.this.report.spiderFoundEMail(href);
				return;
			}
		
			handleLink(href);
		}
		
		@Override
		public void handleStartTag(HTML.Tag t,MutableAttributeSet a,int pos)
		{
			handleSimpleTag(t,a,pos);    // handle the same way
		}
		
		protected void handleLink(String str)
		{
			try {
				URL url = new URL(this.base,str);
				if ( Spider.this.report.spiderFoundURL(this.base,url) )
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

	/**
	 * Adds the Spider's headers to the connection
	 * 
	 * @param connection the connection to set the request properties for
	 */
	public void setRequestProperties(URLConnection connection) {
		for(String key: REQUEST_PROPERTIES.keySet())
		{
			connection.setRequestProperty(key, REQUEST_PROPERTIES.get(key));
		}
		
		
	}
}