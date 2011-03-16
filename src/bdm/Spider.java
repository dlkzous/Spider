package bdm;

import java.util.*;
import java.net.*;
import java.io.*;

import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * That class implements a spider
 * 
 * @author BDM based on Jeff Heaton's Spider
 * @version 1.0
 */
public class Spider {

	private static final String ROBOTS_TXT_URL = "http://poplar.dcs.shef.ac.uk/~u0082/intelweb2/robots.txt";

	/*
	 * A string value for the user agent field
	 */
	public static final String USER_AGENT_FIELD = "User-Agent";

	/*
	 * A string value for the user agent value
	 */
	public static final String USER_AGENT_VALUE = "BDM_University_of_Sheffield/1.0";

	public static final String ACCEPT_LANGUAGE_FIELD = "Accept-Language";

	public static final String ACCEPT_LANGUAGE_VALUE = "en";

	public static final String CONTENT_TYPE_FIELD = "Content-Type";

	public static final String CONTENT_TYPE_VALUE = "application/xwww-form-urlencoded";

	/**
	 * Request properties for the spider crawling the web
	 */
	private static final Map<String, String> REQUEST_PROPERTIES = new HashMap<String, String>();
	static {
		REQUEST_PROPERTIES.put(USER_AGENT_FIELD, USER_AGENT_VALUE);
		REQUEST_PROPERTIES.put(ACCEPT_LANGUAGE_FIELD, ACCEPT_LANGUAGE_VALUE);
		REQUEST_PROPERTIES.put(CONTENT_TYPE_FIELD, CONTENT_TYPE_VALUE);

	}

	/**
	 * urls disallowed by robots.txt
	 */
	private Set<URL> disallowedURLs = new HashSet<URL>();
	
	/**
	 * base url this spider operates on
	 */
	private URL base;
	
	/**
	 * delay between fetching urls
	 */
	private long crawlDelay;
	
	/**
	 * file path to save local urls
	 */
	private String localURLsPath;
	
	/**
	 * file path to save external urls
	 */
	private String externalURLsPath;
	/**
	 * A collection of URLs that resulted in an error
	 */
	private Collection<URL> deadLinksProcessed = new HashSet<URL>();

	/**
	 * A collection of URLs that are waiting to be processed
	 */
	private Collection<URL> activeLinkQueue = new HashSet<URL>();

	/**
	 * A collection of internal URLs that were processed
	 */
	private Collection<URL> internalLinksProcessed = new HashSet<URL>();

	/**
	 * A collection of external URLs that were processed
	 */
	private Collection<URL> externalLinksProcessed = new HashSet<URL>();
	/**
	 * The class that the spider should report its URLs to
	 */

	/**
	 * The spider thread
	 */
	private Thread processingThread;

	/**
	 * Is the spider working at the moment?
	 */
	private volatile boolean running = false;

	/**
	 * The constructor intitalizes the base url, the file paths and read robots.txt
	 * 
	 * @param base
	 *            
	 */
	public Spider(URL base) {
		this.base = base;
		this.localURLsPath = base.getHost() + "_localIWURLs";
		this.externalURLsPath = base.getHost() + "_externalIWURLs";
		
		getActiveLinkQueue().add(base);
		initDisallowedURLs();
	}

	/**
	 * set up disallowed urls from robots.txt
	 */
	private void initDisallowedURLs() {
		try {
			URL robotURL = new URL(ROBOTS_TXT_URL);
			URLConnection robotConn = robotURL.openConnection();
			Scanner reader = new Scanner(robotConn.getInputStream());
			boolean userAgentMatched = false;
			while (reader.hasNextLine()){
				String line = reader.nextLine();
				line = line.trim().toLowerCase();
				
				if (line.startsWith("user-agent:")){
					final int userAgentLength = 11;
					line = line.substring(userAgentLength).trim();
					if (line.equals(USER_AGENT_VALUE) || line.equals("*")){
						userAgentMatched = true;
					} else {
						userAgentMatched = false;
					}
					continue;
				}
				
				if (line.startsWith("disallow:")){
					if (!userAgentMatched){
						continue;
					}
					final int disallowedLength = 9;
					line = line.substring(disallowedLength).trim();
					URL disallowedURL = new URL(this.base,line);
					this.disallowedURLs.add(disallowedURL);
				}
				
				if (line.startsWith("crawl-delay:")){
					final int crawlDelayLength = 12;
					line = line.substring(crawlDelayLength).trim();
					this.crawlDelay = (long) (Double.parseDouble(line)*1000);
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the URLs that resulted in an error.
	 * 
	 * @return A collection of URL's.
	 */
	public Collection<URL> getDeadLinksProcessed() {
		return this.deadLinksProcessed;
	}

	/**
	 * Get the URLs that were waiting to be processed. You should add one URL to
	 * this collection to begin the spider.
	 * 
	 * @return A collection of URLs.
	 */
	public Collection<URL> getActiveLinkQueue() {
		return this.activeLinkQueue;
	}

	/**
	 * Get the URLs that were processed by this spider.
	 * 
	 * @return A collection of URLs.
	 */
	public Collection<URL> getInternalLinksProcessed() {
		return this.internalLinksProcessed;
	}

	public Collection<URL> getExternalLinksProcessed() {
		return this.externalLinksProcessed;
	}

	/**
//	 * Clear all of the workloads.
//	 */
//	public void clear() {
//		getDeadLinksProcessed().clear();
//		getActiveLinkQueue().clear();
//		getGoodInternalLinksProcessed().clear();
//		getGoodExternalLinksProcessed().clear();
//	}

	/**
	 * Add a URL for processing.
	 * 
	 * @param url
	 */
	public void addURL(URL url) {
		if (getActiveLinkQueue().contains(url))
			return;
		if (getDeadLinksProcessed().contains(url))
			return;
		if (getInternalLinksProcessed().contains(url))
			return;
		if (getExternalLinksProcessed().contains(url))
			return;
		log("Adding to workload: " + url);
		getActiveLinkQueue().add(url);
	}

	public boolean isParseable(URLConnection connection){
		return !((connection.getContentType() != null)
		&& !connection.getContentType().toLowerCase()
		.startsWith("text/"));
	}
	/**
	 * Called internally to process a URL
	 * 
	 * @param url
	 *            The URL to be processed.
	 */
	public void processURL(URL url) {
		getActiveLinkQueue().remove(url);
		log("Processing: " + url);
		try {
			
			
			if (!isInternal(url)){
				log("External link - " + url);
				getExternalLinksProcessed().add(url);
				return;
			}
			if (!isRobotAllowed(url)){
				log("Disallowed by robots.txt - " + url);
				getInternalLinksProcessed().add(url);
				return;
			}
			
			URLConnection connection = url.openConnection();
			if (!isParseable(connection)) {
				log("Not processing because content type is: "
						+ connection.getContentType());
				getInternalLinksProcessed().add(url);
				return;
			}

			// read the URL
			InputStream is = connection.getInputStream();
			Reader r = new InputStreamReader(is);
			// parse the URL
			HTMLEditorKit.Parser parser = new HTMLParser().getParser();
			parser.parse(r, new Parser(url), true);

			// mark URL as complete
			getInternalLinksProcessed().add(url);
			log("Complete: " + url);
		} catch (IOException e) {
			getDeadLinksProcessed().add(url);
			log("Error: " + url);
		}
	}
	

	public boolean isInternal(URL url) {
		return url.getHost().equalsIgnoreCase(this.base.getHost());
	}

	/**
	 * Called to start the spider with a base url
	 */
	public void start() {
		this.processingThread = Thread.currentThread();
		
		processActiveQueue();

	}

	/**
	 * Stops the spider permanently.
	 */
	public void stop() {
		if (this.processingThread != null) {
			this.processingThread.interrupt();
		}
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					printToFile();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
			}
			
		}).start();
	}

	/**
	 * Resumes processing active links
	 */
	public void resume() {
		processActiveQueue();
	}

	/**
	 * Pauses the spider
	 */
	public void pause() {
		this.running = false;
	}

	/**
	 * 
	 * @return is the parser running?
	 */
	public boolean isRunning() {
		return this.running;
	}
	
	/**
	 * takes a url from the active queue and processes it, then 
	 * prints to file in the end. Stops if paused.
	 */
	public synchronized void processActiveQueue() {
		if (this.running) {
			return;
		}

		this.running = true;
		for (URL currUrl : getActiveLinkQueue()) {
			if (!this.running) {
				return;
			}
			processURL(currUrl);
			try {
				Thread.sleep(this.crawlDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			printToFile();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
	}

	/**
	 * A HTML parser callback used by this class to detect links
	 * 
	 */
	private class Parser extends HTMLEditorKit.ParserCallback {
		private URL parserBase;

		public Parser(URL base) {
			this.parserBase = base;
		}

		@Override
		public void handleSimpleTag(HTML.Tag tag,
				MutableAttributeSet attributes, int pos) {
			String href = (String) attributes.getAttribute(HTML.Attribute.HREF);

			if ((href == null) && (tag == HTML.Tag.FRAME))
				href = (String) attributes.getAttribute(HTML.Attribute.SRC);

			if (href == null)
				return;

			int i = href.indexOf('#');
			if (i != -1)
				href = href.substring(0, i);

			if (href.toLowerCase().startsWith("mailto:")) {
				return;
			}

			handleLink(href);
		}

		@Override
		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			handleSimpleTag(t, a, pos); // handle the same way
		}

		protected void handleLink(String str) {
			try {
				URL url = new URL(this.parserBase, str);
//				if (Spider.this.report.spiderFoundURL(this.parserBase, url))
					addURL(url);
			} catch (MalformedURLException e) {
				log("Found malformed URL: " + str);
			}
		}
	}

	/**
	 * Called internally to log information This basic method just writes the
	 * log out to the stdout.
	 * 
	 * @param entry
	 *            The information to be written to the log.
	 */
	public void log(String entry) {
		System.out.println((new Date()) + ":" + entry);
	}

	/**
	 * Adds the Spider's headers to the connection
	 * 
	 * @param connection
	 *            the connection to set the request properties for
	 */
	public void setRequestProperties(URLConnection connection) {
		for (String key : REQUEST_PROPERTIES.keySet()) {
			connection.setRequestProperty(key, REQUEST_PROPERTIES.get(key));
		}

	}

	/**
	 * cecks that a url is allowed by robots.txt
	 * @param checkURL the url to check
	 * @return is the url allowed?
	 */
	public boolean isRobotAllowed(URL checkURL) {
		return !this.disallowedURLs.contains(checkURL);
	}

	/**
	 * 
	 * @return a set of disallowed urls
	 */
	public Set<URL> getDisallowedURLs() {
		return this.disallowedURLs;
	}
	
	/**
	 * prints internal and external urls to two files
	 * @throws FileNotFoundException
	 */
	public void printToFile() throws FileNotFoundException{
		PrintWriter internalURLWriter = new PrintWriter(this.localURLsPath);
		for (URL url : getInternalLinksProcessed()){
			internalURLWriter.println(url);
		}
		internalURLWriter.flush();
		internalURLWriter.close();
		
		PrintWriter externalURLWriter = new PrintWriter(this.externalURLsPath);
		for (URL url : getExternalLinksProcessed()){
			externalURLWriter.println(url);
		}
		externalURLWriter.flush();
		externalURLWriter.close();
	}
}