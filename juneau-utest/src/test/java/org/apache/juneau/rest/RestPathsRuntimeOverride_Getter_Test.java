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
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code getPaths()} virtual-getter rung of the runtime-override resolution chain on
 * {@link RestContext#getPaths()}: a {@link RestServlet#getPaths() RestServlet.getPaths()} (or
 * {@link RestResource#getPaths() RestResource.getPaths()}) override beats the annotation default; a
 * {@code null} return falls through to the annotation; an explicit {@code new String[0]} return clears.
 *
 * <p>
 * The programmatic Builder rung outranks this getter, so all tests here pass {@code null} into
 * {@code Args.paths()} to keep the highest rung neutral.
 *
 * @since 9.5.0
 */
class RestPathsRuntimeOverride_Getter_Test extends TestBase {

	@Rest(paths={"/from-annotation"})
	public static class A_GetterOverride extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[]{"/from-getter-1", "/from-getter-2"}; }
	}

	@Test
	void a01_getterPaths_overrideAnnotation() throws Exception {
		var args = new RestContext.Args(A_GetterOverride.class, null, null, A_GetterOverride::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-getter-1", "/from-getter-2"}, ctx.getPaths(),
			"getPaths() override should beat the @Rest(paths=...) annotation default");
	}

	@Rest(paths={"/from-annotation"})
	public static class B_GetterReturnsNull extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return null; }
	}

	@Test
	void b01_getterNull_inheritAnnotation() throws Exception {
		var args = new RestContext.Args(B_GetterReturnsNull.class, null, null, B_GetterReturnsNull::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-annotation"}, ctx.getPaths(),
			"null return from getPaths() should fall through to the annotation default");
	}

	@Rest(paths={"/from-annotation"})
	public static class C_GetterReturnsEmpty extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[0]; }
	}

	@Test
	void c01_getterEmptyArray_clears() throws Exception {
		var args = new RestContext.Args(C_GetterReturnsEmpty.class, null, null, C_GetterReturnsEmpty::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertEquals(0, ctx.getPaths().length,
			"new String[0] from getPaths() should explicitly clear, beating the annotation default");
	}

	@Rest(paths={"/from-annotation"})
	public static class D_ProgrammaticBeatsGetter extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[]{"/from-getter"}; }
	}

	@Test
	void d01_programmaticBeatsGetter() throws Exception {
		var args = new RestContext.Args(D_ProgrammaticBeatsGetter.class, null, null, D_ProgrammaticBeatsGetter::new, "", null, null, new String[]{"/from-builder"}, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-builder"}, ctx.getPaths(),
			"Programmatic Args.paths() should beat the getPaths() getter");
	}

	@Rest(paths={"/from-annotation"})
	public static class E_RestResourceGetter extends RestResource {
		@Override public String[] getPaths() { return new String[]{"/from-restobject-getter"}; }
	}

	@Test
	void e01_restObjectGetter_overrideAnnotation() throws Exception {
		var args = new RestContext.Args(E_RestResourceGetter.class, null, null, E_RestResourceGetter::new, "", null, null, null, false);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-restobject-getter"}, ctx.getPaths(),
			"RestResource.getPaths() override should also beat the annotation default");
	}
}
