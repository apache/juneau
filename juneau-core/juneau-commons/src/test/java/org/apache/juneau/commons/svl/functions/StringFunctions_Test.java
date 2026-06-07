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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/** Tests for {@link StringFunctions}. */
class StringFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(StringFunctions.ALL).build();

	@Test void substring_2arg() { assertEquals("llo", vr.resolve("#{substring(hello, 2)}")); }
	@Test void substring_3arg() { assertEquals("ell", vr.resolve("#{substring(hello, 1, 4)}")); }
	@Test void substring_clamps() { assertEquals("hello", vr.resolve("#{substring(hello, -5, 99)}")); }

	@Test void upper() { assertEquals("HELLO", vr.resolve("#{upper(hello)}")); }
	@Test void lower() { assertEquals("hello", vr.resolve("#{lower(HELLO)}")); }
	@Test void trim() { assertEquals("hi", vr.resolve("#{trim(\"  hi  \")}")); }
	@Test void stripLeading() { assertEquals("hi  ", vr.resolve("#{stripLeading(\"  hi  \")}")); }
	@Test void stripTrailing() { assertEquals("  hi", vr.resolve("#{stripTrailing(\"  hi  \")}")); }
	@Test void stripSlashes() { assertEquals("a/b", vr.resolve("#{stripSlashes(\"/a/b/\")}")); }

	@Test void pathToken_bare() { assertEquals("jsp", vr.resolve("#{pathToken(jsp)}")); }
	@Test void pathToken_leading() { assertEquals("jsp", vr.resolve("#{pathToken(/jsp)}")); }
	@Test void pathToken_trailingSlash() { assertEquals("jsp", vr.resolve("#{pathToken(/jsp/)}")); }
	@Test void pathToken_trailingStar() { assertEquals("jsp", vr.resolve("#{pathToken(/jsp/*)}")); }
	@Test void pathToken_bareTrailingStar() { assertEquals("jsp", vr.resolve("#{pathToken(jsp/*)}")); }
	@Test void pathToken_multi() { assertEquals("api/v1", vr.resolve("#{pathToken(/api/v1/*)}")); }
	@Test void pathToken_emptyJustSlash() { assertEquals("", vr.resolve("#{pathToken(/)}")); }
	@Test void pathToken_starOnly() { assertEquals("", vr.resolve("#{pathToken(/*)}")); }

	@Test void len() { assertEquals("5", vr.resolve("#{len(hello)}")); }
	@Test void len_empty() { assertEquals("0", vr.resolve("#{len(\"\")}")); }

	@Test void replace() { assertEquals("hxllo", vr.resolve("#{replace(hello, e, x)}")); }
	@Test void contains_true() { assertEquals("true", vr.resolve("#{contains(hello, ell)}")); }
	@Test void contains_false() { assertEquals("false", vr.resolve("#{contains(hello, xyz)}")); }
	@Test void startsWith() { assertEquals("true", vr.resolve("#{startsWith(hello, hel)}")); }
	@Test void endsWith() { assertEquals("true", vr.resolve("#{endsWith(hello, llo)}")); }

	@Test void concat() { assertEquals("abc", vr.resolve("#{concat(a, b, c)}")); }
	@Test void repeat() { assertEquals("abcabcabc", vr.resolve("#{repeat(abc, 3)}")); }
	@Test void repeat_zero() { assertEquals("", vr.resolve("#{repeat(abc, 0)}")); }
	@Test void reverse() { assertEquals("olleh", vr.resolve("#{reverse(hello)}")); }

	@Test void format_string() { assertEquals("hi-007", vr.resolve("#{format(\"%s-%03d\", hi, 7)}")); }
	@Test void split() { assertEquals("[\"a\",\"b\",\"c\"]", vr.resolve("#{split(\"a,b,c\", \",\")}")); }
	@Test void join() { assertEquals("a/b/c", vr.resolve("#{join(\"/\", a, b, c)}")); }

	@Test void compose_join_split() {
		assertEquals("X-Y-Z", vr.resolve("#{join(\"-\", X, Y, Z)}"));
	}
}
