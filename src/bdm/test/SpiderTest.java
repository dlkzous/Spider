package bdm.test;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import bdm.Spider;
import static bdm.Spider.*;

public class SpiderTest extends TestCase {
	
	Spider spider;
	private static final String TEST_PATH = "http://www.bestdealaz.com";

	@Override
	@Before
	public void setUp() throws Exception {
		this.spider = new Spider(null);
	}

	@Override
	@After
	public void tearDown() throws Exception {
	}
	
	public void testSetRequestProperties() throws IOException
	{
		
			URL url = new URL(TEST_PATH);
			URLConnection connection = url.openConnection();
			this.spider.setRequestProperties(connection);
			assertEquals(USER_AGENT_VALUE,connection.getRequestProperty(USER_AGENT_FIELD));		
			
	}

	public void testPause() throws IOException, InterruptedException
	{
			final Thread spiderThread = new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						SpiderTest.this.spider.start(new URL(TEST_PATH));
						
					} catch (MalformedURLException e) {
						fail(e.toString());
					}
				}
				
			});	
			spiderThread.start();
			Thread.sleep(1000); // wait for other thread to finish
			SpiderTest.this.spider.stop();
			assertTrue(spiderThread.isInterrupted());
			
	}
}
