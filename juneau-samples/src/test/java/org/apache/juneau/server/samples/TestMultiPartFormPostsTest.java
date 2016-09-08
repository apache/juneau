// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.server.samples;

import static org.junit.Assert.*;

import java.io.*;

import org.apache.http.*;
import org.apache.http.entity.mime.*;
import org.apache.juneau.client.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;
import org.junit.*;

public class TestMultiPartFormPostsTest {

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