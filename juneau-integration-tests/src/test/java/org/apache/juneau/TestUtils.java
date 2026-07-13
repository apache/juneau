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

import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;

/**
 * Cross-module test utilities for the integration-test residual.
 *
 * <p>Contains the marshall-bound and REST-bound helpers layered on top of the shared {@link BasicTestUtils} base.</p>
 */
public class TestUtils extends BasicTestUtils {

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertJson(String expected, Object value) {
		assertEquals(expected, json5(value));
	}

	/**
	 * Asserts the serialized representation of the specified object.
	 */
	public static void assertSerialized(Object actual, WriterSerializer s, String expected) {
		assertEquals(expected, s.toString(actual));
	}

	public static <T extends Throwable> T assertThrowable(Class<? extends Throwable> expectedType, String expectedSubstring, T t) {
		assertTrue(expectedType.isInstance(t), fs("Expected throwable of type: {0}.\nActual: {1}", expectedType.getName(), t == null ? "null" : t.getClass().getName()));
		var messages = getMessages(t);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return t;
	}

	/**
	 * Gets the swagger for the specified @Resource-annotated object.
	 */
	public static Swagger getSwagger(Class<?> c) {
		try {
			var r = c.getDeclaredConstructor().newInstance();
			var rc = new RestContext(new RestContext.Args(r.getClass(), null, null, () -> r, "", null, null, null, RestContext.ContextKind.ROOT));
			var ctx = new RestOpContext(TestUtils.class.getMethod("getSwagger", Class.class), rc);
			var session = RestSession.create(rc).resource(r).req(new MockServletRequest()).res(new MockServletResponse()).build();
			var req = ctx.createRequest(session);
			var ip = rc.getSwaggerProvider();
			return ip.getSwagger(rc, req.getLocale());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String json(Object o) {
		return json5(o);
	}

	public static <T> T json(String o, Class<T> c) {
		return safe(()->json5(o, c));
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.serialize(o);
		XmlTestUtils.checkXmlWhitespace(xml);
	}
}
