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
package org.apache.juneau.console.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

/**
 * The shared shape checks of a job-log declaration, used by the console's page-level {@code job-log} card and by a
 * {@link JobLogRegion}, so the two cannot drift.
 *
 * <p>
 * Every check throws an {@link IllegalArgumentException} whose message names the offending key relative to the
 * job-log body (for example {@code "submit.fields[0] type must be ..."}); each caller prefixes its own context.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192", // Duplicated key names are wire keys of the job-log body; constants would obscure the schema.
	"java:S3776", // Validation and rendering chains many independent guards; splitting them adds no clarity.
	"java:S6353" // Pattern text mirrors the JS-side check and is pinned by tests.
})
public final class JobLogChecks {

	/** Where a job-log declaration lives. */
	public enum Mode {
		/** The console's page-level {@code job-log} card. */
		PAGE,
		/** A row-detail region with {@code populate: "job-log"}. */
		REGION
	}

	/** The grammar of a field's {@code source}: {@code row.<identifier>}, nothing nested. */
	public static final Pattern SOURCE = Pattern.compile("row\\.[A-Za-z_][A-Za-z0-9_]{0,63}");

	/** The grammar of {@code output.from}. */
	public static final Pattern OUTPUT_FROM = Pattern.compile("message|row\\.[A-Za-z_][A-Za-z0-9_]{0,63}");

	/** The grammar of {@code submit.action}; the page schema's {@code id}. */
	public static final Pattern ACTION = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

	/** The page card's {@code submit} keys, in message order. */
	public static final List<String> PAGE_SUBMIT_KEYS = List.of("endpoint", "method", "envelope", "action", "fields", "submitLabel");

	/** A region's {@code submit} keys, in message order. */
	public static final List<String> REGION_SUBMIT_KEYS = List.of("endpoint", "method", "envelope", "action", "keyUrl", "fields", "submitLabel");

	/** A field's keys, in message order. */
	public static final List<String> FIELD_KEYS = List.of("name", "label", "type", "placeholder", "required", "default", "source");

	/** The field types. */
	public static final Set<String> FIELD_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(List.of("text", "textarea", "number", "hidden")));

	/** The {@code output} keys, in message order. */
	public static final List<String> OUTPUT_KEYS = List.of("target", "mode", "from", "when");

	/** The {@code detail} keys, in message order. */
	public static final List<String> DETAIL_KEYS = List.of("renderer", "fields");

	private static final Set<String> METHODS = Set.of("POST", "PUT");
	private static final Set<String> MODES = Set.of("replace", "append");
	private static final Set<String> WHENS = Set.of("success", "any");
	private static final Pattern TOKEN = Pattern.compile("\\{[^}]*\\}");

	// The page card adds a fresh idempotencyKey to every flat submit body, so a field of that name would be overwritten.
	private static final String RESERVED_FIELD = "idempotencyKey";

	private JobLogChecks() {}

	/**
	 * Checks a {@code submit} object's keys, method, label, envelope, action, key URL and fields.
	 *
	 * <p>
	 * The page card checks its own {@code endpoint} first (it has the console's same-origin rule); a region's
	 * {@code endpoint} and {@code keyUrl} are checked here as same-origin templates whose only token is {@code {id}}.
	 *
	 * @param submit The {@code submit} object.
	 * @param mode Where the declaration lives.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public static void submit(Map<?,?> submit, Mode mode) {
		var keys = mode == Mode.PAGE ? PAGE_SUBMIT_KEYS : REGION_SUBMIT_KEYS;
		for (var k : submit.keySet())
			if (! keys.contains(k))
				throw iaex("submit unknown key '%s'; allowed: '%s'.", k, String.join(", ", keys));
		if (mode == Mode.REGION) {
			template("submit.endpoint", submit.get("endpoint"));
			template("submit.keyUrl", submit.get("keyUrl"));
		}
		var method = submit.get("method");
		if (nn(method) && ! METHODS.contains(method))
			throw iaex("submit.method must be POST or PUT; got '%s'.", method);
		var submitLabel = submit.get("submitLabel");
		if (nn(submitLabel) && ! (submitLabel instanceof String))
			throw iaex("submit.submitLabel must be a string; got '%s'.", submitLabel);
		var envelope = submit.get("envelope");
		if (mode == Mode.REGION) {
			if (nn(envelope) && ! "action".equals(envelope))
				throw iaex("submit.envelope must be 'action' in a row-detail region (the row-bound key is verified against targetId); got '%s'.", envelope);
		} else if (nn(envelope) && ! ("flat".equals(envelope) || "action".equals(envelope))) {
			throw iaex("submit.envelope must be 'flat' or 'action'; got '%s'.", envelope);
		}
		var actionEnvelope = mode == Mode.REGION || "action".equals(envelope);
		var action = submit.get("action");
		if (actionEnvelope && action == null)
			throw iaex("submit.action is required with envelope 'action'.");
		if (nn(action) && ! (action instanceof String a && ACTION.matcher(a).matches()))
			throw iaex("submit.action must match '%s'; got '%s'.", ACTION.pattern(), action);
		var fields = submit.get("fields");
		if (nn(fields)) {
			if (! (fields instanceof List<?> list))
				throw iaex("submit.fields must be an array; got '%s'.", fields);
			for (var i = 0; i < list.size(); i++)
				field(i, list.get(i), mode, actionEnvelope);
		}
	}

	private static void template(String name, Object v) {
		if (! (v instanceof String s) || s.isBlank())
			throw iaex("%s is required.", name);
		if (! RegionDef.isSafeDetailEndpoint(s))
			throw iaex("%s must be a same-origin path (no scheme, no leading '//', no '..' segment); got '%s'.", name, s);
		var m = TOKEN.matcher(s);
		while (m.find())
			if (! "{id}".equals(m.group()))
				throw iaex("%s may only use the '%s' token; got '%s'.", name, "{id}", m.group());
	}

	/**
	 * Checks one field.
	 *
	 * @param i The field's index, for the message.
	 * @param f The field.
	 * @param mode Where the declaration lives; {@code source} needs a row, so the page card rejects it.
	 * @param actionEnvelope Whether the fields are nested under {@code fields} (no reserved names) or flat.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public static void field(int i, Object f, Mode mode, boolean actionEnvelope) {
		if (! (f instanceof Map<?,?> m))
			throw iaex("submit.fields[%s] must be an object; got '%s'.", i, f);
		for (var k : m.keySet())
			if (! FIELD_KEYS.contains(k))
				throw iaex("submit.fields[%s] unknown key '%s'; allowed: '%s'.", i, k, String.join(", ", FIELD_KEYS));
		if (! (m.get("name") instanceof String name) || name.isBlank())
			throw iaex("submit.fields[%s] requires a non-blank 'name'.", i);
		if (! actionEnvelope && RESERVED_FIELD.equals(name))
			throw iaex("submit.fields name '%s' is reserved; the runtime sends it with every submit.", RESERVED_FIELD);
		var type = m.get("type");
		if (nn(type) && ! FIELD_TYPES.contains(type))
			throw iaex("submit.fields[%s] type must be 'text', 'textarea', 'number' or 'hidden'; got '%s'.", i, type);
		for (var k : List.of("label", "placeholder"))
			if (nn(m.get(k)) && ! (m.get(k) instanceof String))
				throw iaex("submit.fields[%s] %s must be a string; got '%s'.", i, k, m.get(k));
		var required = m.get("required");
		if (nn(required) && ! (required instanceof Boolean))
			throw iaex("submit.fields[%s] required must be a boolean; got '%s'.", i, required);
		var source = m.get("source");
		if (nn(source)) {
			if (mode == Mode.PAGE)
				throw iaex("submit.fields[%s] source needs a row; use a row-detail region.", i);
			if (! (source instanceof String s && SOURCE.matcher(s).matches()))
				throw iaex("submit.fields[%s] source must match '%s'; got '%s'.", i, SOURCE.pattern(), source);
		}
	}

	/**
	 * Checks an {@code output} object.
	 *
	 * @param o The {@code output} value.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public static void output(Object o) {
		if (! (o instanceof Map<?,?> m))
			throw iaex("output must be an object; got '%s'.", o);
		for (var k : m.keySet())
			if (! OUTPUT_KEYS.contains(k))
				throw iaex("output unknown key '%s'; allowed: '%s'.", k, String.join(", ", OUTPUT_KEYS));
		if (! (m.get("target") instanceof Map<?,?> t))
			throw iaex("output.target must be an object with exactly one of 'field' or 'selector'.");
		for (var k : t.keySet())
			if (! ("field".equals(k) || "selector".equals(k)))
				throw iaex("output.target unknown key '%s'; allowed: 'field, selector'.", k);
		if (t.size() != 1)
			throw iaex("output.target must set exactly one of 'field' or 'selector'.");
		var v = t.values().iterator().next();
		if (! (v instanceof String s) || s.isBlank())
			throw iaex("output.target.%s must be a non-blank string; got '%s'.", t.keySet().iterator().next(), v);
		var mode = m.get("mode");
		if (nn(mode) && ! MODES.contains(mode))
			throw iaex("output.mode must be 'replace' or 'append'; got '%s'.", mode);
		var from = m.get("from");
		if (nn(from) && ! (from instanceof String fs && OUTPUT_FROM.matcher(fs).matches()))
			throw iaex("output.from must match '%s'; got '%s'.", OUTPUT_FROM.pattern(), from);
		var when = m.get("when");
		if (nn(when) && ! WHENS.contains(when))
			throw iaex("output.when must be 'success' or 'any'; got '%s'.", when);
	}

	/**
	 * Checks a region's {@code detail} object: {@code {renderer, fields}}.
	 *
	 * @param o The {@code detail} value.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public static void detail(Object o) {
		if (! (o instanceof Map<?,?> m))
			throw iaex("detail must be an object; got '%s'.", o);
		for (var k : m.keySet())
			if (! DETAIL_KEYS.contains(k))
				throw iaex("detail unknown key '%s'; allowed: '%s'.", k, String.join(", ", DETAIL_KEYS));
		if (! (m.get("renderer") instanceof String r) || r.isBlank())
			throw iaex("detail.renderer must be a non-blank string.");
		if (! (m.get("fields") instanceof List<?> l))
			throw iaex("detail.fields must be an array.");
		for (var i = 0; i < l.size(); i++)
			if (! (l.get(i) instanceof Map))
				throw iaex("detail.fields[%s] must be an object; got '%s'.", i, l.get(i));
	}

	/**
	 * Checks {@code height}.
	 *
	 * @param height The value.  Can be <jk>null</jk>.
	 * @param min The smallest accepted height, in pixels.
	 * @throws IllegalArgumentException If it is set and not an integer of at least {@code min}.
	 */
	public static void height(Object height, int min) {
		if (nn(height) && ! ((height instanceof Integer || height instanceof Long) && ((Number)height).longValue() >= min))
			throw iaex("height must be an integer of at least '%s'; got '%s'.", min, height);
	}

	/**
	 * Checks {@code clearOnSubmit}.
	 *
	 * @param clear The value.  Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If it is set and not a boolean.
	 */
	public static void clearOnSubmit(Object clear) {
		if (nn(clear) && ! (clear instanceof Boolean))
			throw iaex("clearOnSubmit must be a boolean; got '%s'.", clear);
	}
}
