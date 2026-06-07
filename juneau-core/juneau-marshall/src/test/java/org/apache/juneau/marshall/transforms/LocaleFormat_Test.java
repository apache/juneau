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

import java.util.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class LocaleFormat_Test {

	@Test void a01_bcp47() {
		assertEquals("en-US", LocaleFormat.BCP_47.format(Locale.US));
		assertEquals("en-US", LocaleFormat.NOT_SET.format(Locale.US));
		assertEquals("ja-JP", LocaleFormat.BCP_47.format(Locale.JAPAN));
		assertEquals(Locale.US, LocaleFormat.BCP_47.parse("en-US"));
		assertEquals(Locale.JAPAN, LocaleFormat.BCP_47.parse("ja-JP"));
	}

	@Test void a02_underscore() {
		assertEquals("en_US", LocaleFormat.UNDERSCORE.format(Locale.US));
		assertEquals("ja_JP", LocaleFormat.UNDERSCORE.format(Locale.JAPAN));
		assertEquals(Locale.US, LocaleFormat.UNDERSCORE.parse("en_US"));
		assertEquals(Locale.JAPAN, LocaleFormat.UNDERSCORE.parse("ja_JP"));
	}

	@Test void a03_underscore_languageOnly() {
		assertEquals(Locale.ENGLISH, LocaleFormat.UNDERSCORE.parse("en"));
	}

	@Test void a04_underscore_languageCountryVariant() {
		var l = LocaleFormat.UNDERSCORE.parse("en_US_POSIX");
		assertEquals("en", l.getLanguage());
		assertEquals("US", l.getCountry());
		assertEquals("POSIX", l.getVariant());
	}

	@Test void a05_nullAndBlank() {
		assertNull(LocaleFormat.BCP_47.format(null));
		assertNull(LocaleFormat.BCP_47.parse(null));
		assertNull(LocaleFormat.BCP_47.parse("   "));
		assertNull(LocaleFormat.UNDERSCORE.format(null));
		assertNull(LocaleFormat.UNDERSCORE.parse(null));
	}
}
