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
	
	private Spider spider;
	private static final String TEST_PATH = "http://www.bestdealaz.com";

	@Before
	public void setUp() throws Exception {
		spider = new Spider(null);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	public void testSetRequestProperties() throws IOException
	{
		
			URL url = new URL(TEST_PATH);
			URLConnection connection = url.openConnection();
			spider.setRequestProperties(connection);
			assertEquals(USER_AGENT_VALUE,connection.getRequestProperty(USER_AGENT_FIELD));		
			
	}

	public void testPause() throws IOException
	{
					
	}
}
