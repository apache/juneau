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
package org.apache.juneau.rest.server.management;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Child-resource flavor of the diagnostic dump endpoints ({@code /threaddump}, {@code /heapdump}).
 *
 * <p>
 * Mounts as a routed child via {@link Rest#children() @Rest(children=DumpsResource.class)} and delegates to a
 * shared {@link DumpsManager} worker &mdash; the same logic the {@link DumpsMixin mixin} flavor uses, so the
 * two forms cannot drift.  Extends {@link BasicRestResource} (mirrors {@code HealthResource}).
 *
 * <p>
 * <b>Both dumps are deny-by-default</b> &mdash; they respond {@code 403 Forbidden} unless a
 * {@link DumpsSettings} bean enabling them is registered.  {@code /heapdump} additionally degrades to
 * {@code 501 Not Implemented} on a JVM without HotSpot heap-dump support.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DumpsMixin}
 * 	<li class='jc'>{@link DumpsManager}
 * 	<li class='jc'>{@link DumpsSettings}
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(path="/dumps")
public class DumpsResource extends BasicRestResource {

	private final DumpsManager defaultManager = new DumpsManager();

	/**
	 * Returns the {@link DumpsManager} worker, resolved from the bean store when a consumer registers one, else a
	 * built-in default.  Resolving through the bean store lets consumers (and tests) supply an alternate worker
	 * without subclassing.
	 *
	 * @param req The HTTP request (its context's bean store is searched).
	 * @return The resolved manager; never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this only borrows a bean and must not close it.
	})
	protected DumpsManager manager(RestRequest req) {
		return req.getContext().getBeanStore().getBean(DumpsManager.class).orElse(defaultManager);
	}

	/**
	 * [GET /threaddump] - Full thread dump from the {@link java.lang.management.ThreadMXBean ThreadMXBean}.
	 *
	 * @param req The HTTP request.
	 * @return The thread dump text.
	 * @throws Forbidden If the thread-dump endpoint is not enabled.
	 */
	@RestGet(
		path="/threaddump",
		summary="Thread dump",
		description="Renders a full thread dump from the JVM ThreadMXBean.  Disabled by default; opt in via DumpsSettings."
	)
	public String getThreadDump(RestRequest req) {
		var manager = manager(req);
		if (! manager.resolveSettings(req.getContext()).isThreadDumpEnabled())
			throw new Forbidden("The /threaddump endpoint is disabled.  Register a DumpsSettings bean with threadDump enabled to use it.");
		return manager.threadDump();
	}

	/**
	 * [GET /heapdump] - HotSpot heap dump (hprof) stream.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @return A self-deleting stream over the hprof file.
	 * @throws Forbidden If the heap-dump endpoint is not enabled.
	 * @throws NotImplemented If this JVM has no HotSpot heap-dump support.
	 * @throws IOException If the dump could not be produced.
	 */
	@RestGet(
		path="/heapdump",
		summary="Heap dump",
		description="Produces a HotSpot heap dump (hprof).  Disabled by default; opt in via DumpsSettings."
	)
	@SuppressWarnings({
		"resource" // The returned stream is handed off to the framework's InputStreamProcessor, which pipes then closes it (and the backing temp file self-deletes on close).
	})
	public InputStream getHeapDump(RestRequest req, RestResponse res) throws IOException {
		var manager = manager(req);
		if (! manager.resolveSettings(req.getContext()).isHeapDumpEnabled())
			throw new Forbidden("The /heapdump endpoint is disabled.  Register a DumpsSettings bean with heapDump enabled to use it.");
		var stream = manager.heapDumpStream(true);
		if (stream == null)  // HTT: null only on a non-HotSpot JVM — not reproducible on HotSpot CI.
			throw new NotImplemented("Heap dumps are not supported on this JVM (no HotSpotDiagnosticMXBean).");
		res.setHeader("Content-Type", "application/octet-stream");
		res.setHeader("Content-Disposition", "attachment; filename=\"heapdump.hprof\"");
		return stream;
	}
}
