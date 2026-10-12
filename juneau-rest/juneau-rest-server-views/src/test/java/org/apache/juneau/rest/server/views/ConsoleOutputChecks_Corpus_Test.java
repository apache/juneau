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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Runs the shared same-origin path corpus ({@code same-origin-path-corpus.json}) through the package-private
 * {@link ConsoleOutputChecks} guards, which the console-ui-freemarker corpus test cannot reach.
 *
 * <p>
 * Neither {@code isSafeLineHref} nor {@code isSafeLineImageSrc} folds a backslash, so both agree with the corpus row
 * for row.  This test still honors a row's {@code guards} overrides, should one ever be added.
 */
class ConsoleOutputChecks_Corpus_Test extends TestBase {

	private record Row(String name, String input, boolean accepted, Map<String,Object> guards) {}

	private static List<Row> rows() throws IOException {
		try (var in = ConsoleOutputChecks_Corpus_Test.class.getResourceAsStream("/same-origin-path-corpus.json")) {
			assertNotNull(in, "same-origin-path-corpus.json missing from the test classpath");
			var corpus = JsonMap.ofString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			var out = new ArrayList<Row>();
			for (var o : corpus.getList("cases")) {
				var c = (JsonMap)o;
				out.add(new Row(c.getString("name"), c.getString("input"), c.getBoolean("accepted"),
					c.containsKey("guards") ? c.getMap("guards") : Map.of()));
			}
			return out;
		}
	}

	private static boolean expected(Row r, String key) {
		return r.guards().containsKey(key) ? (Boolean)r.guards().get(key) : r.accepted();
	}

	@TestFactory Stream<DynamicTest> a01_isSafeLineHrefAgreesWithTheCorpus() throws Exception {
		return rows().stream().map(r -> DynamicTest.dynamicTest(r.name(), () ->
			assertEquals(expected(r, "isSafeLineHref"), ConsoleOutputChecks.isSafeLineHref(r.input()), r.name())));
	}

	@TestFactory Stream<DynamicTest> a02_isSafeLineImageSrcAgreesWithTheCorpus() throws Exception {
		return rows().stream().map(r -> DynamicTest.dynamicTest(r.name(), () ->
			assertEquals(expected(r, "isSafeLineImageSrc"), ConsoleOutputChecks.isSafeLineImageSrc(r.input()), r.name())));
	}
}
