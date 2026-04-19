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
package org.apache.juneau.rest.annotation;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for the BeanCreator-based annotation overrides on `@Rest(...)`:
 * `debugEnablement`, `staticFiles`, and `swaggerProvider`.
 *
 * <p>
 * Each of these settings was refactored to use the {@code findXxx()} memoizer pattern that
 * walks the `@Rest` hierarchy via `getRestAnnotationsForProperty(...)` and picks the first
 * non-{@code Void} class. This test exercises the "non-Void annotation wins" branch that was
 * previously uncovered by tests.
 *
 * <p>
 * See {@code todo/TODO-16-restcontext-memoized-fields.md} (Decision #21) for the coverage
 * mandate.
 */
class Rest_BeanCreatorOverrides_Test extends TestBase {

	private static RestContext build(Class<?> resourceClass) throws Exception {
		var resource = resourceClass.getDeclaredConstructor().newInstance();
		return new RestContext(new RestContextInit(resourceClass, () -> resource));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debugEnablement=X.class)
	//------------------------------------------------------------------------------------------------------------------

	public static class CustomDebugEnablement extends BasicDebugEnablement {
		public CustomDebugEnablement(BasicBeanStore beanStore) {
			super(beanStore);
		}
	}

	@Rest(debugEnablement=CustomDebugEnablement.class)
	public static class A {}

	@Test void a01_customDebugEnablement_viaAnnotation() throws Exception {
		var rc = build(A.class);
		assertInstanceOf(CustomDebugEnablement.class, rc.getDebugEnablement(),
			"@Rest(debugEnablement=...) should select the annotated class.");
	}

	@Rest
	public static class A_Default {}

	@Test void a02_defaultDebugEnablement() throws Exception {
		var rc = build(A_Default.class);
		assertEquals(BasicDebugEnablement.class, rc.getDebugEnablement().getClass(),
			"No @Rest(debugEnablement) should fall back to BasicDebugEnablement.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(staticFiles=X.class)
	//------------------------------------------------------------------------------------------------------------------

	public static class CustomStaticFiles extends BasicStaticFiles {
		public CustomStaticFiles(BasicBeanStore beanStore) {
			super(beanStore);
		}
	}

	@Rest(staticFiles=CustomStaticFiles.class)
	public static class B {}

	@Test void b01_customStaticFiles_viaAnnotation() throws Exception {
		var rc = build(B.class);
		assertInstanceOf(CustomStaticFiles.class, rc.getStaticFiles(),
			"@Rest(staticFiles=...) should select the annotated class.");
	}

	@Rest
	public static class B_Default {}

	@Test void b02_defaultStaticFiles() throws Exception {
		var rc = build(B_Default.class);
		assertEquals(BasicStaticFiles.class, rc.getStaticFiles().getClass(),
			"No @Rest(staticFiles) should fall back to BasicStaticFiles.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(swaggerProvider=X.class)
	//------------------------------------------------------------------------------------------------------------------

	public static class CustomSwaggerProvider extends BasicSwaggerProvider {
		public CustomSwaggerProvider(BasicBeanStore beanStore) {
			super(beanStore);
		}
	}

	@Rest(swaggerProvider=CustomSwaggerProvider.class)
	public static class C {}

	@Test void c01_customSwaggerProvider_viaAnnotation() throws Exception {
		var rc = build(C.class);
		assertInstanceOf(CustomSwaggerProvider.class, rc.getSwaggerProvider(),
			"@Rest(swaggerProvider=...) should select the annotated class.");
	}

	@Rest
	public static class C_Default {}

	@Test void c02_defaultSwaggerProvider() throws Exception {
		var rc = build(C_Default.class);
		assertEquals(BasicSwaggerProvider.class, rc.getSwaggerProvider().getClass(),
			"No @Rest(swaggerProvider) should fall back to BasicSwaggerProvider.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Inheritance — child class @Rest(...) overrides parent's setting (most-derived wins).
	//------------------------------------------------------------------------------------------------------------------

	public static class CustomDebugEnablement2 extends BasicDebugEnablement {
		public CustomDebugEnablement2(BasicBeanStore beanStore) {
			super(beanStore);
		}
	}

	@Rest(debugEnablement=CustomDebugEnablement.class)
	public static class D_Parent {}

	@Rest(debugEnablement=CustomDebugEnablement2.class)
	public static class D_Child extends D_Parent {}

	@Test void d01_childAnnotationOverridesParent() throws Exception {
		var rc = build(D_Child.class);
		assertInstanceOf(CustomDebugEnablement2.class, rc.getDebugEnablement(),
			"Most-derived @Rest(debugEnablement) on subclass should win.");
	}
}
