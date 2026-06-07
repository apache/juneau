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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class ParserSet_Test extends TestBase {

	//====================================================================================================
	// Test parser group matching
	//====================================================================================================
	@Test void a01_parserGroupMatching() {

		var s = ParserSet.create().add(Parser1.class, Parser2.class, Parser3.class).build();
		assertInstanceOf(Parser1.class, s.getParser("text/foo"));
		assertInstanceOf(Parser1.class, s.getParser("text/foo_a"));
		assertInstanceOf(Parser1.class, s.getParser("text/foo_a+xxx"));
		assertInstanceOf(Parser1.class, s.getParser("text/xxx+foo_a"));
		assertInstanceOf(Parser2.class, s.getParser("text/foo+bar"));
		assertInstanceOf(Parser2.class, s.getParser("text/foo+bar_a"));
		assertInstanceOf(Parser2.class, s.getParser("text/bar+foo"));
		assertInstanceOf(Parser2.class, s.getParser("text/bar+foo+xxx"));
		assertInstanceOf(Parser3.class, s.getParser("text/baz"));
		assertInstanceOf(Parser3.class, s.getParser("text/baz_a"));
		assertInstanceOf(Parser3.class, s.getParser("text/baz+yyy"));
		assertInstanceOf(Parser3.class, s.getParser("text/baz_a+yyy"));
		assertInstanceOf(Parser3.class, s.getParser("text/yyy+baz"));
		assertInstanceOf(Parser3.class, s.getParser("text/yyy+baz_a"));
	}

	public static class Parser1 extends JsonParser { public Parser1(JsonParser.Builder<?> b) { super(b.consumes("text/foo,text/foo_a")); }}
	public static class Parser2 extends JsonParser { public Parser2(JsonParser.Builder<?> b) { super(b.consumes("text/foo+bar,text/foo+bar_a")); }}
	public static class Parser3 extends JsonParser { public Parser3(JsonParser.Builder<?> b) { super(b.consumes("text/baz,text/baz_a")); }}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test void a02_inheritence() {
		var sb = ParserSet.create().add(P1.class, P2.class);
		var s = sb.build();
		assertList(s.getSupportedMediaTypes(), "text/1", "text/2", "text/2a");

		sb = ParserSet.create().add(P1.class, P2.class).add(P3.class, P4.class);
		s = sb.build();
		assertList(s.getSupportedMediaTypes(), "text/3", "text/4", "text/4a", "text/1", "text/2", "text/2a");

		sb = ParserSet.create().add(P1.class, P2.class).add(P3.class, P4.class).add(P5.class);
		s = sb.build();
		assertList(s.getSupportedMediaTypes(), "text/5", "text/3", "text/4", "text/4a", "text/1", "text/2", "text/2a");
	}

	public static class P1 extends JsonParser { public P1(JsonParser.Builder<?> b) { super(b.consumes("text/1")); }}
	public static class P2 extends JsonParser { public P2(JsonParser.Builder<?> b) { super(b.consumes("text/2,text/2a")); }}
	public static class P3 extends JsonParser { public P3(JsonParser.Builder<?> b) { super(b.consumes("text/3")); }}
	public static class P4 extends JsonParser { public P4(JsonParser.Builder<?> b) { super(b.consumes("text/4,text/4a"));} }
	public static class P5 extends JsonParser { public P5(JsonParser.Builder<?> b) { super(b.consumes("text/5"));}}

	public static class SimpleParser extends JsonParser {
		public SimpleParser() {
			super(JsonParser.create().consumes("text/simple"));
		}
	}

	//====================================================================================================
	// Builder edge-case coverage
	//====================================================================================================

	@Test void b01_builder_addInstancesDirectly() {
		var instance = new P1(JsonParser.create().consumes("text/1"));
		var s = ParserSet.create().add(instance).build();
		assertInstanceOf(P1.class, s.getParser("text/1"));
	}

	@Test void b02_builder_clear_removesAllEntries() {
		var sb = ParserSet.create().add(P1.class, P2.class);
		assertEquals(2, sb.inner().size());
		sb.clear();
		assertTrue(sb.inner().isEmpty());
	}

	@Test void b03_builder_addInvalidClassThrows() {
		assertThrows(RuntimeException.class, () ->
			ParserSet.create().add(String.class));
	}

	@Test void b04_builder_setWithInherit() {
		var sb = ParserSet.create().add(P1.class, P2.class);
		sb.set(ParserSet.Inherit.class, P3.class);
		var s = sb.build();
		assertInstanceOf(P1.class, s.getParser("text/1"));
		assertInstanceOf(P2.class, s.getParser("text/2"));
		assertInstanceOf(P3.class, s.getParser("text/3"));
	}

	@Test void b05_builder_setWithInvalidClassThrows() {
		assertThrows(RuntimeException.class, () ->
			ParserSet.create().set(String.class));
	}

	@Test void b06_builder_implBypassesBuild() {
		var preset = ParserSet.create().add(P1.class).build();
		var s = ParserSet.create().impl(preset).build();
		assertSame(preset, s);
	}

	@Test void b07_builder_inner_returnsEntries() {
		var sb = ParserSet.create().add(P1.class);
		assertFalse(sb.inner().isEmpty());
	}

	@Test void b08_builder_toStringWithBuilder() {
		var sb = ParserSet.create().add(JsonParser.class);
		assertTrue(sb.toString().contains("builder:"));
	}

	@Test void b09_builder_toStringWithInstance() {
		var sb = ParserSet.create().add(new P1(JsonParser.create().consumes("text/1")));
		assertTrue(sb.toString().contains("parser:"));
	}

	@Test void b10_builder_toStringWithNull() {
		var sb = ParserSet.create();
		sb.inner().add(null);
		assertTrue(sb.toString().contains("null"));
	}

	@Test void b11_builder_create_withBeanStore() {
		var bs = new BasicBeanStore(null);
		var sb = ParserSet.create(bs);
		assertSame(bs, sb.beanStore());
	}

	@Test void b12_builder_copy_isIndependent() {
		var sb1 = ParserSet.create().add(P1.class);
		var sb2 = sb1.copy();
		sb2.add(P2.class);
		assertTrue(sb2.build().getParser("text/2") instanceof P2);
		assertNull(sb1.build().getParser("text/2"));
	}

	@Test void b13_builder_beanContext_propagatesToBuilders() {
		var bcb = MarshallingContext.create();
		var sb = ParserSet.create().add(JsonParser.class);
		sb.marshallingContext(bcb);
		assertNotNull(sb.beanStore());
	}

	@Test void b14_builder_beanContext_consumer_withNonNullBcBuilder() {
		boolean[] called = {false};
		var sb = ParserSet.create().add(JsonParser.class);
		sb.marshallingContext(MarshallingContext.create());
		sb.marshallingContext((MarshallingContext.Builder b) -> called[0] = true);
		assertTrue(called[0]);
	}

	@Test void b15_builder_beanContext_consumer_withNullBcBuilder_isNoop() {
		boolean[] called = {false};
		var sb = ParserSet.create().add(JsonParser.class);
		// bcBuilder is null by default — consumer should NOT be called
		sb.marshallingContext((MarshallingContext.Builder b) -> called[0] = true);
		assertFalse(called[0]);
	}

	@Test void b16_builder_forEachRP_actsOnReaderParsers() {
		int[] count = {0};
		ParserSet.create().add(JsonParser.class).forEachRP(b -> count[0]++);
		assertEquals(1, count[0]);
	}

	@Test void b17_builder_forEachISP_noAction_whenNoISPEntries() {
		int[] count = {0};
		ParserSet.create().add(JsonParser.class).forEachISP(b -> count[0]++);
		assertEquals(0, count[0]);
	}

	@Test void b18_builder_copy_withBcBuilder_propagatesBcBuilder() {
		var sb = ParserSet.create().add(JsonParser.class);
		sb.marshallingContext(MarshallingContext.create());
		var copy = sb.copy();
		assertNotNull(copy.beanStore());
	}

	@Test void b19_builder_addAfterBcBuilder_propagatesContext() {
		var sb = ParserSet.create();
		sb.marshallingContext(MarshallingContext.create());
		sb.add(JsonParser.class);
		assertFalse(sb.inner().isEmpty());
	}

	@Test void b20_parserSet_copy_returnsNewBuilder() {
		var s = ParserSet.create().add(P1.class).build();
		var copy = s.copy();
		assertInstanceOf(P1.class, copy.build().getParser("text/1"));
	}

	@Test void b21_getParser_byMediaType() {
		var s = ParserSet.create().add(P1.class).build();
		assertInstanceOf(P1.class, s.getParser(MediaType.of("text/1")));
		assertNull(s.getParser(MediaType.of("text/unknown")));
	}

	@Test void b22_add_parserWithNoArgConstructor_instantiatesDirectly() {
		var s = ParserSet.create().add(SimpleParser.class).build();
		assertInstanceOf(SimpleParser.class, s.getParser("text/simple"));
	}

	@Test void b23_copy_withParserInstance_coversNonBuilderBranch() {
		var instance = new SimpleParser();
		var sb = ParserSet.create().add(instance);
		var copy = sb.copy();
		assertFalse(copy.inner().isEmpty());
	}
}