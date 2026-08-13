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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.processor.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the mixin-declared {@link Rest#mergeResponseProcessorsIntoHost() @Rest(mergeResponseProcessorsIntoHost=true)}
 * opt-in: a mixin that declares it on its own class has its {@code responseProcessors} folded into the host's own chain
 * under a plain {@link Rest#mixins() @Rest(mixins=...)} reference, while a mixin that does NOT declare it keeps today's
 * isolated scoping (the non-silent contract).
 *
 * <p>
 * This is the mixin-side, response-processor-scoped counterpart to
 * {@code MixinInheritance_ResponseProcessors_Test} (a02/a04) in {@code juneau-integration-tests}, exercised here at the
 * {@link RestContext} level without a mock client so it runs inside {@code juneau-rest-server}'s own test suite.
 *
 * @since 10.0.0
 */
class MixinResponseProcessorFold_Test extends org.apache.juneau.TestBase {

	//-----------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------

	public static class HostRp1 implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, NotAcceptable, BasicHttpException { return NEXT; }
	}

	public static class MixinRp1 implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, NotAcceptable, BasicHttpException { return NEXT; }
	}

	/** Opted-in mixin: declares the response-processor fold on its own class. */
	@Rest(responseProcessors={MixinRp1.class}, mergeResponseProcessorsIntoHost=true)
	public static class M_OptedIn {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	/** Non-opted-in mixin: same response processor, but no fold opt-in (regression guard for the non-silent contract). */
	@Rest(responseProcessors={MixinRp1.class})
	public static class M_NotOptedIn {
		@RestGet(path="/my") public String my() { return "my"; }
	}

	@Rest(responseProcessors={HostRp1.class}, mixins={M_OptedIn.class})
	public static class HostWithOptedInMixin {
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Rest(responseProcessors={HostRp1.class}, mixins={M_NotOptedIn.class})
	public static class HostWithNonOptedInMixin {
		@RestGet(path="/h") public String h() { return "h"; }
	}

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	private static List<Class<?>> classesOf(ResponseProcessor[] rps) {
		var l = new ArrayList<Class<?>>(rps.length);
		for (var rp : rps) l.add(rp.getClass());
		return l;
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - opt-in folds the mixin's response processor into the host chain
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_optedInMixin_foldsResponseProcessorIntoHostChain() throws Exception {
		var hostCtx = new RestContext(argsOf(HostWithOptedInMixin.class, HostWithOptedInMixin::new));
		var hostRps = classesOf(hostCtx.getResponseProcessors());

		assertTrue(hostRps.contains(MixinRp1.class),
			"Host must fold in the opted-in mixin's MixinRp1 under a plain mixins= reference");
		assertTrue(hostRps.contains(HostRp1.class),
			"Host must still register its own HostRp1");

		var mixinCtx = hostCtx.getMixinContexts().get(M_OptedIn.class);
		assertNotNull(mixinCtx);
		assertTrue(classesOf(mixinCtx.getResponseProcessors()).contains(MixinRp1.class),
			"Mixin endpoint must still have its own MixinRp1");
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - non-opted-in mixin keeps today's isolated scoping (non-silent contract)
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_nonOptedInMixin_keepsIsolatedScoping() throws Exception {
		var hostCtx = new RestContext(argsOf(HostWithNonOptedInMixin.class, HostWithNonOptedInMixin::new));
		var hostRps = classesOf(hostCtx.getResponseProcessors());

		assertFalse(hostRps.contains(MixinRp1.class),
			"Host must NOT have MixinRp1 — a mixin without the opt-in stays scoped to its own endpoints");
		assertTrue(hostRps.contains(HostRp1.class),
			"Host must still register its own HostRp1");

		var mixinCtx = hostCtx.getMixinContexts().get(M_NotOptedIn.class);
		assertNotNull(mixinCtx);
		assertTrue(classesOf(mixinCtx.getResponseProcessors()).contains(MixinRp1.class),
			"Mixin endpoint must still have its own MixinRp1 via its own @Rest(responseProcessors=)");
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - ordering / precedence: the host's own processor precedes the folded mixin processor
	//-----------------------------------------------------------------------------------------------------------

	@Test void c01_foldedProcessorAppendedAfterHostChain() throws Exception {
		var hostCtx = new RestContext(argsOf(HostWithOptedInMixin.class, HostWithOptedInMixin::new));
		var hostRps = classesOf(hostCtx.getResponseProcessors());

		var hostIdx = hostRps.indexOf(HostRp1.class);
		var mixinIdx = hostRps.indexOf(MixinRp1.class);
		assertTrue(hostIdx >= 0 && mixinIdx >= 0, "Both processors must be present");
		assertTrue(hostIdx < mixinIdx,
			"Host's own HostRp1 must precede the folded MixinRp1 (append semantics)");
	}
}
