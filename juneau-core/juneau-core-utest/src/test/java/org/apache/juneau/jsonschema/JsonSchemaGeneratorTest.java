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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.jsonschema.annotation.*;
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();

		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16'}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16'}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32'}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32'}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64'}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64'}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float'}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float'}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double'}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double'}");
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'boolean'}");
		assertObject(s.getSchema(String.class)).json().is("{type:'string'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string'}");
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three']}");
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string'}}}");
	}

	public static class SimpleBean {
		public String f1;
	}

	//====================================================================================================
	// Arrays
	//====================================================================================================

	@Test
	public void arrays1d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();

		assertObject(s.getSchema(short[].class)).json().is("{type:'array',items:{type:'integer',format:'int16'}}");
		assertObject(s.getSchema(Short[].class)).json().is("{type:'array',items:{type:'integer',format:'int16'}}");
		assertObject(s.getSchema(int[].class)).json().is("{type:'array',items:{type:'integer',format:'int32'}}");
		assertObject(s.getSchema(Integer[].class)).json().is("{type:'array',items:{type:'integer',format:'int32'}}");
		assertObject(s.getSchema(long[].class)).json().is("{type:'array',items:{type:'integer',format:'int64'}}");
		assertObject(s.getSchema(Long[].class)).json().is("{type:'array',items:{type:'integer',format:'int64'}}");
		assertObject(s.getSchema(float[].class)).json().is("{type:'array',items:{type:'number',format:'float'}}");
		assertObject(s.getSchema(Float[].class)).json().is("{type:'array',items:{type:'number',format:'float'}}");
		assertObject(s.getSchema(double[].class)).json().is("{type:'array',items:{type:'number',format:'double'}}");
		assertObject(s.getSchema(Double[].class)).json().is("{type:'array',items:{type:'number',format:'double'}}");
		assertObject(s.getSchema(boolean[].class)).json().is("{type:'array',items:{type:'boolean'}}");
		assertObject(s.getSchema(Boolean[].class)).json().is("{type:'array',items:{type:'boolean'}}");
		assertObject(s.getSchema(String[].class)).json().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(StringBuilder[].class)).json().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(char[].class)).json().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(Character[].class)).json().is("{type:'array',items:{type:'string'}}");
		assertObject(s.getSchema(TestEnumToString[].class)).json().is("{type:'array',items:{type:'string','enum':['one','two','three']}}");
		assertObject(s.getSchema(SimpleBean[].class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void arrays2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();

		assertObject(s.getSchema(short[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}");
		assertObject(s.getSchema(Short[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}");
		assertObject(s.getSchema(int[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
		assertObject(s.getSchema(Integer[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
		assertObject(s.getSchema(long[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}");
		assertObject(s.getSchema(Long[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}");
		assertObject(s.getSchema(float[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}");
		assertObject(s.getSchema(Float[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}");
		assertObject(s.getSchema(double[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}");
		assertObject(s.getSchema(Double[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}");
		assertObject(s.getSchema(boolean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'boolean'}}}");
		assertObject(s.getSchema(Boolean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'boolean'}}}");
		assertObject(s.getSchema(String[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(char[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(Character[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string'}}}");
		assertObject(s.getSchema(TestEnumToString[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three']}}}");
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	//====================================================================================================
	// Collections
	//====================================================================================================

	@Test
	public void simpleList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObject(s.getSchema(SimpleList.class)).json().is("{type:'array',items:{type:'integer',format:'int32'}}");
	}

	@SuppressWarnings("serial")
	public static class SimpleList extends LinkedList<Integer> {}

	@Test
	public void simpleList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObject(s.getSchema(Simple2dList.class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
	}

	@SuppressWarnings("serial")
	public static class Simple2dList extends LinkedList<LinkedList<Integer>> {}

	//====================================================================================================
	// Bean collections
	//====================================================================================================

	@Test
	public void beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanList extends LinkedList<SimpleBean> {}

	@Test
	public void beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObject(s.getSchema(BeanList2d.class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanList2d extends LinkedList<LinkedList<SimpleBean>> {}

	//====================================================================================================
	// Maps
	//====================================================================================================

	@Test
	public void beanMap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanMap extends LinkedHashMap<Integer,SimpleBean> {}

	@Test
	public void beanMap2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObject(s.getSchema(BeanMap2d.class)).json().is("{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}");
	}

	@SuppressWarnings("serial")
	public static class BeanMap2d extends LinkedHashMap<Integer,LinkedHashMap<Integer,SimpleBean>> {}


	//====================================================================================================
	// JSONSCHEMA_useBeanDefs
	//====================================================================================================

	@Test
	public void useBeanDefs() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{'$ref':'#/definitions/SimpleBean'}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void useBeanDefs_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void useBeanDefs_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObject(s.getSchema(BeanList2d.class)).json().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void useBeanDefs_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_useBeanDefs - preload definition.
	//====================================================================================================

	@Test
	public void beanDefsPreloaded() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(SimpleBean.class)).json().is("{'$ref':'#/definitions/SimpleBean'}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{test:123}}");
	}

	@Test
	public void useBeanDefsPreloaded_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{test:123}}");
	}

	@Test
	public void useBeanDefsPreloaded_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(BeanList2d.class)).json().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{test:123}}");
	}

	@Test
	public void useBeanDefsPreloaded_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new OMap().a("test", 123));
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}");
		assertObject(s.getBeanDefs()).json().is("{SimpleBean:{test:123}}");
	}

	//====================================================================================================
	// JSONSCHEMA_beanDefMapper
	//====================================================================================================

	@Test
	public void customBeanDefMapper() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().beanDefMapper(CustomBeanDefMapper.class).build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getBeanDefs()).json().is("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void customBeanDefMapperInstance() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().beanDefMapper(new CustomBeanDefMapper()).build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getBeanDefs()).json().is("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
	}

	public static class CustomBeanDefMapper extends BasicBeanDefMapper {
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getFullName();
		}
	}

	@Test
	public void customBeanDefMapper_customURI() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().beanDefMapper(CustomBeanDefMapper2.class).build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{'$ref':'/foo/bar/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getBeanDefs()).json().is("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string'}}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B1.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_wDefault() throws Exception {
		B1 b = new B1();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(B1.class, b).build().createSession();
		assertObject(s.getSchema(B1.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B1[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B1c.class).build().createSession();
		assertObject(s.getSchema(B1c.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_wDefault_usingConfig() throws Exception {
		B1c b = new B1c();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B1c.class).example(B1c.class, b).build().createSession();
		assertObject(s.getSchema(B1c.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethod_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B1c.class).build().createSession();
		assertObject(s.getSchema(B1c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
	}

	@BeanConfig(applyExample=@Example(on="B1c.example"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(B2.class, b).build().createSession();
		assertObject(s.getSchema(B2.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B2[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B2c.class).example(B2c.class, b).build().createSession();
		assertObject(s.getSchema(B2c.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}");
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B2c.class).build().createSession();
		assertObject(s.getSchema(B2c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
	}

	@BeanConfig(applyExample=@Example(on="B2c.example2"))
	public static class B2c extends B1c {

		public static B2c example2() {
			B2c ex = new B2c();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test
	public void addExample_BEAN_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B3.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B3[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B3c.class).build().createSession();
		assertObject(s.getSchema(B3c.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B3c.class).build().createSession();
		assertObject(s.getSchema(B3c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
	}

	@BeanConfig(applyExample=@Example(on="B3c.EXAMPLE"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B4.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObject(s.getSchema(B4[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
	}

	@Example("{f1:'foobar'}")
	public static class B4 extends SimpleBean {}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B4c.class).build().createSession();
		assertObject(s.getSchema(B4c.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").applyAnnotations(B4c.class).build().createSession();
		assertObject(s.getSchema(B4c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
	}

	@BeanConfig(applyExample=@Example(on="B4c", value="{f1:'foobar'}"))
	public static class B4c extends SimpleBean {}

	@Test
	public void addExample_BEAN_exampleBeanProperty() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(SimpleBean.class, b).build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}");
	}

	@Test
	public void addExample_BEAN_exampleBeanProperty_2darray() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(SimpleBean.class, b).build().createSession();
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - MAP
	//====================================================================================================

	@Test
	public void addExample_MAP_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void addExample_MAP_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObject(s.getSchema(C1.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleMethod_wDefault() throws Exception {
		C1 b = new C1();
		b.put(456, B1.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").example(C1.class, b).build().createSession();
		assertObject(s.getSchema(C1.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'456':{f1:'foobar'}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").applyAnnotations(C1c.class).build().createSession();
		assertObject(s.getSchema(C1c.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleMethod_wDefault_usingConfig() throws Exception {
		C1c b = new C1c();
		b.put(456, B1.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").applyAnnotations(C1c.class).example(C1c.class, b).build().createSession();
		assertObject(s.getSchema(C1c.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'456':{f1:'foobar'}}}");
	}

	@BeanConfig(applyExample=@Example(on="C1c.example"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObject(s.getSchema(C2.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObject(s.getSchema(C2[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").applyAnnotations(C2c.class).build().createSession();
		assertObject(s.getSchema(C2c.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").applyAnnotations(C2c.class).build().createSession();
		assertObject(s.getSchema(C2c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}}}");
	}

	@SuppressWarnings("serial")
	@BeanConfig(applyExample=@Example(on="C2c.EXAMPLE"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObject(s.getSchema(C3.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}");
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObject(s.getSchema(C3[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}}}");
	}

	@SuppressWarnings("serial")
	@Example("{'123':{f1:'baz'}}")
	public static class C3 extends BeanMap {}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").applyAnnotations(C3c.class).build().createSession();
		assertObject(s.getSchema(C3c.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}");
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").applyAnnotations(C3c.class).build().createSession();
		assertObject(s.getSchema(C3c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}}}");
	}

	@SuppressWarnings("serial")
	@BeanConfig(applyExample=@Example(on="C3c", value="{'123':{f1:'baz'}}"))
	public static class C3c extends BeanMap {}

	@Test
	public void addExample_MAP_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").example(BeanMap.class, C1.example()).build().createSession();
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}");
	}

	@Test
	public void addExample_MAP_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").example(BeanMap.class, C1.example()).build().createSession();
		assertObject(s.getSchema(BeanMap[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - COLLECTION / ARRAY
	//====================================================================================================

	@Test
	public void addExample_COLLECTION_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void addExample_COLLECTION_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObject(s.getSchema(D1.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_COLLECTION_exampleMethod_wDefault() throws Exception {
		D1 b = new D1();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").example(D1.class, b).build().createSession();
		assertObject(s.getSchema(D1.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").applyAnnotations(D1c.class).build().createSession();
		assertObject(s.getSchema(D1c.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_COLLECTION_exampleMethod_wDefault_usingConfig() throws Exception {
		D1c b = new D1c();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").applyAnnotations(D1c.class).example(D1c.class, b).build().createSession();
		assertObject(s.getSchema(D1c.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}");
	}

	@BeanConfig(applyExample=@Example(on="D1c.example"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObject(s.getSchema(D2.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").build().createSession();
		assertObject(s.getSchema(D2[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").applyAnnotations(D2c.class).build().createSession();
		assertObject(s.getSchema(D2c.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleField_array2d_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").applyAnnotations(D2c.class).build().createSession();
		assertObject(s.getSchema(D2c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}");
	}

	@SuppressWarnings("serial")
	@BeanConfig(applyExample=@Example(on="D2c.EXAMPLE"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObject(s.getSchema(D3.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").build().createSession();
		assertObject(s.getSchema(D3[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'baz'}]]]}");
	}

	@SuppressWarnings("serial")
	@Example("[{f1:'baz'}]")
	public static class D3 extends BeanList {}

	@Test
	public void addExample_COLLECTION_exampleBeanAnnotation_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").applyAnnotations(D3c.class).build().createSession();
		assertObject(s.getSchema(D3c.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleBeanAnnotation_2darray_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").applyAnnotations(D3c.class).build().createSession();
		assertObject(s.getSchema(D3c[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'baz'}]]]}");
	}

	@SuppressWarnings("serial")
	@BeanConfig(applyExample=@Example(on="D3c", value="[{f1:'baz'}]"))
	public static class D3c extends BeanList {}

	@Test
	public void addExample_COLLECTION_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s =JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").example(BeanList.class, D1.example()).build().createSession();
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}");
	}

	@Test
	public void addExample_ARRAY_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").example(BeanList.class, D1.example()).build().createSession();
		assertObject(s.getSchema(BeanList[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BOOLEAN
	//====================================================================================================
	@Test
	public void addExample_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("boolean").build().createSession();
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean','x-example':true}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'boolean','x-example':true}");
	}

	@Test
	public void addExample_BOOLEAN_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("boolean")
			.example(boolean.class, false)
			.example(Boolean.class, false)
			.build().createSession();
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean','x-example':false}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'boolean','x-example':false}");
	}

	@Test
	public void addExample_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("boolean").build().createSession();
		assertObject(s.getSchema(boolean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'boolean','x-example':true}}}");
		assertObject(s.getSchema(Boolean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'boolean','x-example':true}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - NUMBER
	//====================================================================================================
	@Test
	public void addExample_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("number").build().createSession();
		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16','x-example':1}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16','x-example':1}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32','x-example':1}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32','x-example':1}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64','x-example':1}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64','x-example':1}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float','x-example':1.0}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float','x-example':1.0}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double','x-example':1.0}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double','x-example':1.0}");
	}

	@Test
	public void addExample_NUMBER_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("number")
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
			.build().createSession();
		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16','x-example':2}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16','x-example':3}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32','x-example':4}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32','x-example':5}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64','x-example':6}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64','x-example':7}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float','x-example':8.0}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float','x-example':9.0}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double','x-example':10.0}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double','x-example':11.0}");
	}

	@Test
	public void addExample_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("number").build().createSession();
		assertObject(s.getSchema(short[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16','x-example':1}}}");
		assertObject(s.getSchema(Short[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16','x-example':1}}}");
		assertObject(s.getSchema(int[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32','x-example':1}}}");
		assertObject(s.getSchema(Integer[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32','x-example':1}}}");
		assertObject(s.getSchema(long[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64','x-example':1}}}");
		assertObject(s.getSchema(Long[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64','x-example':1}}}");
		assertObject(s.getSchema(float[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'float','x-example':1.0}}}");
		assertObject(s.getSchema(Float[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'float','x-example':1.0}}}");
		assertObject(s.getSchema(double[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'double','x-example':1.0}}}");
		assertObject(s.getSchema(Double[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'double','x-example':1.0}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - STRING
	//====================================================================================================

	@Test
	public void addExample_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string").build().createSession();
		assertObject(s.getSchema(String.class)).json().is("{type:'string','x-example':'foo'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string','x-example':'foo'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string','x-example':'a'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string','x-example':'a'}");
	}

	@Test
	public void addExample_STRING_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string")
			.exampleJson(String.class, "bar1")
			.example(StringBuilder.class, new StringBuilder("bar2"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().createSession();
		assertObject(s.getSchema(String.class)).json().is("{type:'string','x-example':'bar1'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string','x-example':'bar2'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string','x-example':'b'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string','x-example':'c'}");
	}

	@Test
	public void addExample_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string").build().createSession();
		assertObject(s.getSchema(String[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'foo'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'foo'}}}");
		assertObject(s.getSchema(Character[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'a'}}}");
		assertObject(s.getSchema(char[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'a'}}}");
	}

	@Test
	public void addExample_STRING_2darray_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string")
			.exampleJson(String.class, "bar1")
			.example(StringBuilder.class, new StringBuilder("bar2"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().createSession();
		assertObject(s.getSchema(String[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'bar1'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'bar2'}}}");
		assertObject(s.getSchema(Character[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'b'}}}");
		assertObject(s.getSchema(char[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','x-example':'c'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ENUM
	//====================================================================================================

	@Test
	public void addExample_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("enum").build().createSession();
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three'],'x-example':'one'}");
	}

	@Test
	public void addExample_ENUM_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("enum").example(TestEnumToString.class, TestEnumToString.TWO).build().createSession();
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three'],'x-example':'two'}");
	}

	@Test
	public void addExample_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("enum").build().createSession();
		assertObject(s.getSchema(TestEnumToString[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],'x-example':'one'}}}");
	}

	@Test
	public void addExample_ENUM_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useEnumNames().addExamplesTo("enum").build().createSession();
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['ONE','TWO','THREE'],'x-example':'ONE'}");
	}

	@Test
	public void addExample_ENUM_wDefault_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useEnumNames().addExamplesTo("enum").example(TestEnumToString.class, TestEnumToString.TWO).build().createSession();
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['ONE','TWO','THREE'],'x-example':'TWO'}");
	}

	@Test
	public void addExample_ENUM_2darray_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useEnumNames().addExamplesTo("enum").build().createSession();
		assertObject(s.getSchema(TestEnumToString[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','enum':['ONE','TWO','THREE'],'x-example':'ONE'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ANY
	//====================================================================================================
	@Test
	public void addExample_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("any").build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string','x-example':'foo'}}}");
		assertObject(s.getSchema(C1.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}");
		assertObject(s.getSchema(D1.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}");
		assertObject(s.getSchema(D2[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}");
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean','x-example':true}");
		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16','x-example':1}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16','x-example':1}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32','x-example':1}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32','x-example':1}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64','x-example':1}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64','x-example':1}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float','x-example':1.0}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float','x-example':1.0}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double','x-example':1.0}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double','x-example':1.0}");
		assertObject(s.getSchema(String.class)).json().is("{type:'string','x-example':'foo'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string','x-example':'foo'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string','x-example':'a'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string','x-example':'a'}");
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three'],'x-example':'one'}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BEAN
	//====================================================================================================

	@Test
	public void addDescription_BEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("bean").build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
	}

	@Test
	public void addDescription_BEAN_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("bean").build().createSession();
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - MAP
	//====================================================================================================

	@Test
	public void addDescription_MAP() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("map").build().createSession();
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test
	public void addDescription_MAP_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("map").build().createSession();
		assertObject(s.getSchema(BeanMap[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - COLLECTION / ARRAY
	//====================================================================================================

	@Test
	public void addDescription_COLLECTION() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("collection").build().createSession();
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test
	public void addDescription_COLLECTION_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("collection").build().createSession();
		assertObject(s.getSchema(BeanList[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}");
	}

	@Test
	public void addDescription_ARRAY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("array").build().createSession();
		assertObject(s.getSchema(BeanList[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BOOLEAN
	//====================================================================================================
	@Test
	public void addDescription_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("boolean").build().createSession();
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean',description:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'boolean',description:'java.lang.Boolean'}");
	}

	@Test
	public void addDescription_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("boolean").build().createSession();
		assertObject(s.getSchema(boolean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'boolean',description:'boolean'}}}");
		assertObject(s.getSchema(Boolean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'boolean',description:'java.lang.Boolean'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - NUMBER
	//====================================================================================================
	@Test
	public void addDescription_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("number").build().createSession();
		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16',description:'short'}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16',description:'java.lang.Short'}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32',description:'int'}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32',description:'java.lang.Integer'}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64',description:'long'}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64',description:'java.lang.Long'}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float',description:'float'}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float',description:'java.lang.Float'}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double',description:'double'}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double',description:'java.lang.Double'}");
	}

	@Test
	public void addDescription_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("number").build().createSession();
		assertObject(s.getSchema(short[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'short'}}}");
		assertObject(s.getSchema(Short[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'java.lang.Short'}}}");
		assertObject(s.getSchema(int[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'int'}}}");
		assertObject(s.getSchema(Integer[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'java.lang.Integer'}}}");
		assertObject(s.getSchema(long[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'long'}}}");
		assertObject(s.getSchema(Long[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'java.lang.Long'}}}");
		assertObject(s.getSchema(float[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'float'}}}");
		assertObject(s.getSchema(Float[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'java.lang.Float'}}}");
		assertObject(s.getSchema(double[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'double'}}}");
		assertObject(s.getSchema(Double[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'java.lang.Double'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - STRING
	//====================================================================================================

	@Test
	public void addDescription_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("string").build().createSession();
		assertObject(s.getSchema(String.class)).json().is("{type:'string',description:'java.lang.String'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string',description:'java.lang.StringBuilder'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string',description:'java.lang.Character'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string',description:'char'}");
	}

	@Test
	public void addDescription_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("string").build().createSession();
		assertObject(s.getSchema(String[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.String'}}}");
		assertObject(s.getSchema(StringBuilder[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.StringBuilder'}}}");
		assertObject(s.getSchema(Character[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.Character'}}}");
		assertObject(s.getSchema(char[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string',description:'char'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ENUM
	//====================================================================================================

	@Test
	public void addDescription_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("enum").build().createSession();
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}");
	}

	@Test
	public void addDescription_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("enum").build().createSession();
		assertObject(s.getSchema(TestEnumToString[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ANY
	//====================================================================================================
	@Test
	public void addDescription_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("any").build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}");
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
		assertObject(s.getSchema(BeanList[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}");
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean',description:'boolean'}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'boolean',description:'java.lang.Boolean'}");
		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16',description:'short'}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16',description:'java.lang.Short'}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32',description:'int'}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32',description:'java.lang.Integer'}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64',description:'long'}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64',description:'java.lang.Long'}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float',description:'float'}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float',description:'java.lang.Float'}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double',description:'double'}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double',description:'java.lang.Double'}");
		assertObject(s.getSchema(String.class)).json().is("{type:'string',description:'java.lang.String'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string',description:'java.lang.StringBuilder'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string',description:'java.lang.Character'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string',description:'char'}");
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}");
	}

	//====================================================================================================
	// JSONSCHEMA_defaultSchemas
	//====================================================================================================

	// If default schema contains 'type', it's considered complete.
	@Test
	public void defaultSchemas() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
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
			.build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(BeanList[][].class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(boolean.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(short.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(int.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(long.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(float.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(double.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(String.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'bar'}");
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'bar'}");
	}

	// If default schema does not contain 'type', the value is augmented
	@Test
	public void defaultSchemasNoType() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
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
			.build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'}");
		assertObject(s.getSchema(BeanMap.class)).json().is("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'},foo:'bar'}");
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'},foo:'bar'}");
		assertObject(s.getSchema(BeanList[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'},foo:'bar'}},foo:'bar'}");
		assertObject(s.getSchema(boolean.class)).json().is("{type:'boolean',foo:'bar'}");
		assertObject(s.getSchema(Boolean.class)).json().is("{type:'boolean',foo:'bar'}");
		assertObject(s.getSchema(short.class)).json().is("{type:'integer',format:'int16',foo:'bar'}");
		assertObject(s.getSchema(Short.class)).json().is("{type:'integer',format:'int16',foo:'bar'}");
		assertObject(s.getSchema(int.class)).json().is("{type:'integer',format:'int32',foo:'bar'}");
		assertObject(s.getSchema(Integer.class)).json().is("{type:'integer',format:'int32',foo:'bar'}");
		assertObject(s.getSchema(long.class)).json().is("{type:'integer',format:'int64',foo:'bar'}");
		assertObject(s.getSchema(Long.class)).json().is("{type:'integer',format:'int64',foo:'bar'}");
		assertObject(s.getSchema(float.class)).json().is("{type:'number',format:'float',foo:'bar'}");
		assertObject(s.getSchema(Float.class)).json().is("{type:'number',format:'float',foo:'bar'}");
		assertObject(s.getSchema(double.class)).json().is("{type:'number',format:'double',foo:'bar'}");
		assertObject(s.getSchema(Double.class)).json().is("{type:'number',format:'double',foo:'bar'}");
		assertObject(s.getSchema(String.class)).json().is("{type:'string',foo:'bar'}");
		assertObject(s.getSchema(StringBuilder.class)).json().is("{type:'string',foo:'bar'}");
		assertObject(s.getSchema(Character.class)).json().is("{type:'string',foo:'bar'}");
		assertObject(s.getSchema(char.class)).json().is("{type:'string',foo:'bar'}");
		assertObject(s.getSchema(TestEnumToString.class)).json().is("{type:'string','enum':['one','two','three'],foo:'bar'}");
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedExamples
	//====================================================================================================

	@Test
	public void allowNestedExamples_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.allowNestedExamples()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo("collection,bean")
			.build().createSession();

		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	@Test
	public void allowNestedExamples_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo("collection,bean")
			.build().createSession();

		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}");
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedDescriptions
	//====================================================================================================

	@Test
	public void allowNestedDescriptions_enabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.allowNestedDescriptions()
			.addDescriptionsTo("collection,bean")
			.build().createSession();

		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	@Test
	public void allowNestedDescriptions_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.addDescriptionsTo("collection,bean")
			.build().createSession();

		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}");
	}

	//====================================================================================================
	// Swaps
	//====================================================================================================

	@Test
	public void swaps_int() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.swaps(IntSwap.class)
			.build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{type:'integer',format:'int32'}");
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{type:'integer',format:'int32'}}");
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}");
	}

	public static class IntSwap extends PojoSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on class
	//====================================================================================================

	@Test
	public void jsonSchema_onclass() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObject(s.getSchema(A1.class)).json().is("{description:'baz',format:'bar',type:'foo','x-example':'{f1:123}',properties:{f1:{type:'integer',format:'int32'}}}");
	}

	@Schema(type="foo",format="bar",description="baz",example="{f1:123}")
	public static class A1 {
		public int f1;
	}

	@Test
	public void jsonSchema_onclass_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().applyAnnotations(A1a.class).build().createSession();
		assertObject(s.getSchema(A1a.class)).json().is("{description:'baz',format:'bar',type:'foo','x-example':'{f1:123}',properties:{f1:{type:'integer',format:'int32'}}}");
	}

	@JsonSchemaConfig(applySchema=@Schema(on="A1a",type="foo",format="bar",description="baz",example="{f1:123}"))
	public static class A1a {
		public int f1;
	}

	@Test
	public void jsonSchema_onbeanfield() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObject(s.getSchema(A2.class)).json().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	public static class A2 {
		@Schema(type="foo",format="bar",description="baz",example="123")
		public int f1;
	}

	@Test
	public void jsonSchema_onbeanfield_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().applyAnnotations(A2a.class).build().createSession();
		assertObject(s.getSchema(A2a.class)).json().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	@JsonSchemaConfig(applySchema=@Schema(on="A2a.f1",type="foo",format="bar",description="baz",example="123"))
	public static class A2a {
		public int f1;
	}

	@Test
	public void jsonSchema_onbeangetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObject(s.getSchema(A3.class)).json().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	public static class A3 {
		@Schema(type="foo",format="bar",description="baz",example="123")
		public int getF1() {
			return 123;
		}
	}

	@Test
	public void jsonSchema_onbeangetter_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().applyAnnotations(A3a.class).build().createSession();
		assertObject(s.getSchema(A3a.class)).json().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	@JsonSchemaConfig(applySchema=@Schema(on="A3a.getF1",type="foo",format="bar",description="baz",example="123"))
	public static class A3a {
		public int getF1() {
			return 123;
		}
	}

	@Test
	public void jsonSchema_onbeansetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObject(s.getSchema(A4.class)).json().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().applyAnnotations(A4a.class).build().createSession();
		assertObject(s.getSchema(A4a.class)).json().is("{type:'object',properties:{f1:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	@JsonSchemaConfig(applySchema=@Schema(on="A4a.setF1",type="foo",format="bar",description="baz",example="123"))
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
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.swaps(SwapWithAnnotation.class)
			.build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{description:'baz',format:'bar',type:'foo','x-example':'123'}");
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{description:'baz',format:'bar',type:'foo','x-example':'123'}}");
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	@Schema(type="foo",format="bar",description="baz",example="123")
	public static class SwapWithAnnotation extends PojoSwap<SimpleBean,Integer> {}

	@Test
	public void jsonschema_onpojoswap_usingConfig() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().applyAnnotations(SwapWithAnnotation2.class)
			.swaps(SwapWithAnnotation2.class)
			.build().createSession();
		assertObject(s.getSchema(SimpleBean.class)).json().is("{description:'baz',format:'bar',type:'foo','x-example':'123'}");
		assertObject(s.getSchema(BeanList.class)).json().is("{type:'array',items:{description:'baz',format:'bar',type:'foo','x-example':'123'}}");
		assertObject(s.getSchema(SimpleBean[][].class)).json().is("{type:'array',items:{type:'array',items:{description:'baz',format:'bar',type:'foo','x-example':'123'}}}");
	}

	@JsonSchemaConfig(applySchema=@Schema(on="SwapWithAnnotation2", type="foo",format="bar",description="baz",example="123"))
	public static class SwapWithAnnotation2 extends PojoSwap<SimpleBean,Integer> {}
}