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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ParquetParser}.
 */
class ParquetParser_Test extends TestBase {

	@Test
	void a01_parseSimpleBean() throws Exception {
		var a = new ParquetSerializer_Test.SimpleBean();
		a.name = "Alice";
		a.age = 30;
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		var parsed = ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.SimpleBean.class);
		assertBeans(parsed, "name,age", "Alice,30");
	}

	@Test
	void a02_parseCollectionOfBeans() throws Exception {
		var a = new ParquetSerializer_Test.SimpleBean();
		a.name = "a";
		a.age = 1;
		var b = new ParquetSerializer_Test.SimpleBean();
		b.name = "b";
		b.age = 2;
		var list = new ArrayList<ParquetSerializer_Test.SimpleBean>();
		list.add(a);
		list.add(b);
		var bytes = ParquetSerializer.DEFAULT.serialize(list);
		var parsed = ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.SimpleBean.class);
		assertBeans(parsed, "name,age", "a,1", "b,2");
	}

	@Test
	void a03_parseEmptyCollection() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var parsed = ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.SimpleBean.class);
		assertBeans(parsed, "name,age");
	}

	@Test
	void a03b_parseScalarString() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize("foobar");
		var parsed = ParquetParser.DEFAULT.parse(bytes, String.class);
		assertEquals("foobar", parsed);
	}

	@Test
	void a04_parsePrimitiveTypes() throws Exception {
		var a = new ParquetSerializer_Test.PrimitiveBean();
		a.i = 42;
		a.l = 12345L;
		a.d = 3.14;
		a.b = true;
		a.s = "hello";
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		var parsed = ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.PrimitiveBean.class);
		assertBeans(parsed, "i,l,d,b,s", "42,12345,3.14,true,hello");
	}
}
