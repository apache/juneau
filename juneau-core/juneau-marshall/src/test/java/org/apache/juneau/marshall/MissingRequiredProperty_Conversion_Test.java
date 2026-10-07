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
package org.apache.juneau.marshall;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@code @BeanProp(required)} enforcement in Map-to-bean conversion (WORK-J0585).
 */
class MissingRequiredProperty_Conversion_Test extends TestBase {

	public static class A {
		@BeanProp(required=true) public String name;
		public int age;
	}

	@Marshalled(typeName="TA")
	public static class TA {
		@BeanProp(required=true) public String name;
	}

	public static class Holder { public List<A> items; }

	@Test void a01_convertToType_missing() {
		var e = assertThrows(MissingRequiredPropertyException.class,
			() -> MarshallingContext.DEFAULT_SESSION.convertToType(JsonMap.of("age", 1), A.class));
		assertList(e.getPropertyNames(), "name");
		assertFalse(e.getMessage().contains("At: "), e.getMessage());   // no parser session, so no position
	}

	@Test void a02_convertToType_present() {
		assertBean(MarshallingContext.DEFAULT_SESSION.convertToType(JsonMap.of("name", "n", "age", 1), A.class), "name,age", "n,1");
	}

	@Test void a03_convertToType_explicitNull() {
		assertBean(MarshallingContext.DEFAULT_SESSION.convertToType(new JsonMap().append("name", null), A.class), "name", "<null>");
	}

	@Test void a04_marshalledMapCast_typed() {
		var ctx = MarshallingContext.create().beanDictionary(TA.class).build();
		var m = new JsonMap(ctx.getSession()).append("_type", "TA");
		assertThrows(MissingRequiredPropertyException.class, () -> m.cast(TA.class));
	}

	@Test void a05_hjsonParse_goesThroughConversion() {
		assertThrows(MissingRequiredPropertyException.class, () -> HjsonParser.DEFAULT.read("{age: 1}", A.class));
	}

	@Test void a06_nestedConversion_notWrapped() {
		var in = JsonMap.of("items", List.of(JsonMap.of("age", 1)));
		assertThrows(MissingRequiredPropertyException.class, () -> MarshallingContext.DEFAULT_SESSION.convertToType(in, Holder.class));
	}
}
