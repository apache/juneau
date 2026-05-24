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
 * Validates the programmatic (highest-precedence) rung of the runtime-override resolution chain on
 * {@link RestContext#getPaths()}: an explicit {@link RestContext.Args#paths()} array (consumed by
 * {@code RestContext.Builder.paths(String...)}) overrides the {@link Rest#paths()} annotation default,
 * and {@code null} vs. {@code new String[0]} distinguish "inherit annotation" from "explicitly clear".
 *
 * <p>
 * See {@link RestContext#getPaths()} for the full precedence order
 * (programmatic &gt; getter &gt; annotation default).
 *
 * @since 9.5.0
 */
class RestPathsRuntimeOverride_Programmatic_Test extends TestBase {

	@Rest(paths={"/from-annotation"})
	public static class A_AnnotationDefault {}

	@Test
	void a01_programmaticPaths_overrideAnnotation() throws Exception {
		var args = new RestContext.Args(A_AnnotationDefault.class, null, null, A_AnnotationDefault::new, "", null, null, new String[]{"/from-builder", "/also-from-builder"});
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-builder", "/also-from-builder"}, ctx.getPaths(),
			"Programmatic paths override should beat the @Rest(paths=...) annotation default");
	}

	@Test
	void a02_programmaticNull_inheritAnnotation() throws Exception {
		// Args.paths == null  → no programmatic override; annotation default wins.
		var args = new RestContext.Args(A_AnnotationDefault.class, null, null, A_AnnotationDefault::new, "", null, null, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-annotation"}, ctx.getPaths(),
			"null Args.paths should fall through to the @Rest(paths=...) annotation default");
	}

	@Test
	void a03_programmaticEmptyArray_clearsAllRungs() throws Exception {
		// new String[0] is the explicit-clear sentinel — wins over the annotation, leaving no top-level mounts.
		var args = new RestContext.Args(A_AnnotationDefault.class, null, null, A_AnnotationDefault::new, "", null, null, new String[0]);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertEquals(0, ctx.getPaths().length,
			"new String[0] should explicitly clear all lower rungs and leave no top-level mounts");
	}

	@Rest
	public static class B_NoAnnotationPaths {}

	@Test
	void b01_noAnnotationPaths_emptyResolve() throws Exception {
		var args = new RestContext.Args(B_NoAnnotationPaths.class, null, null, B_NoAnnotationPaths::new, "", null, null, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertNotNull(ctx.getPaths(), "getPaths() must not return null — empty array is the empty-state contract");
		assertEquals(0, ctx.getPaths().length,
			"With no annotation paths and no programmatic override the resolved array should be empty");
	}
}
