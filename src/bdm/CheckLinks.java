package bdm;

import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;

/**
 * This example uses a Java spider to scan a Web site
 * and check for broken links. Written by Jeff Heaton.
 * Jeff Heaton is the author of "Programming Spiders,
 * Bots, and Aggregators" by Sybex. Jeff can be contacted
 * through his Web site at http://www.jeffheaton.com.
 * 
 * @author Jeff Heaton(http://www.jeffheaton.com)
 * @version 1.0
 */
@SuppressWarnings("serial")
public class CheckLinks extends javax.swing.JFrame implements Runnable,ISpiderReportable {
	
	/**
	 * The constructor. Perform setup here.
	 */
	public CheckLinks()
	{
		//{{INIT_CONTROLS
		setTitle("Find Broken Links");
		getContentPane().setLayout(null);
		setSize(405,288);
		setVisible(false);
		this.label1.setText("Enter a URL:");
		getContentPane().add(this.label1);
		this.label1.setBounds(12,12,84,12);
		this.begin.setText("Begin");
		this.begin.setActionCommand("Begin");
		getContentPane().add(this.begin);
		this.begin.setBounds(12,36,84,24);
		getContentPane().add(this.url);
		this.url.setBounds(108,36,288,24);
		this.errorScroll.setAutoscrolls(true);
		this.errorScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.errorScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.errorScroll.setOpaque(true);
		getContentPane().add(this.errorScroll);
		this.errorScroll.setBounds(12,120,384,156);
		this.errors.setEditable(false);
		this.errorScroll.getViewport().add(this.errors);
		this.errors.setBounds(0,0,366,138);
		this.current.setText("Currently Processing: ");
		getContentPane().add(this.current);
		this.current.setBounds(12,72,384,12);
		this.goodLinksLabel.setText("Good Links: 0");
		getContentPane().add(this.goodLinksLabel);
		this.goodLinksLabel.setBounds(12,96,192,12);
		this.badLinksLabel.setText("Bad Links: 0");
		getContentPane().add(this.badLinksLabel);
		this.badLinksLabel.setBounds(216,96,96,12);
		SymAction lSymAction = new SymAction();
		this.begin.addActionListener(lSymAction);
	}
	
	/**
	 * Main method for the application
	 * @param args Not used
	 */
	static public void main(String args[])
	{
		(new CheckLinks()).setVisible(true);
	}
	
	/**
	* Add notifications.
	*/
	@Override
	public void addNotify()
	{
		// Record the size of the window prior to calling parent's
		// addNotify.
		Dimension size = getSize();
	
		super.addNotify();
	
		if ( this.frameSizeAdjusted )
			return;
		this.frameSizeAdjusted = true;
	
		// Adjust size of frame according to the insets and menu bar
	    Insets insets = getInsets();
	    javax.swing.JMenuBar menuBar = getRootPane().getJMenuBar();
	    int menuBarHeight = 0;
	    if ( menuBar != null )
	      menuBarHeight = menuBar.getPreferredSize().height;
	    setSize(insets.left + insets.right + size.width, insets.top + insets.bottom + size.height + menuBarHeight);
	}
	
	// Used by addNotify
	boolean frameSizeAdjusted = false;
	
	//{{DECLARE_CONTROLS
	javax.swing.JLabel label1 = new javax.swing.JLabel();
	
	/**
	 * The begin or cancel button
	 */
	javax.swing.JButton begin = new javax.swing.JButton();
	
	/**
	 * The URL being processed
	 */
	javax.swing.JTextField url = new javax.swing.JTextField();
	
	/**
	 * Scroll the errors.
	 */
	javax.swing.JScrollPane errorScroll =
	new javax.swing.JScrollPane();
	
	/**
	 * A place to store the errors created
	 */
	javax.swing.JTextArea errors = new javax.swing.JTextArea();
	javax.swing.JLabel current = new javax.swing.JLabel();
	javax.swing.JLabel goodLinksLabel = new javax.swing.JLabel();
	javax.swing.JLabel badLinksLabel = new javax.swing.JLabel();
	
	/**
	 * The background spider thread
	 */
	protected Thread backgroundThread;
	
	/**
	 * The spider object being used
	 */
	protected Spider spider;
	
	/**
	 * The URL that the spider began with
	 */
	protected URL base;
	
	/**
	 * How many bad links have been found
	 */
	protected int badLinksCount = 0;
	
	/**
	 * How many good links have been found
	 */
	protected int goodLinksCount = 0; 
	
	
	/**
	 * Internal class used to dispatch events
	 * 
	 * @author Jeff Heaton
	 * @version 1.0
	 */
	class SymAction implements java.awt.event.ActionListener 
	{
		@Override
		public void actionPerformed(java.awt.event.ActionEvent event)
		{
			Object object = event.getSource();
			if ( object == CheckLinks.this.begin )
				begin_actionPerformed(event);
	    }
	}
	
	
	/**
	 * Called when the begin or cancel buttons are clicked
	 * 
	 * @param event The event associated with the button.
	 */
	@SuppressWarnings("deprecation")
	void begin_actionPerformed(java.awt.event.ActionEvent event)
	{
		if ( this.backgroundThread==null ) 
		{
			  this.begin.setLabel("Cancel");
			  this.backgroundThread = new Thread(this);
			  this.backgroundThread.start();
			  this.goodLinksCount=0;
			  this.badLinksCount=0;
		} else 
		{
			this.spider.pause();
		}
	
	}
	
	/**
	 * Perform the background thread operation. This method
	 * actually starts the background thread.
	 */
	@Override
	public void run()
	{
		try 
		{
			this.errors.setText("");
			this.base = new URL(this.url.getText());
			this.spider = new Spider(this.base);
			
			this.spider.start();
			Runnable doLater = new Runnable()
			{
				@Override
				public void run()
				{
					CheckLinks.this.begin.setText("Begin");
				}
			};
			
			SwingUtilities.invokeLater(doLater);
		
		} catch ( MalformedURLException e ) 
		{
			this.backgroundThread=null;
			UpdateErrors err = new UpdateErrors();
			err.msg = "Bad address.";
			SwingUtilities.invokeLater(err);
		
		}
	}
	
	/**
	 * Called by the spider when a URL is found. It is here
	 * that links are validated.
	 * 
	 * @param base The page that the link was found on.
	 * @param url The actual link address.
	 */
	@Override
	public boolean spiderFoundURL(URL base,URL url)
	{
		UpdateCurrentStats cs = new UpdateCurrentStats();
		cs.msg = url.toString();
		SwingUtilities.invokeLater(cs);
		
		if ( !checkLink(url) ) 
		{
			UpdateErrors err = new UpdateErrors();
			err.msg = url+"(on page " + base + ")\n";
			SwingUtilities.invokeLater(err);
			this.badLinksCount++;
			return false;
		}
		
		this.goodLinksCount++;
		if ( !url.getHost().equalsIgnoreCase(base.getHost()) )
		  return false;
		else
		  return true;
	  }
	
	/**
	 * Called when a URL error is found
	 * 
	 * @param url The URL that resulted in an error.
	 */
	@Override
	public void spiderURLError(URL url)
	{
	}
	
	/**
	 * Called internally to check whether a link is good
	 * 
	 * @param url The link that is being checked.
	 * @return True if the link was good, false otherwise.
	 */
	protected boolean checkLink(URL url)
	{
		try 
		{
			URLConnection connection = url.openConnection();
			connection.connect();
			return true;
		} catch ( IOException e ) 
		{
		  return false;
		}
	}
	
	/**
	 * Called when the spider finds an e-mail address
	 * 
	 * @param email The email address the spider found.
	 */
	  
	@Override
	public void spiderFoundEMail(String email)
	{
	}
	  
	/**
	 * Internal class used to update the error information
	 * in a Thread-Safe way
	 * 
	 * @author Jeff Heaton
	 * @version 1.0
	 */
	
	class UpdateErrors implements Runnable 
	{
		public String msg;
		@Override
		public void run()
		{
			CheckLinks.this.errors.append(this.msg);
		}
	}
	
	/**
	 * Used to update the current status information
	 * in a "Thread-Safe" way
	 * 
	 * @author Jeff Heaton
	 * @version 1.0
	 */
	
	class UpdateCurrentStats implements Runnable 
	{
		public String msg;
		@Override
		public void run()
		{
			CheckLinks.this.current.setText("Currently Processing: " + this.msg );
			CheckLinks.this.goodLinksLabel.setText("Good Links: " + CheckLinks.this.goodLinksCount);
			CheckLinks.this.badLinksLabel.setText("Bad Links: " + CheckLinks.this.badLinksCount);
		}
	}
}