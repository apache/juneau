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
package org.apache.juneau.rest.server.console;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * Loads {@code juneau-page.schema.json} from the classpath and validates contracts against it, plus
 * the cross-reference rules R-1..R-4 the schema cannot express. Used by {@code PageCapture} in dev
 * mode and by {@code PageContractAssert} in tests.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	List&lt;String&gt; <jv>errors</jv> = PageContractSchema.<jsm>get</jsm>().validate(<jv>contractJson</jv>);
 * 	<jk>if</jk> (! <jv>errors</jv>.isEmpty())
 * 		<jk>throw new</jk> IllegalStateException(String.<jsm>join</jsm>(<js>"\n"</js>, <jv>errors</jv>));
 *
 * 	<jc>// With the &lt;template&gt; ids found in the rendered page, R-4 is checked too:</jc>
 * 	<jv>errors</jv> = PageContractSchema.<jsm>get</jsm>().validate(<jv>contractJson</jv>, Set.<jsm>of</jsm>(<js>"jc-seg-1"</js>, <js>"header.banner"</js>));
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"unchecked" // validate() casts the parsed JSON contract (nav, cards, activeNav) to List/Map, after the schema validator has checked those shapes
})
public final class PageContractSchema {

	/** Classpath location of the schema. */
	public static final String RESOURCE = "org/apache/juneau/console/juneau-page.schema.json";

	private static final PageContractSchema INSTANCE = new PageContractSchema();

	private final String schemaJson;
	private final SchemaSubsetValidator validator;

	private PageContractSchema() {
		try (var in = PageContractSchema.class.getClassLoader().getResourceAsStream(RESOURCE)) {
			if (in == null)
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			schemaJson = new String(in.readAllBytes(), UTF_8);
			validator = new SchemaSubsetValidator(JsonMap.ofString(schemaJson));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (Exception e) {
			throw new IllegalStateException("Unparseable " + RESOURCE, e);
		}
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return The shared instance, never <jk>null</jk>.
	 */
	public static PageContractSchema get() {
		return INSTANCE;
	}

	/**
	 * Returns the schema text exactly as shipped.
	 *
	 * @return The schema JSON.
	 */
	public String schemaJson() {
		return schemaJson;
	}

	/**
	 * Validates a contract against the schema and rules R-1..R-3.
	 *
	 * @param contractJson The contract JSON.
	 * @return The findings, empty when valid.
	 */
	public List<String> validate(String contractJson) {
		return validate(contractJson, null);
	}

	/**
	 * Validates a contract against the schema and rules R-1..R-4.
	 *
	 * @param contractJson The contract JSON.
	 * @param templateIds The {@code <template data-card|data-slot>} ids present in the page, or <jk>null</jk> to skip R-4.
	 * @return The findings, empty when valid.
	 */
	@SuppressWarnings({
		"java:S3776" // Rules R-1..R-4 read best as one straight-line pass.
	})
	public List<String> validate(String contractJson, Set<String> templateIds) {
		Map<String,Object> c;
		try {
			c = JsonMap.ofString(contractJson);
		} catch (Exception e) {
			return List.of("$: unparseable JSON: " + e.getMessage());
		}
		var errors = new ArrayList<>(validator.validate(c));
		if (! errors.isEmpty())
			return errors;
		var nav = (List<Object>)c.get("nav");
		checkSiblingIds(nav, "nav", errors);                                   // R-1
		var cardIds = new HashSet<String>();
		for (var o : (List<Object>)c.get("cards")) {                           // R-2
			var id = (String)((Map<String,Object>)o).get("id");
			if (! cardIds.add(id))
				errors.add("R-2: duplicate card id '" + id + "'");
		}
		var level = nav;                                                        // R-3
		for (var id : (List<String>)c.get("activeNav")) {
			var next = level == null ? null : level.stream().map(x -> (Map<String,Object>)x)
				.filter(n -> id.equals(n.get("id"))).findFirst().orElse(null);
			if (next == null) {
				errors.add("R-3: activeNav " + c.get("activeNav") + " is not a path in nav (failed at '" + id + "')");
				break;
			}
			level = (List<Object>)next.get("children");
		}
		if (templateIds != null)                                                // R-4
			for (var ref : templateRefs(c))
				if (! templateIds.contains(ref))
					errors.add("R-4: template '" + ref + "' is referenced but not present");
		return errors;
	}

	private static void checkSiblingIds(List<Object> nodes, String where, List<String> errors) {
		if (nodes == null)
			return;
		var seen = new HashSet<String>();
		for (var o : nodes) {
			var n = (Map<String,Object>)o;
			var id = (String)n.get("id");
			if (! seen.add(id))
				errors.add("R-1: duplicate nav id '" + id + "' under " + where);
			checkSiblingIds((List<Object>)n.get("children"), where + "/" + id, errors);
		}
	}

	static List<String> templateRefs(Map<String,Object> c) {
		var refs = new ArrayList<String>();
		for (var scope : List.of("header", "footer")) {
			var sec = (Map<String,Object>)c.get(scope);
			if (sec != null && sec.get("slots") instanceof Map<?,?> slots)
				for (var v : slots.values())
					refs.add((String)v);
		}
		for (var o : (List<Object>)c.get("cards")) {
			var t = ((Map<String,Object>)o).get("template");
			if (t != null)
				refs.add((String)t);
		}
		return refs;
	}
}
