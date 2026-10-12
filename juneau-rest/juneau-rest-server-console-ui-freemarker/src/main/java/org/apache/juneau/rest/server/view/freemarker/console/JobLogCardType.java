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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;

/**
 * The built-in {@code job-log} card type: a form that submits to an endpoint returning an {@code AsyncJobRef}, then
 * streams the job's progress from {@code AsyncJobsMixin} into a log pane, with cancel and reload re-attach.
 * {@code juneau-joblog.js} draws it; this handler checks the body's shape.
 *
 * <p>
 * The body keys are {@code submit} ({@code endpoint}, required and same-origin; {@code method} {@code POST} or
 * {@code PUT}; {@code fields}; {@code submitLabel}), {@code height} (at least {@value #MIN_HEIGHT} pixels),
 * {@code urlParam} and {@code clearOnSubmit} (a boolean).  A field takes {@code name} (required), {@code label},
 * {@code type} ({@code text}, {@code textarea} or {@code number}), {@code placeholder}, {@code required} and
 * {@code default}.  Unknown keys are rejected at every level, as the page schema rejects them.  No field may be named
 * {@code idempotencyKey}: the runtime adds that key, with a fresh value, to every submit body.  There is no typed builder; build it with
 * {@link CardSpec#of(String, String)}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@card type="job-log" id="rebuild" title="Rebuild index"&gt;
 * 	{ submit: { endpoint: '/ops/jobs/rebuild', fields: [ { name: 'shards', label: 'Shards', type: 'number', default: 4 } ] },
 * 	  urlParam: 'job' }
 * 	&lt;/@card&gt;
 * </p>
 * <p class='bjava'>
 * 	<jv>page</jv>.card(CardSpec.<jsm>of</jsm>(<js>"job-log"</js>, <js>"rebuild"</js>).title(<js>"Rebuild index"</js>)
 * 		.body(<js>"submit"</js>, Map.<jsm>of</jsm>(<js>"endpoint"</js>, <js>"/ops/jobs/rebuild"</js>)).body(<js>"urlParam"</js>, <js>"job"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public final class JobLogCardType implements CardTypeHandler {

	/** The smallest accepted {@code height}, in pixels. */
	public static final int MIN_HEIGHT = 80;

	private static final Set<String> BODY_KEYS = new LinkedHashSet<>(List.of("submit", "height", "urlParam", "clearOnSubmit", "visibleWhen"));
	private static final Set<String> SUBMIT_KEYS = new LinkedHashSet<>(List.of("endpoint", "method", "fields", "submitLabel"));
	private static final Set<String> FIELD_KEYS = new LinkedHashSet<>(List.of("name", "label", "type", "placeholder", "required", "default"));
	private static final Set<String> FIELD_TYPES = Set.of("text", "textarea", "number");
	private static final Set<String> METHODS = Set.of("POST", "PUT");
	// juneau-joblog.js adds a fresh idempotencyKey to every submit body, so a field of that name would be overwritten.
	private static final String RESERVED_FIELD = "idempotencyKey";
	private static final Pattern URL_PARAM = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public JobLogCardType() {}

	@Override
	public String type() {
		return "job-log";
	}

	/** A job-log card neither publishes nor subscribes. */
	@Override
	public Set<String> acceptedRoles() {
		return Set.of();
	}

	/** A job-log card neither publishes nor subscribes. */
	@Override
	public Set<String> acceptedOps() {
		return Set.of();
	}

	@Override
	public JsonMap toFragment(CardSource source) {
		if (! source.hasJsonBody())
			throw source.error("type='job-log' requires a JSON5 body with a 'submit' object.");
		var body = source.json();
		for (var k : body.keySet())
			if (! BODY_KEYS.contains(k))
				throw source.error("type='job-log' unknown key '%s'; allowed: '%s'.", k, String.join(", ", BODY_KEYS));
		if (! (body.get("submit") instanceof Map<?,?> submit) || ! (submit.get("endpoint") instanceof String endpoint) || endpoint.isBlank())
			throw source.error("type='job-log' requires submit.endpoint.");
		if (! SafePathTemplate.isSameOriginPath(endpoint))
			throw source.error("type='job-log' submit.endpoint must be a same-origin path; got '%s'.", endpoint);
		for (var k : submit.keySet())
			if (! SUBMIT_KEYS.contains(k))
				throw source.error("type='job-log' submit unknown key '%s'; allowed: '%s'.", k, String.join(", ", SUBMIT_KEYS));
		var method = submit.get("method");
		if (nn(method) && ! METHODS.contains(method))
			throw source.error("type='job-log' submit.method must be POST or PUT; got '%s'.", method);
		var submitLabel = submit.get("submitLabel");
		if (nn(submitLabel) && ! (submitLabel instanceof String))
			throw source.error("type='job-log' submit.submitLabel must be a string; got '%s'.", submitLabel);
		var fields = submit.get("fields");
		if (nn(fields)) {
			if (! (fields instanceof List<?> list))
				throw source.error("type='job-log' submit.fields must be an array; got '%s'.", fields);
			for (var i = 0; i < list.size(); i++)
				checkField(source, i, list.get(i));
		}
		var height = body.get("height");
		if (nn(height) && ! ((height instanceof Integer || height instanceof Long) && ((Number)height).longValue() >= MIN_HEIGHT))
			throw source.error("type='job-log' height must be an integer of at least '%s'; got '%s'.", MIN_HEIGHT, height);
		var urlParam = body.get("urlParam");
		if (nn(urlParam) && ! (urlParam instanceof String u && URL_PARAM.matcher(u).matches()))
			throw source.error("type='job-log' urlParam must match '%s'; got '%s'.", URL_PARAM.pattern(), urlParam);
		var clear = body.get("clearOnSubmit");
		if (nn(clear) && ! (clear instanceof Boolean))
			throw source.error("type='job-log' clearOnSubmit must be a boolean; got '%s'.", clear);
		var frag = new JsonMap();
		frag.putAll(body);
		frag.remove("visibleWhen");
		return frag;
	}

	// The same shape the page schema's jobLogCard holds a field to.
	private static void checkField(CardSource source, int i, Object f) {
		if (! (f instanceof Map<?,?> m))
			throw source.error("type='job-log' submit.fields[%s] must be an object; got '%s'.", i, f);
		for (var k : m.keySet())
			if (! FIELD_KEYS.contains(k))
				throw source.error("type='job-log' submit.fields[%s] unknown key '%s'; allowed: '%s'.", i, k, String.join(", ", FIELD_KEYS));
		if (! (m.get("name") instanceof String name) || name.isBlank())
			throw source.error("type='job-log' submit.fields[%s] requires a non-blank 'name'.", i);
		if (RESERVED_FIELD.equals(name))
			throw source.error("type='job-log' submit.fields name '%s' is reserved; the runtime sends it with every submit.", RESERVED_FIELD);
		var type = m.get("type");
		if (nn(type) && ! FIELD_TYPES.contains(type))
			throw source.error("type='job-log' submit.fields[%s] type must be 'text', 'textarea' or 'number'; got '%s'.", i, type);
		for (var k : List.of("label", "placeholder"))
			if (nn(m.get(k)) && ! (m.get(k) instanceof String))
				throw source.error("type='job-log' submit.fields[%s] %s must be a string; got '%s'.", i, k, m.get(k));
		var required = m.get("required");
		if (nn(required) && ! (required instanceof Boolean))
			throw source.error("type='job-log' submit.fields[%s] required must be a boolean; got '%s'.", i, required);
	}
}
