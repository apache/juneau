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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link ContentLanguage}.
 */
class ContentLanguage_Test extends TestBase {

	@Test void a01_of_wireString() {
		var h = ContentLanguage.of("en, fr");
		assertEquals("Content-Language", h.getName());
		assertEquals("en, fr", h.getValue());
	}

	@Test void a02_of_varargs() {
		var h = ContentLanguage.of(new String[]{"en", "fr"});
		assertEquals("en, fr", h.getValue());
	}

	// Fixed: ContentLanguage declares its own of(String,String) overload -- see Allow_Test.a03.
	@Test void a03_of_twoStringLiterals_resolvesToOwnTypeSafeFactory() {
		Object h = ContentLanguage.of("en", "fr");
		assertInstanceOf(ContentLanguage.class, h);
		var contentLanguage = (ContentLanguage)h;
		assertEquals("Content-Language", contentLanguage.getName());
		assertEquals("en, fr", contentLanguage.getValue());
	}
}
