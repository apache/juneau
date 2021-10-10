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
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.jsonschema.TypeCategory.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.transform.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class JsonSchemaGeneratorTest {

	//====================================================================================================
	// Simple objects
	//====================================================================================================

	@Test
	public void simpleObjects() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();

		assertObject(s.getSchema(short.class)).asJson().is("{type:'integer',format:'int16'}");
		assertObject(s.getSchema(Short.class)).asJson().is("{type:'integer',format:'int16'}");
		assertObject(s.getSchema(int.class)).asJson().is("{type:'integer',format:'int32'}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{type:'integer',format:'int32'}");
		assertObject(s.getSchema(long.class)).asJson().is("{type:'integer',format:'int64'}");
		assertObject(s.getSchema(Long.class)).asJson().is("{type:'integer',format:'int64'}");
		assertObject(s.getSchema(float.class)).asJson().is("{type:'number',format:'float'}");
		assertObject(s.getSchema(Float.class)).asJson().is("{type:'number',format:'float'}");
		assertObject(s.getSchema(double.class)).asJson().is("{type:'number',format:'double'}");
		assertObject(s.getSchema(Double.class)).asJson().is("{type:'number',format:'double'}");
		assertObject(s.getSchema(boolean.class)).asJson().is("{type:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).asJson().is("{type:'boolean'}");
		assertObject(s.getSchema(String.class)).asJson().is("{type:'string'}");
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{type:'string'}");
		assertObject(s.getSchema(char.class)).asJson().is("{type:'string'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{type:'string'}");
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['one','two','three']}");
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}}}");
	}

	public static class SimpleBean {
		public String f1;
	}

	//====================================================================================================
	// Arrays
	//====================================================================================================

	@Test
	public void arrays1d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();

		assertObject(s.getSchema(short[].class)).asJson().is("{type:'array',items:{type:'integer',format:'int16'}}");
		assertObject(s.getSchema(Short[].class)).asJson().is("{type:'array',items:{type:'integer',format:'int16'}}");
		assertObject(s.getSchema(int[].class)).asJson().is("{type:'array',items:{type:'integer',format:'int32'}}");
		assertObject(s.getSchema(Integer[].class)).asJson().is("{type:'array',items:{type:'integer',format:'int32'}}");
		assertObject(s.getSchema(long[].class)).asJson().is("{type:'array',items:{type:'integer',format:'int64'}}");
		assertObject(s.getSchema(Long[].class)).asJson().is("{type:'array',items:{type:'integer',format:'int64'}}");
		assertObject(s.getSchema(float[].class)).asJson().is("{type:'array',items:{type:'number',format:'float'}}");
		assertObject(s.getSchema(Float[].class)).asJson().is("{type:'array',items:{type:'number',format:'float'}}");
		assertObject(s.getSchema(double[].class)).asJson().is("{type:'array',items:{type:'number',format:'double'}}");
		assertObject(s.getSchema(Double[].class)).asJson().is("{type:'array',items:{type:'number',format:'double'}}");
		assertObject(s.getSchema(boolean[].class)).asJson().is("{type:'array',items:{type:'boolean'}}");
		assertObject(s.getSchema(Boolean[].class)).asJson().is("{type:'array',items:{type:'boolean'}}");
		assertObject(s.getSchema(String[].class)).asJson().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(StringBuilder[].class)).asJson().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(char[].class)).asJson().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(Character[].class)).asJson().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(TestEnumToString[].class)).asJson().is("{type:'array',items:{type:'string','enum':['one','two','three']}}");
		assertObject(s.getSchema(SimpleBean[].class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void arrays2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();

		assertObject(s.getSchema(short[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}");
		assertObject(s.getSchema(Short[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}");
		assertObject(s.getSchema(int[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
		assertObject(s.getSchema(Integer[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
		assertObject(s.getSchema(long[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}");
		assertObject(s.getSchema(Long[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}");
		assertObject(s.getSchema(float[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}");
		assertObject(s.getSchema(Float[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}");
		assertObject(s.getSchema(double[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}");
		assertObject(s.getSchema(Double[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}");
		assertObject(s.getSchema(boolean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'boolean'}}}");
		assertObject(s.getSchema(Boolean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'boolean'}}}");
		assertObject(s.getSchema(String[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(char[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(Character[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(TestEnumToString[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three']}}}");
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	//====================================================================================================
	// Collections
	//====================================================================================================

	@Test
	public void simpleList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertObject(s.getSchema(SimpleList.class)).asJson().is("{type:'array',items:{type:'integer',format:'int32'}}");
	}

	@SuppressWarnings("serial")
	public static class SimpleList extends LinkedList<Integer> {}

	@Test
	public void simpleList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertObject(s.getSchema(Simple2dList.class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
	}

	@SuppressWarnings("serial")
	public static class Simple2dList extends LinkedList<LinkedList<Integer>> {}

	//====================================================================================================
	// Bean collections
	//====================================================================================================

	@Test
	public void beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanList extends LinkedList<SimpleBean> {}

	@Test
	public void beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertObject(s.getSchema(BeanList2d.class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanList2d extends LinkedList<LinkedList<SimpleBean>> {}

	//====================================================================================================
	// Maps
	//====================================================================================================

	@Test
	public void beanMap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertObject(s.getSchema(BeanMap.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanMap extends LinkedHashMap<Integer,SimpleBean> {}

	@Test
	public void beanMap2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.getSession();
		assertObject(s.getSchema(BeanMap2d.class)).asJson().is("{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanMap2d extends LinkedHashMap<Integer,LinkedHashMap<Integer,SimpleBean>> {}


	//====================================================================================================
	// JSONSCHEMA_useBeanDefs
	//====================================================================================================

	@Test
	public void useBeanDefs() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{'$ref':'#/definitions/SimpleBean'}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void useBeanDefs_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void useBeanDefs_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertObject(s.getSchema(BeanList2d.class)).asJson().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void useBeanDefs_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_useBeanDefs - preload definition.
	//====================================================================================================

	@Test
	public void beanDefsPreloaded() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{'$ref':'#/definitions/SimpleBean'}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{test:123}}");
	}

	@Test
	public void useBeanDefsPreloaded_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{test:123}}");
	}

	@Test
	public void useBeanDefsPreloaded_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(BeanList2d.class)).asJson().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{test:123}}");
	}

	@Test
	public void useBeanDefsPreloaded_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().build().getSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).asJson().is("{SimpleBean:{test:123}}");
	}

	//====================================================================================================
	// JSONSCHEMA_beanDefMapper
	//====================================================================================================

	@Test
	public void customBeanDefMapper() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().beanDefMapper(CustomBeanDefMapper.class).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getBeanDefs()).asJson().is("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
	}

	public static class CustomBeanDefMapper extends BasicBeanDefMapper {
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getFullName();
		}
	}

	@Test
	public void customBeanDefMapper_customURI() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useBeanDefs().beanDefMapper(CustomBeanDefMapper2.class).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{'$ref':'/foo/bar/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getBeanDefs()).asJson().is("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
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

	@Test
	public void addExample_BEAN_noBeanExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B1.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_wDefault() throws Exception {
		B1 b = new B1();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(B1.class, b).build().getSession();
		assertObject(s.getSchema(B1.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B1[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	public static class B1 extends SimpleBean {

		@Example
		public static B1 example() {
			B1 ex = new B1();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test
	public void addExample_BEAN_exampleMethod_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).build().getSession();
		assertObject(s.getSchema(B1c.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_wDefault_usingConfig() throws Exception {
		B1c b = new B1c();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).example(B1c.class, b).build().getSession();
		assertObject(s.getSchema(B1c.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B1cConfig.class).build().getSession();
		assertObject(s.getSchema(B1c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
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

	@Test
	public void addExample_BEAN_exampleMethodOverridden_wDefault() throws Exception {
		B2 b = new B2();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(B2.class, b).build().getSession();
		assertObject(s.getSchema(B2.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B2[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	public static class B2 extends B1 {

		@Example
		public static B2 example2() {
			B2 ex = new B2();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_wDefault_usingConfig() throws Exception {
		B2c b = new B2c();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B2cConfig.class).example(B2c.class, b).build().getSession();
		assertObject(s.getSchema(B2c.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B2cConfig.class).build().getSession();
		assertObject(s.getSchema(B2c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
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

	@Test
	public void addExample_BEAN_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B3.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B3[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
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

	@Test
	public void addExample_BEAN_exampleField_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B3cConfig.class).build().getSession();
		assertObject(s.getSchema(B3c.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B3cConfig.class).build().getSession();
		assertObject(s.getSchema(B3c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
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

	@Test
	public void addExample_BEAN_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B4.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).build().getSession();
		assertObject(s.getSchema(B4[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example("{f1:'foobar'}")
	public static class B4 extends SimpleBean {}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B4cConfig.class).build().getSession();
		assertObject(s.getSchema(B4c.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).applyAnnotations(B4cConfig.class).build().getSession();
		assertObject(s.getSchema(B4c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	@Example(on="B4c", value="{f1:'foobar'}")
	private static class B4cConfig {}

	public static class B4c extends SimpleBean {}

	@Test
	public void addExample_BEAN_exampleBeanProperty() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(SimpleBean.class, b).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleBeanProperty_2darray() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BEAN).example(SimpleBean.class, b).build().getSession();
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},example:{f1:'foobar'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - MAP
	//====================================================================================================

	@Test
	public void addExample_MAP_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertObject(s.getSchema(BeanMap.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void addExample_MAP_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertObject(s.getSchema(C1.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleMethod_wDefault() throws Exception {
		C1 b = new C1();
		b.put(456, B1.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(C1.class, b).build().getSession();
		assertObject(s.getSchema(C1.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'456':{f1:'foobar'}}}");
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

	@Test
	public void addExample_MAP_exampleMethod_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C1cConfig.class).build().getSession();
		assertObject(s.getSchema(C1c.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleMethod_wDefault_usingConfig() throws Exception {
		C1c b = new C1c();
		b.put(456, B1.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C1cConfig.class).example(C1c.class, b).build().getSession();
		assertObject(s.getSchema(C1c.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'456':{f1:'foobar'}}}");
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

	@Test
	public void addExample_MAP_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertObject(s.getSchema(C2.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertObject(s.getSchema(C2[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}");
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

	@Test
	public void addExample_MAP_exampleField_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C2cConfig.class).build().getSession();
		assertObject(s.getSchema(C2c.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C2cConfig.class).build().getSession();
		assertObject(s.getSchema(C2c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}");
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

	@Test
	public void addExample_MAP_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertObject(s.getSchema(C3.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}");
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).build().getSession();
		assertObject(s.getSchema(C3[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}}}");
	}

	@SuppressWarnings("serial")
	@Example("{'123':{f1:'baz'}}")
	public static class C3 extends BeanMap {}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C3cConfig.class).build().getSession();
		assertObject(s.getSchema(C3c.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}");
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).applyAnnotations(C3cConfig.class).build().getSession();
		assertObject(s.getSchema(C3c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'baz'}}}}}");
	}

	@Example(on="C3c", value="{'123':{f1:'baz'}}")
	private static class C3cConfig {}

	@SuppressWarnings("serial")
	public static class C3c extends BeanMap {}

	@Test
	public void addExample_MAP_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(BeanMap.class, C1.example()).build().getSession();
		assertObject(s.getSchema(BeanMap.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(MAP).example(BeanMap.class, C1.example()).build().getSession();
		assertObject(s.getSchema(BeanMap[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - COLLECTION / ARRAY
	//====================================================================================================

	@Test
	public void addExample_COLLECTION_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void addExample_COLLECTION_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertObject(s.getSchema(D1.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_COLLECTION_exampleMethod_wDefault() throws Exception {
		D1 b = new D1();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).example(D1.class, b).build().getSession();
		assertObject(s.getSchema(D1.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
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

	@Test
	public void addExample_COLLECTION_exampleMethod_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D1c.class).build().getSession();
		assertObject(s.getSchema(D1c.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_COLLECTION_exampleMethod_wDefault_usingConfig() throws Exception {
		D1c b = new D1c();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D1cConfig.class).example(D1c.class, b).build().getSession();
		assertObject(s.getSchema(D1c.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
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

	@Test
	public void addExample_COLLECTION_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertObject(s.getSchema(D2.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).build().getSession();
		assertObject(s.getSchema(D2[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
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

	@Test
	public void addExample_COLLECTION_exampleField_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D2cConfig.class).build().getSession();
		assertObject(s.getSchema(D2c.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).applyAnnotations(D2cConfig.class).build().getSession();
		assertObject(s.getSchema(D2c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
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

	@Test
	public void addExample_COLLECTION_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).build().getSession();
		assertObject(s.getSchema(D3.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).build().getSession();
		assertObject(s.getSchema(D3[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'baz'}]]]}");
	}

	@SuppressWarnings("serial")
	@Example("[{f1:'baz'}]")
	public static class D3 extends BeanList {}

	@Test
	public void addExample_COLLECTION_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).applyAnnotations(D3cConfig.class).build().getSession();
		assertObject(s.getSchema(D3c.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'baz'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).applyAnnotations(D3cConfig.class).build().getSession();
		assertObject(s.getSchema(D3c[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'baz'}]]]}");
	}

	@Example(on="D3c", value="[{f1:'baz'}]")
	private static class D3cConfig {}

	@SuppressWarnings("serial")
	public static class D3c extends BeanList {}

	@Test
	public void addExample_COLLECTION_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s =JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(COLLECTION).example(BeanList.class, D1.example()).build().getSession();
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ARRAY).example(BeanList.class, D1.example()).build().getSession();
		assertObject(s.getSchema(BeanList[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BOOLEAN
	//====================================================================================================
	@Test
	public void addExample_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN).build().getSession();
		assertObject(s.getSchema(boolean.class)).asJson().is("{type:'boolean',example:true}");
		assertObject(s.getSchema(Boolean.class)).asJson().is("{type:'boolean',example:true}");
	}

	@Test
	public void addExample_BOOLEAN_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN)
			.example(boolean.class, false)
			.example(Boolean.class, false)
			.build().getSession();
		assertObject(s.getSchema(boolean.class)).asJson().is("{type:'boolean',example:false}");
		assertObject(s.getSchema(Boolean.class)).asJson().is("{type:'boolean',example:false}");
	}

	@Test
	public void addExample_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(BOOLEAN).build().getSession();
		assertObject(s.getSchema(boolean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'boolean',example:true}}}");
		assertObject(s.getSchema(Boolean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'boolean',example:true}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - NUMBER
	//====================================================================================================
	@Test
	public void addExample_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER).build().getSession();
		assertObject(s.getSchema(short.class)).asJson().is("{type:'integer',format:'int16',example:1}");
		assertObject(s.getSchema(Short.class)).asJson().is("{type:'integer',format:'int16',example:1}");
		assertObject(s.getSchema(int.class)).asJson().is("{type:'integer',format:'int32',example:1}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{type:'integer',format:'int32',example:1}");
		assertObject(s.getSchema(long.class)).asJson().is("{type:'integer',format:'int64',example:1}");
		assertObject(s.getSchema(Long.class)).asJson().is("{type:'integer',format:'int64',example:1}");
		assertObject(s.getSchema(float.class)).asJson().is("{type:'number',format:'float',example:1.0}");
		assertObject(s.getSchema(Float.class)).asJson().is("{type:'number',format:'float',example:1.0}");
		assertObject(s.getSchema(double.class)).asJson().is("{type:'number',format:'double',example:1.0}");
		assertObject(s.getSchema(Double.class)).asJson().is("{type:'number',format:'double',example:1.0}");
	}

	@Test
	public void addExample_NUMBER_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER)
			.example(short.class, (short)2)
			.example(Short.class, (short)3)
			.example(int.class, 4)
			.example(Integer.class, 5)
			.example(long.class, 6l)
			.example(Long.class, 7l)
			.example(float.class, 8f)
			.example(Float.class, 9f)
			.example(double.class, 10d)
			.example(Double.class, 11d)
			.build().getSession();
		assertObject(s.getSchema(short.class)).asJson().is("{type:'integer',format:'int16',example:2}");
		assertObject(s.getSchema(Short.class)).asJson().is("{type:'integer',format:'int16',example:3}");
		assertObject(s.getSchema(int.class)).asJson().is("{type:'integer',format:'int32',example:4}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{type:'integer',format:'int32',example:5}");
		assertObject(s.getSchema(long.class)).asJson().is("{type:'integer',format:'int64',example:6}");
		assertObject(s.getSchema(Long.class)).asJson().is("{type:'integer',format:'int64',example:7}");
		assertObject(s.getSchema(float.class)).asJson().is("{type:'number',format:'float',example:8.0}");
		assertObject(s.getSchema(Float.class)).asJson().is("{type:'number',format:'float',example:9.0}");
		assertObject(s.getSchema(double.class)).asJson().is("{type:'number',format:'double',example:10.0}");
		assertObject(s.getSchema(Double.class)).asJson().is("{type:'number',format:'double',example:11.0}");
	}

	@Test
	public void addExample_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(NUMBER).build().getSession();
		assertObject(s.getSchema(short[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',example:1}}}");
		assertObject(s.getSchema(Short[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',example:1}}}");
		assertObject(s.getSchema(int[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',example:1}}}");
		assertObject(s.getSchema(Integer[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',example:1}}}");
		assertObject(s.getSchema(long[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',example:1}}}");
		assertObject(s.getSchema(Long[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',example:1}}}");
		assertObject(s.getSchema(float[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'float',example:1.0}}}");
		assertObject(s.getSchema(Float[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'float',example:1.0}}}");
		assertObject(s.getSchema(double[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'double',example:1.0}}}");
		assertObject(s.getSchema(Double[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'double',example:1.0}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - STRING
	//====================================================================================================

	@Test
	public void addExample_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING).build().getSession();
		assertObject(s.getSchema(String.class)).asJson().is("{type:'string',example:'foo'}");
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{type:'string',example:'foo'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{type:'string',example:'a'}");
		assertObject(s.getSchema(char.class)).asJson().is("{type:'string',example:'a'}");
	}

	@Test
	public void addExample_STRING_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING)
			.example(StringBuilder.class, new StringBuilder("foo"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().getSession();
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{type:'string',example:'foo'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{type:'string',example:'b'}");
		assertObject(s.getSchema(char.class)).asJson().is("{type:'string',example:'c'}");
	}

	@Test
	public void addExample_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING).build().getSession();
		assertObject(s.getSchema(String[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}");
		assertObject(s.getSchema(Character[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'a'}}}");
		assertObject(s.getSchema(char[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'a'}}}");
	}

	@Test
	public void addExample_STRING_2darray_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(STRING)
			.example(StringBuilder.class, new StringBuilder("foo"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().getSession();
		assertObject(s.getSchema(StringBuilder[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'foo'}}}");
		assertObject(s.getSchema(Character[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'b'}}}");
		assertObject(s.getSchema(char[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',example:'c'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ENUM
	//====================================================================================================

	@Test
	public void addExample_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).build().getSession();
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['one','two','three'],example:'one'}");
	}

	@Test
	public void addExample_ENUM_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).example(TestEnumToString.class, TestEnumToString.TWO).build().getSession();
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['one','two','three'],example:'two'}");
	}

	@Test
	public void addExample_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ENUM).build().getSession();
		assertObject(s.getSchema(TestEnumToString[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],example:'one'}}}");
	}

	@Test
	public void addExample_ENUM_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).build().getSession();
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['ONE','TWO','THREE'],example:'ONE'}");
	}

	@Test
	public void addExample_ENUM_wDefault_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).example(TestEnumToString.class, "'TWO'").build().getSession();
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['ONE','TWO','THREE'],example:'TWO'}");
	}

	@Test
	public void addExample_ENUM_2darray_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().useEnumNames().addExamplesTo(ENUM).build().getSession();
		assertObject(s.getSchema(TestEnumToString[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string','enum':['ONE','TWO','THREE'],example:'ONE'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ANY
	//====================================================================================================
	@Test
	public void addExample_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addExamplesTo(ANY).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'object',properties:{f1:{type:'string',example:'foo'}}}");
		assertObject(s.getSchema(C1.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},example:{'123':{f1:'foobar'}}}");
		assertObject(s.getSchema(D1.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},example:[{f1:'foobar'}]}");
		assertObject(s.getSchema(D2[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},example:[[[{f1:'foobar'}]]]}");
		assertObject(s.getSchema(boolean.class)).asJson().is("{type:'boolean',example:true}");
		assertObject(s.getSchema(short.class)).asJson().is("{type:'integer',format:'int16',example:1}");
		assertObject(s.getSchema(Short.class)).asJson().is("{type:'integer',format:'int16',example:1}");
		assertObject(s.getSchema(int.class)).asJson().is("{type:'integer',format:'int32',example:1}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{type:'integer',format:'int32',example:1}");
		assertObject(s.getSchema(long.class)).asJson().is("{type:'integer',format:'int64',example:1}");
		assertObject(s.getSchema(Long.class)).asJson().is("{type:'integer',format:'int64',example:1}");
		assertObject(s.getSchema(float.class)).asJson().is("{type:'number',format:'float',example:1.0}");
		assertObject(s.getSchema(Float.class)).asJson().is("{type:'number',format:'float',example:1.0}");
		assertObject(s.getSchema(double.class)).asJson().is("{type:'number',format:'double',example:1.0}");
		assertObject(s.getSchema(Double.class)).asJson().is("{type:'number',format:'double',example:1.0}");
		assertObject(s.getSchema(String.class)).asJson().is("{type:'string',example:'foo'}");
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{type:'string',example:'foo'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{type:'string',example:'a'}");
		assertObject(s.getSchema(char.class)).asJson().is("{type:'string',example:'a'}");
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['one','two','three'],example:'one'}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BEAN
	//====================================================================================================

	@Test
	public void addDescription_BEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BEAN).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
	}

	@Test
	public void addDescription_BEAN_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BEAN).build().getSession();
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - MAP
	//====================================================================================================

	@Test
	public void addDescription_MAP() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(MAP).build().getSession();
		assertObject(s.getSchema(BeanMap.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test
	public void addDescription_MAP_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(MAP).build().getSession();
		assertObject(s.getSchema(BeanMap[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - COLLECTION / ARRAY
	//====================================================================================================

	@Test
	public void addDescription_COLLECTION() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(COLLECTION).build().getSession();
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test
	public void addDescription_COLLECTION_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(COLLECTION).build().getSession();
		assertObject(s.getSchema(BeanList[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}");
	}

	@Test
	public void addDescription_ARRAY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ARRAY).build().getSession();
		assertObject(s.getSchema(BeanList[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BOOLEAN
	//====================================================================================================
	@Test
	public void addDescription_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BOOLEAN).build().getSession();
		assertObject(s.getSchema(boolean.class)).asJson().is("{type:'boolean',description:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).asJson().is("{type:'boolean',description:'java.lang.Boolean'}");
	}

	@Test
	public void addDescription_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(BOOLEAN).build().getSession();
		assertObject(s.getSchema(boolean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'boolean',description:'boolean'}}}");
		assertObject(s.getSchema(Boolean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'boolean',description:'java.lang.Boolean'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - NUMBER
	//====================================================================================================
	@Test
	public void addDescription_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(NUMBER).build().getSession();
		assertObject(s.getSchema(short.class)).asJson().is("{type:'integer',format:'int16',description:'short'}");
		assertObject(s.getSchema(Short.class)).asJson().is("{type:'integer',format:'int16',description:'java.lang.Short'}");
		assertObject(s.getSchema(int.class)).asJson().is("{type:'integer',format:'int32',description:'int'}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{type:'integer',format:'int32',description:'java.lang.Integer'}");
		assertObject(s.getSchema(long.class)).asJson().is("{type:'integer',format:'int64',description:'long'}");
		assertObject(s.getSchema(Long.class)).asJson().is("{type:'integer',format:'int64',description:'java.lang.Long'}");
		assertObject(s.getSchema(float.class)).asJson().is("{type:'number',format:'float',description:'float'}");
		assertObject(s.getSchema(Float.class)).asJson().is("{type:'number',format:'float',description:'java.lang.Float'}");
		assertObject(s.getSchema(double.class)).asJson().is("{type:'number',format:'double',description:'double'}");
		assertObject(s.getSchema(Double.class)).asJson().is("{type:'number',format:'double',description:'java.lang.Double'}");
	}

	@Test
	public void addDescription_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(NUMBER).build().getSession();
		assertObject(s.getSchema(short[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'short'}}}");
		assertObject(s.getSchema(Short[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'java.lang.Short'}}}");
		assertObject(s.getSchema(int[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'int'}}}");
		assertObject(s.getSchema(Integer[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'java.lang.Integer'}}}");
		assertObject(s.getSchema(long[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'long'}}}");
		assertObject(s.getSchema(Long[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'java.lang.Long'}}}");
		assertObject(s.getSchema(float[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'float'}}}");
		assertObject(s.getSchema(Float[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'java.lang.Float'}}}");
		assertObject(s.getSchema(double[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'double'}}}");
		assertObject(s.getSchema(Double[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'java.lang.Double'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - STRING
	//====================================================================================================

	@Test
	public void addDescription_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(STRING).build().getSession();
		assertObject(s.getSchema(String.class)).asJson().is("{type:'string',description:'java.lang.String'}");
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{type:'string',description:'java.lang.StringBuilder'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{type:'string',description:'java.lang.Character'}");
		assertObject(s.getSchema(char.class)).asJson().is("{type:'string',description:'char'}");
	}

	@Test
	public void addDescription_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(STRING).build().getSession();
		assertObject(s.getSchema(String[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.String'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.StringBuilder'}}}");
		assertObject(s.getSchema(Character[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.Character'}}}");
		assertObject(s.getSchema(char[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string',description:'char'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ENUM
	//====================================================================================================

	@Test
	public void addDescription_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ENUM).build().getSession();
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}");
	}

	@Test
	public void addDescription_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ENUM).build().getSession();
		assertObject(s.getSchema(TestEnumToString[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ANY
	//====================================================================================================
	@Test
	public void addDescription_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().addDescriptionsTo(ANY).build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getSchema(BeanMap.class)).asJson().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
		assertObject(s.getSchema(BeanList[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}");
		assertObject(s.getSchema(boolean.class)).asJson().is("{type:'boolean',description:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).asJson().is("{type:'boolean',description:'java.lang.Boolean'}");
		assertObject(s.getSchema(short.class)).asJson().is("{type:'integer',format:'int16',description:'short'}");
		assertObject(s.getSchema(Short.class)).asJson().is("{type:'integer',format:'int16',description:'java.lang.Short'}");
		assertObject(s.getSchema(int.class)).asJson().is("{type:'integer',format:'int32',description:'int'}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{type:'integer',format:'int32',description:'java.lang.Integer'}");
		assertObject(s.getSchema(long.class)).asJson().is("{type:'integer',format:'int64',description:'long'}");
		assertObject(s.getSchema(Long.class)).asJson().is("{type:'integer',format:'int64',description:'java.lang.Long'}");
		assertObject(s.getSchema(float.class)).asJson().is("{type:'number',format:'float',description:'float'}");
		assertObject(s.getSchema(Float.class)).asJson().is("{type:'number',format:'float',description:'java.lang.Float'}");
		assertObject(s.getSchema(double.class)).asJson().is("{type:'number',format:'double',description:'double'}");
		assertObject(s.getSchema(Double.class)).asJson().is("{type:'number',format:'double',description:'java.lang.Double'}");
		assertObject(s.getSchema(String.class)).asJson().is("{type:'string',description:'java.lang.String'}");
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{type:'string',description:'java.lang.StringBuilder'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{type:'string',description:'java.lang.Character'}");
		assertObject(s.getSchema(char.class)).asJson().is("{type:'string',description:'char'}");
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}");
	}

	//====================================================================================================
	// JSONSCHEMA_defaultSchemas
	//====================================================================================================

	// If default schema contains 'type', it's considered complete.
	@Test
	public void defaultSchemas() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.defaultSchema(SimpleBean.class, OMap.of("type", "bar"))
			.defaultSchema(BeanMap.class, OMap.of("type", "bar"))
			.defaultSchema(BeanList.class, OMap.of("type", "bar"))
			.defaultSchema(BeanList[][].class, OMap.of("type", "bar"))
			.defaultSchema(boolean.class, OMap.of("type", "bar"))
			.defaultSchema(Boolean.class, OMap.of("type", "bar"))
			.defaultSchema(short.class, OMap.of("type", "bar"))
			.defaultSchema(Short.class, OMap.of("type", "bar"))
			.defaultSchema(int.class, OMap.of("type", "bar"))
			.defaultSchema(Integer.class, OMap.of("type", "bar"))
			.defaultSchema(long.class, OMap.of("type", "bar"))
			.defaultSchema(Long.class, OMap.of("type", "bar"))
			.defaultSchema(float.class, OMap.of("type", "bar"))
			.defaultSchema(Float.class, OMap.of("type", "bar"))
			.defaultSchema(double.class, OMap.of("type", "bar"))
			.defaultSchema(Double.class, OMap.of("type", "bar"))
			.defaultSchema(String.class, OMap.of("type", "bar"))
			.defaultSchema(StringBuilder.class, OMap.of("type", "bar"))
			.defaultSchema(Character.class, OMap.of("type", "bar"))
			.defaultSchema(char.class, OMap.of("type", "bar"))
			.defaultSchema(TestEnumToString.class, OMap.of("type", "bar"))
			.build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().contains("type:'bar'}");
		assertObject(s.getSchema(BeanMap.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(BeanList.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(BeanList[][].class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(boolean.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Boolean.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(short.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Short.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(int.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Integer.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(long.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Long.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(float.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Float.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(double.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Double.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(String.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(StringBuilder.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(Character.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(char.class)).asJson().contains("type:'bar'");
		assertObject(s.getSchema(TestEnumToString.class)).asJson().contains("type:'bar'");
	}

	// If default schema does not contain 'type', the value is augmented
	@Test
	public void defaultSchemasNoType() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.defaultSchema(SimpleBean.class, OMap.of("foo", "bar"))
			.defaultSchema(BeanMap.class, OMap.of("foo", "bar"))
			.defaultSchema(BeanList.class, OMap.of("foo", "bar"))
			.defaultSchema(BeanList[][].class, OMap.of("foo", "bar"))
			.defaultSchema(boolean.class, OMap.of("foo", "bar"))
			.defaultSchema(Boolean.class, OMap.of("foo", "bar"))
			.defaultSchema(short.class, OMap.of("foo", "bar"))
			.defaultSchema(Short.class, OMap.of("foo", "bar"))
			.defaultSchema(int.class, OMap.of("foo", "bar"))
			.defaultSchema(Integer.class, OMap.of("foo", "bar"))
			.defaultSchema(long.class, OMap.of("foo", "bar"))
			.defaultSchema(Long.class, OMap.of("foo", "bar"))
			.defaultSchema(float.class, OMap.of("foo", "bar"))
			.defaultSchema(Float.class, OMap.of("foo", "bar"))
			.defaultSchema(double.class, OMap.of("foo", "bar"))
			.defaultSchema(Double.class, OMap.of("foo", "bar"))
			.defaultSchema(String.class, OMap.of("foo", "bar"))
			.defaultSchema(StringBuilder.class, OMap.of("foo", "bar"))
			.defaultSchema(Character.class, OMap.of("foo", "bar"))
			.defaultSchema(char.class, OMap.of("foo", "bar"))
			.defaultSchema(TestEnumToString.class, OMap.of("foo", "bar"))
			.build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{foo:'bar',type:'object',properties:{f1:{foo:'bar',type:'string'}}}");
		assertObject(s.getSchema(BeanMap.class)).asJson().is("{foo:'bar',type:'object',additionalProperties:{foo:'bar',type:'object',properties:{f1:{foo:'bar',type:'string'}}}}");
		assertObject(s.getSchema(BeanList.class)).asJson().is("{foo:'bar',type:'array',items:{foo:'bar',type:'object',properties:{f1:{foo:'bar',type:'string'}}}}");
		assertObject(s.getSchema(BeanList[][].class)).asJson().is("{foo:'bar',type:'array',items:{type:'array',items:{foo:'bar',type:'array',items:{foo:'bar',type:'object',properties:{f1:{foo:'bar',type:'string'}}}}}}");
		assertObject(s.getSchema(boolean.class)).asJson().is("{foo:'bar',type:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).asJson().is("{foo:'bar',type:'boolean'}");
		assertObject(s.getSchema(short.class)).asJson().is("{foo:'bar',type:'integer',format:'int16'}");
		assertObject(s.getSchema(Short.class)).asJson().is("{foo:'bar',type:'integer',format:'int16'}");
		assertObject(s.getSchema(int.class)).asJson().is("{foo:'bar',type:'integer',format:'int32'}");
		assertObject(s.getSchema(Integer.class)).asJson().is("{foo:'bar',type:'integer',format:'int32'}");
		assertObject(s.getSchema(long.class)).asJson().is("{foo:'bar',type:'integer',format:'int64'}");
		assertObject(s.getSchema(Long.class)).asJson().is("{foo:'bar',type:'integer',format:'int64'}");
		assertObject(s.getSchema(float.class)).asJson().is("{foo:'bar',type:'number',format:'float'}");
		assertObject(s.getSchema(Float.class)).asJson().is("{foo:'bar',type:'number',format:'float'}");
		assertObject(s.getSchema(double.class)).asJson().is("{foo:'bar',type:'number',format:'double'}");
		assertObject(s.getSchema(Double.class)).asJson().is("{foo:'bar',type:'number',format:'double'}");
		assertObject(s.getSchema(String.class)).asJson().is("{foo:'bar',type:'string'}");
		assertObject(s.getSchema(StringBuilder.class)).asJson().is("{foo:'bar',type:'string'}");
		assertObject(s.getSchema(Character.class)).asJson().is("{foo:'bar',type:'string'}");
		assertObject(s.getSchema(char.class)).asJson().is("{foo:'bar',type:'string'}");
		assertObject(s.getSchema(TestEnumToString.class)).asJson().is("{foo:'bar',type:'string','enum':['one','two','three']}");
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedExamples
	//====================================================================================================

	@Test
	public void allowNestedExamples_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.allowNestedExamples()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo(COLLECTION,BEAN)
			.build().getSession();

		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void allowNestedExamples_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo(COLLECTION,BEAN)
			.build().getSession();

		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedDescriptions
	//====================================================================================================

	@Test
	public void allowNestedDescriptions_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.allowNestedDescriptions()
			.addDescriptionsTo(COLLECTION,BEAN)
			.build().getSession();

		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test
	public void allowNestedDescriptions_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.addDescriptionsTo(COLLECTION,BEAN)
			.build().getSession();

		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	//====================================================================================================
	// Swaps
	//====================================================================================================

	@Test
	public void swaps_int() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.swaps(IntSwap.class)
			.build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{type:'integer',format:'int32'}");
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{type:'integer',format:'int32'}}");
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
	}

	public static class IntSwap extends PojoSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on class
	//====================================================================================================

	@Test
	public void jsonSchema_onclass() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertObject(s.getSchema(A1.class)).asJson().is("{description:'baz',format:'bar',type:'foo',example:'{f1:123}',properties:{f1:{type:'integer',format:'int32'}}}");
	}

	@Schema(type="foo",format="bar",description="baz",example="{f1:123}")
	public static class A1 {
		public int f1;
	}

	@Test
	public void jsonSchema_onclass_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A1aConfig.class).build().getSession();
		assertObject(s.getSchema(A1a.class)).asJson().is("{description:'baz',format:'bar',type:'foo',example:'{f1:123}',properties:{f1:{type:'integer',format:'int32'}}}");
	}

	@Schema(on="Dummy1",type="foo",format="bar",description="baz",example="{f1:123}")
	@Schema(on="A1a",type="foo",format="bar",description="baz",example="{f1:123}")
	@Schema(on="Dummy2",type="foo",format="bar",description="baz",example="{f1:123}")
	private static class A1aConfig {}

	public static class A1a {
		public int f1;
	}

	@Test
	public void jsonSchema_onbeanfield() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertObject(s.getSchema(A2.class)).asJson().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	public static class A2 {
		@Schema(type="foo",format="bar",description="baz",example="123")
		public int f1;
	}

	@Test
	public void jsonSchema_onbeanfield_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A2aConfig.class).build().getSession();
		assertObject(s.getSchema(A2a.class)).asJson().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	@Schema(on="A2a.f1",type="foo",format="bar",description="baz",example="123")
	private static class A2aConfig {}

	public static class A2a {
		public int f1;
	}

	@Test
	public void jsonSchema_onbeangetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertObject(s.getSchema(A3.class)).asJson().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	public static class A3 {
		@Schema(type="foo",format="bar",description="baz",example="123")
		public int getF1() {
			return 123;
		}
	}

	@Test
	public void jsonSchema_onbeangetter_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A3aConfig.class).build().getSession();
		assertObject(s.getSchema(A3a.class)).asJson().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	@Schema(on="A3a.getF1",type="foo",format="bar",description="baz",example="123")
	private static class A3aConfig {}

	public static class A3a {
		public int getF1() {
			return 123;
		}
	}

	@Test
	public void jsonSchema_onbeansetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().build().getSession();
		assertObject(s.getSchema(A4.class)).asJson().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	public static class A4 {
		public int getF1() {
			return 123;
		}

		@Schema(type="foo",format="bar",description="baz",example="123")
		public void setF1(int f1) {}
	}

	@Test
	public void jsonSchema_onbeansetter_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(A4aConfig.class).build().getSession();
		assertObject(s.getSchema(A4a.class)).asJson().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	@Schema(on="A4a.setF1",type="foo",format="bar",description="baz",example="123")
	private static class A4aConfig {}

	public static class A4a {
		public int getF1() {
			return 123;
		}

		public void setF1(int f1) {}
	}

	//====================================================================================================
	// @JsonSchema on PojoSwap
	//====================================================================================================

	@Test
	public void jsonschema_onpojoswap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy()
			.swaps(SwapWithAnnotation.class)
			.build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{description:'baz',format:'bar',type:'foo',example:'123'}");
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{description:'baz',format:'bar',type:'foo',example:'123'}}");
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	@Schema(type="foo",format="bar",description="baz",example="123")
	public static class SwapWithAnnotation extends PojoSwap<SimpleBean,Integer> {}

	@Test
	public void jsonschema_onpojoswap_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.copy().applyAnnotations(SwapWithAnnotation2Config.class)
			.swaps(SwapWithAnnotation2.class)
			.build().getSession();
		assertObject(s.getSchema(SimpleBean.class)).asJson().is("{description:'baz',format:'bar',type:'foo',example:'123'}");
		assertObject(s.getSchema(BeanList.class)).asJson().is("{type:'array',items:{description:'baz',format:'bar',type:'foo',example:'123'}}");
		assertObject(s.getSchema(SimpleBean[][].class)).asJson().is("{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo',example:'123'}}}");
	}

	@Schema(on="SwapWithAnnotation2", type="foo",format="bar",description="baz",example="123")
	private static class SwapWithAnnotation2Config {}

	public static class SwapWithAnnotation2 extends PojoSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on PojoSwap
	//====================================================================================================

	@Schema(onClass=B.class,value="{foo:'bar'}")
	static class BConfig {}

	static class B {}
	static ClassInfo bConfig = ClassInfo.of(BConfig.class);

	@Test
	public void schemaOnClass_onConfig() throws Exception {
		AnnotationWorkList al = bConfig.getAnnotationList().getWork(null);
		JsonSchemaGeneratorSession x = JsonSchemaGenerator.create().apply(al).build().getSession();
		assertObject(x.getSchema(new B())).asJson().contains("foo:'bar'");
	}

}