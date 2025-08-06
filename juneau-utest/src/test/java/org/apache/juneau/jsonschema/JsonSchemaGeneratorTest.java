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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.jsonschema.TypeCategory.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

public class JsonSchemaGeneratorTest extends SimpleTestBase {

	//====================================================================================================
	// Simple objects
	//====================================================================================================

	@Test void simpleObjects() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();

		assertJson(s.getSchema(short.class), "{type:'integer',format:'int16'}");
		assertJson(s.getSchema(Short.class), "{type:'integer',format:'int16'}");
		assertJson(s.getSchema(int.class), "{type:'integer',format:'int32'}");
		assertJson(s.getSchema(Integer.class), "{type:'integer',format:'int32'}");
		assertJson(s.getSchema(long.class), "{type:'integer',format:'int64'}");
		assertJson(s.getSchema(Long.class), "{type:'integer',format:'int64'}");
		assertJson(s.getSchema(float.class), "{type:'number',format:'float'}");
		assertJson(s.getSchema(Float.class), "{type:'number',format:'float'}");
		assertJson(s.getSchema(double.class), "{type:'number',format:'double'}");
		assertJson(s.getSchema(Double.class), "{type:'number',format:'double'}");
		assertJson(s.getSchema(boolean.class), "{type:'boolean'}");
		assertJson(s.getSchema(Boolean.class), "{type:'boolean'}");
		assertJson(s.getSchema(String.class), "{type:'string'}");
		assertJson(s.getSchema(StringBuilder.class), "{type:'string'}");
		assertJson(s.getSchema(char.class), "{type:'string'}");
		assertJson(s.getSchema(Character.class), "{type:'string'}");
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['one','two','three']}");
		assertJson(s.getSchema(SimpleBean.class), "{type:'object',properties:{f1:{type:'string'}}}");
	}

	public static class SimpleBean {
		public String f1;
	}

	//====================================================================================================
	// Arrays
	//====================================================================================================

	@Test void arrays1d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();

		assertJson(s.getSchema(short[].class), "{type:'array',items:{type:'integer',format:'int16'}}");
		assertJson(s.getSchema(Short[].class), "{type:'array',items:{type:'integer',format:'int16'}}");
		assertJson(s.getSchema(int[].class), "{type:'array',items:{type:'integer',format:'int32'}}");
		assertJson(s.getSchema(Integer[].class), "{type:'array',items:{type:'integer',format:'int32'}}");
		assertJson(s.getSchema(long[].class), "{type:'array',items:{type:'integer',format:'int64'}}");
		assertJson(s.getSchema(Long[].class), "{type:'array',items:{type:'integer',format:'int64'}}");
		assertJson(s.getSchema(float[].class), "{type:'array',items:{type:'number',format:'float'}}");
		assertJson(s.getSchema(Float[].class), "{type:'array',items:{type:'number',format:'float'}}");
		assertJson(s.getSchema(double[].class), "{type:'array',items:{type:'number',format:'double'}}");
		assertJson(s.getSchema(Double[].class), "{type:'array',items:{type:'number',format:'double'}}");
		assertJson(s.getSchema(boolean[].class), "{type:'array',items:{type:'boolean'}}");
		assertJson(s.getSchema(Boolean[].class), "{type:'array',items:{type:'boolean'}}");
		assertJson(s.getSchema(String[].class), "{type:'array',items:{type:'string'}}");
		assertJson(s.getSchema(StringBuilder[].class), "{type:'array',items:{type:'string'}}");
		assertJson(s.getSchema(char[].class), "{type:'array',items:{type:'string'}}");
		assertJson(s.getSchema(Character[].class), "{type:'array',items:{type:'string'}}");
		assertJson(s.getSchema(TestEnumToString[].class), "{type:'array',items:{type:'string','enum':['one','two','three']}}");
		assertJson(s.getSchema(SimpleBean[].class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void arrays2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();

		assertJson(s.getSchema(short[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}");
		assertJson(s.getSchema(Short[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}");
		assertJson(s.getSchema(int[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
		assertJson(s.getSchema(Integer[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
		assertJson(s.getSchema(long[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}");
		assertJson(s.getSchema(Long[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}");
		assertJson(s.getSchema(float[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}");
		assertJson(s.getSchema(Float[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}");
		assertJson(s.getSchema(double[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}");
		assertJson(s.getSchema(Double[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}");
		assertJson(s.getSchema(boolean[][].class), "{type:'array',items:{type:'array',items:{type:'boolean'}}}");
		assertJson(s.getSchema(Boolean[][].class), "{type:'array',items:{type:'array',items:{type:'boolean'}}}");
		assertJson(s.getSchema(String[][].class), "{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertJson(s.getSchema(StringBuilder[][].class), "{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertJson(s.getSchema(char[][].class), "{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertJson(s.getSchema(Character[][].class), "{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertJson(s.getSchema(TestEnumToString[][].class), "{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three']}}}");
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	//====================================================================================================
	// Collections
	//====================================================================================================

	@Test void simpleList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson(s.getSchema(SimpleList.class), "{type:'array',items:{type:'integer',format:'int32'}}");
	}

	@SuppressWarnings("serial")
	public static class SimpleList extends LinkedList<Integer> {}

	@Test void simpleList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson(s.getSchema(Simple2dList.class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
	}

	@SuppressWarnings("serial")
	public static class Simple2dList extends LinkedList<LinkedList<Integer>> {}

	//====================================================================================================
	// Bean collections
	//====================================================================================================

	@Test void beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanList extends LinkedList<SimpleBean> {}

	@Test void beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson(s.getSchema(BeanList2d.class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanList2d extends LinkedList<LinkedList<SimpleBean>> {}

	//====================================================================================================
	// Maps
	//====================================================================================================

	@Test void beanMap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson(s.getSchema(BeanMap.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanMap extends LinkedHashMap<Integer,SimpleBean> {}

	@Test void beanMap2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertJson(s.getSchema(BeanMap2d.class), "{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanMap2d extends LinkedHashMap<Integer,LinkedHashMap<Integer,SimpleBean>> {}


	//====================================================================================================
	// JSONSCHEMA_useBeanDefs
	//====================================================================================================

	@Test void useBeanDefs() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{'$ref':'#/definitions/SimpleBean'}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void useBeanDefs_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void useBeanDefs_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson(s.getSchema(BeanList2d.class), "{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void useBeanDefs_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_useBeanDefs - preload definition.
	//====================================================================================================

	@Test void beanDefsPreloaded() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson(s.getSchema(SimpleBean.class), "{'$ref':'#/definitions/SimpleBean'}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{test:123}}");
	}

	@Test void useBeanDefsPreloaded_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{test:123}}");
	}

	@Test void useBeanDefsPreloaded_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson(s.getSchema(BeanList2d.class), "{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{test:123}}");
	}

	@Test void useBeanDefsPreloaded_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new JsonMap().append("test", 123));
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertJson(s.getBeanDefs(), "{SimpleBean:{test:123}}");
	}

	//====================================================================================================
	// JSONSCHEMA_beanDefMapper
	//====================================================================================================

	@Test void customBeanDefMapper() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().beanDefMapper(CustomBeanDefMapper.class).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertJson(s.getBeanDefs(), "{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
	}

	public static class CustomBeanDefMapper extends BasicBeanDefMapper {
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getFullName();
		}
	}

	@Test void customBeanDefMapper_customURI() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().beanDefMapper(CustomBeanDefMapper2.class).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{'$ref':'/foo/bar/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertJson(s.getBeanDefs(), "{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{type:'object',properties:{f1:{type:'string'}}}");
	}

	@Test void addExample_BEAN_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B1.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleMethod_wDefault() throws Exception {
		B1 b = new B1();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(B1.class, b).build().getSession();
		assertJson(s.getSchema(B1.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test void addExample_BEAN_exampleMethod_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B1[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	public static class B1 extends SimpleBean {

		@Example
		public static B1 example() {
			B1 ex = new B1();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleMethod_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).build().getSession();
		assertJson(s.getSchema(B1c.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleMethod_wDefault_usingConfig() throws Exception {
		B1c b = new B1c();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).example(B1c.class, b).build().getSession();
		assertJson(s.getSchema(B1c.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test void addExample_BEAN_exampleMethod_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).build().getSession();
		assertJson(s.getSchema(B1c[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example(on="Dummy1.example")
	@Example(on="B1c.example")
	@Example(on="Dummy2.example")
	private static class B1cConfig {}

	public static class B1c extends SimpleBean {

		public static B1c example() {
			B1c ex = new B1c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleMethodOverridden_wDefault() throws Exception {
		B2 b = new B2();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(B2.class, b).build().getSession();
		assertJson(s.getSchema(B2.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test void addExample_BEAN_exampleMethodOverridden_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B2[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	public static class B2 extends B1 {

		@Example
		public static B2 example2() {
			B2 ex = new B2();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleMethodOverridden_wDefault_usingConfig() throws Exception {
		B2c b = new B2c();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B2cConfig.class).example(B2c.class, b).build().getSession();
		assertJson(s.getSchema(B2c.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test void addExample_BEAN_exampleMethodOverridden_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B2cConfig.class).build().getSession();
		assertJson(s.getSchema(B2c[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example(on="Dummy1.example2")
	@Example(on="B2c.example2")
	@Example(on="Dummy2.example2")
	private static class B2cConfig {}

	public static class B2c extends B1c {

		public static B2c example2() {
			B2c ex = new B2c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B3.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B3[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	public static class B3 extends SimpleBean {

		@Example
		public static B3 EXAMPLE = getExample();

		private static B3 getExample() {
			B3 ex = new B3();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleField_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B3cConfig.class).build().getSession();
		assertJson(s.getSchema(B3c.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B3cConfig.class).build().getSession();
		assertJson(s.getSchema(B3c[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example(on="B3c.EXAMPLE")
	private static class B3cConfig {}

	public static class B3c extends SimpleBean {

		public static B3c EXAMPLE = getExample();

		private static B3c getExample() {
			B3c ex = new B3c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test void addExample_BEAN_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B4.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertJson(s.getSchema(B4[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example("{f1:'foobar'}")
	public static class B4 extends SimpleBean {}

	@Test void addExample_BEAN_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B4cConfig.class).build().getSession();
		assertJson(s.getSchema(B4c.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B4cConfig.class).build().getSession();
		assertJson(s.getSchema(B4c[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example(on="B4c", value="{f1:'foobar'}")
	private static class B4cConfig {}

	public static class B4c extends SimpleBean {}

	@Test void addExample_BEAN_exampleBeanProperty() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(SimpleBean.class, b).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test void addExample_BEAN_exampleBeanProperty_2darray() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(SimpleBean.class, b).build().getSession();
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - MAP
	//====================================================================================================

	@Test void addExample_MAP_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson(s.getSchema(BeanMap.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void addExample_MAP_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson(s.getSchema(C1.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test void addExample_MAP_exampleMethod_wDefault() throws Exception {
		C1 b = new C1();
		b.put(456, B1.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(C1.class, b).build().getSession();
		assertJson(s.getSchema(C1.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'456':{f1:'foobar'}}}");
	}

	@SuppressWarnings("serial")
	public static class C1 extends BeanMap {

		@Example
		public static C1 example() {
			C1 m = new C1();
			m.put(123, B1.example());
			return m;
		}
	}

	@Test void addExample_MAP_exampleMethod_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C1cConfig.class).build().getSession();
		assertJson(s.getSchema(C1c.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test void addExample_MAP_exampleMethod_wDefault_usingConfig() throws Exception {
		C1c b = new C1c();
		b.put(456, B1.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C1cConfig.class).example(C1c.class, b).build().getSession();
		assertJson(s.getSchema(C1c.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'456':{f1:'foobar'}}}");
	}

	@Example(on="C1c.example")
	private static class C1cConfig {}

	@SuppressWarnings("serial")
	public static class C1c extends BeanMap {

		public static C1c example() {
			C1c m = new C1c();
			m.put(123, B1.example());
			return m;
		}
	}

	@Test void addExample_MAP_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson(s.getSchema(C2.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test void addExample_MAP_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson(s.getSchema(C2[][].class), "{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class C2 extends BeanMap {

		@Example
		public static C2 EXAMPLE = getExample();

		private static C2 getExample() {
			C2 ex = new C2();
			ex.put(123, B1.example());
			return ex;
		}
	}

	@Test void addExample_MAP_exampleField_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C2cConfig.class).build().getSession();
		assertJson(s.getSchema(C2c.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test void addExample_MAP_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C2cConfig.class).build().getSession();
		assertJson(s.getSchema(C2c[][].class), "{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}");
	}

	@Example(on="C2c.EXAMPLE")
	private static class C2cConfig {}

	@SuppressWarnings("serial")
	public static class C2c extends BeanMap {

		public static C2c EXAMPLE = getExample();

		private static C2c getExample() {
			C2c ex = new C2c();
			ex.put(123, B1.example());
			return ex;
		}
	}

	@Test void addExample_MAP_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson(s.getSchema(C3.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}");
	}

	@Test void addExample_MAP_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertJson(s.getSchema(C3[][].class), "{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}}}");
	}

	@SuppressWarnings("serial")
	@Example("{'123':{f1:'baz'}}")
	public static class C3 extends BeanMap {}

	@Test void addExample_MAP_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C3cConfig.class).build().getSession();
		assertJson(s.getSchema(C3c.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}");
	}

	@Test void addExample_MAP_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C3cConfig.class).build().getSession();
		assertJson(s.getSchema(C3c[][].class), "{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}}}");
	}

	@Example(on="C3c", value="{'123':{f1:'baz'}}")
	private static class C3cConfig {}

	@SuppressWarnings("serial")
	public static class C3c extends BeanMap {}

	@Test void addExample_MAP_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(BeanMap.class, C1.example()).build().getSession();
		assertJson(s.getSchema(BeanMap.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test void addExample_MAP_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(BeanMap.class, C1.example()).build().getSession();
		assertJson(s.getSchema(BeanMap[][].class), "{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - COLLECTION / ARRAY
	//====================================================================================================

	@Test void addExample_COLLECTION_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void addExample_COLLECTION_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson(s.getSchema(D1.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test void addExample_COLLECTION_exampleMethod_wDefault() throws Exception {
		D1 b = new D1();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).example(D1.class, b).build().getSession();
		assertJson(s.getSchema(D1.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
	}

	@SuppressWarnings("serial")
	public static class D1 extends BeanList {

		@Example
		public static D1 example() {
			D1 m = new D1();
			m.add(B1.example());
			return m;
		}
	}

	@Test void addExample_COLLECTION_exampleMethod_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D1c.class).build().getSession();
		assertJson(s.getSchema(D1c.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test void addExample_COLLECTION_exampleMethod_wDefault_usingConfig() throws Exception {
		D1c b = new D1c();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D1cConfig.class).example(D1c.class, b).build().getSession();
		assertJson(s.getSchema(D1c.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
	}

	@Example(on="D1c.example")
	private static class D1cConfig {}

	@SuppressWarnings("serial")
	public static class D1c extends BeanList {

		public static D1c example() {
			D1c m = new D1c();
			m.add(B1.example());
			return m;
		}
	}

	@Test void addExample_COLLECTION_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson(s.getSchema(D2.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test void addExample_ARRAY_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).build().getSession();
		assertJson(s.getSchema(D2[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
	}

	@SuppressWarnings("serial")
	public static class D2 extends BeanList {

		@Example
		public static D2 EXAMPLE = getExample();

		private static D2 getExample() {
			D2 ex = new D2();
			ex.add(B1.example());
			return ex;
		}
	}

	@Test void addExample_COLLECTION_exampleField_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D2cConfig.class).build().getSession();
		assertJson(s.getSchema(D2c.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test void addExample_ARRAY_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).applyAnnotations(D2cConfig.class).build().getSession();
		assertJson(s.getSchema(D2c[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
	}

	@Example(on="D2c.EXAMPLE")
	private static class D2cConfig {}

	@SuppressWarnings("serial")
	public static class D2c extends BeanList {

		public static D2c EXAMPLE = getExample();

		private static D2c getExample() {
			D2c ex = new D2c();
			ex.add(B1.example());
			return ex;
		}
	}

	@Test void addExample_COLLECTION_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertJson(s.getSchema(D3.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
	}

	@Test void addExample_ARRAY_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).build().getSession();
		assertJson(s.getSchema(D3[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'baz'}]]]}");
	}

	@SuppressWarnings("serial")
	@Example("[{f1:'baz'}]")
	public static class D3 extends BeanList {}

	@Test void addExample_COLLECTION_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D3cConfig.class).build().getSession();
		assertJson(s.getSchema(D3c.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
	}

	@Test void addExample_ARRAY_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).applyAnnotations(D3cConfig.class).build().getSession();
		assertJson(s.getSchema(D3c[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'baz'}]]]}");
	}

	@Example(on="D3c", value="[{f1:'baz'}]")
	private static class D3cConfig {}

	@SuppressWarnings("serial")
	public static class D3c extends BeanList {}

	@Test void addExample_COLLECTION_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s =JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).example(BeanList.class, D1.example()).build().getSession();
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test void addExample_ARRAY_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).example(BeanList.class, D1.example()).build().getSession();
		assertJson(s.getSchema(BeanList[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BOOLEAN
	//====================================================================================================
	@Test void addExample_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN).build().getSession();
		assertJson(s.getSchema(boolean.class), "{type:'boolean',example:true}");
		assertJson(s.getSchema(Boolean.class), "{type:'boolean',example:true}");
	}

	@Test void addExample_BOOLEAN_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN)
			.example(boolean.class, false)
			.example(Boolean.class, false)
			.build().getSession();
		assertJson(s.getSchema(boolean.class), "{type:'boolean',example:false}");
		assertJson(s.getSchema(Boolean.class), "{type:'boolean',example:false}");
	}

	@Test void addExample_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN).build().getSession();
		assertJson(s.getSchema(boolean[][].class), "{type:'array',items:{type:'array',items:{type:'boolean',example:true}}}");
		assertJson(s.getSchema(Boolean[][].class), "{type:'array',items:{type:'array',items:{type:'boolean',example:true}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - NUMBER
	//====================================================================================================
	@Test void addExample_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER).build().getSession();
		assertJson(s.getSchema(short.class), "{type:'integer',format:'int16',example:1}");
		assertJson(s.getSchema(Short.class), "{type:'integer',format:'int16',example:1}");
		assertJson(s.getSchema(int.class), "{type:'integer',format:'int32',example:1}");
		assertJson(s.getSchema(Integer.class), "{type:'integer',format:'int32',example:1}");
		assertJson(s.getSchema(long.class), "{type:'integer',format:'int64',example:1}");
		assertJson(s.getSchema(Long.class), "{type:'integer',format:'int64',example:1}");
		assertJson(s.getSchema(float.class), "{type:'number',format:'float',example:1.0}");
		assertJson(s.getSchema(Float.class), "{type:'number',format:'float',example:1.0}");
		assertJson(s.getSchema(double.class), "{type:'number',format:'double',example:1.0}");
		assertJson(s.getSchema(Double.class), "{type:'number',format:'double',example:1.0}");
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
		assertJson(s.getSchema(short.class), "{type:'integer',format:'int16',example:2}");
		assertJson(s.getSchema(Short.class), "{type:'integer',format:'int16',example:3}");
		assertJson(s.getSchema(int.class), "{type:'integer',format:'int32',example:4}");
		assertJson(s.getSchema(Integer.class), "{type:'integer',format:'int32',example:5}");
		assertJson(s.getSchema(long.class), "{type:'integer',format:'int64',example:6}");
		assertJson(s.getSchema(Long.class), "{type:'integer',format:'int64',example:7}");
		assertJson(s.getSchema(float.class), "{type:'number',format:'float',example:8.0}");
		assertJson(s.getSchema(Float.class), "{type:'number',format:'float',example:9.0}");
		assertJson(s.getSchema(double.class), "{type:'number',format:'double',example:10.0}");
		assertJson(s.getSchema(Double.class), "{type:'number',format:'double',example:11.0}");
	}

	@Test void addExample_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER).build().getSession();
		assertJson(s.getSchema(short[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int16',example:1}}}");
		assertJson(s.getSchema(Short[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int16',example:1}}}");
		assertJson(s.getSchema(int[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32',example:1}}}");
		assertJson(s.getSchema(Integer[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32',example:1}}}");
		assertJson(s.getSchema(long[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int64',example:1}}}");
		assertJson(s.getSchema(Long[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int64',example:1}}}");
		assertJson(s.getSchema(float[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'float',example:1.0}}}");
		assertJson(s.getSchema(Float[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'float',example:1.0}}}");
		assertJson(s.getSchema(double[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'double',example:1.0}}}");
		assertJson(s.getSchema(Double[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'double',example:1.0}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - STRING
	//====================================================================================================

	@Test void addExample_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING).build().getSession();
		assertJson(s.getSchema(String.class), "{type:'string',example:'foo'}");
		assertJson(s.getSchema(StringBuilder.class), "{type:'string',example:'foo'}");
		assertJson(s.getSchema(Character.class), "{type:'string',example:'a'}");
		assertJson(s.getSchema(char.class), "{type:'string',example:'a'}");
	}

	@Test void addExample_STRING_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING)
			.example(StringBuilder.class, new StringBuilder("foo"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().getSession();
		assertJson(s.getSchema(StringBuilder.class), "{type:'string',example:'foo'}");
		assertJson(s.getSchema(Character.class), "{type:'string',example:'b'}");
		assertJson(s.getSchema(char.class), "{type:'string',example:'c'}");
	}

	@Test void addExample_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING).build().getSession();
		assertJson(s.getSchema(String[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}");
		assertJson(s.getSchema(StringBuilder[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}");
		assertJson(s.getSchema(Character[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'a'}}}");
		assertJson(s.getSchema(char[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'a'}}}");
	}

	@Test void addExample_STRING_2darray_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING)
			.example(StringBuilder.class, new StringBuilder("foo"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().getSession();
		assertJson(s.getSchema(StringBuilder[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}");
		assertJson(s.getSchema(Character[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'b'}}}");
		assertJson(s.getSchema(char[][].class), "{type:'array',items:{type:'array',items:{type:'string',example:'c'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ENUM
	//====================================================================================================

	@Test void addExample_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).build().getSession();
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['one','two','three'],example:'one'}");
	}

	@Test void addExample_ENUM_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).example(TestEnumToString.class, TestEnumToString.TWO).build().getSession();
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['one','two','three'],example:'two'}");
	}

	@Test void addExample_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).build().getSession();
		assertJson(s.getSchema(TestEnumToString[][].class), "{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],example:'one'}}}");
	}

	@Test void addExample_ENUM_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).build().getSession();
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['ONE','TWO','THREE'],example:'ONE'}");
	}

	@Test void addExample_ENUM_wDefault_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).example(TestEnumToString.class, "'TWO'").build().getSession();
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['ONE','TWO','THREE'],example:'TWO'}");
	}

	@Test void addExample_ENUM_2darray_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).build().getSession();
		assertJson(s.getSchema(TestEnumToString[][].class), "{type:'array',items:{type:'array',items:{type:'string','enum':['ONE','TWO','THREE'],example:'ONE'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ANY
	//====================================================================================================
	@Test void addExample_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ANY).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{type:'object',properties:{f1:{type:'string',example:'foo'}}}");
		assertJson(s.getSchema(C1.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
		assertJson(s.getSchema(D1.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
		assertJson(s.getSchema(D2[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
		assertJson(s.getSchema(boolean.class), "{type:'boolean',example:true}");
		assertJson(s.getSchema(short.class), "{type:'integer',format:'int16',example:1}");
		assertJson(s.getSchema(Short.class), "{type:'integer',format:'int16',example:1}");
		assertJson(s.getSchema(int.class), "{type:'integer',format:'int32',example:1}");
		assertJson(s.getSchema(Integer.class), "{type:'integer',format:'int32',example:1}");
		assertJson(s.getSchema(long.class), "{type:'integer',format:'int64',example:1}");
		assertJson(s.getSchema(Long.class), "{type:'integer',format:'int64',example:1}");
		assertJson(s.getSchema(float.class), "{type:'number',format:'float',example:1.0}");
		assertJson(s.getSchema(Float.class), "{type:'number',format:'float',example:1.0}");
		assertJson(s.getSchema(double.class), "{type:'number',format:'double',example:1.0}");
		assertJson(s.getSchema(Double.class), "{type:'number',format:'double',example:1.0}");
		assertJson(s.getSchema(String.class), "{type:'string',example:'foo'}");
		assertJson(s.getSchema(StringBuilder.class), "{type:'string',example:'foo'}");
		assertJson(s.getSchema(Character.class), "{type:'string',example:'a'}");
		assertJson(s.getSchema(char.class), "{type:'string',example:'a'}");
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['one','two','three'],example:'one'}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BEAN
	//====================================================================================================

	@Test void addDescription_BEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BEAN).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
	}

	@Test void addDescription_BEAN_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BEAN).build().getSession();
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - MAP
	//====================================================================================================

	@Test void addDescription_MAP() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(MAP).build().getSession();
		assertJson(s.getSchema(BeanMap.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test void addDescription_MAP_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(MAP).build().getSession();
		assertJson(s.getSchema(BeanMap[][].class), "{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - COLLECTION / ARRAY
	//====================================================================================================

	@Test void addDescription_COLLECTION() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(COLLECTION).build().getSession();
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test void addDescription_COLLECTION_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(COLLECTION).build().getSession();
		assertJson(s.getSchema(BeanList[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}");
	}

	@Test void addDescription_ARRAY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ARRAY).build().getSession();
		assertJson(s.getSchema(BeanList[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BOOLEAN
	//====================================================================================================
	@Test void addDescription_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BOOLEAN).build().getSession();
		assertJson(s.getSchema(boolean.class), "{type:'boolean',description:'boolean'}");
		assertJson(s.getSchema(Boolean.class), "{type:'boolean',description:'java.lang.Boolean'}");
	}

	@Test void addDescription_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BOOLEAN).build().getSession();
		assertJson(s.getSchema(boolean[][].class), "{type:'array',items:{type:'array',items:{type:'boolean',description:'boolean'}}}");
		assertJson(s.getSchema(Boolean[][].class), "{type:'array',items:{type:'array',items:{type:'boolean',description:'java.lang.Boolean'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - NUMBER
	//====================================================================================================
	@Test void addDescription_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(NUMBER).build().getSession();
		assertJson(s.getSchema(short.class), "{type:'integer',format:'int16',description:'short'}");
		assertJson(s.getSchema(Short.class), "{type:'integer',format:'int16',description:'java.lang.Short'}");
		assertJson(s.getSchema(int.class), "{type:'integer',format:'int32',description:'int'}");
		assertJson(s.getSchema(Integer.class), "{type:'integer',format:'int32',description:'java.lang.Integer'}");
		assertJson(s.getSchema(long.class), "{type:'integer',format:'int64',description:'long'}");
		assertJson(s.getSchema(Long.class), "{type:'integer',format:'int64',description:'java.lang.Long'}");
		assertJson(s.getSchema(float.class), "{type:'number',format:'float',description:'float'}");
		assertJson(s.getSchema(Float.class), "{type:'number',format:'float',description:'java.lang.Float'}");
		assertJson(s.getSchema(double.class), "{type:'number',format:'double',description:'double'}");
		assertJson(s.getSchema(Double.class), "{type:'number',format:'double',description:'java.lang.Double'}");
	}

	@Test void addDescription_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(NUMBER).build().getSession();
		assertJson(s.getSchema(short[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'short'}}}");
		assertJson(s.getSchema(Short[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'java.lang.Short'}}}");
		assertJson(s.getSchema(int[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'int'}}}");
		assertJson(s.getSchema(Integer[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'java.lang.Integer'}}}");
		assertJson(s.getSchema(long[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'long'}}}");
		assertJson(s.getSchema(Long[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'java.lang.Long'}}}");
		assertJson(s.getSchema(float[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'float'}}}");
		assertJson(s.getSchema(Float[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'java.lang.Float'}}}");
		assertJson(s.getSchema(double[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'double'}}}");
		assertJson(s.getSchema(Double[][].class), "{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'java.lang.Double'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - STRING
	//====================================================================================================

	@Test void addDescription_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(STRING).build().getSession();
		assertJson(s.getSchema(String.class), "{type:'string',description:'java.lang.String'}");
		assertJson(s.getSchema(StringBuilder.class), "{type:'string',description:'java.lang.StringBuilder'}");
		assertJson(s.getSchema(Character.class), "{type:'string',description:'java.lang.Character'}");
		assertJson(s.getSchema(char.class), "{type:'string',description:'char'}");
	}

	@Test void addDescription_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(STRING).build().getSession();
		assertJson(s.getSchema(String[][].class), "{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.String'}}}");
		assertJson(s.getSchema(StringBuilder[][].class), "{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.StringBuilder'}}}");
		assertJson(s.getSchema(Character[][].class), "{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.Character'}}}");
		assertJson(s.getSchema(char[][].class), "{type:'array',items:{type:'array',items:{type:'string',description:'char'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ENUM
	//====================================================================================================

	@Test void addDescription_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ENUM).build().getSession();
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}");
	}

	@Test void addDescription_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ENUM).build().getSession();
		assertJson(s.getSchema(TestEnumToString[][].class), "{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ANY
	//====================================================================================================
	@Test void addDescription_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ANY).build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertJson(s.getSchema(BeanMap.class), "{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
		assertJson(s.getSchema(BeanList[][].class), "{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}");
		assertJson(s.getSchema(boolean.class), "{type:'boolean',description:'boolean'}");
		assertJson(s.getSchema(Boolean.class), "{type:'boolean',description:'java.lang.Boolean'}");
		assertJson(s.getSchema(short.class), "{type:'integer',format:'int16',description:'short'}");
		assertJson(s.getSchema(Short.class), "{type:'integer',format:'int16',description:'java.lang.Short'}");
		assertJson(s.getSchema(int.class), "{type:'integer',format:'int32',description:'int'}");
		assertJson(s.getSchema(Integer.class), "{type:'integer',format:'int32',description:'java.lang.Integer'}");
		assertJson(s.getSchema(long.class), "{type:'integer',format:'int64',description:'long'}");
		assertJson(s.getSchema(Long.class), "{type:'integer',format:'int64',description:'java.lang.Long'}");
		assertJson(s.getSchema(float.class), "{type:'number',format:'float',description:'float'}");
		assertJson(s.getSchema(Float.class), "{type:'number',format:'float',description:'java.lang.Float'}");
		assertJson(s.getSchema(double.class), "{type:'number',format:'double',description:'double'}");
		assertJson(s.getSchema(Double.class), "{type:'number',format:'double',description:'java.lang.Double'}");
		assertJson(s.getSchema(String.class), "{type:'string',description:'java.lang.String'}");
		assertJson(s.getSchema(StringBuilder.class), "{type:'string',description:'java.lang.StringBuilder'}");
		assertJson(s.getSchema(Character.class), "{type:'string',description:'java.lang.Character'}");
		assertJson(s.getSchema(char.class), "{type:'string',description:'char'}");
		assertJson(s.getSchema(TestEnumToString.class), "{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}");
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

		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test void allowNestedExamples_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedDescriptions
	//====================================================================================================

	@Test void allowNestedDescriptions_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.allowNestedDescriptions()
			.addDescriptionsTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test void allowNestedDescriptions_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.addDescriptionsTo(COLLECTION,BEAN)
			.build().getSession();

		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	//====================================================================================================
	// Swaps
	//====================================================================================================

	@Test void swaps_int() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.swaps(IntSwap.class)
			.build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{type:'integer',format:'int32'}");
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{type:'integer',format:'int32'}}");
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
	}

	public static class IntSwap extends ObjectSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on class
	//====================================================================================================

	@Test void jsonSchema_onclass() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson(s.getSchema(A1.class), "{description:'baz',format:'bar',type:'foo',properties:{f1:{type:'integer',format:'int32'}}}");
	}

	@Schema(type="foo",format="bar",description="baz")
	public static class A1 {
		public int f1;
	}

	@Test void jsonSchema_onclass_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A1aConfig.class).build().getSession();
		assertJson(s.getSchema(A1a.class), "{description:'baz',format:'bar',type:'foo',properties:{f1:{type:'integer',format:'int32'}}}");
	}

	@Schema(on="Dummy1",type="foo",format="bar",description="baz")
	@Schema(on="A1a",type="foo",format="bar",description="baz")
	@Schema(on="Dummy2",type="foo",format="bar",description="baz")
	private static class A1aConfig {}

	public static class A1a {
		public int f1;
	}

	@Test void jsonSchema_onbeanfield() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson(s.getSchema(A2.class), "{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}");
	}

	public static class A2 {
		@Schema(type="foo",format="bar",description="baz")
		public int f1;
	}

	@Test void jsonSchema_onbeanfield_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A2aConfig.class).build().getSession();
		assertJson(s.getSchema(A2a.class), "{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}");
	}

	@Schema(on="A2a.f1",type="foo",format="bar",description="baz")
	private static class A2aConfig {}

	public static class A2a {
		public int f1;
	}

	@Test void jsonSchema_onbeangetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson(s.getSchema(A3.class), "{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}");
	}

	public static class A3 {
		@Schema(type="foo",format="bar",description="baz")
		public int getF1() {
			return 123;
		}
	}

	@Test void jsonSchema_onbeangetter_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A3aConfig.class).build().getSession();
		assertJson(s.getSchema(A3a.class), "{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}");
	}

	@Schema(on="A3a.getF1",type="foo",format="bar",description="baz")
	private static class A3aConfig {}

	public static class A3a {
		public int getF1() {
			return 123;
		}
	}

	@Test void jsonSchema_onbeansetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertJson(s.getSchema(A4.class), "{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}");
	}

	public static class A4 {
		public int getF1() {
			return 123;
		}

		@Schema(type="foo",format="bar",description="baz")
		public void setF1(int f1) { /* no-op */ }
	}

	@Test void jsonSchema_onbeansetter_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A4aConfig.class).build().getSession();
		assertJson(s.getSchema(A4a.class), "{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo'}}}");
	}

	@Schema(on="A4a.setF1",type="foo",format="bar",description="baz")
	private static class A4aConfig {}

	public static class A4a {
		public int getF1() {
			return 123;
		}

		public void setF1(int f1) { /* no-op */ }
	}

	//====================================================================================================
	// @JsonSchema on ObjectSwap
	//====================================================================================================

	@Test void jsonschema_onpojoswap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.swaps(SwapWithAnnotation.class)
			.build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{description:'baz',format:'bar',type:'foo'}");
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{description:'baz',format:'bar',type:'foo'}}");
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo'}}}");
	}

	@Schema(type="foo",format="bar",description="baz")
	public static class SwapWithAnnotation extends ObjectSwap<SimpleBean,Integer> {}

	@Test void jsonschema_onpojoswap_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(SwapWithAnnotation2Config.class)
			.swaps(SwapWithAnnotation2.class)
			.build().getSession();
		assertJson(s.getSchema(SimpleBean.class), "{description:'baz',format:'bar',type:'foo'}");
		assertJson(s.getSchema(BeanList.class), "{type:'array',items:{description:'baz',format:'bar',type:'foo'}}");
		assertJson(s.getSchema(SimpleBean[][].class), "{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo'}}}");
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
		AnnotationWorkList al = AnnotationWorkList.of(bConfig.getAnnotationList());
		JsonSchemaGeneratorSession x = JsonSchemaGenerator.create().apply(al).build().getSession();
		assertObject(x.getSchema(new B())).asJson().isContains("'$ref':'ref'");
	}

}