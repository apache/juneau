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
package org.apache.juneau.rest.server.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.jsp.*;
import org.apache.juneau.rest.server.view.mustache.*;
import org.apache.juneau.rest.server.view.thymeleaf.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link ResponseProcessorList} partition pass that repositions
 * {@link ViewRenderer} processors before the first {@link CatchAllResponseProcessor}.
 *
 * @since 10.0.0
 */
class ResponseProcessorList_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Stub processors used across tests.
	// -----------------------------------------------------------------------------------------------------------------

	static class OtherA implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, BasicHttpException { return NEXT; }
	}

	static class OtherB implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, BasicHttpException { return NEXT; }
	}

	static class OtherC implements ResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, BasicHttpException { return NEXT; }
	}

	static class ViewRendererA implements ViewRenderer {
		@Override public int process(RestOpSession s) throws IOException, BasicHttpException { return NEXT; }
	}

	static class ViewRendererB implements ViewRenderer {
		@Override public int process(RestOpSession s) throws IOException, BasicHttpException { return NEXT; }
	}

	static class CatchAllA implements CatchAllResponseProcessor {
		@Override public int process(RestOpSession s) throws IOException, BasicHttpException { return NEXT; }
	}

	// Convenience factory to avoid repeating new BasicBeanStore(null) everywhere.
	private static ResponseProcessorList.Builder builder() {
		return ResponseProcessorList.create(new BasicBeanStore(null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a: Partition-pass no-op cases.
	// -----------------------------------------------------------------------------------------------------------------

	/** No ViewRenderer present — chain must be unchanged. */
	@Test void a01_noViewRenderer_chainUnchanged() {
		var a = new OtherA();
		var b = new OtherB();
		var chain = builder().add(a, b).build().toArray();
		assertArrayEquals(new ResponseProcessor[]{a, b}, chain);
	}

	/** ViewRenderer present but no CatchAllResponseProcessor — no reorder, no crash. */
	@Test void a02_noCatchAll_chainUnchanged() {
		var a = new OtherA();
		var vr = new ViewRendererA();
		var chain = builder().add(a, vr).build().toArray();
		assertArrayEquals(new ResponseProcessor[]{a, vr}, chain);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b: Partition-pass reorder cases.
	// -----------------------------------------------------------------------------------------------------------------

	/** [OtherA, ViewRendererA, OtherB, CatchAllA, OtherC] → [OtherA, OtherB, ViewRendererA, CatchAllA, OtherC]. */
	@Test void b01_singleViewRenderer_prependsBeforeCatchAll() {
		var a  = new OtherA();
		var vr = new ViewRendererA();
		var b  = new OtherB();
		var ca = new CatchAllA();
		var c  = new OtherC();
		var chain = builder().add(a, vr, b, ca, c).build().toArray();
		assertArrayEquals(new ResponseProcessor[]{a, b, vr, ca, c}, chain);
	}

	/** [CatchAllA, ViewRendererB, ViewRendererA] → [ViewRendererB, ViewRendererA, CatchAllA].
	 * Registration order among renderers is preserved. */
	@Test void b02_multipleViewRenderers_allPrependInOrder() {
		var ca  = new CatchAllA();
		var vrb = new ViewRendererB();
		var vra = new ViewRendererA();
		var chain = builder().add(ca, vrb, vra).build().toArray();
		assertArrayEquals(new ResponseProcessor[]{vrb, vra, ca}, chain);
	}

	/** ViewRenderer already before CatchAll — chain preserved (no double-move). */
	@Test void b03_viewRendererAlreadyBeforeCatchAll_unchanged() {
		var vr = new ViewRendererA();
		var a  = new OtherA();
		var ca = new CatchAllA();
		var chain = builder().add(vr, a, ca).build().toArray();
		// vr is already a ViewRenderer; after reorder: non-viewrenderers first (a), then renderers (vr), then catch-all (ca).
		assertArrayEquals(new ResponseProcessor[]{a, vr, ca}, chain);
	}

	/** [SerializedPojoProcessor, JspViewRenderer] → JspViewRenderer runs before SerializedPojoProcessor. */
	@Test void b04_viewRendererAfterCatchAll_movedBefore() {
		var chain = builder()
			.add(SerializedPojoProcessor.class, JspViewRenderer.class)
			.build()
			.toArray();
		assertEquals(2, chain.length);
		assertInstanceOf(JspViewRenderer.class, chain[0]);
		assertInstanceOf(SerializedPojoProcessor.class, chain[1]);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c: Deduplication.
	// -----------------------------------------------------------------------------------------------------------------

	/** Adding JspViewRenderer.class twice yields only one instance. */
	@Test void c01_sameClassDedup_classToken() {
		var chain = builder()
			.add(JspViewRenderer.class, SerializedPojoProcessor.class)
			.add(JspViewRenderer.class)   // duplicate — should be silently ignored
			.build()
			.toArray();
		var jspCount = 0;
		for (var p : chain)
			if (p instanceof JspViewRenderer)
				jspCount++;
		assertEquals(1, jspCount, "JspViewRenderer should appear exactly once");
	}

	/** Adding a JspViewRenderer instance twice yields only one instance. */
	@Test void c02_sameClassDedup_instances() {
		var jspA = new JspViewRenderer();
		var jspB = new JspViewRenderer();  // different instance, same class
		var chain = builder()
			.add(SerializedPojoProcessor.class)
			.add(jspA)
			.add(jspB)   // duplicate by class — should be silently ignored
			.build()
			.toArray();
		var jspCount = 0;
		for (var p : chain)
			if (p instanceof JspViewRenderer)
				jspCount++;
		assertEquals(1, jspCount, "JspViewRenderer should appear exactly once");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d: Default-chain ordering invariant.
	// -----------------------------------------------------------------------------------------------------------------

	/** All four built-in renderers, when mixed into the default chain alongside SerializedPojoProcessor,
	 * appear before SerializedPojoProcessor in the final list. */
	@Test void d01_defaultChain_renderersPrecedeCatchAll() {
		// Simulate the default chain (from DefaultConfig), adding all four renderers as
		// well. ViewRenderers are appended at the end; the partition pass must hoist them.
		var chain = builder()
			.add(
				AsyncResponseProcessor.class,
				ReaderProcessor.class,
				InputStreamProcessor.class,
				ThrowableProcessor.class,
				ProblemDetailsProcessor.class,
				HttpResponseProcessor.class,
				HttpResourceProcessor.class,
				HttpBodyProcessor.class,
				ResponseBeanProcessor.class,
				PlainTextPojoProcessor.class,
				SerializedPojoProcessor.class,  // catch-all
				JspViewRenderer.class,
				ThymeleafViewRenderer.class,
				MustacheViewRenderer.class,
				FreemarkerViewRenderer.class
			)
			.build()
			.toArray();

		// Find the index of the first CatchAllResponseProcessor.
		var catchAllIdx = -1;
		for (var i = 0; i < chain.length; i++) {
			if (chain[i] instanceof CatchAllResponseProcessor) {
				catchAllIdx = i;
				break;
			}
		}
		assertNotEquals(-1, catchAllIdx, "Chain must contain a CatchAllResponseProcessor");

		// Every ViewRenderer must appear before the catch-all.
		for (var i = 0; i < chain.length; i++) {
			if (chain[i] instanceof ViewRenderer)
				assertTrue(i < catchAllIdx,
					chain[i].getClass().getSimpleName() + " at index " + i + " must be before catch-all at " + catchAllIdx);
		}

		// The four built-in renderers must all be present.
		var types = new java.util.HashSet<Class<?>>();
		for (var p : chain)
			types.add(p.getClass());
		assertTrue(types.contains(JspViewRenderer.class),       "JspViewRenderer must be in chain");
		assertTrue(types.contains(ThymeleafViewRenderer.class), "ThymeleafViewRenderer must be in chain");
		assertTrue(types.contains(MustacheViewRenderer.class),  "MustacheViewRenderer must be in chain");
		assertTrue(types.contains(FreemarkerViewRenderer.class),"FreemarkerViewRenderer must be in chain");
	}
}
