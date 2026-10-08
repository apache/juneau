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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@code @BeanProp(required)} enforcement in the JSON parser.
 */
@SuppressWarnings("unused") // Fixture fields are read reflectively.
class MissingRequiredProperty_Json_Test extends TestBase {

	public static class A {
		@BeanProp(required=true) public String name;
		public Integer age;
		@BeanProp(required=true) public String zip;
	}

	public record B(@BeanProp(required=true) String name, int age) {
		public B {
			Objects.requireNonNull(name, "name");   // proves the check runs before the constructor
		}
	}

	public static class C {
		private final String name;
		private final int age;
		@BeanCtor(properties="name,age")
		public C(String name, int age) { this.name = Objects.requireNonNull(name); this.age = age; }
		@BeanProp(required=true) public String getName() { return name; }
		public int getAge() { return age; }
	}

	public static class D {
		private String name;
		public String getName() { return name; }
		@BeanProp(required=true) public void setName(String v) { name = v; }
	}

	public static class E { @BeanProp(required=true) public int count; }

	public static class Outer { public A inner; }

	public static class G {
		@BeanProp(required=true) public String name;
		@BeanProp(required=true) public List<String> tags;
	}

	public static class H {
		private final String name;
		private H(Builder b) { name = b.name; }
		public static Builder builder() { return new Builder(); }
		public String getName() { return name; }

		@BeanType(findFluentSetters=true)
		public static class Builder {
			private String name;
			@BeanProp(required=true) public Builder name(String v) { name = v; return this; }
			public H build() { return new H(this); }
		}
	}

	@Marshalled(typeName="T")
	public static class T1 { @BeanProp(required=true) public String name; public int age; }

	//-----------------------------------------------------------------------------------------------------------------
	// a - basic
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_missingOne() {
		var e = assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{\"name\":\"x\",\"age\":1}", A.class));
		assertTrue(e.getMessage().contains("Missing required properties on bean class 'A': [zip]"), e.getMessage());
		assertTrue(e.getMessage().contains("At: "), e.getMessage());
		assertList(e.getPropertyNames(), "zip");
		assertEquals(A.class, e.getBeanClass());
	}

	@Test void a02_missingSeveral_inPropertyOrder() {
		var e = assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{\"age\":1}", A.class));
		assertList(e.getPropertyNames(), "name", "zip");
	}

	@Test void a03_isParseException() {
		assertThrows(ParseException.class, () -> JsonParser.DEFAULT.read("{}", A.class));
	}

	@Test void a04_allPresent() throws Exception {
		assertBean(JsonParser.DEFAULT.read("{\"name\":\"x\",\"zip\":\"y\"}", A.class), "name,zip,age", "x,y,<null>");
	}

	@Test void a05_explicitNullsArePresent() throws Exception {
		assertBean(JsonParser.DEFAULT.read("{\"name\":null,\"zip\":null}", A.class), "name,zip", "<null>,<null>");
	}

	@Test void a06_emptyValuesArePresent() throws Exception {
		assertBean(JsonParser.DEFAULT.read("{\"name\":\"\",\"tags\":[]}", G.class), "name,tags", ",[]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - construction paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_record_clearMessageNotNpe() {
		var e = assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{\"age\":3}", B.class));
		assertList(e.getPropertyNames(), "name");
	}

	@Test void b02_record_present() throws Exception {
		assertBean(JsonParser.DEFAULT.read("{\"name\":\"n\",\"age\":3}", B.class), "name,age", "n,3");
	}

	@Test void b03_beanCtor() {
		var e = assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{\"age\":3}", C.class));
		assertList(e.getPropertyNames(), "name");
	}

	@Test void b04_setterAnnotation() {
		assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{}", D.class));
	}

	@Test void b05_primitive_missingFails_zeroPasses() throws Exception {
		assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{}", E.class));
		assertBean(JsonParser.DEFAULT.read("{\"count\":0}", E.class), "count", "0");
	}

	@Test void b06_builder() {
		assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{}", H.class));
	}

	@Test void b07_builder_present() throws Exception {
		assertBean(JsonParser.DEFAULT.read("{\"name\":\"n\"}", H.class), "name", "n");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - nesting, typed and lenient parsing
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_nestedBean() {
		var e = assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("{\"inner\":{\"name\":\"x\"}}", Outer.class));
		assertList(e.getPropertyNames(), "zip");
	}

	@Test void c02_nestedNullBeanIsNotChecked() throws Exception {
		assertBean(JsonParser.DEFAULT.read("{\"inner\":null}", Outer.class), "inner", "<null>");
	}

	@Test void c03_typedViaTypeProperty() {
		var p = JsonParser.create().beanDictionary(T1.class).build();
		var e = assertThrows(MissingRequiredPropertyException.class, () -> p.read("{\"_type\":\"T\",\"age\":1}", Object.class));
		assertList(e.getPropertyNames(), "name");
	}

	@Test void c04_ignoreUnknownStillChecksRequired() {
		var p = JsonParser.create().ignoreUnknownBeanProperties().build();
		assertThrows(MissingRequiredPropertyException.class, () -> p.read("{\"bogus\":1,\"name\":\"x\"}", A.class));
	}

	@Test void c05_listOfBeans() {
		assertThrows(MissingRequiredPropertyException.class, () -> JsonParser.DEFAULT.read("[{\"name\":\"x\"}]", List.class, A.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - no check when wrapping existing objects
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_toBeanMapOfExistingBean() {
		assertNotNull(MarshallingContext.DEFAULT_SESSION.toBeanMap(new A()).getBean());
	}

	@Test void d02_partialNewBeanMap() {
		var m = MarshallingContext.DEFAULT_SESSION.newBeanMap(A.class);
		m.put("age", 1);
		assertBean(m.getBean(), "age", "1");
		assertList(m.getMissingRequiredProperties(), "name", "zip");   // tracked, but nothing enforces it here
	}
}
