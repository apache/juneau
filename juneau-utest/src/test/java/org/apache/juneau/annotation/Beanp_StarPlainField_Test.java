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
package org.apache.juneau.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.Test;

/**
 * {@link Beanp @Beanp} with name/value {@code "*"} on non-{@link Map} fields exposes a normal property named after the
 * field; {@code Map} fields keep the dyna property {@code *}.
 */
class Beanp_StarPlainField_Test extends TestBase {

	/** {@code *} via shorthand {@link Beanp#value()}. */
	public static class A {
		@Beanp("*")
		public String taggedValue;
	}

	/** {@code *} via {@link Beanp#name()}. */
	public static class B {
		@Beanp(name = "*")
		public int count;
	}

	/** Dynamic {@code Map} field must remain the dyna property {@code *}. */
	public static class C {
		@Beanp("*")
		public Map<String, Object> extra = new LinkedHashMap<>();
	}

	@Test
	void a01_beanpStarOnStringField_isPropertyNamedAfterField() {
		var cm = BeanContext.DEFAULT.getClassMeta(A.class);
		assertTrue(cm.isBean(), cm.getNotABeanReason());
		var bm = cm.getBeanMeta();
		assertNotNull(bm);
		var props = bm.getProperties();
		assertTrue(props.containsKey("taggedValue"), () -> "keys=" + props.keySet());
		assertFalse(props.get("taggedValue").isDyna());
		var a = new A();
		a.taggedValue = "x";
		assertEquals("x", jsonRoundTrip(a, A.class).taggedValue);
	}

	@Test
	void a02_beanpNameStarOnPrimitiveField_isPropertyNamedAfterField() {
		var cm = BeanContext.DEFAULT.getClassMeta(B.class);
		assertTrue(cm.isBean(), cm.getNotABeanReason());
		var bm = cm.getBeanMeta();
		assertNotNull(bm);
		var props = bm.getProperties();
		assertTrue(props.containsKey("count"), () -> "keys=" + props.keySet());
		assertFalse(props.get("count").isDyna());
		var b = new B();
		b.count = 7;
		assertEquals(7, jsonRoundTrip(b, B.class).count);
	}

	@Test
	void b01_beanpStarOnMapField_staysDynaProperty() {
		var cm = BeanContext.DEFAULT.getClassMeta(C.class);
		assertTrue(cm.isBean(), cm.getNotABeanReason());
		var bm = cm.getBeanMeta();
		assertNotNull(bm);
		var props = bm.getProperties();
		assertTrue(props.containsKey("*"), () -> "keys=" + props.keySet());
		assertTrue(props.get("*").isDyna());
	}
}
