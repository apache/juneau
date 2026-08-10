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
package org.apache.juneau.xml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that DTD processing stays disabled even when the {@code validating} parser option is enabled.
 *
 * <p>
 * DTD support is not needed for data binding, so it is kept off regardless of the {@code validating} flag:
 * a document declaring an internal DTD with nested entity references must be rejected rather than expanding
 * those entities, closing the input-amplification surface that would otherwise be reachable by toggling
 * {@code validating}.
 */
class XmlValidatingDtd_Test extends TestBase {

	@Test void a01_internalDtdRejectedWhenValidatingEnabled() {
		var p = XmlParser.create().validating().build();
		var xml = "<?xml version=\"1.0\"?>"
			+ "<!DOCTYPE A [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;\">]>"
			+ "<A>&b;</A>";

		// DTD processing is disabled, so a document declaring an internal DTD with nested entity references is
		// rejected rather than parsed — the input-amplification surface is never entered.
		assertThrows(Exception.class, () -> p.parse(xml, JsonMap.class));
	}

	@Test void a03_internalDtdRejectedByDefaultParser() {
		var p = XmlParser.DEFAULT;
		var xml = "<?xml version=\"1.0\"?>"
			+ "<!DOCTYPE A [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;\">]>"
			+ "<A>&b;</A>";

		// Same guard on the default (non-validating) parser.
		assertThrows(Exception.class, () -> p.parse(xml, JsonMap.class));
	}

	@Test void a02_doctypeRejectedWhenValidatingEnabled() {
		var p = XmlParser.create().validating().build();
		var xml = "<?xml version=\"1.0\"?>"
			+ "<!DOCTYPE A [<!ELEMENT A ANY>]>"
			+ "<A>x</A>";

		assertThrows(Exception.class, () -> p.parse(xml, JsonMap.class));
	}
}
