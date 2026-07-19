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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.marshall.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Test utilities for the juneau-marshall module.
 *
 * <p>Contains the marshall-bound helpers layered on top of the shared {@link BasicTestUtils} base.</p>
 */
public class TestUtils extends BasicTestUtils {

	public static String assertJson(String expected, Object value) {
		assertEquals(expected, json5(value));
		return expected;
	}

	public static String json(Object o) {
		return json5(o);
	}

	public static <T> T json(String o, Class<T> c) {
		return safe(()->json5(o, c));
	}

	public static <T> T jsonRoundTrip(T o, Class<T> c) {
		return json(json(o), c);
	}

	public static void assertSerialized(Object actual, WriterSerializer s, String expected) {
		assertEquals(expected, s.toString(actual));
	}

	/**
	 * Validates XML whitespace and namespace formatting on a serialized object.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static final void validateXml(Object o) throws Exception {
		validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Validates XML whitespace and namespace formatting on a serialized object.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.write(o);
		XmlTestUtils.checkXmlWhitespace(xml);
	}
}
