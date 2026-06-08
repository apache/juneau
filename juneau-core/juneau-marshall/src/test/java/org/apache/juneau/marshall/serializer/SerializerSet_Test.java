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
package org.apache.juneau.marshall.serializer;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"resource"    // Stream/reader instances are intentional short-lived test fixtures; auto-close not required for these assertions.
})
class SerializerSet_Test extends TestBase {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test void a01_serializerGroupMatching() {

		var sg = SerializerSet.create().add(SA1.class, SA2.class, SA3.class).build();
		assertInstanceOf(SA1.class, sg.getSerializer("text/foo"));
		assertInstanceOf(SA1.class, sg.getSerializer("text/foo_a"));
		assertInstanceOf(SA1.class, sg.getSerializer("text/xxx+foo_a"));
		assertInstanceOf(SA1.class, sg.getSerializer("text/foo_a+xxx"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/foo+bar"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/foo+bar_a"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/bar+foo"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/bar_a+foo"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/bar+foo+xxx"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/bar_a+foo+xxx"));
		assertInstanceOf(SA3.class, sg.getSerializer("text/baz"));
		assertInstanceOf(SA3.class, sg.getSerializer("text/baz_a"));
		assertInstanceOf(SA3.class, sg.getSerializer("text/baz+yyy"));
		assertInstanceOf(SA3.class, sg.getSerializer("text/baz_a+yyy"));
		assertInstanceOf(SA3.class, sg.getSerializer("text/yyy+baz"));
		assertInstanceOf(SA3.class, sg.getSerializer("text/yyy+baz_a"));

		assertInstanceOf(SA1.class, sg.getSerializer("text/foo;q=0.9,text/foo+bar;q=0.8"));
		assertInstanceOf(SA2.class, sg.getSerializer("text/foo;q=0.8,text/foo+bar;q=0.9"));
	}

	public static class SA1 extends JsonSerializer {
		public SA1(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/foo+*,text/foo_a+*"));
		}
	}

	public static class SA2 extends JsonSerializer {
		public SA2(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/foo+bar+*,text/foo+bar_a+*"));
		}
	}

	public static class SA3 extends JsonSerializer {
		public SA3(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/baz+*,text/baz_a+*"));
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test void a02_inheritence() {
		var gb = SerializerSet.create().add(SB1.class, SB2.class);
		var g = gb.build();
		assertList(g.getSupportedMediaTypes(), "text/1", "text/2", "text/2a");

		gb = SerializerSet.create().add(SB1.class, SB2.class).add(SB3.class, SB4.class);
		g = gb.build();
		assertList(g.getSupportedMediaTypes(), "text/3", "text/4", "text/4a", "text/1", "text/2", "text/2a");

		gb = SerializerSet.create().add(SB1.class, SB2.class).add(SB3.class, SB4.class).add(SB5.class);
		g = gb.build();
		assertList(g.getSupportedMediaTypes(), "text/5", "text/3", "text/4", "text/4a", "text/1", "text/2", "text/2a");
	}

	public static class SB1 extends JsonSerializer {
		public SB1(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/1"));
		}
	}

	public static class SB2 extends JsonSerializer {
		public SB2(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/2,text/2a"));
		}
	}

	public static class SB3 extends JsonSerializer {
		public SB3(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/3"));
		}
	}

	public static class SB4 extends JsonSerializer {
		public SB4(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/4,text/4a"));
		}
	}

	public static class SB5 extends JsonSerializer {
		public SB5(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/5"));
		}
	}

	//====================================================================================================
	// Test media type with meta-characters
	//====================================================================================================
	@Test void a03_mediaTypesWithMetaCharacters() {
		var gb = SerializerSet.create().add(SC1.class, SC2.class, SC3.class);
		var g = gb.build();
		assertInstanceOf(SC1.class, g.getSerializer("text/foo"));
		assertInstanceOf(SC2.class, g.getSerializer("foo/json"));
		assertInstanceOf(SC3.class, g.getSerializer("foo/foo"));
	}

	public static class SC1 extends JsonSerializer {
		public SC1(JsonSerializer.Builder<?> builder) {
			super(builder.accept("text/*"));
		}
	}

	public static class SC2 extends JsonSerializer {
		public SC2(JsonSerializer.Builder<?> builder) {
			super(builder.accept("*/json"));
		}
	}

	public static class SC3 extends JsonSerializer {
		public SC3(JsonSerializer.Builder<?> builder) {
			super(builder.accept("*/*"));
		}
	}

	public static class SimpleSerializer extends JsonSerializer {
		public SimpleSerializer() {
			super(JsonSerializer.create().accept("text/simple"));
		}
	}

	//====================================================================================================
	// Builder edge-case coverage
	//====================================================================================================

	@Test void b01_builder_addInstancesDirectly() {
		var instance = new SB1(JsonSerializer.create().accept("text/1"));
		var s = SerializerSet.create().add(instance).build();
		assertInstanceOf(SB1.class, s.getSerializer("text/1"));
	}

	@Test void b02_builder_addInvalidClassThrows() {
		assertThrows(RuntimeException.class, () ->
			SerializerSet.create().add(String.class));
	}

	@Test void b03_builder_setWithInherit() {
		var sb = SerializerSet.create().add(SB1.class, SB2.class);
		sb.set(SerializerSet.Inherit.class, SB3.class);
		var s = sb.build();
		assertInstanceOf(SB1.class, s.getSerializer("text/1"));
		assertInstanceOf(SB2.class, s.getSerializer("text/2"));
		assertInstanceOf(SB3.class, s.getSerializer("text/3"));
	}

	@Test void b04_builder_setWithInvalidClassThrows() {
		assertThrows(RuntimeException.class, () ->
			SerializerSet.create().set(String.class));
	}

	@Test void b05_builder_implBypassesBuild() {
		var preset = SerializerSet.create().add(SB1.class).build();
		var s = SerializerSet.create().impl(preset).build();
		assertSame(preset, s);
	}

	@Test void b06_builder_inner_returnsEntries() {
		var sb = SerializerSet.create().add(SB1.class);
		assertFalse(sb.inner().isEmpty());
	}

	@Test void b07_builder_toStringWithBuilder() {
		var sb = SerializerSet.create().add(JsonSerializer.class);
		assertTrue(sb.toString().contains("builder:"));
	}

	@Test void b08_builder_toStringWithInstance() {
		var sb = SerializerSet.create().add(new SB1(JsonSerializer.create().accept("text/1")));
		assertTrue(sb.toString().contains("serializer:"));
	}

	@Test void b09_builder_toStringWithNull() {
		var sb = SerializerSet.create();
		sb.inner().add(null);
		assertTrue(sb.toString().contains("null"));
	}

	@Test void b10_builder_create_withBeanStore() {
		var bs = new BasicBeanStore(null);
		var sb = SerializerSet.create(bs);
		assertSame(bs, sb.beanStore());
	}

	@Test void b11_builder_copy_isIndependent() {
		var sb1 = SerializerSet.create().add(SB1.class);
		var sb2 = sb1.copy();
		sb2.add(SB2.class);
		assertNotNull(sb2.build().getSerializer("text/2"));
		assertNull(sb1.build().getSerializer("text/2"));
	}

	@Test void b12_builder_beanContext_propagatesToBuilders() {
		var bcb = MarshallingContext.create();
		var sb = SerializerSet.create().add(JsonSerializer.class);
		sb.marshallingContext(bcb);
		assertNotNull(sb.beanStore());
	}

	@Test void b13_builder_beanContext_consumer_withNonNullBcBuilder() {
		boolean[] called = {false};
		var sb = SerializerSet.create().add(JsonSerializer.class);
		sb.marshallingContext(MarshallingContext.create());
		sb.marshallingContext((MarshallingContext.Builder b) -> called[0] = true);
		assertTrue(called[0]);
	}

	@Test void b14_builder_beanContext_consumer_withNullBcBuilder_isNoop() {
		boolean[] called = {false};
		var sb = SerializerSet.create().add(JsonSerializer.class);
		sb.marshallingContext((MarshallingContext.Builder b) -> called[0] = true);
		assertFalse(called[0]);
	}

	@Test void b15_builder_forEachWS_actsOnWriterSerializers() {
		int[] count = {0};
		SerializerSet.create().add(JsonSerializer.class).forEachWS(b -> count[0]++);
		assertEquals(1, count[0]);
	}

	@Test void b16_builder_forEachOSS_noAction_whenNoOSSEntries() {
		int[] count = {0};
		SerializerSet.create().add(JsonSerializer.class).forEachOSS(b -> count[0]++);
		assertEquals(0, count[0]);
	}

	@Test void b17_builder_copy_withBcBuilder_propagatesBcBuilder() {
		var sb = SerializerSet.create().add(JsonSerializer.class);
		sb.marshallingContext(MarshallingContext.create());
		var copy = sb.copy();
		assertNotNull(copy.beanStore());
	}

	@Test void b18_builder_addAfterBcBuilder_propagatesContext() {
		var sb = SerializerSet.create();
		sb.marshallingContext(MarshallingContext.create());
		sb.add(JsonSerializer.class);
		assertFalse(sb.inner().isEmpty());
	}

	@Test void b19_serializerSet_copy_returnsNewBuilder() {
		var s = SerializerSet.create().add(SB1.class).build();
		var copy = s.copy();
		assertInstanceOf(SB1.class, copy.build().getSerializer("text/1"));
	}

	@Test void b20_getSerializer_byMediaType() {
		var s = SerializerSet.create().add(SB1.class).build();
		assertInstanceOf(SB1.class, s.getSerializer(MediaType.of("text/1")));
		assertNull(s.getSerializer(MediaType.of("text/unknown")));
	}

	@Test void b21_getSerializer_nullMediaType_returnsNull() {
		var s = SerializerSet.create().add(SB1.class).build();
		assertNull(s.getSerializer((MediaType) null));
	}

	@Test void b22_builder_clear_removesAllEntries() {
		var sb = SerializerSet.create().add(SB1.class, SB2.class);
		assertEquals(2, sb.inner().size());
		sb.clear();
		assertTrue(sb.inner().isEmpty());
	}

	@Test void b23_getSerializerMatch_byMediaType() {
		var s = SerializerSet.create().add(SB1.class).build();
		assertNotNull(s.getSerializerMatch(MediaType.of("text/1")));
	}

	@Test void b24_getSerializerMatch_nullString_returnsNull() {
		var s = SerializerSet.create().add(SB1.class).build();
		assertNull(s.getSerializerMatch((String) null));
	}

	@Test void b25_getWriterSerializer_byMediaType() {
		var s = SerializerSet.create().add(SB1.class).build();
		assertInstanceOf(SB1.class, s.getWriterSerializer(MediaType.of("text/1")));
	}

	@Test void b26_add_serializerWithNoArgConstructor_instantiatesDirectly() {
		var s = SerializerSet.create().add(SimpleSerializer.class).build();
		assertInstanceOf(SimpleSerializer.class, s.getSerializer("text/simple"));
	}

	@Test void b27_copy_withSerializerInstance_coversNonBuilderBranch() {
		var instance = new SimpleSerializer();
		var sb = SerializerSet.create().add(instance);
		var copy = sb.copy();
		assertFalse(copy.inner().isEmpty());
	}

	@Test void b28_getStreamSerializer_withNoMatch_returnsNull() {
		var s = SerializerSet.create().add(SB1.class).build();
		assertNull(s.getStreamSerializer(MediaType.of("text/unknown")));
		assertNull(s.getStreamSerializer("text/unknown"));
	}
}