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
package org.apache.juneau.examples.guides;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class GuideRegion_Test extends TestBase {

	private static final String SAMPLE = "GuideRegion/sample.txt";

	@Test
	void a01_hashMarkersTabsAndDedent() {
		assertEquals("indented\n    deeper", GuideRegion.read(SAMPLE, "hash"));
	}

	@Test
	void a02_xmlMarkers() {
		assertEquals("<a/>", GuideRegion.read(SAMPLE, "xml"));
	}

	@Test
	void a03_slashMarkers() {
		assertEquals("x", GuideRegion.read(SAMPLE, "slash"));
	}

	@Test
	void a04_errors() {
		assertThrows(IllegalArgumentException.class, () -> GuideRegion.read(SAMPLE, "open"));
		assertThrows(IllegalArgumentException.class, () -> GuideRegion.read(SAMPLE, "missing"));
		assertThrows(IllegalArgumentException.class, () -> GuideRegion.read("GuideRegion/nope.txt", "hash"));
	}

	@Test
	void a05_normalizeText() {
		assertEquals("a\n  b", GuideRegion.normalize("\r\n\t a  \r\n\t   b\n\n"));
	}
}
