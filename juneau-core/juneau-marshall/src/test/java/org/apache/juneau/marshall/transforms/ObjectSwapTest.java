/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

/**
 * Tests the example code in the ObjectSwap class.
 */
class ObjectSwapTest extends TestBase {

	public static class MyPojo {}

	public static class MyJsonSwap extends ObjectSwap<MyPojo,String> {

		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/json");
		}

		@Override
		public String swap(MarshallingSession session, MyPojo o) throws Exception {
			return "It's JSON!";
		}
	}

	public static class MyXmlSwap extends ObjectSwap<MyPojo,String> {

		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/xml");
		}

		@Override
		public String swap(MarshallingSession session, MyPojo o) throws Exception {
			return "It's XML!";
		}
	}

	public static class MyOtherSwap extends ObjectSwap<MyPojo,String> {

		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.ofAll("*/*");
		}

		@Override
		public String swap(MarshallingSession session, MyPojo o) throws Exception {
			return "It's something else!";
		}
	}

	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	@Test void doTest() throws Exception {

		var s = SerializerSet.create()
			.add(JsonSerializer.class, XmlSerializer.class, HtmlSerializer.class)
			.forEach(WriterSerializer.Builder.class, WriterSerializer.Builder::sq)
			.forEach(Serializer.Builder.class, x -> x.swaps(MyJsonSwap.class, MyXmlSwap.class, MyOtherSwap.class))
			.build();

		var myPojo = new MyPojo();

		var json = s.getWriterSerializer("text/json").orElseThrow().write(myPojo);
		assertEquals("'It\\'s JSON!'", json);

		var xml = s.getWriterSerializer("text/xml").orElseThrow().write(myPojo);
		assertEquals("<string>It's XML!</string>", xml);

		var html = s.getWriterSerializer("text/html").orElseThrow().write(myPojo);
		assertEquals("<string>It's something else!</string>", html);
	}
}