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
package org.apache.juneau.test.assertions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Messages}, the lightweight message-bundle loader used by the assertions classes.
 *
 * <p>
 * Covers all four resolution outcomes of {@link Messages#getString(String)}: no bundle found at all, a
 * fully-qualified ({@code <SimpleClassName>.<key>}) hit, a bare-key fallback hit, and a totally-missing key.
 * The production classes in this package only ever exercise the "bundle found + fully-qualified hit" path
 * (every real key in {@code Messages.properties} is prefixed with its owning class's simple name), so the
 * other three outcomes are driven directly here against a dedicated test-only bundle
 * ({@code Messages_TestBundle.properties}, sibling to this test class).
 */
class Messages_Test extends TestBase {

	@Test void a01_bundleNotFound_returnsPlaceholder() {
		var messages = Messages.of(Messages_Test.class, "NoSuchBundleForThisTest");
		assertEquals("{!anyKey}", messages.getString("anyKey"));
	}

	@Test void a02_fullyQualifiedKey_isResolvedFirst() {
		var messages = Messages.of(Messages_Test.class, "Messages_TestBundle");
		assertEquals("fully-qualified-hit", messages.getString("fullyQualifiedKey"));
	}

	@Test void a03_bareKey_isResolvedWhenFullyQualifiedKeyMissing() {
		var messages = Messages.of(Messages_Test.class, "Messages_TestBundle");
		assertEquals("bare-key-hit", messages.getString("bareOnlyKey"));
	}

	@Test void a04_missingKey_returnsPlaceholder() {
		var messages = Messages.of(Messages_Test.class, "Messages_TestBundle");
		assertEquals("{!noSuchKey}", messages.getString("noSuchKey"));
	}
}
