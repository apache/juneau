/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import org.apache.juneau.client.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@Ignore
public class TestLargePojosTest {

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