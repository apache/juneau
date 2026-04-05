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
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.XVar;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

class RdfAnnotation_Test extends TestBase {

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	@Test void a01_defaults() {
		var x = RdfAnnotation.create().build();
		assertFalse(x.beanUri());
		assertEquals(RdfCollectionFormat.DEFAULT, x.collectionFormat());
		assertEquals("", x.prefix());
		assertEquals("", x.namespace());
		assertEquals(0, x.on().length);
		assertEquals(0, x.onClass().length);
	}

	@Test void a02_beanUri() {
		var x = RdfAnnotation.create().beanUri(true).build();
		assertTrue(x.beanUri());
	}

	@Test void a03_beanUri_false() {
		var x = RdfAnnotation.create().beanUri(false).build();
		assertFalse(x.beanUri());
	}

	@Test void a04_collectionFormat_bag() {
		var x = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.BAG).build();
		assertEquals(RdfCollectionFormat.BAG, x.collectionFormat());
	}

	@Test void a05_collectionFormat_list() {
		var x = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.LIST).build();
		assertEquals(RdfCollectionFormat.LIST, x.collectionFormat());
	}

	@Test void a06_collectionFormat_seq() {
		var x = RdfAnnotation.create().collectionFormat(RdfCollectionFormat.SEQ).build();
		assertEquals(RdfCollectionFormat.SEQ, x.collectionFormat());
	}

	@Test void a07_namespace() {
		var x = RdfAnnotation.create().namespace("http://foo/").build();
		assertEquals("http://foo/", x.namespace());
	}

	@Test void a08_prefix() {
		var x = RdfAnnotation.create().prefix("foo").build();
		assertEquals("foo", x.prefix());
	}

	@Test void a09_on_class() {
		var x = RdfAnnotation.create().on(String.class).build();
		assertEquals("java.lang.String", x.on()[0]);
	}

	@Test void a10_on_string() {
		var x = RdfAnnotation.create().on("my.Class").build();
		assertEquals("my.Class", x.on()[0]);
	}

	@Test void a11_onClass() {
		var x = RdfAnnotation.create().onClass(String.class).build();
		assertEquals(String.class, x.onClass()[0]);
	}

	@Test void a12_on_multiple_classes() {
		var x = RdfAnnotation.create().on(String.class, Integer.class).build();
		assertEquals(2, x.on().length);
		assertEquals("java.lang.String", x.on()[0]);
		assertEquals("java.lang.Integer", x.on()[1]);
	}

	@Test void a13_prefix_and_namespace() {
		var x = RdfAnnotation.create().prefix("foo").namespace("http://foo/").build();
		assertEquals("foo", x.prefix());
		assertEquals("http://foo/", x.namespace());
	}

	@Test void a14_create_with_class() {
		var x = RdfAnnotation.create(String.class).build();
		assertEquals("java.lang.String", x.on()[0]);
	}

	@Test void a15_create_with_string() {
		var x = RdfAnnotation.create("my.ClassName").build();
		assertEquals("my.ClassName", x.on()[0]);
	}

	@Test void a16_default_constant() {
		assertNotNull(RdfAnnotation.DEFAULT);
		assertFalse(RdfAnnotation.DEFAULT.beanUri());
		assertEquals(RdfCollectionFormat.DEFAULT, RdfAnnotation.DEFAULT.collectionFormat());
	}

	@Test void a17_on_classInfo() {
		var x = RdfAnnotation.create().on(ClassInfo.of(String.class)).build();
		assertEquals("java.lang.String", x.on()[0]);
	}

	@Test void a18_onClass_classInfo() {
		var x = RdfAnnotation.create().onClass(ClassInfo.of(String.class)).build();
		assertEquals(String.class, x.onClass()[0]);
	}

	@Test void a19_on_fieldInfo() throws Exception {
		var field = String.class.getDeclaredField("value");
		var x = RdfAnnotation.create().on(FieldInfo.of(ClassInfo.of(String.class), field)).build();
		assertTrue(x.on().length > 0);
	}

	@Test void a20_on_methodInfo() throws Exception {
		var method = String.class.getDeclaredMethod("length");
		var x = RdfAnnotation.create().on(MethodInfo.of(ClassInfo.of(String.class), method)).build();
		assertTrue(x.on().length > 0);
	}

	@Test void a21_copy() {
		var a = RdfAnnotation.create().beanUri(true).collectionFormat(RdfCollectionFormat.BAG).prefix("foo").namespace("http://foo/").on("MyClass").build();
		var vr = VarResolver.DEFAULT.createSession();
		var copy = RdfAnnotation.copy(a, vr);
		assertTrue(copy.beanUri());
		assertEquals(RdfCollectionFormat.BAG, copy.collectionFormat());
		assertEquals("foo", copy.prefix());
		assertEquals("http://foo/", copy.namespace());
		assertEquals("MyClass", copy.on()[0]);
	}

	@Test void a22_on_field() throws Exception {
		var field = String.class.getDeclaredField("value");
		var x = RdfAnnotation.create().on(field).build();
		assertTrue(x.on().length > 0);
	}

	@Test void a23_on_method() throws Exception {
		var method = String.class.getDeclaredMethod("length");
		var x = RdfAnnotation.create().on(method).build();
		assertTrue(x.on().length > 0);
	}

	// Targeted @Rdf annotation (on() is non-empty) - exercises the non-early-return path in apply()
	@Rdf(on = "SomeBean")
	static class A24_TargetedOnClass {}

	@Test void a24_serializerApplier_targeted() {
		var al = AnnotationWorkList.of(sr, rstream(ClassInfo.of(A24_TargetedOnClass.class).getAnnotations()));
		assertDoesNotThrow(() -> RdfSerializer.create().apply(al).build());
	}

	@Test void a25_parserApplier_targeted() {
		var al = AnnotationWorkList.of(sr, rstream(ClassInfo.of(A24_TargetedOnClass.class).getAnnotations()));
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}

	// Empty @Rdf annotation (on() and onClass() both empty) - exercises the early-return path in apply()
	@Rdf
	static class A26_EmptyAnnotation {}

	@Test void a26_serializerApplier_empty() {
		var al = AnnotationWorkList.of(sr, rstream(ClassInfo.of(A26_EmptyAnnotation.class).getAnnotations()));
		assertDoesNotThrow(() -> RdfSerializer.create().apply(al).build());
	}

	@Test void a27_parserApplier_empty() {
		var al = AnnotationWorkList.of(sr, rstream(ClassInfo.of(A26_EmptyAnnotation.class).getAnnotations()));
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}

	// @Rdf with onClass() only (on() is empty, onClass() is non-empty) - covers A=true, B=false branch
	@Rdf(onClass = String.class)
	static class A28_OnlyOnClass {}

	@Test void a28_serializerApplier_onClass_only() {
		var al = AnnotationWorkList.of(sr, rstream(ClassInfo.of(A28_OnlyOnClass.class).getAnnotations()));
		assertDoesNotThrow(() -> RdfSerializer.create().apply(al).build());
	}

	@Test void a29_parserApplier_onClass_only() {
		var al = AnnotationWorkList.of(sr, rstream(ClassInfo.of(A28_OnlyOnClass.class).getAnnotations()));
		assertDoesNotThrow(() -> RdfParser.create().apply(al).build());
	}
}
