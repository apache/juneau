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
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests asserting that {@code @Inject} on {@code @Rest} resource fields and methods
 * is honoured during context initialization.
 *
 * <p>Three flavours of {@code @Inject} are exercised, all matched by FQN via
 * {@link JsrSupport}:
 * <ul>
 * 	<li>{@link Inject} (Juneau-owned).
 * 	<li>{@link jakarta.inject.Inject} (test classpath stub at the canonical JSR-330 FQN).
 * 	<li>{@link org.springframework.beans.factory.annotation.Autowired} (test classpath stub
 * 		at the canonical Spring FQN).
 * </ul>
 *
 * <p>Also covers a {@link PostConstruct} method on the resource, which must fire after
 * injection completes.
 */
class RestInjectAnnotation_Test extends TestBase {

	public static class Service {
		public final String tag;
		public Service(String tag) { this.tag = tag; }
	}

	private static final Service JUNEAU_SVC = new Service("juneau");
	private static final Service JAKARTA_SVC = new Service("jakarta");
	private static final Service SPRING_SVC = new Service("spring");

	//------------------------------------------------------------------------------------------------
	// Juneau @Inject + @PostConstruct.
	//------------------------------------------------------------------------------------------------

	@Rest
	public static class A_JuneauInject {
		public static Service capturedService;
		public static boolean capturedPostConstruct;
		@Bean public Service service() { return JUNEAU_SVC; }
		@Inject public Service injected;
		@PostConstruct public void init() {
			capturedService = injected;
			capturedPostConstruct = true;
		}
	}

	@Test
	void a01_juneauInject_populatesField_andRunsPostConstruct() {
		A_JuneauInject.capturedService = null;
		A_JuneauInject.capturedPostConstruct = false;
		MockRestClient.buildLax(A_JuneauInject.class);
		assertSame(JUNEAU_SVC, A_JuneauInject.capturedService, "Juneau @Inject field should be populated from bean store");
		assertTrue(A_JuneauInject.capturedPostConstruct, "@PostConstruct should run after injection");
	}

	//------------------------------------------------------------------------------------------------
	// jakarta.inject.Inject (JSR-330 path).
	//------------------------------------------------------------------------------------------------

	@Rest
	public static class B_JakartaInject {
		public static Service capturedService;
		@Bean public Service service() { return JAKARTA_SVC; }
		@jakarta.inject.Inject public Service injected;
		@PostConstruct public void init() { capturedService = injected; }
	}

	@Test
	void b01_jakartaInject_populatesField() {
		B_JakartaInject.capturedService = null;
		MockRestClient.buildLax(B_JakartaInject.class);
		assertSame(JAKARTA_SVC, B_JakartaInject.capturedService, "jakarta.inject.Inject field should be populated by FQN match");
	}

	//------------------------------------------------------------------------------------------------
	// Spring @Autowired.
	//------------------------------------------------------------------------------------------------

	@Rest
	public static class C_SpringAutowired {
		public static Service capturedService;
		@Bean public Service service() { return SPRING_SVC; }
		@org.springframework.beans.factory.annotation.Autowired public Service injected;
		@PostConstruct public void init() { capturedService = injected; }
	}

	@Test
	void c01_springAutowired_populatesField() {
		C_SpringAutowired.capturedService = null;
		MockRestClient.buildLax(C_SpringAutowired.class);
		assertSame(SPRING_SVC, C_SpringAutowired.capturedService, "Spring @Autowired field should be populated by FQN match");
	}

	//------------------------------------------------------------------------------------------------
	// @Inject on a setter method.
	//------------------------------------------------------------------------------------------------

	@Rest
	public static class D_MethodInject {
		public static Service capturedService;
		@Bean public Service service() { return JUNEAU_SVC; }
		@Inject public void setService(Service s) { capturedService = s; }
	}

	@Test
	void d01_methodInject_invokesSetter() {
		D_MethodInject.capturedService = null;
		MockRestClient.buildLax(D_MethodInject.class);
		assertSame(JUNEAU_SVC, D_MethodInject.capturedService, "@Inject method should be invoked");
	}
}
