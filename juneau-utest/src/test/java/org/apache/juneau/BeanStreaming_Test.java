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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Integration tests for large-dataset streaming via {@link BeanSupplier}, {@link BeanConsumer},
 * {@link BeanChannel}, {@link ListBeanChannel}, and {@code Supplier<T>} unwrapping.
 */
class BeanStreaming_Test extends TestBase {

	// ====================================================================================================
	// Supplier<T> unwrapping tests
	// ====================================================================================================

	@Nested
	class A_supplierUnwrapping extends TestBase {

		@Test
		void a01_singleLevelSupplier() throws Exception {
			Supplier<String> a = () -> "hello";
			var json = Json5Serializer.DEFAULT.serialize(a);
			assertEquals("'hello'", json);
		}

		@Test
		void a02_nestedSupplier() throws Exception {
			Supplier<Supplier<String>> a = () -> () -> "nested";
			var json = Json5Serializer.DEFAULT.serialize(a);
			assertEquals("'nested'", json);
		}

		@Test
		void a03_supplierReturningNull() throws Exception {
			Supplier<String> a = () -> null;
			var json = Json5Serializer.DEFAULT.serialize(a);
			assertEquals("null", json);
		}

		@Test
		void a04_supplierReturningList() throws Exception {
			Supplier<List<String>> a = () -> list("x", "y", "z");
			var json = Json5Serializer.DEFAULT.serialize(a);
			assertEquals("['x','y','z']", json);
		}

		@Test
		void a05_supplierDepthExceeds10_throws() {
			// Create a chain of 12 nested suppliers
			Supplier<?> chain = () -> "bottom";
			for (int i = 0; i < 11; i++) {
				final Supplier<?> prev = chain;
				chain = () -> prev;
			}
			final Supplier<?> deep = chain;
			assertThrows(SerializeException.class, () -> Json5Serializer.DEFAULT.serialize(deep));
		}

		@Test
		void a06_beanSupplier_notUnwrapped_treatedAsIterable() throws Exception {
			var channel = new ListBeanChannel<String>();
			channel.acceptThrows("a");
			channel.acceptThrows("b");
			var json = Json5Serializer.DEFAULT.serialize((BeanSupplier<String>) channel);
			assertEquals("['a','b']", json);
		}
	}

	// ====================================================================================================
	// BeanSupplier serialization lifecycle tests
	// ====================================================================================================

	@Nested
	class B_beanSupplierSerialization extends TestBase {

		@Test
		void b01_beanSupplier_lifecycle_begin_complete_called() throws Exception {
			var lifecycleLog = new ArrayList<String>();
			BeanSupplier<String> a = new BeanSupplier<>() {
				@Override public void begin() throws Exception { lifecycleLog.add("begin"); }
				@Override public Iterator<String> iterator() { return list("x", "y").iterator(); }
				@Override public void complete() throws Exception { lifecycleLog.add("complete"); }
			};
			var json = Json5Serializer.DEFAULT.serialize(a);
			assertEquals("['x','y']", json);
			assertEquals(list("begin", "complete"), lifecycleLog);
		}

		@Test
		void b02_beanSupplier_onError_called_on_iteration_failure() throws Exception {
			var lifecycleLog = new ArrayList<String>();
			BeanSupplier<String> a = new BeanSupplier<>() {
				@Override public void begin() throws Exception { lifecycleLog.add("begin"); }
				@Override public Iterator<String> iterator() {
					return new Iterator<>() {
						int i = 0;
						@Override public boolean hasNext() { return i < 3; }
						@Override public String next() {
							if (++i == 2) throw new RuntimeException("fail at 2");
							return "item" + i;
						}
					};
				}
				@Override public void onError(Exception e) throws Exception {
					lifecycleLog.add("onError:" + e.getMessage());
					throw e;
				}
				@Override public void complete() throws Exception { lifecycleLog.add("complete"); }
			};
			assertThrows(Exception.class, () -> Json5Serializer.DEFAULT.serialize(a));
			assertTrue(lifecycleLog.contains("begin"), "begin should be called");
			assertTrue(lifecycleLog.contains("complete"), "complete should always be called");
			assertTrue(lifecycleLog.stream().anyMatch(s -> s.startsWith("onError")), "onError should be called");
		}

		@Test
		void b03_beanConsumer_used_as_serializer_source_throws() {
			BeanConsumer<String> a = item -> {};
			assertThrows(SerializeException.class, () -> Json5Serializer.DEFAULT.serialize(a));
		}
	}

	// ====================================================================================================
	// BeanConsumer parsing lifecycle tests
	// ====================================================================================================

	@Nested
	class C_beanConsumerParsing extends TestBase {

		@Test
		void c01_parseToBeanConsumer_basic() throws Exception {
			var received = new ArrayList<String>();
			BeanConsumer<String> a = item -> received.add(item);
			Json5Parser.DEFAULT.getSession().parseToBeanConsumer("['x','y','z']", a, String.class);
			assertEquals(list("x", "y", "z"), received);
		}

		@Test
		void c02_parseToBeanConsumer_begin_complete_called() throws Exception {
			var lifecycleLog = new ArrayList<String>();
			BeanConsumer<String> a = new BeanConsumer<>() {
				@Override public void begin() throws Exception { lifecycleLog.add("begin"); }
				@Override public void acceptThrows(String item) { lifecycleLog.add("accept:" + item); }
				@Override public void complete() throws Exception { lifecycleLog.add("complete"); }
			};
			Json5Parser.DEFAULT.getSession().parseToBeanConsumer("['x','y']", a, String.class);
			assertEquals(list("begin", "accept:x", "accept:y", "complete"), lifecycleLog);
		}

		@Test
		void c03_parseToBeanConsumer_onError_rethrow_stops_parsing() throws Exception {
			var lifecycleLog = new ArrayList<String>();
			BeanConsumer<String> a = new BeanConsumer<>() {
				@Override public void acceptThrows(String item) throws Exception {
					if ("bad".equals(item)) throw new Exception("bad item");
					lifecycleLog.add("accept:" + item);
				}
				@Override public void onError(Exception e) throws Exception {
					lifecycleLog.add("onError");
					throw e;
				}
				@Override public void complete() throws Exception { lifecycleLog.add("complete"); }
			};
			assertThrows(ParseException.class, () ->
				Json5Parser.DEFAULT.getSession().parseToBeanConsumer("['good','bad','ignored']", a, String.class));
			assertTrue(lifecycleLog.contains("complete"), "complete should always be called");
			assertTrue(lifecycleLog.contains("onError"), "onError should be called");
			assertFalse(lifecycleLog.contains("accept:ignored"), "parsing should stop after rethrow");
		}

		@Test
		void c04_parseToBeanConsumer_onError_absorb_continues_parsing() throws Exception {
			var received = new ArrayList<String>();
			var skipped = new ArrayList<String>();
			BeanConsumer<String> a = new BeanConsumer<>() {
				@Override public void acceptThrows(String item) throws Exception {
					if ("bad".equals(item)) throw new Exception("bad item");
					received.add(item);
				}
				@Override public void onError(Exception e) {
					skipped.add(e.getMessage());
				}
			};
			Json5Parser.DEFAULT.getSession().parseToBeanConsumer("['good','bad','also-good']", a, String.class);
			assertEquals(list("good", "also-good"), received);
			assertEquals(list("bad item"), skipped);
		}

		@Test
		void c05_beanSupplier_used_as_parser_target_throws() {
			BeanSupplier<String> a = () -> Collections.emptyIterator();
			assertThrows(ParseException.class, () ->
				Json5Parser.DEFAULT.getSession().parse("['x']", a.getClass()));
		}
	}

	// ====================================================================================================
	// ListBeanChannel round-trip tests
	// ====================================================================================================

	@Nested
	class D_listBeanChannelRoundTrip extends TestBase {

		@Test
		void d01_listBeanChannel_serialize() throws Exception {
			var a = new ListBeanChannel<String>();
			a.acceptThrows("one");
			a.acceptThrows("two");
			a.acceptThrows("three");
			var json = Json5Serializer.DEFAULT.serialize(a);
			assertEquals("['one','two','three']", json);
		}

		@Test
		void d02_listBeanChannel_parse_to_consumer() throws Exception {
			var a = new ListBeanChannel<String>();
			Json5Parser.DEFAULT.getSession().parseToBeanConsumer("['x','y','z']", a, String.class);
			assertEquals(list("x", "y", "z"), a.getList());
		}

		@Test
		void d03_listBeanChannel_roundTrip() throws Exception {
			var original = new ListBeanChannel<String>();
			original.acceptThrows("alpha");
			original.acceptThrows("beta");
			original.acceptThrows("gamma");

			var json = Json5Serializer.DEFAULT.serialize(original);

			var parsed = new ListBeanChannel<String>();
			Json5Parser.DEFAULT.getSession().parseToBeanConsumer(json, parsed, String.class);

			assertEquals(original.getList(), parsed.getList());
		}
	}

	// ====================================================================================================
	// @Bean(factory=) tests
	// ====================================================================================================

	@Nested
	class E_beanFactoryAnnotation extends TestBase {

		public static class A_SimpleBean {
			public String name;
			public A_SimpleBean() {}
			public A_SimpleBean(String name) { this.name = name; }
		}

		public static class A_SimpleFactory implements BeanFactory<A_SimpleBean> {
			@Override public A_SimpleBean create() { return new A_SimpleBean("from-factory"); }
		}

		@Test
		void e01_factory_class_can_be_instantiated_directly() throws Exception {
			var factory = new A_SimpleFactory();
			var bean = factory.create();
			assertEquals("from-factory", bean.name);
		}

		@Test @SuppressWarnings("rawtypes")
		void e02_beanFactory_void_sentinel() throws Exception {
			var ctor = BeanFactory.Void.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			var instance = (BeanFactory) ctor.newInstance();
			assertThrows(UnsupportedOperationException.class, instance::create);
		}
	}
}
