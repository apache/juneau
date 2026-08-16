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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Contract test for the {@code debugMarshalling} annotation attribute (TODO-372a).
 *
 * <p>
 * Verifies mechanical parity across all eight REST annotation types &mdash; {@link Rest} plus {@link RestOp} and the
 * six HTTP-method convenience annotations &mdash; and their mutable {@code *Annotation} implementations:
 * <ul>
 * 	<li>each annotation interface defaults {@code debugMarshalling()} to {@code ""} (blank = inherit); and
 * 	<li>each {@code *Annotation} fluent builder stores and exposes {@code "true"}, {@code "false"}, and {@code ""}.
 * </ul>
 *
 * @since 10.0.0
 */
class DebugMarshalling_AnnotationContract_Test extends TestBase {

	private static Object defaultOf(Class<? extends Annotation> annotationType) throws Exception {
		return annotationType.getMethod("debugMarshalling").getDefaultValue();
	}

	@Test void a01_allInterfaceDefaultsAreBlank() throws Exception {
		assertEquals("", defaultOf(Rest.class));
		assertEquals("", defaultOf(RestOp.class));
		assertEquals("", defaultOf(RestGet.class));
		assertEquals("", defaultOf(RestPost.class));
		assertEquals("", defaultOf(RestPut.class));
		assertEquals("", defaultOf(RestPatch.class));
		assertEquals("", defaultOf(RestDelete.class));
		assertEquals("", defaultOf(RestOptions.class));
	}

	@Test void a02_restAnnotationBuilderRoundTrip() {
		assertEquals("", RestAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestAnnotation.create().debugMarshalling("false").build().debugMarshalling());
		assertEquals("", RestAnnotation.create().debugMarshalling("").build().debugMarshalling());
	}

	@Test void a03_restOpAnnotationBuilderRoundTrip() {
		assertEquals("", RestOpAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestOpAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestOpAnnotation.create().debugMarshalling("false").build().debugMarshalling());
		assertEquals("", RestOpAnnotation.create().debugMarshalling("").build().debugMarshalling());
	}

	@Test void a04_restGetAnnotationBuilderRoundTrip() {
		assertEquals("", RestGetAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestGetAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestGetAnnotation.create().debugMarshalling("false").build().debugMarshalling());
	}

	@Test void a05_restPostAnnotationBuilderRoundTrip() {
		assertEquals("", RestPostAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestPostAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestPostAnnotation.create().debugMarshalling("false").build().debugMarshalling());
	}

	@Test void a06_restPutAnnotationBuilderRoundTrip() {
		assertEquals("", RestPutAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestPutAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestPutAnnotation.create().debugMarshalling("false").build().debugMarshalling());
	}

	@Test void a07_restPatchAnnotationBuilderRoundTrip() {
		assertEquals("", RestPatchAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestPatchAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestPatchAnnotation.create().debugMarshalling("false").build().debugMarshalling());
	}

	@Test void a08_restDeleteAnnotationBuilderRoundTrip() {
		assertEquals("", RestDeleteAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestDeleteAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestDeleteAnnotation.create().debugMarshalling("false").build().debugMarshalling());
	}

	@Test void a09_restOptionsAnnotationBuilderRoundTrip() {
		assertEquals("", RestOptionsAnnotation.create().build().debugMarshalling());
		assertEquals("true", RestOptionsAnnotation.create().debugMarshalling("true").build().debugMarshalling());
		assertEquals("false", RestOptionsAnnotation.create().debugMarshalling("false").build().debugMarshalling());
	}
}
