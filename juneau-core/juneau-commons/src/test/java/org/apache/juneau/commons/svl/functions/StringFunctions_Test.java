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

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link StringFunctions}. */
class StringFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(StringFunctions.ALL).build();

	@Test void a01_substring_2arg() { assertEquals("llo", vr.resolve("#{substring(hello, 2)}")); }
	@Test void a02_substring_3arg() { assertEquals("ell", vr.resolve("#{substring(hello, 1, 4)}")); }
	@Test void a03_substring_clamps() { assertEquals("hello", vr.resolve("#{substring(hello, -5, 99)}")); }

	@Test void a04_upper() { assertEquals("HELLO", vr.resolve("#{upper(hello)}")); }
	@Test void a05_lower() { assertEquals("hello", vr.resolve("#{lower(HELLO)}")); }
	@Test void a06_trim() { assertEquals("hi", vr.resolve("#{trim(\"  hi  \")}")); }
	@Test void a07_stripLeading() { assertEquals("hi  ", vr.resolve("#{stripLeading(\"  hi  \")}")); }
	@Test void a08_stripTrailing() { assertEquals("  hi", vr.resolve("#{stripTrailing(\"  hi  \")}")); }
	@Test void a09_stripSlashes() { assertEquals("a/b", vr.resolve("#{stripSlashes(\"/a/b/\")}")); }

	@Test void a10_pathToken_bare() { assertEquals("jsp", vr.resolve("#{pathToken(jsp)}")); }
	@Test void a11_pathToken_leading() { assertEquals("jsp", vr.resolve("#{pathToken(/jsp)}")); }
	@Test void a12_pathToken_trailingSlash() { assertEquals("jsp", vr.resolve("#{pathToken(/jsp/)}")); }
	@Test void a13_pathToken_trailingStar() { assertEquals("jsp", vr.resolve("#{pathToken(/jsp/*)}")); }
	@Test void a14_pathToken_bareTrailingStar() { assertEquals("jsp", vr.resolve("#{pathToken(jsp/*)}")); }
	@Test void a15_pathToken_multi() { assertEquals("api/v1", vr.resolve("#{pathToken(/api/v1/*)}")); }
	@Test void a16_pathToken_emptyJustSlash() { assertEquals("", vr.resolve("#{pathToken(/)}")); }
	@Test void a17_pathToken_starOnly() { assertEquals("", vr.resolve("#{pathToken(/*)}")); }

	@Test void a18_len() { assertEquals("5", vr.resolve("#{len(hello)}")); }
	@Test void a19_len_empty() { assertEquals("0", vr.resolve("#{len(\"\")}")); }

	@Test void a20_replace() { assertEquals("hxllo", vr.resolve("#{replace(hello, e, x)}")); }
	@Test void a21_contains_true() { assertEquals("true", vr.resolve("#{contains(hello, ell)}")); }
	@Test void a22_contains_false() { assertEquals("false", vr.resolve("#{contains(hello, xyz)}")); }
	@Test void a23_startsWith() { assertEquals("true", vr.resolve("#{startsWith(hello, hel)}")); }
	@Test void a24_endsWith() { assertEquals("true", vr.resolve("#{endsWith(hello, llo)}")); }

	@Test void a25_concat() { assertEquals("abc", vr.resolve("#{concat(a, b, c)}")); }
	@Test void a26_repeat() { assertEquals("abcabcabc", vr.resolve("#{repeat(abc, 3)}")); }
	@Test void a27_repeat_zero() { assertEquals("", vr.resolve("#{repeat(abc, 0)}")); }
	@Test void a28_reverse() { assertEquals("olleh", vr.resolve("#{reverse(hello)}")); }

	@Test void a29_format_string() { assertEquals("hi-007", vr.resolve("#{format(\"%s-%03d\", hi, 7)}")); }
	@Test void a30_split() { assertEquals("[\"a\",\"b\",\"c\"]", vr.resolve("#{split(\"a,b,c\", \",\")}")); }
	@Test void a31_join() { assertEquals("a/b/c", vr.resolve("#{join(\"/\", a, b, c)}")); }

	@Test void a32_compose_join_split() {
		assertEquals("X-Y-Z", vr.resolve("#{join(\"-\", X, Y, Z)}"));
	}
}
