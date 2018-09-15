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

import static org.apache.juneau.testutils.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.transform.*;
import org.junit.*;

public class JsonSchemaGeneratorTest {

	//====================================================================================================
	// Simple objects
	//====================================================================================================

	@Test
	public void simpleObjects() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();

		assertObjectEquals("{type:'integer',format:'int16'}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16'}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32'}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32'}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64'}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64'}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float'}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float'}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double'}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double'}", s.getSchema(Double.class));
		assertObjectEquals("{type:'boolean'}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean'}", s.getSchema(Boolean.class));
		assertObjectEquals("{type:'string'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string'}", s.getSchema(char.class));
		assertObjectEquals("{type:'string'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string','enum':['one','two','three']}", s.getSchema(TestEnumToString.class));
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", s.getSchema(SimpleBean.class));
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

		assertObjectEquals("{type:'array',items:{type:'integer',format:'int16'}}", s.getSchema(short[].class));
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int16'}}", s.getSchema(Short[].class));
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.getSchema(int[].class));
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.getSchema(Integer[].class));
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int64'}}", s.getSchema(long[].class));
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int64'}}", s.getSchema(Long[].class));
		assertObjectEquals("{type:'array',items:{type:'number',format:'float'}}", s.getSchema(float[].class));
		assertObjectEquals("{type:'array',items:{type:'number',format:'float'}}", s.getSchema(Float[].class));
		assertObjectEquals("{type:'array',items:{type:'number',format:'double'}}", s.getSchema(double[].class));
		assertObjectEquals("{type:'array',items:{type:'number',format:'double'}}", s.getSchema(Double[].class));
		assertObjectEquals("{type:'array',items:{type:'boolean'}}", s.getSchema(boolean[].class));
		assertObjectEquals("{type:'array',items:{type:'boolean'}}", s.getSchema(Boolean[].class));
		assertObjectEquals("{type:'array',items:{type:'string'}}", s.getSchema(String[].class));
		assertObjectEquals("{type:'array',items:{type:'string'}}", s.getSchema(StringBuilder[].class));
		assertObjectEquals("{type:'array',items:{type:'string'}}", s.getSchema(char[].class));
		assertObjectEquals("{type:'array',items:{type:'string'}}", s.getSchema(Character[].class));
		assertObjectEquals("{type:'array',items:{type:'string','enum':['one','two','three']}}", s.getSchema(TestEnumToString[].class));
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(SimpleBean[].class));
	}

	@Test
	public void arrays2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();

		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}", s.getSchema(short[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}", s.getSchema(Short[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(int[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(Integer[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}", s.getSchema(long[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}", s.getSchema(Long[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}", s.getSchema(float[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}", s.getSchema(Float[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}", s.getSchema(double[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}", s.getSchema(Double[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean'}}}", s.getSchema(boolean[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean'}}}", s.getSchema(Boolean[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.getSchema(String[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.getSchema(StringBuilder[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.getSchema(char[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.getSchema(Character[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three']}}}", s.getSchema(TestEnumToString[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(SimpleBean[][].class));
	}

	//====================================================================================================
	// Collections
	//====================================================================================================

	@Test
	public void simpleList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.getSchema(SimpleList.class));
	}

	@SuppressWarnings("serial")
	public static class SimpleList extends LinkedList<Integer> {}

	@Test
	public void simpleList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(Simple2dList.class));
	}

	@SuppressWarnings("serial")
	public static class Simple2dList extends LinkedList<LinkedList<Integer>> {}

	//====================================================================================================
	// Bean collections
	//====================================================================================================

	@Test
	public void beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@SuppressWarnings("serial")
	public static class BeanList extends LinkedList<SimpleBean> {}

	@Test
	public void beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(BeanList2d.class));
	}

	@SuppressWarnings("serial")
	public static class BeanList2d extends LinkedList<LinkedList<SimpleBean>> {}

	//====================================================================================================
	// Maps
	//====================================================================================================

	@Test
	public void beanMap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanMap.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMap extends LinkedHashMap<Integer,SimpleBean> {}

	@Test
	public void beanMap2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(BeanMap2d.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMap2d extends LinkedHashMap<Integer,LinkedHashMap<Integer,SimpleBean>> {}


	//====================================================================================================
	// JSONSCHEMA_useBeanDefs
	//====================================================================================================

	@Test
	public void useBeanDefs() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{'$ref':'#/definitions/SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefs_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.getSchema(BeanList.class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefs_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(BeanList2d.class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefs_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	//====================================================================================================
	// JSONSCHEMA_useBeanDefs - preload definition.
	//====================================================================================================

	@Test
	public void beanDefsPreloaded() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{'$ref':'#/definitions/SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefsPreloaded_beanList() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.getSchema(BeanList.class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefsPreloaded_beanList2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(BeanList2d.class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefsPreloaded_beanArray2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	//====================================================================================================
	// JSONSCHEMA_beanDefMapper
	//====================================================================================================

	@Test
	public void customBeanDefMapper() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().beanDefMapper(CustomBeanDefMapper.class).build().createSession();
		assertObjectEquals("{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void customBeanDefMapperInstance() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().beanDefMapper(new CustomBeanDefMapper()).build().createSession();
		assertObjectEquals("{'$ref':'#/definitions/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	public static class CustomBeanDefMapper extends BasicBeanDefMapper {
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getReadableName();
		}
	}

	@Test
	public void customBeanDefMapper_customURI() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useBeanDefs().beanDefMapper(CustomBeanDefMapper2.class).build().createSession();
		assertObjectEquals("{'$ref':'/foo/bar/org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	public static class CustomBeanDefMapper2 extends BasicBeanDefMapper {

		public CustomBeanDefMapper2() {
			super("/foo/bar/{0}");
		}
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getReadableName();
		}
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BEAN
	//====================================================================================================

	@Test
	public void addExample_BEAN_noBeanExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", s.getSchema(SimpleBean.class));
	}

	@Test
	public void addExample_BEAN_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBeanWithExampleMethod.class));
	}

	@Test
	public void addExample_BEAN_exampleMethod_wDefault() throws Exception {
		SimpleBeanWithExampleMethod b = new SimpleBeanWithExampleMethod();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(SimpleBeanWithExampleMethod.class, b).build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}", s.getSchema(SimpleBeanWithExampleMethod.class));
	}

	@Test
	public void addExample_BEAN_exampleMethod_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBeanWithExampleMethod[][].class));
	}

	public static class SimpleBeanWithExampleMethod extends SimpleBean {

		@Example
		public static SimpleBeanWithExampleMethod example() {
			SimpleBeanWithExampleMethod ex = new SimpleBeanWithExampleMethod();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_wDefault() throws Exception {
		SimpleBeanWithExampleMethod2 b = new SimpleBeanWithExampleMethod2();
		b.f1 = "baz";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(SimpleBeanWithExampleMethod2.class, b).build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}", s.getSchema(SimpleBeanWithExampleMethod2.class));
	}

	@Test
	public void addExample_BEAN_exampleMethodOverridden_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBeanWithExampleMethod2[][].class));
	}

	public static class SimpleBeanWithExampleMethod2 extends SimpleBeanWithExampleMethod {

		@Example
		public static SimpleBeanWithExampleMethod2 example2() {
			SimpleBeanWithExampleMethod2 ex = new SimpleBeanWithExampleMethod2();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test
	public void addExample_BEAN_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBeanWithExampleField.class));
	}

	@Test
	public void addExample_BEAN_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBeanWithExampleField[][].class));
	}

	public static class SimpleBeanWithExampleField extends SimpleBean {

		@Example
		public static SimpleBeanWithExampleField EXAMPLE = getExample();

		private static SimpleBeanWithExampleField getExample() {
			SimpleBeanWithExampleField ex = new SimpleBeanWithExampleField();
			ex.f1 = "foobar";
			return ex;
		}
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBeanWithExampleAnnotation.class));
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBeanWithExampleAnnotation[][].class));
	}

	@Example("{f1:'foobar'}")
	public static class SimpleBeanWithExampleAnnotation extends SimpleBean {}

	@Test
	public void addExample_BEAN_exampleBeanProperty() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(SimpleBean.class, b).build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBean.class));
	}

	@Test
	public void addExample_BEAN_exampleBeanProperty_2darray() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("bean").example(SimpleBean.class, b).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - MAP
	//====================================================================================================

	@Test
	public void addExample_MAP_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanMap.class));
	}

	@Test
	public void addExample_MAP_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMapWithExampleMethod.class));
	}

	@Test
	public void addExample_MAP_exampleMethod_wDefault() throws Exception {
		BeanMapWithExampleMethod b = new BeanMapWithExampleMethod();
		b.put(456, SimpleBeanWithExampleMethod.example());
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").example(BeanMapWithExampleMethod.class, b).build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'456':{f1:'foobar'}}}", s.getSchema(BeanMapWithExampleMethod.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMapWithExampleMethod extends BeanMap {

		@Example
		public static BeanMapWithExampleMethod example() {
			BeanMapWithExampleMethod m = new BeanMapWithExampleMethod();
			m.put(123, SimpleBeanWithExampleMethod.example());
			return m;
		}
	}

	@Test
	public void addExample_MAP_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMapWithExampleField.class));
	}

	@Test
	public void addExample_MAP_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}}}", s.getSchema(BeanMapWithExampleField[][].class));
	}

	@SuppressWarnings("serial")
	public static class BeanMapWithExampleField extends BeanMap {

		@Example
		public static BeanMapWithExampleField EXAMPLE = getExample();

		private static BeanMapWithExampleField getExample() {
			BeanMapWithExampleField ex = new BeanMapWithExampleField();
			ex.put(123, SimpleBeanWithExampleMethod.example());
			return ex;
		}
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}", s.getSchema(BeanMapWithExampleAnnotation.class));
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}}}", s.getSchema(BeanMapWithExampleAnnotation[][].class));
	}

	@SuppressWarnings("serial")
	@Example("{'123':{f1:'baz'}}")
	public static class BeanMapWithExampleAnnotation extends BeanMap {}

	@Test
	public void addExample_MAP_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").example(BeanMap.class, BeanMapWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMap.class));
	}

	@Test
	public void addExample_MAP_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("map").example(BeanMap.class, BeanMapWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}}}", s.getSchema(BeanMap[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - COLLECTION / ARRAY
	//====================================================================================================

	@Test
	public void addExample_COLLECTION_noExample() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@Test
	public void addExample_COLLECTION_exampleMethod() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanListWithExampleMethod.class));
	}

	@Test
	public void addExample_COLLECTION_exampleMethod_wDefault() throws Exception {
		BeanListWithExampleMethod b = new BeanListWithExampleMethod();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").example(BeanListWithExampleMethod.class, b).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}", s.getSchema(BeanListWithExampleMethod.class));
	}

	@SuppressWarnings("serial")
	public static class BeanListWithExampleMethod extends BeanList {

		@Example
		public static BeanListWithExampleMethod example() {
			BeanListWithExampleMethod m = new BeanListWithExampleMethod();
			m.add(SimpleBeanWithExampleMethod.example());
			return m;
		}
	}

	@Test
	public void addExample_COLLECTION_exampleField() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanListWithExampleField.class));
	}

	@Test
	public void addExample_ARRAY_exampleField_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}", s.getSchema(BeanListWithExampleField[][].class));
	}

	@SuppressWarnings("serial")
	public static class BeanListWithExampleField extends BeanList {

		@Example
		public static BeanListWithExampleField EXAMPLE = getExample();

		private static BeanListWithExampleField getExample() {
			BeanListWithExampleField ex = new BeanListWithExampleField();
			ex.add(SimpleBeanWithExampleMethod.example());
			return ex;
		}
	}

	@Test
	public void addExample_COLLECTION_exampleBeanAnnotation() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}", s.getSchema(BeanListWithExampleAnnotation.class));
	}

	@Test
	public void addExample_ARRAY_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'baz'}]]]}", s.getSchema(BeanListWithExampleAnnotation[][].class));
	}

	@SuppressWarnings("serial")
	@Example("[{f1:'baz'}]")
	public static class BeanListWithExampleAnnotation extends BeanList {}

	@Test
	public void addExample_COLLECTION_exampleBeanProperty() throws Exception {
		JsonSchemaGeneratorSession s =JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("collection").example(BeanList.class, BeanListWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanList.class));
	}

	@Test
	public void addExample_ARRAY_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("array").example(BeanList.class, BeanListWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}", s.getSchema(BeanList[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BOOLEAN
	//====================================================================================================
	@Test
	public void addExample_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("boolean").build().createSession();
		assertObjectEquals("{type:'boolean','x-example':true}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean','x-example':true}", s.getSchema(Boolean.class));
	}

	@Test
	public void addExample_BOOLEAN_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("boolean")
			.example(boolean.class, false)
			.example(Boolean.class, false)
			.build().createSession();
		assertObjectEquals("{type:'boolean','x-example':false}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean','x-example':false}", s.getSchema(Boolean.class));
	}

	@Test
	public void addExample_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("boolean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean','x-example':true}}}", s.getSchema(boolean[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean','x-example':true}}}", s.getSchema(Boolean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - NUMBER
	//====================================================================================================
	@Test
	public void addExample_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("number").build().createSession();
		assertObjectEquals("{type:'integer',format:'int16','x-example':1}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16','x-example':1}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32','x-example':1}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32','x-example':1}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64','x-example':1}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64','x-example':1}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float','x-example':1.0}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float','x-example':1.0}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double','x-example':1.0}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double','x-example':1.0}", s.getSchema(Double.class));
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
		assertObjectEquals("{type:'integer',format:'int16','x-example':2}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16','x-example':3}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32','x-example':4}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32','x-example':5}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64','x-example':6}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64','x-example':7}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float','x-example':8.0}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float','x-example':9.0}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double','x-example':10.0}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double','x-example':11.0}", s.getSchema(Double.class));
	}

	@Test
	public void addExample_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("number").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16','x-example':1}}}", s.getSchema(short[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16','x-example':1}}}", s.getSchema(Short[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32','x-example':1}}}", s.getSchema(int[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32','x-example':1}}}", s.getSchema(Integer[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64','x-example':1}}}", s.getSchema(long[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64','x-example':1}}}", s.getSchema(Long[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float','x-example':1.0}}}", s.getSchema(float[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float','x-example':1.0}}}", s.getSchema(Float[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double','x-example':1.0}}}", s.getSchema(double[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double','x-example':1.0}}}", s.getSchema(Double[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - STRING
	//====================================================================================================

	@Test
	public void addExample_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string").build().createSession();
		assertObjectEquals("{type:'string','x-example':'foo'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string','x-example':'foo'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string','x-example':'a'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string','x-example':'a'}", s.getSchema(char.class));
	}

	@Test
	public void addExample_STRING_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string")
			.example(String.class, "bar1")
			.example(StringBuilder.class, new StringBuilder("bar2"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().createSession();
		assertObjectEquals("{type:'string','x-example':'bar1'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string','x-example':'bar2'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string','x-example':'b'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string','x-example':'c'}", s.getSchema(char.class));
	}

	@Test
	public void addExample_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'foo'}}}", s.getSchema(String[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'foo'}}}", s.getSchema(StringBuilder[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'a'}}}", s.getSchema(Character[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'a'}}}", s.getSchema(char[][].class));
	}

	@Test
	public void addExample_STRING_2darray_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("string")
			.example(String.class, "bar1")
			.example(StringBuilder.class, new StringBuilder("bar2"))
			.example(Character.class, 'b')
			.example(char.class, 'c')
			.build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'bar1'}}}", s.getSchema(String[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'bar2'}}}", s.getSchema(StringBuilder[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'b'}}}", s.getSchema(Character[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'c'}}}", s.getSchema(char[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ENUM
	//====================================================================================================

	@Test
	public void addExample_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'string','enum':['one','two','three'],'x-example':'one'}", s.getSchema(TestEnumToString.class));
	}

	@Test
	public void addExample_ENUM_wDefault() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("enum").example(TestEnumToString.class, TestEnumToString.TWO).build().createSession();
		assertObjectEquals("{type:'string','enum':['one','two','three'],'x-example':'two'}", s.getSchema(TestEnumToString.class));
	}

	@Test
	public void addExample_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],'x-example':'one'}}}", s.getSchema(TestEnumToString[][].class));
	}

	@Test
	public void addExample_ENUM_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useEnumNames().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'string','enum':['ONE','TWO','THREE'],'x-example':'ONE'}", s.getSchema(TestEnumToString.class));
	}

	@Test
	public void addExample_ENUM_wDefault_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useEnumNames().addExamplesTo("enum").example(TestEnumToString.class, TestEnumToString.TWO).build().createSession();
		assertObjectEquals("{type:'string','enum':['ONE','TWO','THREE'],'x-example':'TWO'}", s.getSchema(TestEnumToString.class));
	}

	@Test
	public void addExample_ENUM_2darray_useEnumNames() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().useEnumNames().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['ONE','TWO','THREE'],'x-example':'ONE'}}}", s.getSchema(TestEnumToString[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ANY
	//====================================================================================================
	@Test
	public void addExample_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addExamplesTo("any").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string','x-example':'foo'}}}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMapWithExampleMethod.class));
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanListWithExampleMethod.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}", s.getSchema(BeanListWithExampleField[][].class));
		assertObjectEquals("{type:'boolean','x-example':true}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'integer',format:'int16','x-example':1}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16','x-example':1}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32','x-example':1}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32','x-example':1}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64','x-example':1}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64','x-example':1}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float','x-example':1.0}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float','x-example':1.0}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double','x-example':1.0}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double','x-example':1.0}", s.getSchema(Double.class));
		assertObjectEquals("{type:'string','x-example':'foo'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string','x-example':'foo'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string','x-example':'a'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string','x-example':'a'}", s.getSchema(char.class));
		assertObjectEquals("{type:'string','enum':['one','two','three'],'x-example':'one'}", s.getSchema(TestEnumToString.class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BEAN
	//====================================================================================================

	@Test
	public void addDescription_BEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
	}

	@Test
	public void addDescription_BEAN_array2d() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - MAP
	//====================================================================================================

	@Test
	public void addDescription_MAP() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanMap.class));
	}

	@Test
	public void addDescription_MAP_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("map").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}", s.getSchema(BeanMap[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - COLLECTION / ARRAY
	//====================================================================================================

	@Test
	public void addDescription_COLLECTION() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	@Test
	public void addDescription_COLLECTION_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}}}", s.getSchema(BeanList[][].class));
	}

	@Test
	public void addDescription_ARRAY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("array").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}", s.getSchema(BeanList[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BOOLEAN
	//====================================================================================================
	@Test
	public void addDescription_BOOLEAN() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("boolean").build().createSession();
		assertObjectEquals("{type:'boolean',description:'boolean'}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean',description:'java.lang.Boolean'}", s.getSchema(Boolean.class));
	}

	@Test
	public void addDescription_BOOLEAN_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("boolean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean',description:'boolean'}}}", s.getSchema(boolean[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean',description:'java.lang.Boolean'}}}", s.getSchema(Boolean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - NUMBER
	//====================================================================================================
	@Test
	public void addDescription_NUMBER() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("number").build().createSession();
		assertObjectEquals("{type:'integer',format:'int16',description:'short'}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16',description:'java.lang.Short'}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32',description:'int'}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32',description:'java.lang.Integer'}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64',description:'long'}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64',description:'java.lang.Long'}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float',description:'float'}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float',description:'java.lang.Float'}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double',description:'double'}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double',description:'java.lang.Double'}", s.getSchema(Double.class));
	}

	@Test
	public void addDescription_NUMBER_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("number").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'short'}}}", s.getSchema(short[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16',description:'java.lang.Short'}}}", s.getSchema(Short[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'int'}}}", s.getSchema(int[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32',description:'java.lang.Integer'}}}", s.getSchema(Integer[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'long'}}}", s.getSchema(long[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64',description:'java.lang.Long'}}}", s.getSchema(Long[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'float'}}}", s.getSchema(float[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float',description:'java.lang.Float'}}}", s.getSchema(Float[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'double'}}}", s.getSchema(double[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double',description:'java.lang.Double'}}}", s.getSchema(Double[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - STRING
	//====================================================================================================

	@Test
	public void addDescription_STRING() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("string").build().createSession();
		assertObjectEquals("{type:'string',description:'java.lang.String'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string',description:'java.lang.StringBuilder'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string',description:'java.lang.Character'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string',description:'char'}", s.getSchema(char.class));
	}

	@Test
	public void addDescription_STRING_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("string").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.String'}}}", s.getSchema(String[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.StringBuilder'}}}", s.getSchema(StringBuilder[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string',description:'java.lang.Character'}}}", s.getSchema(Character[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string',description:'char'}}}", s.getSchema(char[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ENUM
	//====================================================================================================

	@Test
	public void addDescription_ENUM() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("enum").build().createSession();
		assertObjectEquals("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}", s.getSchema(TestEnumToString.class));
	}

	@Test
	public void addDescription_ENUM_2darray() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("enum").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}}}", s.getSchema(TestEnumToString[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ANY
	//====================================================================================================
	@Test
	public void addDescription_ANY() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().addDescriptionsTo("any").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanMap<java.lang.Integer,org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanMap.class));
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>[][]'}", s.getSchema(BeanList[][].class));
		assertObjectEquals("{type:'boolean',description:'boolean'}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean',description:'java.lang.Boolean'}", s.getSchema(Boolean.class));
		assertObjectEquals("{type:'integer',format:'int16',description:'short'}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16',description:'java.lang.Short'}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32',description:'int'}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32',description:'java.lang.Integer'}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64',description:'long'}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64',description:'java.lang.Long'}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float',description:'float'}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float',description:'java.lang.Float'}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double',description:'double'}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double',description:'java.lang.Double'}", s.getSchema(Double.class));
		assertObjectEquals("{type:'string',description:'java.lang.String'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string',description:'java.lang.StringBuilder'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string',description:'java.lang.Character'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string',description:'char'}", s.getSchema(char.class));
		assertObjectEquals("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.testutils.pojos.TestEnumToString'}", s.getSchema(TestEnumToString.class));
	}

	//====================================================================================================
	// JSONSCHEMA_defaultSchemas
	//====================================================================================================

	// If default schema contains 'type', it's considered complete.
	@Test
	public void defaultSchemas() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.defaultSchema(SimpleBean.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(BeanMap.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(BeanList.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(BeanList[][].class, new ObjectMap().append("type", "bar"))
			.defaultSchema(boolean.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Boolean.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(short.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Short.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(int.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Integer.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(long.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Long.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(float.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Float.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(double.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Double.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(String.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(StringBuilder.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(Character.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(char.class, new ObjectMap().append("type", "bar"))
			.defaultSchema(TestEnumToString.class, new ObjectMap().append("type", "bar"))
			.build().createSession();
		assertObjectEquals("{type:'bar'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(BeanMap.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(BeanList[][].class));
		assertObjectEquals("{type:'bar'}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Boolean.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(short.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Short.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(int.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(long.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Long.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(float.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Float.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(double.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Double.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(String.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(char.class));
		assertObjectEquals("{type:'bar'}", s.getSchema(TestEnumToString.class));
	}

	// If default schema does not contain 'type', the value is augmented
	@Test
	public void defaultSchemasNoType() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.defaultSchema(SimpleBean.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(BeanMap.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(BeanList.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(BeanList[][].class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(boolean.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Boolean.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(short.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Short.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(int.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Integer.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(long.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Long.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(float.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Float.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(double.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Double.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(String.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(StringBuilder.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(Character.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(char.class, new ObjectMap().append("foo", "bar"))
			.defaultSchema(TestEnumToString.class, new ObjectMap().append("foo", "bar"))
			.build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'},foo:'bar'}", s.getSchema(BeanMap.class));
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'},foo:'bar'}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string',foo:'bar'}},foo:'bar'},foo:'bar'}},foo:'bar'}", s.getSchema(BeanList[][].class));
		assertObjectEquals("{type:'boolean',foo:'bar'}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean',foo:'bar'}", s.getSchema(Boolean.class));
		assertObjectEquals("{type:'integer',format:'int16',foo:'bar'}", s.getSchema(short.class));
		assertObjectEquals("{type:'integer',format:'int16',foo:'bar'}", s.getSchema(Short.class));
		assertObjectEquals("{type:'integer',format:'int32',foo:'bar'}", s.getSchema(int.class));
		assertObjectEquals("{type:'integer',format:'int32',foo:'bar'}", s.getSchema(Integer.class));
		assertObjectEquals("{type:'integer',format:'int64',foo:'bar'}", s.getSchema(long.class));
		assertObjectEquals("{type:'integer',format:'int64',foo:'bar'}", s.getSchema(Long.class));
		assertObjectEquals("{type:'number',format:'float',foo:'bar'}", s.getSchema(float.class));
		assertObjectEquals("{type:'number',format:'float',foo:'bar'}", s.getSchema(Float.class));
		assertObjectEquals("{type:'number',format:'double',foo:'bar'}", s.getSchema(double.class));
		assertObjectEquals("{type:'number',format:'double',foo:'bar'}", s.getSchema(Double.class));
		assertObjectEquals("{type:'string',foo:'bar'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string',foo:'bar'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string',foo:'bar'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string',foo:'bar'}", s.getSchema(char.class));
		assertObjectEquals("{type:'string','enum':['one','two','three'],foo:'bar'}", s.getSchema(TestEnumToString.class));
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

		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@Test
	public void allowNestedExamples_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo("collection,bean")
			.build().createSession();

		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
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

		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean'},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	@Test
	public void allowNestedDescriptions_disabled() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.addDescriptionsTo("collection,bean")
			.build().createSession();

		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$BeanList<org.apache.juneau.jsonschema.JsonSchemaGeneratorTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	//====================================================================================================
	// Swaps
	//====================================================================================================

	@Test
	public void swaps_int() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.pojoSwaps(IntSwap.class)
			.build().createSession();
		assertObjectEquals("{type:'integer',format:'int32'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(SimpleBean[][].class));
	}

	public static class IntSwap extends PojoSwap<SimpleBean,Integer> {}

	//====================================================================================================
	// @JsonSchema on class
	//====================================================================================================

	@Test
	public void jsonSchema_onclass() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObjectEquals("{type:'foo',format:'bar',properties:{f1:{type:'integer',format:'int32'}},description:'baz','x-example':{f1:123}}", s.getSchema(A1.class));
	}

	@JsonSchema(type="foo",format="bar",description="baz",example="{f1:123}")
	public static class A1 {
		public int f1;
	}

	@Test
	public void jsonSchema_onbeanfield() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'foo',format:'bar',description:'baz','x-example':123}}}", s.getSchema(A2.class));
	}

	public static class A2 {
		@JsonSchema(type="foo",format="bar",description="baz",example="123")
		public int f1;
	}

	@Test
	public void jsonSchema_onbeangetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'foo',format:'bar',description:'baz','x-example':123}}}", s.getSchema(A3.class));
	}

	public static class A3 {
		@JsonSchema(type="foo",format="bar",description="baz",example="123")
		public int getF1() {
			return 123;
		}
	}
	@Test

	public void jsonSchema_onbeansetter() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder().build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'foo',format:'bar',description:'baz','x-example':123}}}", s.getSchema(A4.class));
	}

	public static class A4 {
		public int getF1() {
			return 123;
		}

		@JsonSchema(type="foo",format="bar",description="baz",example="123")
		public void setF1(int f1) {}
	}

	//====================================================================================================
	// @JsonSchema on PojoSwap
	//====================================================================================================

	@Test
	public void jsonschema_onpojoswap() throws Exception {
		JsonSchemaGeneratorSession s = JsonSchemaGenerator.DEFAULT.builder()
			.pojoSwaps(SwapWithAnnotation.class)
			.build().createSession();
		assertObjectEquals("{type:'foo',format:'bar',description:'baz','x-example':123}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'array',items:{type:'foo',format:'bar',description:'baz','x-example':123}}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'foo',format:'bar',description:'baz','x-example':123}}}", s.getSchema(SimpleBean[][].class));
	}

	@JsonSchema(type="foo",format="bar",description="baz",example="123")
	public static class SwapWithAnnotation extends PojoSwap<SimpleBean,Integer> {}
}