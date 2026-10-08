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
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Runs {@code visibility-corpus.json} (shared with {@code rules.cjs}) through {@link VisibilityRule#test}, so the
 * Java and JS evaluators are proven to agree on every case from one corpus.
 */
class VisibilityRule_Corpus_Test extends TestBase {

	static Path locateCorpus() {
		for (var rel : List.of(
			"src/test/js/visibility-corpus.json",
			"juneau-rest/juneau-rest-server-views/src/test/js/visibility-corpus.json")) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p;
		}
		throw new IllegalStateException("visibility-corpus.json not found");
	}

	@SuppressWarnings("unchecked")
	@Test void a01_everyCorpusCaseMatchesItsExpectation() throws IOException {
		var cases = (List<Map<String,Object>>)Json.to(Files.readString(locateCorpus()), List.class);
		assertFalse(cases.isEmpty());
		for (var c : cases) {
			var rules = ((List<Map<String,Object>>)c.get("rules")).stream().map(VisibilityRule_Corpus_Test::toRule).toList();
			var facts = (Map<String,Object>)c.get("facts");
			assertEquals(c.get("expected"), VisibilityRule.test(rules, facts), (String)c.get("name"));
		}
	}

	private static VisibilityRule toRule(Map<String,Object> m) {
		var b = VisibilityRule.when((String)m.get("field"));
		var op = (String)m.get("op");
		return switch (op) {
			case "eq" -> b.eq(m.get("value"));
			case "ne" -> b.ne(m.get("value"));
			case "present" -> b.present();
			case "absent" -> b.absent();
			case "in" -> b.in(((List<?>)m.get("value")).toArray());
			case "contains" -> b.contains(m.get("value"));
			default -> throw new IllegalArgumentException("unknown corpus op '" + op + "'");
		};
	}
}
