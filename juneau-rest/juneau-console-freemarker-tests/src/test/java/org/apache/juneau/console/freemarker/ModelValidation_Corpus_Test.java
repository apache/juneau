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
package org.apache.juneau.console.freemarker;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Runs the Java side of the shared model validation corpus ({@code model-validation-corpus.json} in the views module).
 * The JS side (JSON mount and imperative calls) is {@code model.cjs}.  Here, every case that has a {@code build} step
 * on an element is replayed against {@link ModelBlock}/{@link HandlerRef}/{@link ModelAction}, which must refuse it
 * (the early, friendlier duplicate of the client rules), and every {@code schema} case must fail
 * {@link PageContractSchema}.  Cases that only the client can judge (versions, sibling ids, opt-in) are listed in
 * {@link #CLIENT_ONLY}; the opt-in case is Java-checked at page build in {@code ModelSerialization_Test}.
 */
class ModelValidation_Corpus_Test extends TestBase {

	private static final Path CORPUS = Path.of(System.getProperty("basedir", "."),
		"../juneau-console-views/src/test/resources/model-validation-corpus.json");

	private static final Set<String> CLIENT_ONLY = Set.of("modelVersion", "contractVersion", "siblingIdUnique", "inlineWithoutOptIn",
		"subscriptionRole", "cardReservedKeys", "pageHeader");

	private static String kindOfPath(String path) {
		if (path.endsWith("/table/detail") || path.endsWith("/detail"))
			return "rowdetail";
		if (path.contains("/tab:"))
			return "detailtab";
		if (path.contains("/item:"))
			return "ribbonitem";
		if (path.contains("pagetab:"))
			return "pagetab";
		if (path.endsWith("/table"))
			return "table";
		if (path.startsWith("page/") && ! path.contains("/card"))
			return path.indexOf('/', 5) < 0 ? "page" : "card";
		return "card";
	}

	private static String kindOfCtor(String ctor) {
		return switch (ctor) {
			case "Row" -> "row";
			case "SwitcherView" -> "view";
			case "Chart" -> "chart";
			default -> null;
		};
	}

	private static Object handlerOf(Object arg) {
		if (arg instanceof Map<?,?> m)
			return HandlerRef.inline((String)m.get("inline"));
		return HandlerRef.named((String)arg);
	}

	@SuppressWarnings({
		"unchecked" // Casts a parsed JSON value to its known Map/List shape.
	})
	@TestFactory Stream<DynamicTest> a01_corpus() throws Exception {
		var corpus = JsonMap.ofString(Files.readString(CORPUS, UTF_8));
		return corpus.getList("cases").stream().map(o -> (JsonMap)o).map(c -> DynamicTest.dynamicTest(c.getString("name"), () -> {
			var name = c.getString("name");
			if (Boolean.TRUE.equals(c.get("schema"))) {
				var errs = PageContractSchema.get().validate(c.getMap("contract").toString());
				assertFalse(errs.isEmpty(), name + " must fail the schema");
				return;
			}
			if (CLIENT_ONLY.contains(name) || c.get("build") == null)
				return;
			var kinds = new HashMap<String,String>();
			var blocks = new HashMap<String,ModelBlock>();
			var steps = (List<Map<String,Object>>)c.get("build");
			// Replay: each step must be accepted until the last one, which must be refused.
			for (var i = 0; i < steps.size(); i++) {
				var s = steps.get(i);
				var ctor = (String)s.get("ctor");
				if (ctor != null) {
					var k = kindOfCtor(ctor);
					if (k != null) {
						kinds.put("@" + s.get("as"), k);
						blocks.put("@" + s.get("as"), new ModelBlock(k, ctor));
					}
					continue;
				}
				var on = (String)s.get("on");
				var args = (List<Object>)s.get("args");
				var block = blocks.computeIfAbsent(on, p -> new ModelBlock(kinds.getOrDefault(p, kindOfPath(p)), p));
				Runnable r = switch ((String)s.get("call")) {
					case "bind" -> () -> block.on((String)args.get(0), (HandlerRef)handlerOf(args.get(1)));
					case "action" -> () -> block.action(ModelAction.of((String)args.get(0)).handler((String)args.get(2)));
					case "listen" -> () -> block.subscribe((String)args.get(0), (HandlerRef)handlerOf(args.get(1)));
					default -> null;
				};
				if (r == null)
					return;
				if (i < steps.size() - 1)
					r.run();
				else
					assertThrows(IllegalArgumentException.class, r::run, name + " must be refused by the Java builders");
			}
		}));
	}
}
