/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests.sample;

import static org.junit.Assert.*;

import java.io.*;

import org.apache.http.*;
import org.apache.http.entity.mime.*;
import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.utils.*;

public class CT_TestMultiPartFormPosts {

	private static String URL = "/tempDir";
	boolean debug = false;

	//====================================================================================================
	// Test that RestClient can handle multi-part form posts.
	//====================================================================================================
	@Test
	public void testUpload() throws Exception {
		RestClient client = new SamplesRestClient();
		File f = FileUtils.createTempFile("testMultiPartFormPosts.txt");
		IOPipe.create(new StringReader("test!"), new FileWriter(f)).closeOut().run();
		HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody(f.getName(), f).build();
		client.doPost(URL + "/upload", entity);

		String downloaded = client.doGet(URL + '/' + f.getName() + "?method=VIEW").getResponseAsString();
		assertEquals("test!", downloaded);

		client.closeQuietly();
	}
}