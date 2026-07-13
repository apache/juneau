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
package org.apache.juneau.bean.jsonschema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.junit.jupiter.api.*;

class JsonSchemaBeanGenerator_Test extends TestBase {

	@Test void a01_primitives() {
		var g = JsonSchemaBeanGenerator.DEFAULT;
		assertEquals("integer", g.generate(int.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("int32", g.generate(int.class).getFormat());
		assertEquals("number", g.generate(float.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("float", g.generate(float.class).getFormat());
		assertEquals("boolean", g.generate(boolean.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("string", g.generate(String.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("uri", g.generate(URI.class).getFormat());
	}

	@Test void a02_bean_collection_map_enum() {
		var g = JsonSchemaBeanGenerator.DEFAULT;
		assertEquals("object", g.generate(SimpleBean.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertNotNull(g.generate(SimpleBean.class).getProperty("name"));
		assertEquals("array", g.generate(BeanList.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("object", g.generate(BeanList.class).getItemsAsSchema().getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals(Boolean.TRUE, g.generate(BeanSet.class).getUniqueItems());
		assertNotNull(g.generate(BeanMap.class).getAdditionalPropertiesAsSchema());
		assertEquals("string", g.generate(TinyEnum.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals(List.of("one","two"), g.generate(TinyEnum.class).getEnum());
	}

	@Test void a03_useBeanDefs_injectsDefsAtRoot() {
		var bean = JsonSchemaBeanGenerator.create().useBeanDefs().build().generate(SimpleBean.class);
		assertEquals("#/definitions/SimpleBean", bean.getRef().toString());
		assertNotNull(bean.getDefs());
		assertTrue(bean.getDefs().containsKey("SimpleBean"));
	}

	@Test void a05_descriptions_and_examples() {
		var bean = JsonSchemaBeanGenerator.create()
			.addDescriptionsTo(TypeCategory.ANY)
			.addExamplesTo(TypeCategory.ANY)
			.allowNestedDescriptions()
			.allowNestedExamples()
			.build()
			.generate(ExampleBean.class);

		assertEquals("object", bean.getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("org.apache.juneau.bean.jsonschema.JsonSchemaBeanGenerator_Test$ExampleBean", bean.getDescription());
		assertEquals("java.lang.String", bean.getProperty("value").getDescription());
	}

	@Test void a06_roundTripParityWithJsonMapOutput() throws Exception {
		var map = JsonSchemaGenerator.DEFAULT.getSession().getSchema(SimpleBean.class);
		var bean = JsonSchemaBeanGenerator.toBean(map);
		var mapJson = JsonSerializer.DEFAULT.serialize(map);
		var beanJson = JsonSerializer.DEFAULT.serialize(bean);
		assertEquals(
			JsonParser.DEFAULT.parse(mapJson, JsonMap.class),
			JsonParser.DEFAULT.parse(beanJson, JsonMap.class)
		);
	}

	@Test void a07_staticOfFactories() {
		assertEquals("object", JsonSchema.of(SimpleBean.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertNotNull(JsonSchema.of(SimpleBean.class).getProperty("name"));
		assertEquals("object", JsonSchema.of((Type)SimpleBean.class).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertNotNull(JsonSchema.of((Type)SimpleBean.class).getProperty("name"));
	}

	@Test void a08_toBeanConversion() {
		var map = new JsonMap(Json5Map.ofString("{type:'integer',format:'int32','$comment':'c',deprecated:true}"));
		var bean = JsonSchemaBeanGenerator.toBean(map);
		assertEquals("integer", bean.getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertEquals("int32", bean.getFormat());
		assertEquals("c", bean.getComment());
		assertEquals(Boolean.TRUE, bean.getDeprecated());
	}

	@Test void b01_generateObjectPathAndIgnoredTypeNull() {
		var g = JsonSchemaBeanGenerator.DEFAULT;
		assertEquals("object", g.generate(new SimpleBean()).getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
	}

	@Test void b01a_generateIgnoredTypeReturnsNull() {
		var ignored = JsonSchemaBeanGenerator.create().ignoreTypes("SimpleBean").build();
		assertNull(ignored.generate(SimpleBean.class));
		assertNull(ignored.generate(new SimpleBean()));
	}

	@Test void b01b_generateObjectWithBeanDefsAddsDefs() {
		var bean = JsonSchemaBeanGenerator.create().useBeanDefs().build().generate(new SimpleBean());
		assertNotNull(bean.getDefs());
		assertTrue(bean.getDefs().containsKey("SimpleBean"));
	}

	@Test void b01c_generateTypeWithBeanDefsButNoDefinitions() {
		var bean = JsonSchemaBeanGenerator.create().useBeanDefs().build().generate(int.class);
		assertEquals("integer", bean.getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertNull(bean.getDefs());
	}

	@Test void b01d_generateObjectWithBeanDefsButNoDefinitions() {
		var bean = JsonSchemaBeanGenerator.create().useBeanDefs().build().generate(1);
		assertEquals("integer", bean.getTypeAsJsonType().toString().toLowerCase(Locale.ROOT));
		assertNull(bean.getDefs());
	}

	@Test void b02_builderDelegates() {
		var g = JsonSchemaBeanGenerator.create()
			.addDescriptionsTo(TypeCategory.ANY)
			.addExamplesTo(TypeCategory.ANY)
			.allowNestedDescriptions()
			.allowNestedDescriptions(false)
			.allowNestedExamples()
			.allowNestedExamples(false)
			.beanDefMapper(TestBeanDefMapper.class)
			.ignoreTypes("com.example.DoesNotExist")
			.useBeanDefs()
			.useBeanDefs(false)
			.build();
		assertNotNull(g.generate(SimpleBean.class));
	}

	@Test void b03_argumentValidation() {
		assertThrows(RuntimeException.class, () -> JsonSchemaBeanGenerator.DEFAULT.generate((Type)null));
		assertThrows(RuntimeException.class, () -> JsonSchemaBeanGenerator.DEFAULT.generate((Object)null));
		assertThrows(RuntimeException.class, () -> JsonSchemaBeanGenerator.toBean(null));
	}

	@Test void b04_summary_flowsThroughBridge() {
		var bean = JsonSchemaBeanGenerator.DEFAULT.generate(SummaryBean.class);
		assertEquals("A short, AI-friendly description", bean.getSummary());
		assertEquals("The user's display name", bean.getProperty("name").getSummary());
	}

	@Schema(summary="A short, AI-friendly description")
	public static class SummaryBean {
		@Schema(summary="The user's display name")
		public String name;
	}

	public static class SimpleBean {
		public String name;
	}

	public static class ExampleBean {
		public String value;

		@Example
		public static ExampleBean example() {
			var x = new ExampleBean();
			x.value = "v";
			return x;
		}
	}

	public static class BeanList extends LinkedList<SimpleBean> {
		private static final long serialVersionUID = 1L;
	}
	public static class BeanSet extends LinkedHashSet<String> {
		private static final long serialVersionUID = 1L;
	}
	public static class BeanMap extends LinkedHashMap<String,SimpleBean> {
		private static final long serialVersionUID = 1L;
	}

	public static class TestBeanDefMapper extends BasicBeanDefMapper {}

	public enum TinyEnum {
		ONE,
		TWO;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
