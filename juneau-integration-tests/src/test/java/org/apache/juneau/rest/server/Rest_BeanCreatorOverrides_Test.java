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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.debug.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.swagger.*;
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
 */
class Rest_BeanCreatorOverrides_Test extends TestBase {

	private static RestContext build(Class<?> resourceClass) throws Exception {
		var resource = resourceClass.getDeclaredConstructor().newInstance();
		return new RestContext(new RestContext.Args(resourceClass, null, null, () -> resource, "", null, null, null, false));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(debug=@Debug(config=X.class))
	//------------------------------------------------------------------------------------------------------------------

	public static class CustomDebugConfig extends DebugConfig {
		public CustomDebugConfig(BeanStore beanStore) {
			super(beanStore);
		}
	}

	@Rest(debug=@Debug(config=CustomDebugConfig.class))
	public static class A {}

	@Test void a01_customDebugEnablement_viaAnnotation() throws Exception {
		var rc = build(A.class);
		assertNotNull(rc.getDebugConfig(),
			"@Rest(debug=@Debug(config=...)) should resolve a DebugConfig.");
	}

	@Rest
	public static class A_Default {}

	@Test void a02_defaultDebugEnablement() throws Exception {
		var rc = build(A_Default.class);
		assertNotNull(rc.getDebugConfig(),
			"No @Rest(debug=@Debug(config=...)) should still resolve a default DebugConfig.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(staticFiles=X.class)
	//------------------------------------------------------------------------------------------------------------------

	public static class CustomStaticFiles extends BasicStaticFiles {
		public CustomStaticFiles(BeanStore beanStore) {
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
		public CustomSwaggerProvider(BeanStore beanStore) {
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

	public static class CustomDebugConfig2 extends DebugConfig {
		public CustomDebugConfig2(BeanStore beanStore) {
			super(beanStore);
		}
	}

	@Rest(debug=@Debug(config=CustomDebugConfig.class))
	public static class D_Parent {}

	@Rest(debug=@Debug(config=CustomDebugConfig2.class))
	public static class D_Child extends D_Parent {}

	@Test void d01_childAnnotationOverridesParent() throws Exception {
		var rc = build(D_Child.class);
		assertNotNull(rc.getDebugConfig(),
			"Most-derived @Rest(debug=@Debug(config=...)) on subclass should resolve DebugConfig.");
	}
}
