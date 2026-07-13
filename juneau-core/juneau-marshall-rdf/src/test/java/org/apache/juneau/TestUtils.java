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

import static org.apache.juneau.marshall.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Test utilities for the juneau-marshall module.
 *
 * <p>Contains a marshall-compatible subset of the methods available in the full {@code TestUtils}
 * in {@code juneau-integration-tests}.  Methods that depend on {@code juneau-rest-*} modules are excluded.</p>
 */
public class TestUtils extends Shorts {

	public static String assertJson(String expected, Object value) {
		assertEquals(expected, json5(value));
		return expected;
	}

	/**
	 * Validates XML whitespace and namespace formatting on a serialized object.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.serialize(o);
		XmlTestUtils.checkXmlWhitespace(xml);
	}
}
