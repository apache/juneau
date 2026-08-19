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
package org.apache.juneau.rest.server.view.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link FreemarkerServlet} and {@link FreemarkerResource}: pins that both flavors' no-arg
 * constructors build a default {@link FreemarkerDispatcher} worker and that {@code dispatcher()} exposes it
 * (both are {@code protected}, accessible here via same-package test access).
 *
 * @since 10.0.0
 */
class FreemarkerServlet_Test extends TestBase {

	@Test void a01_servlet_noArgCtor_buildsDefaultDispatcher() {
		var servlet = new FreemarkerServlet();
		var dispatcher = servlet.dispatcher();
		assertInstanceOf(FreemarkerDispatcher.class, dispatcher);
		assertEquals(FreemarkerDispatcher.DEFAULT_BASE_PATH, ((FreemarkerDispatcher) dispatcher).getBasePath());
	}

	@Test void a02_servlet_workerCtor_usesGivenWorker() {
		var worker = FreemarkerDispatcher.create().basePath("/views/").build();
		var servlet = new FreemarkerServlet(worker) {
			private static final long serialVersionUID = 1L;
		};
		assertSame(worker, servlet.dispatcher());
	}

	@Test void b01_resource_noArgCtor_buildsDefaultDispatcher() {
		var resource = new FreemarkerResource();
		var dispatcher = resource.dispatcher();
		assertInstanceOf(FreemarkerDispatcher.class, dispatcher);
		assertEquals(FreemarkerDispatcher.DEFAULT_BASE_PATH, ((FreemarkerDispatcher) dispatcher).getBasePath());
	}

	@Test void b02_resource_workerCtor_usesGivenWorker() {
		var worker = FreemarkerDispatcher.create().basePath("/views/").build();
		var resource = new FreemarkerResource(worker) {};
		assertSame(worker, resource.dispatcher());
	}
}
