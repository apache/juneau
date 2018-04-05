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
package org.apache.juneau.json;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.transform.*;
import org.junit.*;

public class JsonSchemaSerializerTest {

	//====================================================================================================
	// Simple objects
	//====================================================================================================
	@Test
	public void simpleObjects() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		
		assertEquals("{type:'integer',format:'int16'}", s.serialize((short)1));
		assertEquals("{type:'integer',format:'int32'}", s.serialize(1));
		assertEquals("{type:'integer',format:'int64'}", s.serialize(1l));
		assertEquals("{type:'number',format:'float'}", s.serialize(1f));
		assertEquals("{type:'number',format:'double'}", s.serialize(1d));
		assertEquals("{type:'boolean'}", s.serialize(true));
		assertEquals("{type:'string'}", s.serialize("foo"));
		assertEquals("{type:'string'}", s.serialize(new StringBuilder("foo")));
		assertEquals("{type:'string'}", s.serialize('c'));
		assertEquals("{type:'string','enum':['one','two','three']}", s.serialize(TestEnum.ONE));
		assertEquals("{type:'object',properties:{f1:{type:'string'}}}", s.serialize(new SimpleBean()));
	}

	@Test
	public void simpleObjects_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		
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
		assertObjectEquals("{type:'string','enum':['one','two','three']}", s.getSchema(TestEnum.class));
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
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		
		assertEquals("{type:'array',items:{type:'integer',format:'int16'}}", s.serialize(new short[]{(short)1}));
		assertEquals("{type:'array',items:{type:'integer',format:'int16'}}", s.serialize(new Short[]{(short)1}));
		assertEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.serialize(new int[]{1}));
		assertEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.serialize(new Integer[]{1}));
		assertEquals("{type:'array',items:{type:'integer',format:'int64'}}", s.serialize(new long[]{1l}));
		assertEquals("{type:'array',items:{type:'integer',format:'int64'}}", s.serialize(new Long[]{1l}));
		assertEquals("{type:'array',items:{type:'number',format:'float'}}", s.serialize(new float[]{1f}));
		assertEquals("{type:'array',items:{type:'number',format:'float'}}", s.serialize(new Float[]{1f}));
		assertEquals("{type:'array',items:{type:'number',format:'double'}}", s.serialize(new double[]{1d}));
		assertEquals("{type:'array',items:{type:'number',format:'double'}}", s.serialize(new Double[]{1d}));
		assertEquals("{type:'array',items:{type:'boolean'}}", s.serialize(new boolean[]{true}));
		assertEquals("{type:'array',items:{type:'boolean'}}", s.serialize(new Boolean[]{true}));
		assertEquals("{type:'array',items:{type:'string'}}", s.serialize(new String[]{"foo"}));
		assertEquals("{type:'array',items:{type:'string'}}", s.serialize(new StringBuilder[]{new StringBuilder("foo")}));
		assertEquals("{type:'array',items:{type:'string'}}", s.serialize(new char[]{'c'}));
		assertEquals("{type:'array',items:{type:'string'}}", s.serialize(new Character[]{'c'}));
		assertEquals("{type:'array',items:{type:'string','enum':['one','two','three']}}", s.serialize(new TestEnum[]{TestEnum.ONE}));
		assertEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.serialize(new SimpleBean[]{new SimpleBean()}));
	}

	@Test
	public void arrays1d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		
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
		assertObjectEquals("{type:'array',items:{type:'string','enum':['one','two','three']}}", s.getSchema(TestEnum[].class));
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(SimpleBean[].class));
	}
	
	@Test
	public void arrays2d() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}", s.serialize(new short[][]{{(short)1}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int16'}}}", s.serialize(new Short[][]{{(short)1}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.serialize(new int[][]{{1}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.serialize(new Integer[][]{{1}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}", s.serialize(new long[][]{{1l}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int64'}}}", s.serialize(new Long[][]{{1l}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}", s.serialize(new float[][]{{1f}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'number',format:'float'}}}", s.serialize(new Float[][]{{1f}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}", s.serialize(new double[][]{{1d}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'number',format:'double'}}}", s.serialize(new Double[][]{{1d}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'boolean'}}}", s.serialize(new boolean[][]{{true}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'boolean'}}}", s.serialize(new Boolean[][]{{true}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.serialize(new String[][]{{"foo"}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.serialize(new StringBuilder[][]{{new StringBuilder("foo")}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.serialize(new char[][]{{'c'}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'string'}}}", s.serialize(new Character[][]{{'c'}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three']}}}", s.serialize(new TestEnum[][]{{TestEnum.ONE}}));
		assertEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.serialize(new SimpleBean[][]{{new SimpleBean()}}));
	}

	@Test
	public void arrays2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		
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
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three']}}}", s.getSchema(TestEnum[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(SimpleBean[][].class));
	}
	
	//====================================================================================================
	// Collections
	//====================================================================================================
	
	@Test
	public void simpleList() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		assertEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.serialize(new SimpleList()));
	}

	@Test
	public void simpleList_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		assertObjectEquals("{type:'array',items:{type:'integer',format:'int32'}}", s.getSchema(SimpleList.class));
	}

	@SuppressWarnings("serial")
	public static class SimpleList extends LinkedList<Integer> {}

	@Test
	public void simpleList2d() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		assertEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.serialize(new Simple2dList()));
	}

	@Test
	public void simpleList2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'integer',format:'int32'}}}", s.getSchema(Simple2dList.class));
	}

	@SuppressWarnings("serial")
	public static class Simple2dList extends LinkedList<LinkedList<Integer>> {}

	//====================================================================================================
	// Bean collections
	//====================================================================================================
	
	@Test
	public void beanList() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		assertEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.serialize(new BeanList()));
	}

	@Test
	public void beanList_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@SuppressWarnings("serial")
	public static class BeanList extends LinkedList<SimpleBean> {}

	@Test
	public void beanList2d() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		assertEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.serialize(new BeanList2d()));
	}

	@Test
	public void beanList2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(BeanList2d.class));
	}

	@SuppressWarnings("serial")
	public static class BeanList2d extends LinkedList<LinkedList<SimpleBean>> {}

	//====================================================================================================
	// Maps
	//====================================================================================================

	@Test
	public void beanMap() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		assertEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.serialize(new BeanMap()));
	}

	@Test
	public void beanMap_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanMap.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMap extends LinkedHashMap<Integer,SimpleBean> {}

	@Test
	public void beanMap2d() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX;
		assertEquals("{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}", s.serialize(new BeanMap2d()));
	}

	@Test
	public void beanMap2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}}", s.getSchema(BeanMap2d.class));
	}

	@SuppressWarnings("serial")
	public static class BeanMap2d extends LinkedHashMap<Integer,LinkedHashMap<Integer,SimpleBean>> {}
	
	
	//====================================================================================================
	// JSONSCHEMA_useBeanDefs
	//====================================================================================================

	@Test
	public void useBeanDefs() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build();
		assertEquals("{'$ref':'#/definitions/SimpleBean'}", s.serialize(new SimpleBean()));
	}

	@Test
	public void useBeanDefs_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{'$ref':'#/definitions/SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}
	
	@Test
	public void useBeanDefs_beanList() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build();
		assertEquals("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.serialize(new BeanList()));
	}

	@Test
	public void useBeanDefs_beanList_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.getSchema(BeanList.class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefs_beanList2d() throws Exception {
		JsonSchemaSerializer s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build();
		assertEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.serialize(new BeanList2d()));
	}

	@Test
	public void useBeanDefs_beanList2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(BeanList2d.class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefs_beanArray2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
		assertObjectEquals("{SimpleBean:{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	//====================================================================================================
	// JSONSCHEMA_useBeanDefs - preload definition.
	//====================================================================================================

	@Test
	public void beanDefsPreloaded() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{'$ref':'#/definitions/SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefsPreloaded_beanList_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}", s.getSchema(BeanList.class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	@Test
	public void useBeanDefsPreloaded_beanList2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(BeanList2d.class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}
	
	@Test
	public void useBeanDefsPreloaded_beanArray2d_getSchema() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().build().createSession();
		s.addBeanDef("SimpleBean", new ObjectMap().append("test", 123));
		assertObjectEquals("{type:'array',items:{type:'array',items:{'$ref':'#/definitions/SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
		assertObjectEquals("{SimpleBean:{test:123}}", s.getBeanDefs());
	}

	//====================================================================================================
	// JSONSCHEMA_beanDefMapper
	//====================================================================================================

	@Test
	public void customBeanDefMapper() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().beanDefMapper(CustomBeanDefMapper.class).build().createSession();
		assertObjectEquals("{'$ref':'#/definitions/org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	@Test
	public void customBeanDefMapperInstance() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().beanDefMapper(new CustomBeanDefMapper()).build().createSession();
		assertObjectEquals("{'$ref':'#/definitions/org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
	}

	public static class CustomBeanDefMapper extends BasicBeanDefMapper {
		@Override
		public String getId(ClassMeta<?> cm) {
			return cm.getReadableName();
		}
	}
	
	@Test
	public void customBeanDefMapper_customURI() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useBeanDefs().beanDefMapper(CustomBeanDefMapper2.class).build().createSession();
		assertObjectEquals("{'$ref':'/foo/bar/org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean':{type:'object',properties:{f1:{type:'string'}}}}", s.getBeanDefs());
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", s.getSchema(SimpleBean.class));
	}

	@Test
	public void addExample_BEAN_exampleMethod() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBeanWithExampleMethod.class));
	}

	@Test
	public void addExample_BEAN_exampleMethod_wDefault() throws Exception {
		SimpleBeanWithExampleMethod b = new SimpleBeanWithExampleMethod();
		b.f1 = "baz";
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").example(SimpleBeanWithExampleMethod.class, b).build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'baz'}}", s.getSchema(SimpleBeanWithExampleMethod.class));
	}

	@Test
	public void addExample_BEAN_exampleMethod_array2d() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
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
	public void addExample_BEAN_exampleField() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBeanWithExampleField.class));
	}

	@Test
	public void addExample_BEAN_exampleField_array2d() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBeanWithExampleAnnotation.class));
	}

	@Test
	public void addExample_BEAN_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBeanWithExampleAnnotation[][].class));
	}

	@Example("{f1:'foobar'}")
	public static class SimpleBeanWithExampleAnnotation extends SimpleBean {}
	
	@Test
	public void addExample_BEAN_exampleBeanProperty() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").example(SimpleBean.class, b).build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}", s.getSchema(SimpleBean.class));
	}

	@Test
	public void addExample_BEAN_exampleBeanProperty_2darray() throws Exception {
		SimpleBean b = new SimpleBean();
		b.f1 = "foobar";
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("bean").example(SimpleBean.class, b).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{f1:'foobar'}}}}", s.getSchema(SimpleBean[][].class));
	}
	
	//====================================================================================================
	// JSONSCHEMA_addExamples - MAP
	//====================================================================================================

	@Test
	public void addExample_MAP_noExample() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanMap.class));
	}

	@Test
	public void addExample_MAP_exampleMethod() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMapWithExampleMethod.class));
	}
	
	@Test
	public void addExample_MAP_exampleMethod_wDefault() throws Exception {
		BeanMapWithExampleMethod b = new BeanMapWithExampleMethod();
		b.put(456, SimpleBeanWithExampleMethod.example());
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").example(BeanMapWithExampleMethod.class, b).build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMapWithExampleField.class));
	}

	@Test
	public void addExample_MAP_exampleField_array2d() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}", s.getSchema(BeanMapWithExampleAnnotation.class));
	}

	@Test
	public void addExample_MAP_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'baz'}}}}}", s.getSchema(BeanMapWithExampleAnnotation[][].class));
	}

	@SuppressWarnings("serial")
	@Example("{'123':{f1:'baz'}}")
	public static class BeanMapWithExampleAnnotation extends BeanMap {}
	
	@Test
	public void addExample_MAP_exampleBeanProperty() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").example(BeanMap.class, BeanMapWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}", s.getSchema(BeanMap.class));
	}

	@Test
	public void addExample_MAP_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("map").example(BeanMap.class, BeanMapWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},'x-example':{'123':{f1:'foobar'}}}}}", s.getSchema(BeanMap[][].class));
	}
	
	//====================================================================================================
	// JSONSCHEMA_addExamples - COLLECTION / ARRAY
	//====================================================================================================
	
	@Test
	public void addExample_COLLECTION_noExample() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}", s.getSchema(BeanList.class));
	}

	@Test
	public void addExample_COLLECTION_exampleMethod() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanListWithExampleMethod.class));
	}

	@Test
	public void addExample_COLLECTION_exampleMethod_wDefault() throws Exception {
		BeanListWithExampleMethod b = new BeanListWithExampleMethod();
		SimpleBean sb = new SimpleBean();
		sb.f1 = "baz";
		b.add(sb);
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("collection").example(BeanListWithExampleMethod.class, b).build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanListWithExampleField.class));
	}

	@Test
	public void addExample_ARRAY_exampleField_array2d() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("array").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'baz'}]}", s.getSchema(BeanListWithExampleAnnotation.class));
	}

	@Test
	public void addExample_ARRAY_exampleBeanAnnotation_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("array").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'baz'}]]]}", s.getSchema(BeanListWithExampleAnnotation[][].class));
	}

	@SuppressWarnings("serial")
	@Example("[{f1:'baz'}]")
	public static class BeanListWithExampleAnnotation extends BeanList {}
	
	@Test
	public void addExample_COLLECTION_exampleBeanProperty() throws Exception {
		JsonSchemaSerializerSession s =JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("collection").example(BeanList.class, BeanListWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[{f1:'foobar'}]}", s.getSchema(BeanList.class));
	}

	@Test
	public void addExample_ARRAY_exampleBeanProperty_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("array").example(BeanList.class, BeanListWithExampleMethod.example()).build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},'x-example':[[[{f1:'foobar'}]]]}", s.getSchema(BeanList[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - BOOLEAN
	//====================================================================================================
	@Test
	public void addExample_BOOLEAN() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("boolean").build().createSession();
		assertObjectEquals("{type:'boolean','x-example':true}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean','x-example':true}", s.getSchema(Boolean.class));
	}
	
	@Test
	public void addExample_BOOLEAN_wDefault() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("boolean")
			.example(boolean.class, false)
			.example(Boolean.class, false)
			.build().createSession();
		assertObjectEquals("{type:'boolean','x-example':false}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean','x-example':false}", s.getSchema(Boolean.class));
	}

	@Test
	public void addExample_BOOLEAN_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("boolean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean','x-example':true}}}", s.getSchema(boolean[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean','x-example':true}}}", s.getSchema(Boolean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - NUMBER
	//====================================================================================================
	@Test
	public void addExample_NUMBER() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("number").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("number")
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("number").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("string").build().createSession();
		assertObjectEquals("{type:'string','x-example':'foo'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string','x-example':'foo'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string','x-example':'a'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string','x-example':'a'}", s.getSchema(char.class));
	}

	@Test
	public void addExample_STRING_wDefault() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("string")
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("string").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'foo'}}}", s.getSchema(String[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'foo'}}}", s.getSchema(StringBuilder[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'a'}}}", s.getSchema(Character[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','x-example':'a'}}}", s.getSchema(char[][].class));
	}

	@Test
	public void addExample_STRING_2darray_wDefault() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("string")
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'string','enum':['one','two','three'],'x-example':'one'}", s.getSchema(TestEnum.class));
	}

	@Test
	public void addExample_ENUM_wDefault() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("enum").example(TestEnum.class, TestEnum.TWO).build().createSession();
		assertObjectEquals("{type:'string','enum':['one','two','three'],'x-example':'two'}", s.getSchema(TestEnum.class));
	}

	@Test
	public void addExample_ENUM_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],'x-example':'one'}}}", s.getSchema(TestEnum[][].class));
	}

	@Test
	public void addExample_ENUM_useEnumNames() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useEnumNames().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'string','enum':['ONE','TWO','THREE'],'x-example':'ONE'}", s.getSchema(TestEnum.class));
	}

	@Test
	public void addExample_ENUM_wDefault_useEnumNames() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useEnumNames().addExamplesTo("enum").example(TestEnum.class, TestEnum.TWO).build().createSession();
		assertObjectEquals("{type:'string','enum':['ONE','TWO','THREE'],'x-example':'TWO'}", s.getSchema(TestEnum.class));
	}

	@Test
	public void addExample_ENUM_2darray_useEnumNames() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().useEnumNames().addExamplesTo("enum").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['ONE','TWO','THREE'],'x-example':'ONE'}}}", s.getSchema(TestEnum[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addExamples - ANY
	//====================================================================================================
	@Test
	public void addExample_ANY() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addExamplesTo("any").build().createSession();
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
		assertObjectEquals("{type:'string','enum':['one','two','three'],'x-example':'one'}", s.getSchema(TestEnum.class));
	}
	
	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BEAN
	//====================================================================================================

	@Test
	public void addDescription_BEAN() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("bean").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'}", s.getSchema(SimpleBean.class));
	}

	@Test
	public void addDescription_BEAN_array2d() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("bean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'}}}", s.getSchema(SimpleBean[][].class));
	}
	
	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - MAP
	//====================================================================================================

	@Test
	public void addDescription_MAP() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("map").build().createSession();
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanMap<java.lang.Integer,org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}", s.getSchema(BeanMap.class));
	}

	@Test
	public void addDescription_MAP_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("map").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanMap<java.lang.Integer,org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}}}", s.getSchema(BeanMap[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - COLLECTION / ARRAY
	//====================================================================================================
	
	@Test
	public void addDescription_COLLECTION() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	@Test
	public void addDescription_COLLECTION_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("collection").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}}}", s.getSchema(BeanList[][].class));
	}
	
	@Test
	public void addDescription_ARRAY() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("array").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>[][]'}", s.getSchema(BeanList[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - BOOLEAN
	//====================================================================================================
	@Test
	public void addDescription_BOOLEAN() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("boolean").build().createSession();
		assertObjectEquals("{type:'boolean',description:'boolean'}", s.getSchema(boolean.class));
		assertObjectEquals("{type:'boolean',description:'java.lang.Boolean'}", s.getSchema(Boolean.class));
	}

	@Test
	public void addDescription_BOOLEAN_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("boolean").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean',description:'boolean'}}}", s.getSchema(boolean[][].class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'boolean',description:'java.lang.Boolean'}}}", s.getSchema(Boolean[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - NUMBER
	//====================================================================================================
	@Test
	public void addDescription_NUMBER() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("number").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("number").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("string").build().createSession();
		assertObjectEquals("{type:'string',description:'java.lang.String'}", s.getSchema(String.class));
		assertObjectEquals("{type:'string',description:'java.lang.StringBuilder'}", s.getSchema(StringBuilder.class));
		assertObjectEquals("{type:'string',description:'java.lang.Character'}", s.getSchema(Character.class));
		assertObjectEquals("{type:'string',description:'char'}", s.getSchema(char.class));
	}

	@Test
	public void addDescription_STRING_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("string").build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("enum").build().createSession();
		assertObjectEquals("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.TestEnum'}", s.getSchema(TestEnum.class));
	}

	@Test
	public void addDescription_ENUM_2darray() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("enum").build().createSession();
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'string','enum':['one','two','three'],description:'org.apache.juneau.TestEnum'}}}", s.getSchema(TestEnum[][].class));
	}

	//====================================================================================================
	// JSONSCHEMA_addDescriptionsTo - ANY
	//====================================================================================================
	@Test
	public void addDescription_ANY() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().addDescriptionsTo("any").build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'object',additionalProperties:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanMap<java.lang.Integer,org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}", s.getSchema(BeanMap.class));
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'array',items:{type:'object',properties:{f1:{type:'string'}}}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>[][]'}", s.getSchema(BeanList[][].class));
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
		assertObjectEquals("{type:'string','enum':['one','two','three'],description:'org.apache.juneau.TestEnum'}", s.getSchema(TestEnum.class));
	}

	//====================================================================================================
	// JSONSCHEMA_defaultSchemas
	//====================================================================================================

	// If default schema contains 'type', it's considered complete.
	@Test
	public void defaultSchemas() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
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
			.defaultSchema(TestEnum.class, new ObjectMap().append("type", "bar"))
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
		assertObjectEquals("{type:'bar'}", s.getSchema(TestEnum.class));
	}
	
	// If default schema does not contain 'type', the value is augmented
	@Test
	public void defaultSchemasNoType() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
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
			.defaultSchema(TestEnum.class, new ObjectMap().append("foo", "bar"))
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
		assertObjectEquals("{type:'string','enum':['one','two','three'],foo:'bar'}", s.getSchema(TestEnum.class));
	}
	
	//====================================================================================================
	// JSONSCHEMA_allowNestedExamples
	//====================================================================================================

	@Test
	public void allowNestedExamples_enabled() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
			.allowNestedExamples()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo("collection,bean")
			.build().createSession();
		
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}},'x-example':{}},'x-example':[]}", s.getSchema(BeanList.class));
	}
	
	@Test
	public void allowNestedExamples_disabled() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
			.example(BeanList.class, new BeanList())
			.example(SimpleBean.class, new SimpleBean())
			.addExamplesTo("collection,bean")
			.build().createSession();
		
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},'x-example':[]}", s.getSchema(BeanList.class));
	}

	//====================================================================================================
	// JSONSCHEMA_allowNestedDescriptions
	//====================================================================================================

	@Test
	public void allowNestedDescriptions_enabled() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
			.allowNestedDescriptions()
			.addDescriptionsTo("collection,bean")
			.build().createSession();
		
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean'},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}
	
	@Test
	public void allowNestedDescriptions_disabled() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
			.addDescriptionsTo("collection,bean")
			.build().createSession();
		
		assertObjectEquals("{type:'array',items:{type:'object',properties:{f1:{type:'string'}}},description:'org.apache.juneau.json.JsonSchemaSerializerTest$BeanList<org.apache.juneau.json.JsonSchemaSerializerTest$SimpleBean>'}", s.getSchema(BeanList.class));
	}

	//====================================================================================================
	// Swaps
	//====================================================================================================

	@Test
	public void swaps_int() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().build().createSession();
		assertObjectEquals("{type:'foo',format:'bar',properties:{f1:{type:'integer',format:'int32'}},description:'baz','x-example':{f1:123}}", s.getSchema(A1.class));
	}
	
	@JsonSchema(type="foo",format="bar",description="baz",example="{f1:123}")
	public static class A1 {
		public int f1;
	}
	
	@Test
	public void jsonSchema_onbeanfield() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().build().createSession();
		assertObjectEquals("{type:'object',properties:{f1:{type:'foo',format:'bar',description:'baz','x-example':123}}}", s.getSchema(A2.class));
	}
	
	public static class A2 {
		@JsonSchema(type="foo",format="bar",description="baz",example="123")
		public int f1;
	}

	@Test
	public void jsonSchema_onbeangetter() throws Exception {
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder().build().createSession();
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
		JsonSchemaSerializerSession s = JsonSchemaSerializer.DEFAULT_LAX.builder()
			.pojoSwaps(SwapWithAnnotation.class)
			.build().createSession();
		assertObjectEquals("{type:'foo',format:'bar',description:'baz','x-example':123}", s.getSchema(SimpleBean.class));
		assertObjectEquals("{type:'array',items:{type:'foo',format:'bar',description:'baz','x-example':123}}", s.getSchema(BeanList.class));
		assertObjectEquals("{type:'array',items:{type:'array',items:{type:'foo',format:'bar',description:'baz','x-example':123}}}", s.getSchema(SimpleBean[][].class));
	}
	
	@JsonSchema(type="foo",format="bar",description="baz",example="123")
	public static class SwapWithAnnotation extends PojoSwap<SimpleBean,Integer> {}
}