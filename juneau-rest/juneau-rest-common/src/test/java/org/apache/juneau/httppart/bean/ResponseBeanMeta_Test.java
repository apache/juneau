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
package org.apache.juneau.httppart.bean;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.uon.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ResponseBeanMeta}, including white-box {@link ResponseBeanMeta.Builder} coverage
 * (package-private: no public-API widening) of branches unreachable through the public
 * {@code create(...)} factories alone.
 */
class ResponseBeanMeta_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Builder.apply(Response) / apply(StatusCode) -- null-defensive branches
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_builder_applyResponse_null_isNoOp() {
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		assertSame(b, b.apply((Response)null));
	}

	@Test void a02_builder_applyStatusCode_null_isNoOp() {
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		assertSame(b, b.apply((StatusCode)null));
		assertEquals(0, b.code);
	}

	@Test void a03_builder_applyStatusCode_emptyValueArray_isNoOp() throws Exception {
		// Bare @StatusCode (no explicit value) yields an int[0] -- the "value().length > 0" branch's false side.
		var a = BareStatusCode.class.getMethod("m").getAnnotation(StatusCode.class);
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		b.apply(a);
		assertEquals(0, b.code);
	}

	@Test void a04_builder_applyStatusCode_withValue_setsCode() throws Exception {
		var a = ExplicitStatusCode.class.getMethod("m").getAnnotation(StatusCode.class);
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		b.apply(a);
		assertEquals(201, b.code);
	}

	//------------------------------------------------------------------------------------------------------------------
	// create(ParameterInfo, ...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_create_parameter_notAnnotated_returnsNull() throws Exception {
		var p = ParameterInfo.of(ParamFixtures.class.getMethod("plain", String.class).getParameters()[0]);
		assertNull(ResponseBeanMeta.create(p, AnnotationWorkList.create()));
	}

	@Test void b02_create_parameter_annotated_returnsMeta() throws Exception {
		var p = ParameterInfo.of(ParamFixtures.class.getMethod("annotated", ResponseBody.class).getParameters()[0]);
		var meta = ResponseBeanMeta.create(p, AnnotationWorkList.create());
		assertNotNull(meta);
	}

	@Test void b03_create_parameter_withStatusCode_appliesCode() throws Exception {
		var p = ParameterInfo.of(ParamFixtures.class.getMethod("annotatedWithStatus", ResponseBody.class).getParameters()[0]);
		var meta = ResponseBeanMeta.create(p, AnnotationWorkList.create());
		assertNotNull(meta);
		assertEquals(201, meta.getCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// BUG (not silently fixed): Builder.partSerializer/partParser (raw Class<?> fields) are
	// declared but no code path in this class -- nor the @Response/@StatusCode apply() overloads, nor any of the
	// three public create(...) factories -- ever assigns them from an @HttpPartMarshalling annotation (the comment
	// on Builder.apply(Response) claims this migration happened, but the actual wiring is missing). The identical
	// gap exists in the sibling RequestBeanMeta.Builder (its serializer/parser BeanInstantiator.Builder fields are
	// likewise never configured). As a result, ResponseBeanMeta.create(...)'s getPartSerializer() is *always*
	// Optional.empty() in production. This test exercises the otherwise-dead map() lambda the only way currently
	// possible -- setting the package-private Builder fields directly -- to pin what happens if/when that wiring is
	// eventually added, without fixing the gap itself.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_builder_partSerializerAndParser_wireIntoOptionals_whenSetDirectly() {
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		b.partSerializer = UonSerializer.class;
		b.partParser = UonParser.class;
		var meta = b.build();
		assertTrue(meta.getPartSerializer().isPresent());
		assertInstanceOf(UonSerializer.class, meta.getPartSerializer().get());
	}

	@Test void c02_create_normalFlow_partSerializerNeverWired() {
		// Pinning the bug: even a fully-annotated @Response class never gets a partSerializer via create(...).
		var meta = ResponseBeanMeta.create(ResponseBody.class, AnnotationWorkList.create());
		assertNotNull(meta);
		assertTrue(meta.getPartSerializer().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ctor -- "getter != null" guards on the content/status property, both currently unreachable through the
	// public create(...) factories (which always populate a ResponseBeanPropertyMeta.Builder's getter from a real
	// reflected Method). Exercised here via the white-box Builder path only.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_ctor_contentMethod_nullGetter_notAddedToProperties() {
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		b.cm = MarshallingContext.DEFAULT.getClassMeta(ResponseBody.class);
		b.contentMethod = new ResponseBeanPropertyMeta.Builder().partType(org.apache.juneau.commons.httppart.HttpPartType.RESPONSE_BODY).getter(null);
		var meta = b.build();
		assertNotNull(meta.getContentMethod());
		assertNull(meta.getContentMethod().getGetter());
		assertTrue(meta.getProperties().isEmpty());
	}

	@Test void d02_ctor_statusMethod_nullGetter_notAddedToProperties() {
		var b = new ResponseBeanMeta.Builder(AnnotationWorkList.create());
		b.cm = MarshallingContext.DEFAULT.getClassMeta(ResponseBody.class);
		b.statusMethod = new ResponseBeanPropertyMeta.Builder().partType(org.apache.juneau.commons.httppart.HttpPartType.RESPONSE_STATUS).getter(null);
		var meta = b.build();
		assertNull(meta.getContentMethod());
		assertTrue(meta.getProperties().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// getProperties()
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_getProperties_returnsContentMethod() throws Exception {
		var p = ParameterInfo.of(ParamFixtures.class.getMethod("annotated", ResponseBody.class).getParameters()[0]);
		var meta = ResponseBeanMeta.create(p, AnnotationWorkList.create());
		assertNotNull(meta);
		assertEquals(1, meta.getProperties().size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//------------------------------------------------------------------------------------------------------------------

	static class BareStatusCode {
		@StatusCode public void m() {}
	}

	static class ExplicitStatusCode {
		@StatusCode(201) public void m() {}
	}

	@Response
	static class ResponseBody {
		@Content public String getBody() { return "x"; }
	}

	static class ParamFixtures {
		@SuppressWarnings({
			"unused" // Parameter exists only to be inspected via reflection; the method itself is never invoked.
		})
		public void plain(String s) {}

		@SuppressWarnings({
			"unused" // Parameter exists only to be inspected via reflection; the method itself is never invoked.
		})
		public void annotated(@Response ResponseBody b) {}

		@SuppressWarnings({
			"unused" // Parameter exists only to be inspected via reflection; the method itself is never invoked.
		})
		public void annotatedWithStatus(@Response @StatusCode(201) ResponseBody b) {}
	}
}
