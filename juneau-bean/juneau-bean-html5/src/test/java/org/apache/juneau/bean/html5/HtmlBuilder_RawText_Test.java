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
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the re-serializable, String-backed {@link HtmlBuilder#rawText(String)} affordance.
 *
 * <p>
 * Unlike a one-shot {@code Reader}, a {@link RawText} is backed by a reusable {@code String} and therefore survives
 * repeated serialization.  Its content is emitted <b>verbatim</b> (no entity/whitespace encoding), mirroring the
 * existing raw {@code Reader}/{@code InputStream} path.
 */
class HtmlBuilder_RawText_Test extends TestBase {

	// A rawText body containing '<' and '&' is emitted verbatim, AND the element re-serializes identically
	// (proves it is not one-shot like a Reader).
	@Test void a01_reSerializable() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;
		var e = script().text(rawText("a<b & c"));
		var r1 = s.write(e);
		var r2 = s.write(e);
		assertEquals("<script>a<b & c</script>", r1);
		assertEquals(r1, r2);
	}

	// rawText(s) produces output identical to the established one-shot Reader raw path for the same input.
	@Test void a02_vsReaderParity() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;
		var body = "if (a < b) { alert('x & y'); }";
		var rawTextOut = s.write(script().text(rawText(body)));
		var readerOut = s.write(script().text(new StringReader(body)));
		assertEquals(readerOut, rawTextOut);
		assertEquals("<script>if (a < b) { alert('x & y'); }</script>", rawTextOut);
	}

	// Also emits verbatim through the XML serializer (RawText is recognized by both).
	@Test void a03_xmlSerializerVerbatim() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var r = s.write(script().text(rawText("x < y & z")));
		assertEquals("<script>x < y & z</script>", r);
	}

	// SECURITY: RAWTEXT/rawText is verbatim by contract -- it does NOT silently neutralize a '</script>'
	// end-tag sequence in the body.  Safety is the caller's responsibility; callers embedding a JSON payload should
	// use StringUtils.escapeForScript(String) rather than hand-rolling it.  This test documents that contract.
	@Test void c01_scriptEndTagIsCallerResponsibility() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;

		// Verbatim: a literal </script> in the body is emitted as-is (would prematurely close the element in a browser).
		var unsafe = s.write(script().text(rawText("var x = '</script>';")));
		assertEquals("<script>var x = '</script>';</script>", unsafe);
		assertEquals(2, countOccurrences(unsafe, "</script>"));

		// Caller neutralizes by escaping '<' -> \u003c (valid JS); the only real </script> is the true close tag.
		var safe = s.write(script().text(rawText("var x = '\\u003c/script>';")));
		assertEquals("<script>var x = '\\u003c/script>';</script>", safe);
		assertEquals(1, countOccurrences(safe, "</script>"));
	}

	private static int countOccurrences(String haystack, String needle) {
		var count = 0;
		var i = 0;
		while ((i = haystack.indexOf(needle, i)) != -1) {
			count++;
			i += needle.length();
		}
		return count;
	}
}
