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
package org.apache.juneau.rest.server.datatables;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin that serves Juneau's first-party DataTables glue script at {@code /juneau-datatables.js}.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=DataTablesMixin.class)}; the
 * {@code /juneau-datatables.js} URL then becomes available alongside the host's own endpoints, so browser pages can
 * load the glue without the application hosting it itself.
 *
 * <h5 class='section'>What this ships (and what it deliberately does not):</h5>
 *
 * <p>
 * This is an Apache project, and the <a class="doclink" href="https://datatables.net">DataTables</a> library's license
 * is <b>not</b> an ASF category-A license, so <b>Juneau does not bundle DataTables' own JS/CSS (nor jQuery).</b> The
 * only asset served here is the thin, first-party {@code juneau-datatables.js} glue &mdash; it auto-initializes any
 * server-rendered {@code <table data-juneau-datatable>} (e.g. one built by {@link DataTablesTable}) by calling
 * {@code $(table).DataTable(opts)} once the DataTables library is present.  The DataTables library itself is
 * <b>caller-provided</b>: reference it from a CDN or self-host it.
 *
 * <h5 class='section'>Wiring the page assets (caller-side {@code @HtmlDocConfig}):</h5>
 *
 * <p>
 * Juneau's {@link org.apache.juneau.marshall.html.HtmlDocConfig @HtmlDocConfig} is resolved per endpoint / per
 * resource-class hierarchy &mdash; it is <b>not</b> contributed across a host's other endpoints by a mixin.  So a mixin
 * cannot silently inject page assets into <i>your</i> data endpoints; instead, add the wire-points to the resource (or
 * op) that renders the HTML table, referencing your chosen DataTables distribution and this served glue.  The URLs are
 * SVL-resolvable, so the DataTables location can be overridden per environment without code changes:
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/releases"</js>, mixins=DataTablesMixin.<jk>class</jk>)
 * 	<ja>@HtmlDocConfig</ja>(
 * 		stylesheet=<js>"$S{juneau.datatables.cssUrl:https://cdn.datatables.net/2.1.8/css/dataTables.dataTables.min.css}"</js>,
 * 		head={
 * 			<js>"&lt;script src='$S{juneau.datatables.jqueryUrl:https://code.jquery.com/jquery-3.7.1.min.js}'&gt;&lt;/script&gt;"</js>,
 * 			<js>"&lt;script src='$S{juneau.datatables.jsUrl:https://cdn.datatables.net/2.1.8/js/dataTables.min.js}'&gt;&lt;/script&gt;"</js>,
 * 			<js>"&lt;script src='servlet:/juneau-datatables.js'&gt;&lt;/script&gt;"</js>
 * 		}
 * 	)
 * 	<jk>public class</jk> ReleasesResource <jk>extends</jk> BasicRestServlet {
 * 		<ja>@RestGet</ja>
 * 		<jk>public</jk> Table getReleases() {
 * 			<jk>return</jk> DataTablesTable.<jsm>of</jsm>(<js>"releases"</js>, <jv>releases</jv>, Release.<jk>class</jk>);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The convenience constants {@link #JQUERY_CDN_URL}, {@link #DATATABLES_JS_CDN_URL}, and
 * {@link #DATATABLES_CSS_CDN_URL} document known-good CDN coordinates for the caller-supplied library.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}.  The mount path is pinned at the op level by
 * {@link RestGet @RestGet(path="/juneau-datatables.js")} on {@link #getGlueScript}; a class-level
 * {@code @Rest(paths=...)} declaration would be silently ignored under the mixin pattern.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DataTablesTable}
 * 	<li class='jc'>{@link DataTablesColumns}
 * 	<li class='jc'>{@link DataTablesQueryProtocol}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/examples/data_sources/dom">DataTables from a pre-rendered DOM table</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class DataTablesMixin {

	/** The URL path at which the first-party glue script is served (relative to the host mount). */
	public static final String GLUE_PATH = "/juneau-datatables.js";

	/** Classpath location of the shipped glue script (resolved against this class's package). */
	static final String GLUE_RESOURCE = "juneau-datatables.js";

	/** Content type emitted for the glue script. */
	static final String GLUE_CONTENT_TYPE = "text/javascript;charset=utf-8";

	/** {@code Cache-Control} header emitted for the glue script (1 day). */
	static final String GLUE_CACHE_CONTROL = "max-age=86400, public";

	/** The shipped glue script bytes, read once from the classpath on first request. */
	@SuppressWarnings({
		"java:S3077" // Double-checked-locking safe publication of one whole immutable array reference, not per-element mutation; AtomicReferenceArray solves a different problem.
	})
	private static volatile byte[] glueScript;

	/** A known-good CDN URL for jQuery (the caller-supplied DataTables dependency).  Documentation aid. */
	public static final String JQUERY_CDN_URL = "https://code.jquery.com/jquery-3.7.1.min.js";

	/** A known-good CDN URL for the DataTables JavaScript (caller-supplied).  Documentation aid. */
	public static final String DATATABLES_JS_CDN_URL = "https://cdn.datatables.net/2.1.8/js/dataTables.min.js";

	/** A known-good CDN URL for the DataTables stylesheet (caller-supplied).  Documentation aid. */
	public static final String DATATABLES_CSS_CDN_URL = "https://cdn.datatables.net/2.1.8/css/dataTables.dataTables.min.css";

	/**
	 * [GET /juneau-datatables.js] &mdash; serve the first-party DataTables glue script.
	 *
	 * @return The glue script as a JavaScript {@link HttpResource}.
	 * @throws IOException If the shipped glue resource could not be read (effectively unreachable &mdash; the resource
	 * 	is shipped in the same jar as this class).
	 */
	@RestGet(
		path=GLUE_PATH,
		summary="Juneau DataTables glue script",
		description="First-party JavaScript that auto-initializes server-rendered <table data-juneau-datatable> elements.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getGlueScript() throws IOException {
		return HttpResourceBean.of(
			ByteArrayBody.of(glueScript(), GLUE_CONTENT_TYPE),
			list(ContentType.of(GLUE_CONTENT_TYPE), CacheControl.of(GLUE_CACHE_CONTROL))
		);
	}

	/** Returns the shipped glue-script bytes, reading (and caching) them from the classpath on first call. */
	// IoUtils.read(InputStream) closes the stream (see its Javadoc); JDT can't see through the call.
	@SuppressWarnings("resource")
	private static byte[] glueScript() throws IOException {
		var g = glueScript;
		if (g == null) {
			synchronized (DataTablesMixin.class) {
				g = glueScript;
				if (g == null) {  // HTT: the "already set" branch is only reachable if another thread wins the race between the two reads - unhittable single-threaded.
					g = IoUtils.read(DataTablesMixin.class.getResourceAsStream(GLUE_RESOURCE)).getBytes(StandardCharsets.UTF_8);
					glueScript = g;
				}
			}
		}
		return g;
	}
}
