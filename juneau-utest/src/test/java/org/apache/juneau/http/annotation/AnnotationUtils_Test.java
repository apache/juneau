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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.oapi.*;
import org.junit.jupiter.api.*;

class AnnotationUtils_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test empty checks
	//-----------------------------------------------------------------------------------------------------------------

	@Target(TYPE)
	@Retention(RUNTIME)
	public @interface X1 {
		Contact contact() default @Contact;
		ExternalDocs externalDocs() default @ExternalDocs;
		License license() default @License;
		Schema schema() default @Schema;
		SubItems subItems() default @SubItems;
		Items items() default @Items;
	}

	@Content
	@Response
	@Header
	@X1
	public static class A1 {
		@Query @Header @FormData @Path @Schema
		public int f1;
	}

	public static class A2 {
		public int f1;
	}

	@Test void a01_Body() {
		assertJsonContains(body().build().annotationType(), "Content");

		assertTrue(ContentAnnotation.empty(A1.class.getAnnotation(Content.class)));
		assertTrue(ContentAnnotation.empty(A2.class.getAnnotation(Content.class)));
		assertTrue(ContentAnnotation.empty(body().build()));
		assertTrue(ContentAnnotation.empty((Content)null));
	}

	@Test void a02_Contact() {
		X1 x1 = A1.class.getAnnotation(X1.class);

		assertJsonContains(contact().build().annotationType(), "Contact");

		assertTrue(ContactAnnotation.empty(x1.contact()));
		assertTrue(ContactAnnotation.empty(contact().build()));
		assertTrue(ContactAnnotation.empty((Contact)null));

		assertFalse(ContactAnnotation.empty(contact().email("foo").build()));
		assertFalse(ContactAnnotation.empty(contact().name("foo").build()));
		assertFalse(ContactAnnotation.empty(contact().url("foo").build()));
	}

	@Test void a03_FormData() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertJsonContains(formData().build().annotationType(), "FormData");

		assertTrue(FormDataAnnotation.empty(f1.getAnnotation(FormData.class)));
		assertTrue(FormDataAnnotation.empty(f2.getAnnotation(FormData.class)));
		assertTrue(FormDataAnnotation.empty((FormData)null));
		assertTrue(FormDataAnnotation.empty(formData().build()));

		assertFalse(FormDataAnnotation.empty(formData().name("foo").build()));
		assertFalse(FormDataAnnotation.empty(formData().parser(OpenApiParser.class).build()));
		assertFalse(FormDataAnnotation.empty(formData().serializer(OpenApiSerializer.class).build()));
		assertFalse(FormDataAnnotation.empty(formData().value("foo").build()));
	}

	@Test void a04_HasFormData() {
		assertJsonContains(hasFormData().build().annotationType(), "HasFormData");

		assertEquals("foo", hasFormData().name("foo").build().name());
		assertEquals("foo", hasFormData().value("foo").build().value());
	}

	@Test void a05_Query() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertJsonContains(query().build().annotationType(), "Query");

		assertTrue(QueryAnnotation.empty(f1.getAnnotation(Query.class)));
		assertTrue(QueryAnnotation.empty(f2.getAnnotation(Query.class)));
		assertTrue(QueryAnnotation.empty((Query)null));
		assertTrue(QueryAnnotation.empty(query().build()));

		assertFalse(QueryAnnotation.empty(query().name("foo").build()));
		assertFalse(QueryAnnotation.empty(query().parser(OpenApiParser.class).build()));
		assertFalse(QueryAnnotation.empty(query().serializer(OpenApiSerializer.class).build()));
		assertFalse(QueryAnnotation.empty(query().value("foo").build()));
	}

	@Test void a06_HasQuery() {
		assertJsonContains(hasQuery().build().annotationType(), "HasQuery");

		assertEquals("foo", hasQuery().name("foo").build().name());
		assertEquals("foo", hasQuery().value("foo").build().value());
	}

	@Test void a07_Header() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertJsonContains(header().build().annotationType(), "Header");

		assertTrue(HeaderAnnotation.empty(f1.getAnnotation(Header.class)));
		assertTrue(HeaderAnnotation.empty(f2.getAnnotation(Header.class)));
		assertTrue(HeaderAnnotation.empty((Header)null));
		assertTrue(HeaderAnnotation.empty(header().build()));

		assertFalse(HeaderAnnotation.empty(header().name("foo").build()));
		assertFalse(HeaderAnnotation.empty(header().parser(OpenApiParser.class).build()));
		assertFalse(HeaderAnnotation.empty(header().serializer(OpenApiSerializer.class).build()));
		assertFalse(HeaderAnnotation.empty(header().value("foo").build()));
	}

	@Test void a08_License() {
		X1 x = A1.class.getAnnotation(X1.class);

		assertJsonContains(license().build().annotationType(), "License");

		assertTrue(LicenseAnnotation.empty(x.license()));
		assertTrue(LicenseAnnotation.empty((License)null));
		assertTrue(LicenseAnnotation.empty(license().build()));

		assertFalse(LicenseAnnotation.empty(license().name("foo").build()));
		assertFalse(LicenseAnnotation.empty(license().url("foo").build()));
	}

	@Test void a09_Path() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertJsonContains(path().build().annotationType(), "Path");

		assertTrue(PathAnnotation.empty(f1.getAnnotation(Path.class)));
		assertTrue(PathAnnotation.empty(f2.getAnnotation(Path.class)));
		assertTrue(PathAnnotation.empty((Path)null));
		assertTrue(PathAnnotation.empty(path().build()));

		assertFalse(PathAnnotation.empty(path().name("foo").build()));
		assertFalse(PathAnnotation.empty(path().parser(OpenApiParser.class).build()));
		assertFalse(PathAnnotation.empty(path().serializer(OpenApiSerializer.class).build()));
		assertFalse(PathAnnotation.empty(path().value("foo").build()));
	}

	@Test void a10_Request() {
		assertJsonContains(request().build().annotationType(), "Request");

		assertString("org.apache.juneau.oapi.OpenApiParser", request().parser(OpenApiParser.class).build().parser().getName());
		assertString("org.apache.juneau.oapi.OpenApiSerializer", request().serializer(OpenApiSerializer.class).build().serializer().getName());
	}

	@Test void a11_Response() {
		assertJsonContains(response().build().annotationType(), "Response");

		assertTrue(ResponseAnnotation.empty(A1.class.getAnnotation(Response.class)));
		assertTrue(ResponseAnnotation.empty(A2.class.getAnnotation(Response.class)));
		assertTrue(ResponseAnnotation.empty(response().build()));
		assertTrue(ResponseAnnotation.empty((Response)null));

		assertFalse(ResponseAnnotation.empty(response().examples(a("foo")).build()));
		assertFalse(ResponseAnnotation.empty(response().headers(new Header[]{header().name("foo").build()}).build()));
		assertFalse(ResponseAnnotation.empty(response().parser(OpenApiParser.class).build()));
		assertFalse(ResponseAnnotation.empty(response().schema(schema().$ref("foo").build()).build()));
		assertFalse(ResponseAnnotation.empty(response().serializer(OpenApiSerializer.class).build()));
	}

	@Test void a14_ResponseStatus() {
		assertJsonContains(responseCode().build().annotationType(), "StatusCode");
	}

	@Test void a15_Tag() {
		assertJsonContains(tag().build().annotationType(), "Tag");

		assertArray(tag().description(a("foo")).build().description(), "foo");
		assertEquals("foo", tag().externalDocs(externalDocs().url("foo").build()).build().externalDocs().url());		assertEquals("foo", tag().name("foo").build().name());	}

	@Test void a16_ExternalDocs() {
		X1 x = A1.class.getAnnotation(X1.class);

		assertJsonContains(externalDocs().build().annotationType(), "ExternalDocs");

		assertTrue(ExternalDocsAnnotation.empty(x.externalDocs()));
		assertTrue(ExternalDocsAnnotation.empty((ExternalDocs)null));

		assertFalse(ExternalDocsAnnotation.empty(externalDocs().description(a("foo")).build()));
		assertFalse(ExternalDocsAnnotation.empty(externalDocs().url("foo").build()));
	}

	@Test void a17_Schema() {
		X1 x = A1.class.getAnnotation(X1.class);

		assertJsonContains(schema().build().annotationType(), "Schema");

		assertTrue(SchemaAnnotation.empty(x.schema()));
		assertTrue(SchemaAnnotation.empty((Schema)null));

		assertFalse(SchemaAnnotation.empty(schema()._default(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema()._enum(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().$ref("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().additionalProperties(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().allOf(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().cf("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().collectionFormat("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().d(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().description(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().df(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().discriminator("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().e(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().emax(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().emin(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().exclusiveMaximum(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().exclusiveMinimum(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().externalDocs(externalDocs().url("foo").build()).build()));
		assertFalse(SchemaAnnotation.empty(schema().f("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().format("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().ignore(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().items(items().$ref("foo").build()).build()));
		assertFalse(SchemaAnnotation.empty(schema().max("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().maxi(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maximum("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().maxItems(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxl(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxLength(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxp(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().maxProperties(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().min("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().mini(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minimum("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().minItems(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minl(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minLength(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minp(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().minProperties(0).build()));
		assertFalse(SchemaAnnotation.empty(schema().mo("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().multipleOf("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().p("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().pattern("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().properties(a("foo")).build()));
		assertFalse(SchemaAnnotation.empty(schema().r(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().readOnly(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().required(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().ro(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().t("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().title("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().type("foo").build()));
		assertFalse(SchemaAnnotation.empty(schema().ui(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().uniqueItems(true).build()));
		assertFalse(SchemaAnnotation.empty(schema().xml(a("foo")).build()));
	}

	@Test void a18_SubItems() {
		X1 x = A1.class.getAnnotation(X1.class);

		assertJsonContains(subItems().build().annotationType(), "SubItems");

		assertTrue(SubItemsAnnotation.empty(x.subItems()));
		assertTrue(SubItemsAnnotation.empty((SubItems)null));

		assertFalse(SubItemsAnnotation.empty(subItems()._default(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems()._enum(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().$ref("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().cf("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().collectionFormat("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().df(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().e(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().emax(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().emin(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().exclusiveMaximum(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().exclusiveMinimum(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().f("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().format("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().items(a("foo")).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().max("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxi(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maximum("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxItems(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxl(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().maxLength(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().min("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().mini(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minimum("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minItems(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minl(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().minLength(0).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().mo("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().multipleOf("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().p("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().pattern("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().t("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().type("foo").build()));
		assertFalse(SubItemsAnnotation.empty(subItems().ui(true).build()));
		assertFalse(SubItemsAnnotation.empty(subItems().uniqueItems(true).build()));
	}

	@Test void a19_Items() {
		X1 x = A1.class.getAnnotation(X1.class);

		assertJsonContains(items().build().annotationType(), "Items");

		assertTrue(ItemsAnnotation.empty(x.items()));
		assertTrue(ItemsAnnotation.empty((Items)null));
		assertFalse(ItemsAnnotation.empty(items()._default(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items()._enum(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items().$ref("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().cf("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().collectionFormat("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().df(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items().e(a("foo")).build()));
		assertFalse(ItemsAnnotation.empty(items().emax(true).build()));
		assertFalse(ItemsAnnotation.empty(items().emin(true).build()));
		assertFalse(ItemsAnnotation.empty(items().exclusiveMaximum(true).build()));
		assertFalse(ItemsAnnotation.empty(items().exclusiveMinimum(true).build()));
		assertFalse(ItemsAnnotation.empty(items().f("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().format("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().items(subItems().$ref("foo").build()).build()));
		assertFalse(ItemsAnnotation.empty(items().max("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().maxi(0).build()));
		assertFalse(ItemsAnnotation.empty(items().maximum("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().maxItems(0).build()));
		assertFalse(ItemsAnnotation.empty(items().maxl(0).build()));
		assertFalse(ItemsAnnotation.empty(items().maxLength(0).build()));
		assertFalse(ItemsAnnotation.empty(items().min("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().mini(0).build()));
		assertFalse(ItemsAnnotation.empty(items().minimum("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().minItems(0).build()));
		assertFalse(ItemsAnnotation.empty(items().minl(0).build()));
		assertFalse(ItemsAnnotation.empty(items().minLength(0).build()));
		assertFalse(ItemsAnnotation.empty(items().mo("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().multipleOf("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().p("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().pattern("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().t("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().type("foo").build()));
		assertFalse(ItemsAnnotation.empty(items().ui(true).build()));
		assertFalse(ItemsAnnotation.empty(items().uniqueItems(true).build()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static ContentAnnotation.Builder body() {
		return ContentAnnotation.create();
	}

	private static ContactAnnotation.Builder contact() {
		return ContactAnnotation.create();
	}

	private static FormDataAnnotation.Builder formData() {
		return FormDataAnnotation.create();
	}

	private static HasFormDataAnnotation.Builder hasFormData() {
		return HasFormDataAnnotation.create();
	}

	private static QueryAnnotation.Builder query() {
		return QueryAnnotation.create();
	}

	private static HasQueryAnnotation.Builder hasQuery() {
		return HasQueryAnnotation.create();
	}

	private static HeaderAnnotation.Builder header() {
		return HeaderAnnotation.create();
	}

	private static LicenseAnnotation.Builder license() {
		return LicenseAnnotation.create();
	}

	private static PathAnnotation.Builder path() {
		return PathAnnotation.create();
	}

	private static RequestAnnotation.Builder request() {
		return RequestAnnotation.create();
	}

	private static ResponseAnnotation.Builder response() {
		return ResponseAnnotation.create();
	}

	private static StatusCodeAnnotation.Builder responseCode() {
		return StatusCodeAnnotation.create();
	}

	private static TagAnnotation.Builder tag() {
		return TagAnnotation.create();
	}

	private static SchemaAnnotation.Builder schema() {
		return SchemaAnnotation.create();
	}

	private static ItemsAnnotation.Builder items() {
		return ItemsAnnotation.create();
	}

	private static SubItemsAnnotation.Builder subItems() {
		return SubItemsAnnotation.create();
	}

	private static ExternalDocsAnnotation.Builder externalDocs() {
		return ExternalDocsAnnotation.create();
	}

	private static String[] a(String...s) {
		return s;
	}
}