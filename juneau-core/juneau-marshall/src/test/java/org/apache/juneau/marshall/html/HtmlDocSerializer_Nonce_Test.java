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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests that the per-response Content-Security-Policy nonce set on an {@link HtmlDocSerializerSession} is stamped onto
 * the inline {@code <script>}/{@code <style>} tags emitted by {@link BasicHtmlDocTemplate}.
 */
class HtmlDocSerializer_Nonce_Test extends TestBase {

	private static String serialize(String nonce, Object o) throws Exception {
		var s = HtmlDocSerializer.create().build();
		return s.createSession().nonce(nonce).build().serializeToString(o);
	}

	@Test void a01_nonceStampedOnStyleAndScript() throws Exception {
		var html = serialize("R4nd0mT0k3n", "x");
		assertTrue(html.contains("<style nonce=\"R4nd0mT0k3n\">"), () -> "Missing nonce on style: " + html);
		assertTrue(html.contains("<script nonce=\"R4nd0mT0k3n\">"), () -> "Missing nonce on script: " + html);
	}

	@Test void a02_noNonceLeavesPlainTags() throws Exception {
		var html = serialize(null, "x");
		assertTrue(html.contains("<style>"), () -> "Expected plain style tag: " + html);
		assertTrue(html.contains("<script>"), () -> "Expected plain script tag: " + html);
		assertFalse(html.contains("nonce="), () -> "Did not expect nonce attribute: " + html);
	}

	@Test void a03_getNonceAccessor() {
		var session = HtmlDocSerializer.create().build().createSession().nonce("abc").build();
		assertEquals("abc", session.getNonce());
	}

	@Test void a04_getNonceDefaultsNull() {
		var session = HtmlDocSerializer.create().build().getSession();
		assertNull(session.getNonce());
	}

	@Test void a05_nonceDoesNotAffectSerializerCache() {
		// The nonce is session-scoped; two sessions from the same serializer must share the same cached context.
		var s = HtmlDocSerializer.create().build();
		var s1 = s.createSession().nonce("a").build();
		var s2 = s.createSession().nonce("b").build();
		assertEquals("a", s1.getNonce());
		assertEquals("b", s2.getNonce());
	}
}
