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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.*;
import org.springframework.context.*;

/**
 * Validates the Spring Boot mount-time integration of the runtime-override resolution chain by
 * exercising {@link RestContext#resolveTopLevelPaths(Class, Object, BeanStore)} the same way a
 * {@code @Configuration}-driven Spring Boot app would when registering its
 * {@code ServletRegistrationBean}: build a {@link SpringBeanStore} backed by an
 * {@link ApplicationContext}, then ask the resolver for the paths.
 *
 * <p>
 * The precedence chain &mdash; programmatic &gt; getter &gt; annotation default &mdash; runs identically
 * under microservice and Spring Boot, so this test focuses on the Spring-side parity assertions:
 * <ul>
 * 	<li>The getter rung beats the annotation default under {@link SpringBeanStore}.
 * 	<li>The annotation default resolves cleanly when there's no override (purely literal {@code paths}
 * 		elements pass through unchanged; SVL is a no-op).
 * 	<li>The resolver tolerates null {@code resource} / null {@code store} arguments &mdash; the
 * 		mount-time entry point must not NPE in those degenerate cases.
 * 	<li>A {@code getPaths()} method that returns a non-array shape (e.g. plain {@link String}) is
 * 		accepted under the permissive {@code Object}-typed contract and flows through the same
 * 		SVL + comma-split pipeline as any other leaf.
 * </ul>
 * 
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@SuppressWarnings({
	"unchecked"  // ApplicationContext.getBeanProvider(Class) is unchecked-bound in the Spring API.
})
class RestPathsRuntimeOverride_Springboot_Test extends TestBase {

	private static SpringBeanStore emptySpringBeanStore() {
		var appContext = mock(ApplicationContext.class);
		// Generic empty provider for any class lookup — keeps the bean store consistent with a Spring Boot
		// app that hasn't registered Config, VarResolver, etc. beans.
		when(appContext.getBeanProvider(any(Class.class))).thenAnswer(inv -> {
			ObjectProvider<?> empty = mock(ObjectProvider.class);
			when(empty.getIfAvailable()).thenReturn(null);
			return empty;
		});
		return new SpringBeanStore(appContext, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — getter rung beats annotation default under SpringBeanStore (parity with microservice path)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation"})
	public static class A_GetterOverride extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public String[] getPaths() { return new String[]{"/from-springboot-getter"}; }
	}

	@Test
	void a01_getterBeatsAnnotation_underSpring() {
		var store = emptySpringBeanStore();
		var paths = RestContext.resolveTopLevelPaths(A_GetterOverride.class, new A_GetterOverride(), store);

		assertArrayEquals(new String[]{"/from-springboot-getter"}, paths,
			"Under SpringBeanStore, the getter override should beat the @Rest(paths) annotation default");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — annotation default falls through cleanly when no override is present
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/from-annotation-1", "/from-annotation-2"})
	public static class B_AnnotationOnly extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void b01_annotationOnly_fallsThroughCleanly() {
		var store = emptySpringBeanStore();
		var paths = RestContext.resolveTopLevelPaths(B_AnnotationOnly.class, new B_AnnotationOnly(), store);

		assertArrayEquals(new String[]{"/from-annotation-1", "/from-annotation-2"}, paths,
			"With no override available, resolution should fall through to the @Rest(paths) annotation default");
	}

	@Test
	void b02_annotationOnly_nullStore_resolvesViaAnnotation() {
		// Null store skips the VarResolver lookup; SVL is a no-op (literal elements pass through).
		var paths = RestContext.resolveTopLevelPaths(B_AnnotationOnly.class, new B_AnnotationOnly(), null);
		assertArrayEquals(new String[]{"/from-annotation-1", "/from-annotation-2"}, paths,
			"Null bean store should still resolve via the annotation default (SVL skipped, literals pass through)");
	}

	@Test
	void b03_annotationOnly_nullResource_skipsGetterRung() {
		// Null resource skips the getter rung but the annotation default still resolves.
		var paths = RestContext.resolveTopLevelPaths(B_AnnotationOnly.class, null, null);
		assertArrayEquals(new String[]{"/from-annotation-1", "/from-annotation-2"}, paths,
			"Null resource should be safe — getter is just skipped, annotation default still resolves");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — degenerate cases that must not NPE
	//-----------------------------------------------------------------------------------------------------------------

	public static class C_NoAnnotation {}

	@Test
	void c01_noRestAnnotation_returnsEmpty() {
		// Class without @Rest annotation — none of the rungs resolve, returns empty array.
		var paths = RestContext.resolveTopLevelPaths(C_NoAnnotation.class, new C_NoAnnotation(), null);
		assertNotNull(paths);
		assertEquals(0, paths.length, "Class without @Rest annotation should yield an empty paths array");
	}

	@Rest
	public static class D_GetterReturnsString {
		// A getPaths() method whose return type is plain String — accepted by the permissive Object contract
		// and flows through the same comma-split pipeline as any other leaf.
		public String getPaths() { return "/from-string-getter"; }
	}

	@Test
	void d01_getterReturningString_acceptedAsSinglePath() {
		var paths = RestContext.resolveTopLevelPaths(D_GetterReturnsString.class, new D_GetterReturnsString(), null);
		assertArrayEquals(new String[]{"/from-string-getter"}, paths,
			"A getPaths() method returning a single String should mount that String as a single path under the Object-typed contract");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — BeanStore parity: getter override resolves identically through SpringBeanStore as it would
	//     through a plain BasicBeanStore (acceptance bullet from the original work item 73 plan).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_springBeanStore_parityWith_plainBeanStore() {
		var resource = new A_GetterOverride();
		var springStore = emptySpringBeanStore();
		var plainStore = new BasicBeanStore();

		var underSpring = RestContext.resolveTopLevelPaths(A_GetterOverride.class, resource, springStore);
		var underPlain = RestContext.resolveTopLevelPaths(A_GetterOverride.class, resource, plainStore);

		assertArrayEquals(underPlain, underSpring,
			"Mixin/resource override behavior must be identical when registered via Juneau BeanStore (microservice path) and via SpringBeanStore (Spring Boot path)");
	}
}
