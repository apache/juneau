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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.console.*;
import org.apache.juneau.console.views.*;

/**
 * The built-in {@code job-log} card type: a form that submits to an endpoint returning an {@code AsyncJobRef}, then
 * streams the job's progress from {@code AsyncJobsMixin} into a log pane, with cancel and reload re-attach.
 * {@code juneau-joblog.js} draws it; this handler checks the body's shape.
 *
 * <p>
 * The body keys are {@code submit} ({@code endpoint}, required and same-origin; {@code method} {@code POST} or
 * {@code PUT}; {@code envelope} {@code flat} (the default) or {@code action}; {@code action}, required with the
 * action envelope; {@code fields}; {@code submitLabel}), {@code height} (at least {@value #MIN_HEIGHT} pixels),
 * {@code urlParam}, {@code clearOnSubmit} (a boolean) and {@code output}.  A field takes {@code name} (required),
 * {@code label}, {@code type} ({@code text}, {@code textarea}, {@code number} or {@code hidden}), {@code placeholder},
 * {@code required} and {@code default}; {@code source} is rejected, since a page card has no row (use a
 * {@link JobLogRegion}).  Unknown keys are rejected at every level, as the page schema rejects them.  In the flat
 * envelope no field may be named {@code idempotencyKey}: the runtime adds that key, with a fresh value, to every
 * submit body.  The action envelope sends {@code {action, targetId, idempotencyKey, fields}}, self-targeted:
 * {@code targetId} is the key's own value.  The shape rules are {@link JobLogChecks}, shared with
 * {@link JobLogRegion}.  There is no typed builder; build it with {@link CardSpec#of(String, String)}.
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
@SuppressWarnings({
	"java:S1192" // Repeated literals are contract wire keys; named constants would obscure the contract shape.
})
public final class JobLogCardType implements CardTypeHandler {

	/** The smallest accepted {@code height}, in pixels. */
	public static final int MIN_HEIGHT = JobLogRegion.MIN_HEIGHT;

	private static final Set<String> BODY_KEYS = new LinkedHashSet<>(List.of("submit", "height", "urlParam", "clearOnSubmit", "output", "visibleWhen"));
	private static final Pattern URL_PARAM = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

	/** Public no-arg constructor, required by {@link java.util.ServiceLoader}. */
	public JobLogCardType() {
		// No state; instantiated by ServiceLoader.
	}

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
		var urlParam = body.get("urlParam");
		try {
			// The shared rules (submit keys, envelope, action, fields, output) live in JobLogChecks, beside the region's.
			JobLogChecks.submit(submit, JobLogChecks.Mode.PAGE);
			JobLogChecks.height(body.get("height"), MIN_HEIGHT);
			if (nn(urlParam) && ! (urlParam instanceof String u && URL_PARAM.matcher(u).matches()))
				throw iaex("urlParam must match '%s'; got '%s'.", URL_PARAM.pattern(), urlParam);
			JobLogChecks.clearOnSubmit(body.get("clearOnSubmit"));
			if (body.containsKey("output"))
				JobLogChecks.output(body.get("output"));
		} catch (IllegalArgumentException e) {
			throw source.error("type='job-log' %s", e.getMessage());
		}
		var frag = new JsonMap();
		frag.putAll(body);
		frag.remove("visibleWhen");
		return frag;
	}
}
