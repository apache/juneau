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
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ReaderFilterTest {

	//====================================================================================================
	// testJson
	//====================================================================================================
	@Test
	public void testJson() throws Exception {
		JsonSerializer s = JsonSerializer.create().ssq().swaps(ParsedReaderSwap.Json.class).build();

		Reader r;
		Map<String,Object> m;

		r = reader("{foo:'bar',baz:'quz'}");
		m = new HashMap<>();
		m.put("X", r);
		assertEquals("{X:{foo:'bar',baz:'quz'}}", s.serialize(m));
	}

	//====================================================================================================
	// testXml
	//====================================================================================================
	@Test
	public void testXml() throws Exception {
		XmlSerializer s = XmlSerializer.create().sq().swaps(ParsedReaderSwap.Xml.class).build();

		Reader r;
		Map<String,Object> m;

		r = reader("<object><foo _type='string'>bar</foo><baz _type='string'>quz</baz></object>");
		m = new HashMap<>();
		m.put("X", r);
		assertEquals("<object><X _type='object'><foo>bar</foo><baz>quz</baz></X></object>", s.serialize(m));
	}

	//====================================================================================================
	// testHtml
	//====================================================================================================
	@Test
	public void testHtml() throws Exception {
		HtmlSerializer s = HtmlSerializer.create().sq().swaps(ParsedReaderSwap.Html.class).build();

		Reader r;
		Map<String,Object> m;

		r = reader("<table><tr><td>foo</td><td>bar</td></tr><tr><td>baz</td><td>quz</td></tr></table>");
		m = new HashMap<>();
		m.put("X", r);
		assertEquals("<table><tr><td>X</td><td><table><tr><td>foo</td><td>bar</td></tr><tr><td>baz</td><td>quz</td></tr></table></td></tr></table>", s.serialize(m));
	}

	//====================================================================================================
	// testPlainText
	//====================================================================================================
	@Test
	public void testPlainText() throws Exception {
		PlainTextSerializer s = PlainTextSerializer.create().swaps(ParsedReaderSwap.PlainText.class).build();

		Reader r;
		Map<String,Object> m;

		r = reader("{foo:'bar',baz:'quz'}");
		m = new HashMap<>();
		m.put("X", r);
		assertEquals("{X:'{foo:\\'bar\\',baz:\\'quz\\'}'}", s.serialize(m));
	}

	//====================================================================================================
	// testUon
	//====================================================================================================
	@Test
	public void testUon() throws Exception {
		UonSerializer s = UonSerializer.create().swaps(ParsedReaderSwap.Uon.class).build();

		Reader r;
		Map<String,Object> m;

		r = reader("(foo=bar,baz=quz)");
		m = new HashMap<>();
		m.put("X", r);
		assertEquals("(X=(foo=bar,baz=quz))", s.serialize(m));
	}

	//====================================================================================================
	// testUrlEncoding
	//====================================================================================================
	@Test
	public void testUrlEncoding() throws Exception {
		UrlEncodingSerializer s = UrlEncodingSerializer.create().swaps(ParsedReaderSwap.PlainText.class).build();

		Reader r;
		Map<String,Object> m;

		r = reader("foo=bar&baz=quz");
		m = new HashMap<>();
		m.put("X", r);
		assertEquals("X='foo=bar%26baz=quz'", s.serialize(m));
	}
}