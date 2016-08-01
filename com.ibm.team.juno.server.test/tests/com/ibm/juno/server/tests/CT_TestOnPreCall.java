/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.client.*;

public class CT_TestOnPreCall {

	private static String URL = "/testOnPreCall";

	//====================================================================================================
	// Properties overridden via properties annotation.
	//====================================================================================================
	@Test
	public void testPropertiesOverriddenByAnnotation() throws Exception {
		RestClient client = new TestRestClient().setContentType("text/a1").setAccept("text/plain");
		String url = URL + "/testPropertiesOverriddenByAnnotation";
		String r;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a1", r);

		r = client.doPut(url, new StringReader("")).setHeader("Override-Content-Type", "text/a2").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a2", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Properties overridden programmatically.
	//====================================================================================================
	@Test
	public void testPropertiesOverriddenProgrammatically() throws Exception {
		RestClient client = new TestRestClient().setContentType("text/a1").setAccept("text/plain");
		String url = URL + "/testPropertiesOverriddenProgrammatically";
		String r;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a1", r);

		r = client.doPut(url, new StringReader("")).setHeader("Override-Content-Type", "text/a2").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a2", r);

		client.closeQuietly();
	}
}
