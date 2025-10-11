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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.jsonschema.TypeCategory.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

class JsonSchemaGeneratorTest extends TestBase {

	//====================================================================================================
	// Simple objects
	//====================================================================================================

	@Test void simpleObjects() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();

		assertBean(s.getSchema(short.class), "type,format", "integer,int16");
		assertBean(s.getSchema(Short.class), "type,format", "integer,int16");
		assertBean(s.getSchema(int.class), "type,format", "integer,int32");
		assertBean(s.getSchema(Integer.class), "type,format", "integer,int32");
		assertBean(s.getSchema(long.class), "type,format", "integer,int64");
		assertBean(s.getSchema(Long.class), "type,format", "integer,int64");
		assertBean(s.getSchema(float.class), "type,format", "number,float");
		assertBean(s.getSchema(Float.class), "type,format", "number,float");
		assertBean(s.getSchema(double.class), "type,format", "number,double");
		assertBean(s.getSchema(Double.class), "type,format", "number,double");
		assertBean(s.getSchema(boolean.class), "type", "boolean");
		assertBean(s.getSchema(Boolean.class), "type", "boolean");
		assertBean(s.getSchema(String.class), "type", "string");
		assertBean(s.getSchema(StringBuilder.class), "type", "string");
		assertBean(s.getSchema(char.class), "type", "string");
		assertBean(s.getSchema(Character.class), "type", "string");
		assertBean(s.getSchema(TestEnumToString.class), "type,enum", "string,[one,two,three]");
		assertBean(s.getSchema(SimpleBean.class), "type,properties{f1{type}}", "object,{{string}}");
	}

	public static class SimpleBean {
		public String f1;
	}

	//====================================================================================================
	// Arrays
	//====================================================================================================

	@Test void arrays1d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();

		assertBean(s.getSchema(short[].class), "type,items{type,format}", "array,{integer,int16}");
		assertBean(s.getSchema(Short[].class), "type,items{type,format}", "array,{integer,int16}");
		assertBean(s.getSchema(int[].class), "type,items{type,format}", "array,{integer,int32}");
		assertBean(s.getSchema(Integer[].class), "type,items{type,format}", "array,{integer,int32}");
		assertBean(s.getSchema(long[].class), "type,items{type,format}", "array,{integer,int64}");
		assertBean(s.getSchema(Long[].class), "type,items{type,format}", "array,{integer,int64}");
		assertBean(s.getSchema(float[].class), "type,items{type,format}", "array,{number,float}");
		assertBean(s.getSchema(Float[].class), "type,items{type,format}", "array,{number,float}");
		assertBean(s.getSchema(double[].class), "type,items{type,format}", "array,{number,double}");
		assertBean(s.getSchema(Double[].class), "type,items{type,format}", "array,{number,double}");
		assertBean(s.getSchema(boolean[].class), "type,items{type}", "array,{boolean}");
		assertBean(s.getSchema(Boolean[].class), "type,items{type}", "array,{boolean}");
		assertBean(s.getSchema(String[].class), "type,items{type}", "array,{string}");
		assertBean(s.getSchema(StringBuilder[].class), "type,items{type}", "array,{string}");
		assertBean(s.getSchema(char[].class), "type,items{type}", "array,{string}");
		assertBean(s.getSchema(Character[].class), "type,items{type}", "array,{string}");
		assertBean(s.getSchema(TestEnumToString[].class), "type,items{type,enum}", "array,{string,[one,two,three]}");
		assertBean(s.getSchema(SimpleBean[].class), "type,items{type,properties{f1{type}}}", "array,{object,{{string}}}");
	}

	@Test void arrays2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();

		assertBean(s.getSchema(short[][].class), "type,items{type,items{type,format}}", "array,{array,{integer,int16}}");
		assertBean(s.getSchema(Short[][].class), "type,items{type,items{type,format}}", "array,{array,{integer,int16}}");
		assertBean(s.getSchema(int[][].class), "type,items{type,items{type,format}}", "array,{array,{integer,int32}}");
		assertBean(s.getSchema(Integer[][].class), "type,items{type,items{type,format}}", "array,{array,{integer,int32}}");
		assertBean(s.getSchema(long[][].class), "type,items{type,items{type,format}}", "array,{array,{integer,int64}}");
		assertBean(s.getSchema(Long[][].class), "type,items{type,items{type,format}}", "array,{array,{integer,int64}}");
		assertBean(s.getSchema(float[][].class), "type,items{type,items{type,format}}", "array,{array,{number,float}}");
		assertBean(s.getSchema(Float[][].class), "type,items{type,items{type,format}}", "array,{array,{number,float}}");
		assertBean(s.getSchema(double[][].class), "type,items{type,items{type,format}}", "array,{array,{number,double}}");
		assertBean(s.getSchema(Double[][].class), "type,items{type,items{type,format}}", "array,{array,{number,double}}");
		assertBean(s.getSchema(boolean[][].class), "type,items{type,items{type}}", "array,{array,{boolean}}");
		assertBean(s.getSchema(Boolean[][].class), "type,items{type,items{type}}", "array,{array,{boolean}}");
		assertBean(s.getSchema(String[][].class), "type,items{type,items{type}}", "array,{array,{string}}");
		assertBean(s.getSchema(StringBuilder[][].class), "type,items{type,items{type}}", "array,{array,{string}}");
		assertBean(s.getSchema(char[][].class), "type,items{type,items{type}}", "array,{array,{string}}");
		assertBean(s.getSchema(Character[][].class), "type,items{type,items{type}}", "array,{array,{string}}");
		assertBean(s.getSchema(TestEnumToString[][].class), "type,items{type,items{type,enum}}", "array,{array,{string,[one,two,three]}}");
		assertBean(s.getSchema(SimpleBean[][].class), "type,items{type,items{type,properties{f1{type}}}}", "array,{array,{object,{{string}}}}");
	}

	//====================================================================================================
	// Collections
	//====================================================================================================

	@Test void simpleList() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();
		assertBean(s.getSchema(SimpleList.class), "type,items{type,format}", "array,{integer,int32}");
	}

	@SuppressWarnings("serial")
	public static class SimpleList extends LinkedList<Integer> {}

	@Test void simpleList2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(Simple2dList.class));
	}

	@SuppressWarnings("serial")
	public static class Simple2dList extends LinkedList<LinkedList<Integer>> {}

	//====================================================================================================
	// Bean collections
	//====================================================================================================

	@Test void beanList() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@SuppressWarnings("serial")
	public static class BeanList extends LinkedList<SimpleBean> {}

	@Test void beanList2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(BeanList2d.class));
	}

	@SuppressWarnings("serial")
	public static class BeanList2d extends LinkedList<LinkedList<SimpleBean>> {}

	//====================================================================================================
	// Maps
	//====================================================================================================

	@Test void beanMap() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanMap.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMap extends LinkedHashMap<Integer,SimpleBean> {}

	@Test void beanMap2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(BeanMap2d.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMap2d extends LinkedHashMap<Integer,LinkedHashMap<Integer,SimpleBean>> {}


	//====================================================================================================
	// JSONSCHEMA_useBeanDefs
	//====================================================================================================

	@Test void useBeanDefs() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson("{'$ref':'#/definitions/SimpleBean'}", s.getSchema(SimpleBean.class));
		assertJson("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test void useBeanDefs_beanList() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.getSchema(BeanList.class));
		assertJson("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test void useBeanDefs_beanList2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(BeanList2d.class));
		assertJson("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test void useBeanDefs_beanArray2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
		assertJson("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	//====================================================================================================
	// JSONSCHEMA_useBeanDefs - preload definition.
	//====================================================================================================

	@Test void beanDefsPreloaded() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson("{'$ref':'#/definitions/SimpleBean'}", s.getSchema(SimpleBean.class));
		assertJson("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test void useBeanDefsPreloaded_beanList() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.getSchema(BeanList.class));
		assertJson("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test void useBeanDefsPreloaded_beanList2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(BeanList2d.class));
		assertJson("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test void useBeanDefsPreloaded_beanArray2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
		assertJson("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	//====================================================================================================
	// JSONSCHEMA_beanDefMapper
	//====================================================================================================

	@Test void customBeanDefMapper() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().beanDefMapper(CustomBeanDefMapper.class).build().getSession();
		assertJson("{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertJson("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	public static class CustomBeanDefMapper extends BasicBeanDefMapper {
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getFullName();
		}
	}

	@Test void customBeanDefMapper_customURI() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().beanDefMapper(CustomBeanDefMapper2.class).build().getSession();
		assertJson("{'$ref':'/foo/bar/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertJson("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	public static class CustomBeanDefMapper2 extends BasicBeanDefMapper {

		public CustomBeanDefMapper2() {
			super("/foo/bar/{0}");
		}
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getFullName();
		}
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BEAN
	//====================================================================================================

	@Test void addExample_BEAN_noBeanExample() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}}}", s.getSchema(SimpleBean.class));
	}

	@Test void addExample_BEAN_exampleMethod() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(B1.class));
	}

	@Test void addExample_BEAN_exampleMethod_wDefault() throws Exception {
		var b = new B1();
		b.f1 = "baz";
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(B1.class, b).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}", s.getSchema(B1.class));
	}

	@Test void addExample_BEAN_exampleMethod_array2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B1[][].class));
	}

	public static class B1 extends SimpleBean {

		@Example
		public static B1 example() {
			var ex = new B1();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleMethod_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(B1c.class));
	}

	@Test void addExample_BEAN_exampleMethod_wDefault_usingConfig() throws Exception {
		var b = new B1c();
		b.f1 = "baz";
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).example(B1c.class, b).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}", s.getSchema(B1c.class));
	}

	@Test void addExample_BEAN_exampleMethod_array2d_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B1c[][].class));
	}

	@Example(on="Dummy1.example")
	@Example(on="B1c.example")
	@Example(on="Dummy2.example")
	private static class B1cConfig {}

	public static class B1c extends SimpleBean {

		public static B1c example() {
			var ex = new B1c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleMethodOverridden_wDefault() throws Exception {
		var b = new B2();
		b.f1 = "baz";
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(B2.class, b).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}", s.getSchema(B2.class));
	}

	@Test void addExample_BEAN_exampleMethodOverridden_array2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B2[][].class));
	}

	public static class B2 extends B1 {

		@Example
		public static B2 example2() {
			var ex = new B2();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleMethodOverridden_wDefault_usingConfig() throws Exception {
		var b = new B2c();
		b.f1 = "baz";
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B2cConfig.class).example(B2c.class, b).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}", s.getSchema(B2c.class));
	}

	@Test void addExample_BEAN_exampleMethodOverridden_array2d_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B2cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B2c[][].class));
	}

	@Example(on="Dummy1.example2")
	@Example(on="B2c.example2")
	@Example(on="Dummy2.example2")
	private static class B2cConfig {}

	public static class B2c extends B1c {

		public static B2c example2() {
			var ex = new B2c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleField() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(B3.class));
	}

	@Test void addExample_BEAN_exampleField_array2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B3[][].class));
	}

	public static class B3 extends SimpleBean {

		@Example
		public static B3 EXAMPLE = getExample();

		private static B3 getExample() {
			var ex = new B3();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleField_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B3cConfig.class).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(B3c.class));
	}

	@Test void addExample_BEAN_exampleField_array2d_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B3cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B3c[][].class));
	}

	@Example(on="B3c.EXAMPLE")
	private static class B3cConfig {}

	public static class B3c extends SimpleBean {

		public static B3c EXAMPLE = getExample();

		private static B3c getExample() {
			var ex = new B3c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleBeanAnnotation() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(B4.class));
	}

	@Test void addExample_BEAN_exampleBeanAnnotation_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B4[][].class));
	}

	@Example("{f1:'foobar'}")
	public static class B4 extends SimpleBean {}

	@Test void addExample_BEAN_exampleBeanAnnotation_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B4cConfig.class).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(B4c.class));
	}

	@Test void addExample_BEAN_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B4cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(B4c[][].class));
	}

	@Example(on="B4c", value="{f1:'foobar'}")
	private static class B4cConfig {}

	public static class B4c extends SimpleBean {}

	@Test void addExample_BEAN_exampleBeanProperty() throws Exception {
		var b = new SimpleBean();
		b.f1 = "foobar";
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(SimpleBean.class, b).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}", s.getSchema(SimpleBean.class));
	}

	@Test void addExample_BEAN_exampleBeanProperty_2darray() throws Exception {
		var b = new SimpleBean();
		b.f1 = "foobar";
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(SimpleBean.class, b).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}", s.getSchema(SimpleBean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - MAP
	//====================================================================================================

	@Test void addExample_MAP_noExample() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanMap.class));
	}

	@Test void addExample_MAP_exampleMethod() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}", s.getSchema(C1.class));
	}

	@Test void addExample_MAP_exampleMethod_wDefault() throws Exception {
		var b = new C1();
		b.put(456, B1.example());
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(C1.class, b).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'456':{f1:'foobar'}}}", s.getSchema(C1.class));
	}

	@SuppressWarnings("serial")
	public static class C1 extends BeanMap {

		@Example
		public static C1 example() {
			var m = new C1();
			m.put(123, B1.example());
			return m;
		}
	}

	@Test void addExample_MAP_exampleMethod_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C1cConfig.class).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}", s.getSchema(C1c.class));
	}

	@Test void addExample_MAP_exampleMethod_wDefault_usingConfig() throws Exception {
		var b = new C1c();
		b.put(456, B1.example());
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C1cConfig.class).example(C1c.class, b).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'456':{f1:'foobar'}}}", s.getSchema(C1c.class));
	}

	@Example(on="C1c.example")
	private static class C1cConfig {}

	@SuppressWarnings("serial")
	public static class C1c extends BeanMap {

		public static C1c example() {
			var m = new C1c();
			m.put(123, B1.example());
			return m;
		}
	}

	@Test void addExample_MAP_exampleField() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}", s.getSchema(C2.class));
	}

	@Test void addExample_MAP_exampleField_array2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}", s.getSchema(C2[][].class));
	}

	@SuppressWarnings("serial")
	public static class C2 extends BeanMap {

		@Example
		public static C2 EXAMPLE = getExample();

		private static C2 getExample() {
			var ex = new C2();
			ex.put(123, B1.example());
			return ex;
		}
	}

	@Test void addExample_MAP_exampleField_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C2cConfig.class).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}", s.getSchema(C2c.class));
	}

	@Test void addExample_MAP_exampleField_array2d_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C2cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}", s.getSchema(C2c[][].class));
	}

	@Example(on="C2c.EXAMPLE")
	private static class C2cConfig {}

	@SuppressWarnings("serial")
	public static class C2c extends BeanMap {

		public static C2c EXAMPLE = getExample();

		private static C2c getExample() {
			var ex = new C2c();
			ex.put(123, B1.example());
			return ex;
		}
	}

	@Test void addExample_MAP_exampleBeanAnnotation() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}", s.getSchema(C3.class));
	}

	@Test void addExample_MAP_exampleBeanAnnotation_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}}}", s.getSchema(C3[][].class));
	}

	@SuppressWarnings("serial")
	@Example("{'123':{f1:'baz'}}")
	public static class C3 extends BeanMap {}

	@Test void addExample_MAP_exampleBeanAnnotation_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C3cConfig.class).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}", s.getSchema(C3c.class));
	}

	@Test void addExample_MAP_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C3cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}}}", s.getSchema(C3c[][].class));
	}

	@Example(on="C3c", value="{'123':{f1:'baz'}}")
	private static class C3cConfig {}

	@SuppressWarnings("serial")
	public static class C3c extends BeanMap {}

	@Test void addExample_MAP_exampleBeanProperty() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(BeanMap.class, C1.example()).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}", s.getSchema(BeanMap.class));
	}

	@Test void addExample_MAP_exampleBeanProperty_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(BeanMap.class, C1.example()).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}", s.getSchema(BeanMap[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - COLLECTION / ARRAY
	//====================================================================================================

	@Test void addExample_COLLECTION_noExample() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@Test void addExample_COLLECTION_exampleMethod() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}", s.getSchema(D1.class));
	}

	@Test void addExample_COLLECTION_exampleMethod_wDefault() throws Exception {
		var b = new D1();
		var sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).example(D1.class, b).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}", s.getSchema(D1.class));
	}

	@SuppressWarnings("serial")
	public static class D1 extends BeanList {

		@Example
		public static D1 example() {
			var m = new D1();
			m.add(B1.example());
			return m;
		}
	}

	@Test void addExample_COLLECTION_exampleMethod_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D1c.class).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}", s.getSchema(D1c.class));
	}

	@Test void addExample_COLLECTION_exampleMethod_wDefault_usingConfig() throws Exception {
		var b = new D1c();
		var sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D1cConfig.class).example(D1c.class, b).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}", s.getSchema(D1c.class));
	}

	@Example(on="D1c.example")
	private static class D1cConfig {}

	@SuppressWarnings("serial")
	public static class D1c extends BeanList {

		public static D1c example() {
			var m = new D1c();
			m.add(B1.example());
			return m;
		}
	}

	@Test void addExample_COLLECTION_exampleField() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}", s.getSchema(D2.class));
	}

	@Test void addExample_ARRAY_exampleField_array2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}", s.getSchema(D2[][].class));
	}

	@SuppressWarnings("serial")
	public static class D2 extends BeanList {

		@Example
		public static D2 EXAMPLE = getExample();

		private static D2 getExample() {
			var ex = new D2();
			ex.add(B1.example());
			return ex;
		}
	}

	@Test void addExample_COLLECTION_exampleField_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D2cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}", s.getSchema(D2c.class));
	}

	@Test void addExample_ARRAY_exampleField_array2d_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).applyAnnotations(D2cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}", s.getSchema(D2c[][].class));
	}

	@Example(on="D2c.EXAMPLE")
	private static class D2cConfig {}

	@SuppressWarnings("serial")
	public static class D2c extends BeanList {

		public static D2c EXAMPLE = getExample();

		private static D2c getExample() {
			var ex = new D2c();
			ex.add(B1.example());
			return ex;
		}
	}

	@Test void addExample_COLLECTION_exampleBeanAnnotation() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}", s.getSchema(D3.class));
	}

	@Test void addExample_ARRAY_exampleBeanAnnotation_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'baz'}]]]}", s.getSchema(D3[][].class));
	}

	@SuppressWarnings("serial")
	@Example("[{f1:'baz'}]")
	public static class D3 extends BeanList {}

	@Test void addExample_COLLECTION_exampleBeanAnnotation_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D3cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}", s.getSchema(D3c.class));
	}

	@Test void addExample_ARRAY_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).applyAnnotations(D3cConfig.class).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'baz'}]]]}", s.getSchema(D3c[][].class));
	}

	@Example(on="D3c", value="[{f1:'baz'}]")
	private static class D3cConfig {}

	@SuppressWarnings("serial")
	public static class D3c extends BeanList {}

	@Test void addExample_COLLECTION_exampleBeanProperty() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).example(BeanList.class, D1.example()).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}", s.getSchema(BeanList.class));
	}

	@Test void addExample_ARRAY_exampleBeanProperty_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).example(BeanList.class, D1.example()).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}", s.getSchema(BeanList[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BOOLEAN
	//====================================================================================================
	@Test void addExample_BOOLEAN() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN).build().getSession();
		assertJson("{type:'boolean',example:true}", s.getSchema(boolean.class));
		assertJson("{type:'boolean',example:true}", s.getSchema(Boolean.class));
	}

	@Test void addExample_BOOLEAN_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN)
			.example(boolean.class, false)
			.example(Boolean.class, false)
			.build().getSession();
		assertJson("{type:'boolean',example:false}", s.getSchema(boolean.class));
		assertJson("{type:'boolean',example:false}", s.getSchema(Boolean.class));
	}

	@Test void addExample_BOOLEAN_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'boolean',example:true}}}", s.getSchema(boolean[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'boolean',example:true}}}", s.getSchema(Boolean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - NUMBER
	//====================================================================================================
	@Test void addExample_NUMBER() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER).build().getSession();
		assertJson("{type:'integer',format:'int16',example:1}", s.getSchema(short.class));
		assertJson("{type:'integer',format:'int16',example:1}", s.getSchema(Short.class));
		assertJson("{type:'integer',format:'int32',example:1}", s.getSchema(int.class));
		assertJson("{type:'integer',format:'int32',example:1}", s.getSchema(Integer.class));
		assertJson("{type:'integer',format:'int64',example:1}", s.getSchema(long.class));
		assertJson("{type:'integer',format:'int64',example:1}", s.getSchema(Long.class));
		assertJson("{type:'number',format:'float',example:1.0}", s.getSchema(float.class));
		assertJson("{type:'number',format:'float',example:1.0}", s.getSchema(Float.class));
		assertJson("{type:'number',format:'double',example:1.0}", s.getSchema(double.class));
		assertJson("{type:'number',format:'double',example:1.0}", s.getSchema(Double.class));
	}

	@Test void addExample_NUMBER_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER)
			.example(short.class, (short)2)
			.example(Short.class, (short)3)
			.example(int.class, 4)
			.example(Integer.class, 5)
			.example(long.class, 6L)
			.example(Long.class, 7L)
			.example(float.class, 8f)
			.example(Float.class, 9f)
			.example(double.class, 10d)
			.example(Double.class, 11d)
			.build().getSession();
		assertJson("{type:'integer',format:'int16',example:2}", s.getSchema(short.class));
		assertJson("{type:'integer',format:'int16',example:3}", s.getSchema(Short.class));
		assertJson("{type:'integer',format:'int32',example:4}", s.getSchema(int.class));
		assertJson("{type:'integer',format:'int32',example:5}", s.getSchema(Integer.class));
		assertJson("{type:'integer',format:'int64',example:6}", s.getSchema(long.class));
		assertJson("{type:'integer',format:'int64',example:7}", s.getSchema(Long.class));
		assertJson("{type:'number',format:'float',example:8.0}", s.getSchema(float.class));
		assertJson("{type:'number',format:'float',example:9.0}", s.getSchema(Float.class));
		assertJson("{type:'number',format:'double',example:10.0}", s.getSchema(double.class));
		assertJson("{type:'number',format:'double',example:11.0}", s.getSchema(Double.class));
	}

	@Test void addExample_NUMBER_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',example:1}}}", s.getSchema(short[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',example:1}}}", s.getSchema(Short[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',example:1}}}", s.getSchema(int[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',example:1}}}", s.getSchema(Integer[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',example:1}}}", s.getSchema(long[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',example:1}}}", s.getSchema(Long[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'float',example:1.0}}}", s.getSchema(float[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'float',example:1.0}}}", s.getSchema(Float[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'double',example:1.0}}}", s.getSchema(double[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'double',example:1.0}}}", s.getSchema(Double[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - STRING
	//====================================================================================================

	@Test void addExample_STRING() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING).build().getSession();
		assertJson("{type:'string',example:'foo'}", s.getSchema(String.class));
		assertJson("{type:'string',example:'foo'}", s.getSchema(StringBuilder.class));
		assertJson("{type:'string',example:'a'}", s.getSchema(Character.class));
		assertJson("{type:'string',example:'a'}", s.getSchema(char.class));
	}

	@Test void addExample_STRING_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING)
			.example(StringBuilder.class, new StringBuilder("foo"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().getSession();
		assertJson("{type:'string',example:'foo'}", s.getSchema(StringBuilder.class));
		assertJson("{type:'string',example:'b'}", s.getSchema(Character.class));
		assertJson("{type:'string',example:'c'}", s.getSchema(char.class));
	}

	@Test void addExample_STRING_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}", s.getSchema(String[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}", s.getSchema(StringBuilder[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'a'}}}", s.getSchema(Character[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'a'}}}", s.getSchema(char[][].class));
	}

	@Test void addExample_STRING_2darray_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING)
			.example(StringBuilder.class, new StringBuilder("foo"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}", s.getSchema(StringBuilder[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'b'}}}", s.getSchema(Character[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',example:'c'}}}", s.getSchema(char[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ENUM
	//====================================================================================================

	@Test void addExample_ENUM() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).build().getSession();
		assertJson("{type:'string','enum':['one','two','three'],example:'one'}", s.getSchema(TestEnumToString.class));
	}

	@Test void addExample_ENUM_wDefault() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).example(TestEnumToString.class, TestEnumToString.TWO).build().getSession();
		assertJson("{type:'string','enum':['one','two','three'],example:'two'}", s.getSchema(TestEnumToString.class));
	}

	@Test void addExample_ENUM_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],example:'one'}}}", s.getSchema(TestEnumToString[][].class));
	}

	@Test void addExample_ENUM_useEnumNames() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).build().getSession();
		assertJson("{type:'string','enum':['ONE','TWO','THREE'],example:'ONE'}", s.getSchema(TestEnumToString.class));
	}

	@Test void addExample_ENUM_wDefault_useEnumNames() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).example(TestEnumToString.class, "'TWO'").build().getSession();
		assertJson("{type:'string','enum':['ONE','TWO','THREE'],example:'TWO'}", s.getSchema(TestEnumToString.class));
	}

	@Test void addExample_ENUM_2darray_useEnumNames() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'string','enum':['ONE','TWO','THREE'],example:'ONE'}}}", s.getSchema(TestEnumToString[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ANY
	//====================================================================================================
	@Test void addExample_ANY() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ANY).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string',example:'foo'}}}", s.getSchema(SimpleBean.class));
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}", s.getSchema(C1.class));
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}", s.getSchema(D1.class));
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}", s.getSchema(D2[][].class));
		assertJson("{type:'boolean',example:true}", s.getSchema(boolean.class));
		assertJson("{type:'integer',format:'int16',example:1}", s.getSchema(short.class));
		assertJson("{type:'integer',format:'int16',example:1}", s.getSchema(Short.class));
		assertJson("{type:'integer',format:'int32',example:1}", s.getSchema(int.class));
		assertJson("{type:'integer',format:'int32',example:1}", s.getSchema(Integer.class));
		assertJson("{type:'integer',format:'int64',example:1}", s.getSchema(long.class));
		assertJson("{type:'integer',format:'int64',example:1}", s.getSchema(Long.class));
		assertJson("{type:'number',format:'float',example:1.0}", s.getSchema(float.class));
		assertJson("{type:'number',format:'float',example:1.0}", s.getSchema(Float.class));
		assertJson("{type:'number',format:'double',example:1.0}", s.getSchema(double.class));
		assertJson("{type:'number',format:'double',example:1.0}", s.getSchema(Double.class));
		assertJson("{type:'string',example:'foo'}", s.getSchema(String.class));
		assertJson("{type:'string',example:'foo'}", s.getSchema(StringBuilder.class));
		assertJson("{type:'string',example:'a'}", s.getSchema(Character.class));
		assertJson("{type:'string',example:'a'}", s.getSchema(char.class));
		assertJson("{type:'string','enum':['one','two','three'],example:'one'}", s.getSchema(TestEnumToString.class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BEAN
	//====================================================================================================

	@Test void addDescription_BEAN() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BEAN).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
	}

	@Test void addDescription_BEAN_array2d() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - MAP
	//====================================================================================================

	@Test void addDescription_MAP() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(MAP).build().getSession();
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanMap.class));
	}

	@Test void addDescription_MAP_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(MAP).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}", s.getSchema(BeanMap[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - COLLECTION / ARRAY
	//====================================================================================================

	@Test void addDescription_COLLECTION() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(COLLECTION).build().getSession();
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	@Test void addDescription_COLLECTION_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(COLLECTION).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}", s.getSchema(BeanList[][].class));
	}

	@Test void addDescription_ARRAY() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ARRAY).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}", s.getSchema(BeanList[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BOOLEAN
	//====================================================================================================
	@Test void addDescription_BOOLEAN() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BOOLEAN).build().getSession();
		assertJson("{type:'boolean',description:'boolean'}", s.getSchema(boolean.class));
		assertJson("{type:'boolean',description:'java.lang.Boolean'}", s.getSchema(Boolean.class));
	}

	@Test void addDescription_BOOLEAN_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BOOLEAN).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'boolean',description:'boolean'}}}", s.getSchema(boolean[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'boolean',description:'java.lang.Boolean'}}}", s.getSchema(Boolean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - NUMBER
	//====================================================================================================
	@Test void addDescription_NUMBER() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(NUMBER).build().getSession();
		assertJson("{type:'integer',format:'int16',description:'short'}", s.getSchema(short.class));
		assertJson("{type:'integer',format:'int16',description:'java.lang.Short'}", s.getSchema(Short.class));
		assertJson("{type:'integer',format:'int32',description:'int'}", s.getSchema(int.class));
		assertJson("{type:'integer',format:'int32',description:'java.lang.Integer'}", s.getSchema(Integer.class));
		assertJson("{type:'integer',format:'int64',description:'long'}", s.getSchema(long.class));
		assertJson("{type:'integer',format:'int64',description:'java.lang.Long'}", s.getSchema(Long.class));
		assertJson("{type:'number',format:'float',description:'float'}", s.getSchema(float.class));
		assertJson("{type:'number',format:'float',description:'java.lang.Float'}", s.getSchema(Float.class));
		assertJson("{type:'number',format:'double',description:'double'}", s.getSchema(double.class));
		assertJson("{type:'number',format:'double',description:'java.lang.Double'}", s.getSchema(Double.class));
	}

	@Test void addDescription_NUMBER_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(NUMBER).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'short'}}}", s.getSchema(short[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'java.lang.Short'}}}", s.getSchema(Short[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'int'}}}", s.getSchema(int[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'java.lang.Integer'}}}", s.getSchema(Integer[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'long'}}}", s.getSchema(long[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'java.lang.Long'}}}", s.getSchema(Long[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'float'}}}", s.getSchema(float[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'java.lang.Float'}}}", s.getSchema(Float[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'double'}}}", s.getSchema(double[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'java.lang.Double'}}}", s.getSchema(Double[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - STRING
	//====================================================================================================

	@Test void addDescription_STRING() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(STRING).build().getSession();
		assertJson("{type:'string',description:'java.lang.String'}", s.getSchema(String.class));
		assertJson("{type:'string',description:'java.lang.StringBuilder'}", s.getSchema(StringBuilder.class));
		assertJson("{type:'string',description:'java.lang.Character'}", s.getSchema(Character.class));
		assertJson("{type:'string',description:'char'}", s.getSchema(char.class));
	}

	@Test void addDescription_STRING_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(STRING).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.String'}}}", s.getSchema(String[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.StringBuilder'}}}", s.getSchema(StringBuilder[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.Character'}}}", s.getSchema(Character[][].class));
		assertJson("{type:'array',items:{type:'array',items:{type:'string',description:'char'}}}", s.getSchema(char[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ENUM
	//====================================================================================================

	@Test void addDescription_ENUM() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ENUM).build().getSession();
		assertJson("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}", s.getSchema(TestEnumToString.class));
	}

	@Test void addDescription_ENUM_2darray() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ENUM).build().getSession();
		assertJson("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}}}", s.getSchema(TestEnumToString[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ANY
	//====================================================================================================
	@Test void addDescription_ANY() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ANY).build().getSession();
		assertJson("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertJson("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanMap.class));
		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
		assertJson("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}", s.getSchema(BeanList[][].class));
		assertJson("{type:'boolean',description:'boolean'}", s.getSchema(boolean.class));
		assertJson("{type:'boolean',description:'java.lang.Boolean'}", s.getSchema(Boolean.class));
		assertJson("{type:'integer',format:'int16',description:'short'}", s.getSchema(short.class));
		assertJson("{type:'integer',format:'int16',description:'java.lang.Short'}", s.getSchema(Short.class));
		assertJson("{type:'integer',format:'int32',description:'int'}", s.getSchema(int.class));
		assertJson("{type:'integer',format:'int32',description:'java.lang.Integer'}", s.getSchema(Integer.class));
		assertJson("{type:'integer',format:'int64',description:'long'}", s.getSchema(long.class));
		assertJson("{type:'integer',format:'int64',description:'java.lang.Long'}", s.getSchema(Long.class));
		assertJson("{type:'number',format:'float',description:'float'}", s.getSchema(float.class));
		assertJson("{type:'number',format:'float',description:'java.lang.Float'}", s.getSchema(Float.class));
		assertJson("{type:'number',format:'double',description:'double'}", s.getSchema(double.class));
		assertJson("{type:'number',format:'double',description:'java.lang.Double'}", s.getSchema(Double.class));
		assertJson("{type:'string',description:'java.lang.String'}", s.getSchema(String.class));
		assertJson("{type:'string',description:'java.lang.StringBuilder'}", s.getSchema(StringBuilder.class));
		assertJson("{type:'string',description:'java.lang.Character'}", s.getSchema(Character.class));
		assertJson("{type:'string',description:'char'}", s.getSchema(char.class));
		assertJson("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}", s.getSchema(TestEnumToString.class));
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedExamples
	//====================================================================================================

	@Test void allowNestedExamples_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.allowNestedExamples()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@Test void allowNestedExamples_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedDescriptions
	//====================================================================================================

	@Test void allowNestedDescriptions_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.allowNestedDescriptions()
			.addDescriptionsTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	@Test void allowNestedDescriptions_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.addDescriptionsTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	//====================================================================================================
	// Swaps
	//====================================================================================================

	@Test void swaps_int() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.swaps(IntSwap.class)
			.build().getSession();
		assertJson("{type:'integer',format:'int32'}", s.getSchema(SimpleBean.class));
		assertJson("{type:'array',items:{type:'integer',format:'int32'}}", s.getSchema(BeanList.class));
		assertJson("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(SimpleBean[][].class));
	}

	public static class IntSwap extends ObjectSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on class
	//====================================================================================================

	@Test void jsonSchema_onclass() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson("{description:'baz',format:'bar',type:'foo',properties:{f1:{type:'integer',format:'int32'}}}", s.getSchema(A1.class));
	}

	@Schema(type="foo",format="bar",description="baz")
	public static class A1 {
		public int f1;
	}

	@Test void jsonSchema_onclass_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A1aConfig.class).build().getSession();
		assertJson("{description:'baz',format:'bar',type:'foo',properties:{f1:{type:'integer',format:'int32'}}}", s.getSchema(A1a.class));
	}

	@Schema(on="Dummy1",type="foo",format="bar",description="baz")
	@Schema(on="A1a",type="foo",format="bar",description="baz")
	@Schema(on="Dummy2",type="foo",format="bar",description="baz")
	private static class A1aConfig {}

	public static class A1a {
		public int f1;
	}

	@Test void jsonSchema_onbeanfield() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(A2.class));
	}

	public static class A2 {
		@Schema(type="foo",format="bar",description="baz")
		public int f1;
	}

	@Test void jsonSchema_onbeanfield_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A2aConfig.class).build().getSession();
		assertJson("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(A2a.class));
	}

	@Schema(on="A2a.f1",type="foo",format="bar",description="baz")
	private static class A2aConfig {}

	public static class A2a {
		public int f1;
	}

	@Test void jsonSchema_onbeangetter() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(A3.class));
	}

	public static class A3 {
		@Schema(type="foo",format="bar",description="baz")
		public int getF1() { return 123; }
	}

	@Test void jsonSchema_onbeangetter_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A3aConfig.class).build().getSession();
		assertJson("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(A3a.class));
	}

	@Schema(on="A3a.getF1",type="foo",format="bar",description="baz")
	private static class A3aConfig {}

	public static class A3a {
		public int getF1() { return 123; }
	}

	@Test void jsonSchema_onbeansetter() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(A4.class));
	}

	public static class A4 {
		public int getF1() { return 123; }

		@Schema(type="foo",format="bar",description="baz")
		public void setF1(int v) { /* no-op */ }
	}

	@Test void jsonSchema_onbeansetter_usingConfig() throws Exception {
		var s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A4aConfig.class).build().getSession();
		assertJson("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(A4a.class));
	}

	@Schema(on="A4a.setF1",type="foo",format="bar",description="baz")
	private static class A4aConfig {}

	public static class A4a {
		public int getF1() { return 123; }

		public void setF1(int v) { /* no-op */ }
	}

	//====================================================================================================
	// @JsonSchema on ObjectSwap
	//====================================================================================================

	@Test void jsonschema_onpojoswap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.swaps(SwapWithAnnotation.class)
			.build().getSession();
		assertJson("{description:'baz',format:'bar',type:'foo'}", s.getSchema(SimpleBean.class));
		assertJson("{type:'array',items:{description:'baz',format:'bar',type:'foo'}}", s.getSchema(BeanList.class));
		assertJson("{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(SimpleBean[][].class));
	}

	@Schema(type="foo",format="bar",description="baz")
	public static class SwapWithAnnotation extends ObjectSwap<SimpleBean,Integer> {}

	@Test void jsonschema_onpojoswap_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(SwapWithAnnotation2Config.class)
			.swaps(SwapWithAnnotation2.class)
			.build().getSession();
		assertJson("{description:'baz',format:'bar',type:'foo'}", s.getSchema(SimpleBean.class));
		assertJson("{type:'array',items:{description:'baz',format:'bar',type:'foo'}}", s.getSchema(BeanList.class));
		assertJson("{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo'}}}", s.getSchema(SimpleBean[][].class));
	}

	@Schema(on="SwapWithAnnotation2", type="foo",format="bar",description="baz")
	private static class SwapWithAnnotation2Config {}

	public static class SwapWithAnnotation2 extends ObjectSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on ObjectSwap
	//====================================================================================================

	@Schema(onClass=B.class,$ref="ref")
	static class BConfig {}

	static class B {}
	static ClassInfo bConfig = ClassInfo.of(BConfig.class);

	@Test void schemaOnClass_onConfig() throws Exception {
		var al = AnnotationWorkList.of(bConfig.getAnnotationList());
		var x = JsonSchemaGenerator.create().apply(al).build().getSession();
		assertContains("$ref", r(x.getSchema(new B())));
	}

}