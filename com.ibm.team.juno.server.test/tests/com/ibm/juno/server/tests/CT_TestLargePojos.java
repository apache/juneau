/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.html.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.xml.*;

@Ignore
public class CT_TestLargePojos {

	private static String URL = "/testLargePojos";
	boolean debug = false;

	//====================================================================================================
	// Test how long it takes to serialize/parse various content types.
	//====================================================================================================
	@Test
	public void test() throws Exception {
		LargePojo p;
		long t;
		RestClient c;

		System.err.println("\n---Testing JSON---");
		c = new TestRestClient(JsonSerializer.class, JsonParser.class);
		for (int i = 1; i <= 3; i++) {
			t = System.currentTimeMillis();
			p = c.doGet(URL).getResponse(LargePojo.class);
			System.err.println("Download: ["+(System.currentTimeMillis() - t)+"] ms");
			t = System.currentTimeMillis();
			c.doPut(URL, p).run();
			System.err.println("Upload: ["+(System.currentTimeMillis() - t)+"] ms");
		}

		System.err.println("\n---Testing XML---");
		c = new TestRestClient(XmlSerializer.class, XmlParser.class);
		for (int i = 1; i <= 3; i++) {
			t = System.currentTimeMillis();
			p = c.doGet(URL).getResponse(LargePojo.class);
			System.err.println("Download: ["+(System.currentTimeMillis() - t)+"] ms");
			t = System.currentTimeMillis();
			c.doPut(URL, p).run();
			System.err.println("Upload: ["+(System.currentTimeMillis() - t)+"] ms");
		}

		System.err.println("\n---Testing HTML---");
		c = new TestRestClient(HtmlSerializer.class, HtmlParser.class).setAccept("text/html+stripped");
		for (int i = 1; i <= 3; i++) {
			t = System.currentTimeMillis();
			p = c.doGet(URL).getResponse(LargePojo.class);
			System.err.println("Download: ["+(System.currentTimeMillis() - t)+"] ms");
			t = System.currentTimeMillis();
			c.doPut(URL, p).run();
			System.err.println("Upload: ["+(System.currentTimeMillis() - t)+"] ms");
		}

		System.err.println("\n---Testing UrlEncoding---");
		c = new TestRestClient(UonSerializer.class, UonParser.class);
		for (int i = 1; i <= 3; i++) {
			t = System.currentTimeMillis();
			p = c.doGet(URL).getResponse(LargePojo.class);
			System.err.println("Download: ["+(System.currentTimeMillis() - t)+"] ms");
			t = System.currentTimeMillis();
			c.doPut(URL, p).run();
			System.err.println("Upload: ["+(System.currentTimeMillis() - t)+"] ms");
		}

		c.closeQuietly();
	}
}