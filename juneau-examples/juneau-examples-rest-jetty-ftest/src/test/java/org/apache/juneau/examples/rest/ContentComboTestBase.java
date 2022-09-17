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
package org.apache.juneau.examples.rest;

import static org.apache.juneau.examples.rest.TestUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class ContentComboTestBase extends RestTestcase {

	// Reusable RestClients keyed by label that live for the duration of a testcase class.
	private static Map<String,RestClient> clients = map();

	protected RestClient getClient(MediaType mediaType) {
		String mt = mediaType.toString();
		switch (mt) {
			case "text/csv": return getClient(mt, CsvSerializer.DEFAULT, CsvParser.DEFAULT);
			case "text/html": return getClient(mt, HtmlSerializer.DEFAULT, HtmlParser.DEFAULT);
			case "application/json": return getClient(mt, JsonSerializer.DEFAULT, JsonParser.DEFAULT);
			case "octal/msgpack": return getClient(mt, MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT, x -> x.queryData("plainText","true"));
			case "text/plain": return getClient(mt, PlainTextSerializer.DEFAULT, PlainTextParser.DEFAULT);
			case "text/uon": return getClient(mt, UonSerializer.DEFAULT, UonParser.DEFAULT);
			case "application/x-www-form-urlencoded": return getClient(mt, UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT);
			case "text/xml": return getClient(mt, XmlSerializer.DEFAULT, XmlParser.DEFAULT);
			default: throw new BasicRuntimeException("Client for mediaType ''{0}'' not found", mt);
		}
	}

	protected RestClient getClient(String label, Serializer serializer, Parser parser, Consumer<RestClient.Builder>...postApply) {
		if (! clients.containsKey(label)) {
			RestClient.Builder b = SamplesMicroservice.client(serializer, parser);
			for (Consumer<RestClient.Builder> c : postApply)
				c.accept(b);
			clients.put(label, b.build());
		}
		return clients.get(label);
	}

	@AfterClass
	public static void tearDown() {
		clients.values().forEach(rc -> {
			try {
				rc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		clients.clear();
	}

	public static class ComboInput {
		public final String name, url;
		public final MediaType mediaType;
		public final String[] expectedResults;

		public ComboInput(String name, String url, MediaType mediaType, String...expectedResults) {
			this.name = name;
			this.url = url;
			this.mediaType = mediaType;
			this.expectedResults = expectedResults;
		}
	}


	private final ComboInput comboInput;

	public ContentComboTestBase(ComboInput comboInput) {
		this.comboInput = comboInput;
	}

	@Test
	public void doTest() throws Exception {
		RestClient rc = getClient(comboInput.mediaType);
		String s = rc.get(comboInput.url).run().getContent().asString();
		assertContains(s, comboInput.expectedResults);
	}
}
