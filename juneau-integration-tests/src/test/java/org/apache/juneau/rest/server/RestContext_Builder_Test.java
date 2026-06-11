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

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.client.classic.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.config.*;
import org.junit.jupiter.api.*;

class RestContext_Builder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// beanStore
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {}

	@Rest
	public static class A1 {
		@Bean static WritableBeanStore beanStore;
	}

	@Test void a01_createBeanStore_default() {
		MockRestClient.buildLax(A1.class);
		assertEquals("BasicBeanStore", cns(A1.beanStore));
	}

	@Rest
	public static class A4 {
		@Bean static WritableBeanStore beanStore;

		@Bean WritableBeanStore beanStore() {
			return new BasicBeanStore(null).addBean(A.class, new A());
		}
	}

	@Test void a04_createBeanStore_restBean2() {
		MockRestClient.buildLax(A4.class);
		assertNotNull(A4.beanStore.getBean(A.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Bean on fields.
	//-----------------------------------------------------------------------------------------------------------------

	public static class B {
		public int id;

		public B(int id) {
			this.id = id;
		}
	}

	@Rest
	public static class B1a implements BasicJsonConfig {
		@Bean static B b1 = new B(1);
		@Bean(name="b2") B b2 = new B(2);

		@Bean static B b3;
		@Bean(name="b2") B b4;

		@RestGet("/a1") public B a1(B b) { return b; }
		@RestGet("/a2") public B a2(@org.apache.juneau.commons.inject.Named("b2") B b) { return b; }
		@RestGet("/a3") public B a3() { return b3; }
		@RestGet("/a4") public B a4() { return b4; }
	}

	@Rest
	public static class B1b extends B1a {
		@RestGet("/a5") public B a5(B b) { return b; }
		@RestGet("/a6") public B a6(@org.apache.juneau.commons.inject.Named("b2") B b) { return b; }
		@RestGet("/a7") public B a7() { return b3; }
		@RestGet("/a8") public B a8() { return b4; }
	}

	static RestClient b1b = MockRestClient.createLax(B1b.class).json().build();

	@Test void b01_RestBean_fields() throws Exception {
		b1b.get("/a1").run().assertContent("{\"id\":1}");
		b1b.get("/a2").run().assertContent("{\"id\":2}");
		b1b.get("/a3").run().assertContent("{\"id\":1}");
		b1b.get("/a4").run().assertContent("{\"id\":2}");
		b1b.get("/a5").run().assertContent("{\"id\":1}");
		b1b.get("/a6").run().assertContent("{\"id\":2}");
		b1b.get("/a7").run().assertContent("{\"id\":1}");
		b1b.get("/a8").run().assertContent("{\"id\":2}");
	}
}