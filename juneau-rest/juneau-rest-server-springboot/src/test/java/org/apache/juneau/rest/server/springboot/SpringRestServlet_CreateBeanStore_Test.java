// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.server.springboot;

import static org.apache.juneau.test.bct.BctAssertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests {@link SpringRestServlet#createBeanStore(Optional)} when the Spring {@code appContext} field is not injected.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Stores and contexts here are test-local; no real resources are held.
})
class SpringRestServlet_CreateBeanStore_Test extends TestBase {

	public static class SpringOnly {
		public String getOrigin() { return "spring"; }
	}

	public static class Servlet extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_fieldNotInjected_returnsStore() {
		var store = new Servlet().createBeanStore(Optional.empty());
		assertBean(store, "class", "SpringBeanStore");
		Assertions.assertNull(store.getBean(SpringOnly.class).orElse(null));
	}

	@Test void a02_fieldNotInjected_fallsBackToParent() {
		var parent = new BasicBeanStore(null);
		parent.addBean(SpringOnly.class, new SpringOnly());
		var store = new Servlet().createBeanStore(Optional.of(parent));
		assertBean(store.getBean(SpringOnly.class).orElse(null), "origin", "spring");
	}

	@Test void a03_emptyOptionalInjected_returnsStore() throws Exception {
		var servlet = new Servlet();
		var f = SpringRestServlet.class.getDeclaredField("appContext");
		f.setAccessible(true);
		f.set(servlet, Optional.empty());
		assertBean(servlet.createBeanStore(Optional.empty()), "class", "SpringBeanStore");
	}

	@Test void a04_realContext_resolvesSpringBean() throws Exception {
		try (var ctx = new AnnotationConfigApplicationContext()) {
			ctx.registerBean(SpringOnly.class);
			ctx.refresh();
			var servlet = new Servlet();
			var f = SpringRestServlet.class.getDeclaredField("appContext");
			f.setAccessible(true);
			f.set(servlet, Optional.of(ctx));
			assertBean(servlet.createBeanStore(Optional.empty()).getBean(SpringOnly.class).orElse(null), "origin", "spring");
		}
	}
}
