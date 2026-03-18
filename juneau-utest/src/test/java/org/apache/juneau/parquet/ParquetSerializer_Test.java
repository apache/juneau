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
package org.apache.juneau.parquet;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.SerializeException;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ParquetSerializer}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types; explicit casts required for typed assertions
})
class ParquetSerializer_Test extends TestBase {

	public static class SimpleBean {
		public String name;
		public int age;
	}

	@Test
	void a01_singleBean() throws Exception {
		var a = new SimpleBean();
		a.name = "Alice";
		a.age = 30;
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		assertTrue(bytes.length >= 4);
		assertEquals('P', bytes[0]);
		assertEquals('A', bytes[1]);
		assertEquals('R', bytes[2]);
		assertEquals('1', bytes[3]);
	}

	@Test
	void a02_beanWithNullFields() throws Exception {
		var a = new SimpleBean();
		a.name = null;
		a.age = 0;
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class PrimitiveBean {
		public int i;
		public long l;
		public double d;
		public boolean b;
		public String s;
	}

	@Test
	void a03_primitiveTypes() throws Exception {
		var a = new PrimitiveBean();
		a.i = 42;
		a.l = 12345L;
		a.d = 3.14;
		a.b = true;
		a.s = "hello";
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	/** Bean with self-reference for cycle handling tests. */
	public static class CyclicBean {
		public String name;
		public CyclicBean child;
	}

	@Test
	void a04_cyclicBeanWithNullHandling() throws Exception {
		var a = new CyclicBean();
		a.name = "root";
		a.child = a;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.NULL).addBeanTypes().build();
		var bytes = ser.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		var parsed = (List<CyclicBean>) ParquetParser.DEFAULT.parse(bytes, List.class, CyclicBean.class);
		assertBeans(parsed, "name,child", "root,<null>");
	}

	@Test
	void a05_cyclicBeanWithThrowHandling() {
		var a = new CyclicBean();
		a.name = "root";
		a.child = a;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).addBeanTypes().build();
		var ex = assertThrows(SerializeException.class, () -> ser.serialize(a));
		assertTrue(ex.getMessage().contains("Cyclic") || ex.getMessage().contains("cyclic"), "Expected cycle-related message: " + ex.getMessage());
	}
}
