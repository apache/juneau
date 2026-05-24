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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Validates the comma-split-per-element pipeline on the {@code @Rest(paths=...)} annotation rung.
 *
 * <p>
 * Post-rework (2026-05-24), each {@code @Rest(paths=...)} element is SVL-resolved and then split on
 * {@code ,}; each piece is trimmed and empty pieces are dropped.  This test exercises the comma-split
 * pipeline directly on pure-literal elements (no SVL needed), so the focus is the split semantics:
 * <ul>
 * 	<li>A single element with a comma expands to multiple mount paths.
 * 	<li>Whitespace around each comma-split piece is trimmed.
 * 	<li>Empty pieces (consecutive commas, trailing comma) are dropped.
 * 	<li>Multiple annotation elements can each contain commas; the resolved array preserves order.
 * </ul>
 *
 * <p>
 * The SVL + comma combination is covered separately by {@link RestPathsRuntimeOverride_SVL_Test}.
 *
 * @since 9.5.0
 */
class RestPathsRuntimeOverride_CommaSplit_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a — single element with a comma expands to multiple mount paths
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/healthz,/readyz"})
	public static class A_SingleElementWithComma {}

	@Test
	void a01_singleElementWithComma_expandsToTwoMounts() throws Exception {
		var args = new RestContext.Args(A_SingleElementWithComma.class, null, null, A_SingleElementWithComma::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/healthz", "/readyz"}, ctx.getPaths(),
			"A single element containing a comma must comma-split into multiple mount paths");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — whitespace around each comma-split piece is trimmed
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"  /a , /b  "})
	public static class B_WhitespaceAroundCommas {}

	@Test
	void b01_whitespace_trimmedAroundEachPiece() throws Exception {
		var args = new RestContext.Args(B_WhitespaceAroundCommas.class, null, null, B_WhitespaceAroundCommas::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/a", "/b"}, ctx.getPaths(),
			"Whitespace around each comma-split piece must be trimmed");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — empty pieces (consecutive commas, trailing comma) are dropped
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/a,,/b,"})
	public static class C_EmptyPiecesDropped {}

	@Test
	void c01_emptyPieces_dropped() throws Exception {
		var args = new RestContext.Args(C_EmptyPiecesDropped.class, null, null, C_EmptyPiecesDropped::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/a", "/b"}, ctx.getPaths(),
			"Consecutive commas and trailing commas must produce empty pieces that are dropped from the final array");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — multiple elements, with commas mixed in, preserve order across the resolved array
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/api", "/a,/b"})
	public static class D_MultiElement_MixedCommas {}

	@Test
	void d01_multipleElements_withCommas_preserveOrder() throws Exception {
		var args = new RestContext.Args(D_MultiElement_MixedCommas.class, null, null, D_MultiElement_MixedCommas::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/api", "/a", "/b"}, ctx.getPaths(),
			"Multi-element paths with commas in some elements should produce the concatenated, order-preserved comma-split sequence");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — degenerate cases: blank-only element produces zero mounts; the resolver doesn't crash
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"   "})
	public static class E_BlankOnlyElement {}

	@Test
	void e01_blankOnlyElement_dropsToEmpty() throws Exception {
		var args = new RestContext.Args(E_BlankOnlyElement.class, null, null, E_BlankOnlyElement::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertNotNull(ctx.getPaths());
		assertEquals(0, ctx.getPaths().length,
			"A blank-only element should comma-split to nothing — final array is empty");
	}

	@Rest(paths={",,,"})
	public static class F_CommasOnlyElement {}

	@Test
	void f01_commasOnlyElement_dropsToEmpty() throws Exception {
		var args = new RestContext.Args(F_CommasOnlyElement.class, null, null, F_CommasOnlyElement::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertNotNull(ctx.getPaths());
		assertEquals(0, ctx.getPaths().length,
			"A commas-only element should comma-split to an array of empty pieces that all get dropped");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — empty-string element short-circuits SVL and produces zero mounts
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={""})
	public static class G_EmptyStringElement {}

	@Test
	void g01_emptyStringElement_shortCircuits() throws Exception {
		// "" is the special-case input that lets the SVL pass skip session.resolve(...) entirely (no
		// variables to resolve in an empty string).  The post-SVL value "" then comma-splits to nothing.
		var args = new RestContext.Args(G_EmptyStringElement.class, null, null, G_EmptyStringElement::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertNotNull(ctx.getPaths());
		assertEquals(0, ctx.getPaths().length,
			"An empty-string element must short-circuit the SVL pass and drop to zero mounts");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h — mixed empty-string and literal elements preserve the literal
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"", "/literal", ""})
	public static class H_MixedEmptyAndLiteral {}

	@Test
	void h01_mixedEmptyAndLiteral_preservesLiteral() throws Exception {
		// Empty elements drop; the literal element stays.  Useful as a structural test that the loop in
		// expandPathsElements iterates per element rather than collapsing the whole array into one buffer.
		var args = new RestContext.Args(H_MixedEmptyAndLiteral.class, null, null, H_MixedEmptyAndLiteral::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/literal"}, ctx.getPaths(),
			"Empty-string elements should drop, leaving the literal element intact");
	}
}
