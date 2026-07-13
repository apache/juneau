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
package org.apache.juneau;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BeanMapLoader}.
 *
 * <p>
 * Confirms that the load helpers are no-ops when given null input — a contract that became achievable
 * once {@link org.apache.juneau.marshall.collections.JsonMap#ofString(java.io.Reader)} and
 * {@link Json5Map#ofString(CharSequence)} switched to returning empty
 * instances instead of {@code null}.
 */
class BeanMapLoader_Test extends TestBase {

	public static class A {
		public String name;
		public int age;
	}

	@Test void a01_loadStringPopulatesBean() throws Exception {
		var m = MarshallingContext.DEFAULT.newBeanMap(A.class);
		BeanMapLoader.load(m, "{name:'John',age:21}");
		assertBean(m.getBean(), "name,age", "John,21");
	}

	@Test void a02_loadNullStringIsNoOp() throws Exception {
		var m = MarshallingContext.DEFAULT.newBeanMap(A.class);
		var result = BeanMapLoader.load(m, (String)null);
		assertSame(m, result);
		assertBean(m.getBean(), "name,age", "<null>,0");
	}

	@Test void a03_loadNullReaderIsNoOp() throws Exception {
		var m = MarshallingContext.DEFAULT.newBeanMap(A.class);
		var result = BeanMapLoader.load(m, null, Json5Parser.DEFAULT);
		assertSame(m, result);
		assertBean(m.getBean(), "name,age", "<null>,0");
	}
}
