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
package org.apache.juneau.rest.server.view.thymeleaf;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ThymeleafServlet} and {@link ThymeleafResource}: pins that both flavors' no-arg
 * constructors build a default {@link ThymeleafDispatcher} worker and that {@code dispatcher()} exposes it
 * (both are {@code protected}, accessible here via same-package test access).
 *
 * @since 10.0.0
 */
class ThymeleafServlet_Test extends TestBase {

	@Test void a01_servlet_noArgCtor_buildsDefaultDispatcher() {
		var servlet = new ThymeleafServlet();
		var dispatcher = servlet.dispatcher();
		assertInstanceOf(ThymeleafDispatcher.class, dispatcher);
		assertEquals(ThymeleafDispatcher.DEFAULT_BASE_PATH, ((ThymeleafDispatcher) dispatcher).getBasePath());
	}

	@Test void a02_servlet_workerCtor_usesGivenWorker() {
		var worker = ThymeleafDispatcher.create().basePath("/views/").build();
		var servlet = new ThymeleafServlet(worker) {
			private static final long serialVersionUID = 1L;
		};
		assertSame(worker, servlet.dispatcher());
	}

	@Test void b01_resource_noArgCtor_buildsDefaultDispatcher() {
		var resource = new ThymeleafResource();
		var dispatcher = resource.dispatcher();
		assertInstanceOf(ThymeleafDispatcher.class, dispatcher);
		assertEquals(ThymeleafDispatcher.DEFAULT_BASE_PATH, ((ThymeleafDispatcher) dispatcher).getBasePath());
	}

	@Test void b02_resource_workerCtor_usesGivenWorker() {
		var worker = ThymeleafDispatcher.create().basePath("/views/").build();
		var resource = new ThymeleafResource(worker) {};
		assertSame(worker, resource.dispatcher());
	}
}
