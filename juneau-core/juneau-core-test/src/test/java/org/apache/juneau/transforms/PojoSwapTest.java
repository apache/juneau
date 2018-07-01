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
package org.apache.juneau.transforms;

import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.junit.*;

/**
 * Tests the example code in the PojoSwap class.
 */
public class PojoSwapTest {

	public static class MyPojo {}

	public static class MyJsonSwap extends PojoSwap<MyPojo,String> {

		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/json");
		}

		@Override
		public String swap(BeanSession session, MyPojo o) throws Exception {
			return "It's JSON!";
		}
	}

	public static class MyXmlSwap extends PojoSwap<MyPojo,String> {

		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/xml");
		}

		@Override
		public String swap(BeanSession session, MyPojo o) throws Exception {
			return "It's XML!";
		}
	}

	public static class MyOtherSwap extends PojoSwap<MyPojo,String> {

		@Override
		public MediaType[] forMediaTypes() {
			return MediaType.forStrings("*/*");
		}

		@Override
		public String swap(BeanSession session, MyPojo o) throws Exception {
			return "It's something else!";
		}
	}

	@Test
	public void doTest() throws Exception {

		SerializerGroup g = SerializerGroup.create()
			.append(JsonSerializer.class, XmlSerializer.class, HtmlSerializer.class)
			.sq()
			.pojoSwaps(MyJsonSwap.class, MyXmlSwap.class, MyOtherSwap.class)
			.build();

		MyPojo myPojo = new MyPojo();

		String json = g.getWriterSerializer("text/json").serialize(myPojo);
		assertEquals("'It\\'s JSON!'", json);

		String xml = g.getWriterSerializer("text/xml").serialize(myPojo);
		assertEquals("<string>It's XML!</string>", xml);

		String html = g.getWriterSerializer("text/html").serialize(myPojo);
		assertEquals("<string>It's something else!</string>", html);
	}
}
