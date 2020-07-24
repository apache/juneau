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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.annotation.AnnotationUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class AnnotationUtils_Test {

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

	@Body
	@Response
	@ResponseHeader
	@X1
	public static class A1 {
		@Query @Header @FormData @Path @Schema
		public int f1;
	}

	public static class A2 {
		public int f1;
	}

	@Test
	public void a01_Body() throws Exception {
		assertObject(body().annotationType()).json().contains("Body");

		assertTrue(empty(A1.class.getAnnotation(Body.class)));
		assertTrue(empty(A2.class.getAnnotation(Body.class)));
		assertTrue(empty(body()));
		assertTrue(empty((Body)null));

		assertFalse(empty(body().api(a("foo"))));
		assertFalse(empty(body().d(a("foo"))));
		assertFalse(empty(body().description(a("foo"))));
		assertFalse(empty(body().ex(a("foo"))));
		assertFalse(empty(body().example(a("foo"))));
		assertFalse(empty(body().examples(a("foo"))));
		assertFalse(empty(body().exs(a("foo"))));
		assertFalse(empty(body().required(true)));
		assertFalse(empty(body().r(true)));
		assertFalse(empty(body().schema(schema().$ref("foo"))));
		assertFalse(empty(body().value(a("foo"))));
	}

	@Test
	public void a02_Contact() throws Exception {
		X1 x1 = A1.class.getAnnotation(X1.class);

		assertObject(contact().annotationType()).json().contains("Contact");

		assertTrue(empty(x1.contact()));
		assertTrue(empty(contact()));
		assertTrue(empty((Contact)null));

		assertFalse(empty(contact().email("foo")));
		assertFalse(empty(contact().name("foo")));
		assertFalse(empty(contact().url("foo")));
		assertFalse(empty(contact().value(a("foo"))));
	}

	@Test
	public void a03_FormData() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(formData().annotationType()).json().contains("FormData");

		assertTrue(empty(f1.getAnnotation(FormData.class)));
		assertTrue(empty(f2.getAnnotation(FormData.class)));
		assertTrue(empty((FormData)null));
		assertTrue(empty(formData()));

		assertFalse(empty(formData()._default(a("foo"))));
		assertFalse(empty(formData()._enum(a("foo"))));
		assertFalse(empty(formData().aev(true)));
		assertFalse(empty(formData().allowEmptyValue(true)));
		assertFalse(empty(formData().api(a("foo"))));
		assertFalse(empty(formData().cf("foo")));
		assertFalse(empty(formData().collectionFormat("foo")));
		assertFalse(empty(formData().d(a("foo"))));
		assertFalse(empty(formData().description(a("foo"))));
		assertFalse(empty(formData().df(a("foo"))));
		assertFalse(empty(formData().e(a("foo"))));
		assertFalse(empty(formData().emax(true)));
		assertFalse(empty(formData().emin(true)));
		assertFalse(empty(formData().ex(a("foo"))));
		assertFalse(empty(formData().example(a("foo"))));
		assertFalse(empty(formData().exclusiveMaximum(true)));
		assertFalse(empty(formData().exclusiveMinimum(true)));
		assertFalse(empty(formData().f("foo")));
		assertFalse(empty(formData().format("foo")));
		assertFalse(empty(formData().items(items().$ref("foo"))));
		assertFalse(empty(formData().max("foo")));
		assertFalse(empty(formData().maxi(0)));
		assertFalse(empty(formData().maximum("foo")));
		assertFalse(empty(formData().maxItems(0)));
		assertFalse(empty(formData().maxl(0)));
		assertFalse(empty(formData().maxLength(0)));
		assertFalse(empty(formData().min("foo")));
		assertFalse(empty(formData().mini(0)));
		assertFalse(empty(formData().minimum("foo")));
		assertFalse(empty(formData().minItems(0)));
		assertFalse(empty(formData().minl(0)));
		assertFalse(empty(formData().minLength(0)));
		assertFalse(empty(formData().mo("foo")));
		assertFalse(empty(formData().multi(true)));
		assertFalse(empty(formData().multipleOf("foo")));
		assertFalse(empty(formData().n("foo")));
		assertFalse(empty(formData().name("foo")));
		assertFalse(empty(formData().p("foo")));
		assertFalse(empty(formData().parser(OpenApiParser.class)));
		assertFalse(empty(formData().pattern("foo")));
		assertFalse(empty(formData().r(true)));
		assertFalse(empty(formData().required(true)));
		assertFalse(empty(formData().serializer(OpenApiSerializer.class)));
		assertFalse(empty(formData().sie(true)));
		assertFalse(empty(formData().skipIfEmpty(true)));
		assertFalse(empty(formData().t("foo")));
		assertFalse(empty(formData().type("foo")));
		assertFalse(empty(formData().ui(true)));
		assertFalse(empty(formData().uniqueItems(true)));
		assertFalse(empty(formData().value("foo")));
	}

	@Test
	public void a04_HasFormData() throws Exception {
		assertObject(hasFormData().annotationType()).json().contains("HasFormData");

		assertObject(hasFormData().n("foo").n()).json().is("'foo'");
		assertObject(hasFormData().name("foo").name()).json().is("'foo'");
		assertObject(hasFormData().value("foo").value()).json().is("'foo'");
	}

	@Test
	public void a05_Query() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(query().annotationType()).json().contains("Query");

		assertTrue(empty(f1.getAnnotation(Query.class)));
		assertTrue(empty(f2.getAnnotation(Query.class)));
		assertTrue(empty((Query)null));
		assertTrue(empty(query()));

		assertFalse(empty(query()._default(a("foo"))));
		assertFalse(empty(query()._enum(a("foo"))));
		assertFalse(empty(query().aev(true)));
		assertFalse(empty(query().allowEmptyValue(true)));
		assertFalse(empty(query().api(a("foo"))));
		assertFalse(empty(query().cf("foo")));
		assertFalse(empty(query().collectionFormat("foo")));
		assertFalse(empty(query().d(a("foo"))));
		assertFalse(empty(query().description(a("foo"))));
		assertFalse(empty(query().df(a("foo"))));
		assertFalse(empty(query().e(a("foo"))));
		assertFalse(empty(query().emax(true)));
		assertFalse(empty(query().emin(true)));
		assertFalse(empty(query().ex(a("foo"))));
		assertFalse(empty(query().example(a("foo"))));
		assertFalse(empty(query().exclusiveMaximum(true)));
		assertFalse(empty(query().exclusiveMinimum(true)));
		assertFalse(empty(query().f("foo")));
		assertFalse(empty(query().format("foo")));
		assertFalse(empty(query().items(items().$ref("foo"))));
		assertFalse(empty(query().max("foo")));
		assertFalse(empty(query().maxi(0)));
		assertFalse(empty(query().maximum("foo")));
		assertFalse(empty(query().maxItems(0)));
		assertFalse(empty(query().maxl(0)));
		assertFalse(empty(query().maxLength(0)));
		assertFalse(empty(query().min("foo")));
		assertFalse(empty(query().mini(0)));
		assertFalse(empty(query().minimum("foo")));
		assertFalse(empty(query().minItems(0)));
		assertFalse(empty(query().minl(0)));
		assertFalse(empty(query().minLength(0)));
		assertFalse(empty(query().mo("foo")));
		assertFalse(empty(query().multi(true)));
		assertFalse(empty(query().multipleOf("foo")));
		assertFalse(empty(query().n("foo")));
		assertFalse(empty(query().name("foo")));
		assertFalse(empty(query().p("foo")));
		assertFalse(empty(query().parser(OpenApiParser.class)));
		assertFalse(empty(query().pattern("foo")));
		assertFalse(empty(query().r(true)));
		assertFalse(empty(query().required(true)));
		assertFalse(empty(query().serializer(OpenApiSerializer.class)));
		assertFalse(empty(query().sie(true)));
		assertFalse(empty(query().skipIfEmpty(true)));
		assertFalse(empty(query().t("foo")));
		assertFalse(empty(query().type("foo")));
		assertFalse(empty(query().ui(true)));
		assertFalse(empty(query().uniqueItems(true)));
		assertFalse(empty(query().value("foo")));
	}

	@Test
	public void a06_HasQuery() throws Exception {
		assertObject(hasQuery().annotationType()).json().contains("HasQuery");

		assertObject(hasQuery().n("foo").n()).json().is("'foo'");
		assertObject(hasQuery().name("foo").name()).json().is("'foo'");
		assertObject(hasQuery().value("foo").value()).json().is("'foo'");
	}

	@Test
	public void a07_Header() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(header().annotationType()).json().contains("Header");

		assertTrue(empty(f1.getAnnotation(Header.class)));
		assertTrue(empty(f2.getAnnotation(Header.class)));
		assertTrue(empty((Header)null));
		assertTrue(empty(header()));

		assertFalse(empty(header()._default(a("foo"))));
		assertFalse(empty(header()._enum(a("foo"))));
		assertFalse(empty(header().aev(true)));
		assertFalse(empty(header().allowEmptyValue(true)));
		assertFalse(empty(header().api(a("foo"))));
		assertFalse(empty(header().cf("foo")));
		assertFalse(empty(header().collectionFormat("foo")));
		assertFalse(empty(header().d(a("foo"))));
		assertFalse(empty(header().description(a("foo"))));
		assertFalse(empty(header().df(a("foo"))));
		assertFalse(empty(header().e(a("foo"))));
		assertFalse(empty(header().emax(true)));
		assertFalse(empty(header().emin(true)));
		assertFalse(empty(header().ex(a("foo"))));
		assertFalse(empty(header().example(a("foo"))));
		assertFalse(empty(header().exclusiveMaximum(true)));
		assertFalse(empty(header().exclusiveMinimum(true)));
		assertFalse(empty(header().f("foo")));
		assertFalse(empty(header().format("foo")));
		assertFalse(empty(header().items(items().$ref("foo"))));
		assertFalse(empty(header().max("foo")));
		assertFalse(empty(header().maxi(0)));
		assertFalse(empty(header().maximum("foo")));
		assertFalse(empty(header().maxItems(0)));
		assertFalse(empty(header().maxl(0)));
		assertFalse(empty(header().maxLength(0)));
		assertFalse(empty(header().min("foo")));
		assertFalse(empty(header().mini(0)));
		assertFalse(empty(header().minimum("foo")));
		assertFalse(empty(header().minItems(0)));
		assertFalse(empty(header().minl(0)));
		assertFalse(empty(header().minLength(0)));
		assertFalse(empty(header().mo("foo")));
		assertFalse(empty(header().multi(true)));
		assertFalse(empty(header().multipleOf("foo")));
		assertFalse(empty(header().n("foo")));
		assertFalse(empty(header().name("foo")));
		assertFalse(empty(header().p("foo")));
		assertFalse(empty(header().parser(OpenApiParser.class)));
		assertFalse(empty(header().pattern("foo")));
		assertFalse(empty(header().r(true)));
		assertFalse(empty(header().required(true)));
		assertFalse(empty(header().serializer(OpenApiSerializer.class)));
		assertFalse(empty(header().sie(true)));
		assertFalse(empty(header().skipIfEmpty(true)));
		assertFalse(empty(header().t("foo")));
		assertFalse(empty(header().type("foo")));
		assertFalse(empty(header().ui(true)));
		assertFalse(empty(header().uniqueItems(true)));
		assertFalse(empty(header().value("foo")));
	}

	@Test
	public void a08_License() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(license().annotationType()).json().contains("License");

		assertTrue(empty(x.license()));
		assertTrue(empty((License)null));
		assertTrue(empty(license()));

		assertFalse(empty(license().name("foo")));
		assertFalse(empty(license().url("foo")));
		assertFalse(empty(license().value(a("foo"))));
	}

	@Test
	public void a09_Path() throws Exception {
		Field f1 = A1.class.getField("f1");
		Field f2 = A2.class.getField("f1");

		assertObject(path().annotationType()).json().contains("Path");

		assertTrue(empty(f1.getAnnotation(Path.class)));
		assertTrue(empty(f2.getAnnotation(Path.class)));
		assertTrue(empty((Path)null));
		assertTrue(empty(path()));

		assertFalse(empty(path()._enum(a("foo"))));
		assertFalse(empty(path().aev(true)));
		assertFalse(empty(path().allowEmptyValue(true)));
		assertFalse(empty(path().api(a("foo"))));
		assertFalse(empty(path().cf("foo")));
		assertFalse(empty(path().collectionFormat("foo")));
		assertFalse(empty(path().d(a("foo"))));
		assertFalse(empty(path().description(a("foo"))));
		assertFalse(empty(path().e(a("foo"))));
		assertFalse(empty(path().emax(true)));
		assertFalse(empty(path().emin(true)));
		assertFalse(empty(path().ex(a("foo"))));
		assertFalse(empty(path().example(a("foo"))));
		assertFalse(empty(path().exclusiveMaximum(true)));
		assertFalse(empty(path().exclusiveMinimum(true)));
		assertFalse(empty(path().f("foo")));
		assertFalse(empty(path().format("foo")));
		assertFalse(empty(path().items(items().$ref("foo"))));
		assertFalse(empty(path().max("foo")));
		assertFalse(empty(path().maxi(0)));
		assertFalse(empty(path().maximum("foo")));
		assertFalse(empty(path().maxItems(0)));
		assertFalse(empty(path().maxl(0)));
		assertFalse(empty(path().maxLength(0)));
		assertFalse(empty(path().min("foo")));
		assertFalse(empty(path().mini(0)));
		assertFalse(empty(path().minimum("foo")));
		assertFalse(empty(path().minItems(0)));
		assertFalse(empty(path().minl(0)));
		assertFalse(empty(path().minLength(0)));
		assertFalse(empty(path().mo("foo")));
		assertFalse(empty(path().multipleOf("foo")));
		assertFalse(empty(path().n("foo")));
		assertFalse(empty(path().name("foo")));
		assertFalse(empty(path().p("foo")));
		assertFalse(empty(path().parser(OpenApiParser.class)));
		assertFalse(empty(path().pattern("foo")));
		assertFalse(empty(path().r(false)));
		assertFalse(empty(path().required(false)));
		assertFalse(empty(path().serializer(OpenApiSerializer.class)));
		assertFalse(empty(path().t("foo")));
		assertFalse(empty(path().type("foo")));
		assertFalse(empty(path().ui(true)));
		assertFalse(empty(path().uniqueItems(true)));
		assertFalse(empty(path().value("foo")));
	}

	@Test
	public void a10_Request() throws Exception {
		assertObject(request().annotationType()).json().contains("Request");

		assertObject(request().parser(OpenApiParser.class).parser()).json().is("'org.apache.juneau.oapi.OpenApiParser'");
		assertObject(request().serializer(OpenApiSerializer.class).serializer()).json().is("'org.apache.juneau.oapi.OpenApiSerializer'");
	}

	@Test
	public void a11_Response() throws Exception {
		assertObject(response().annotationType()).json().contains("Response");

		assertTrue(empty(A1.class.getAnnotation(Response.class)));
		assertTrue(empty(A2.class.getAnnotation(Response.class)));
		assertTrue(empty(response()));
		assertTrue(empty((Response)null));

		assertFalse(empty(response().api(a("foo"))));
		assertFalse(empty(response().code(a(0))));
		assertFalse(empty(response().d(a("foo"))));
		assertFalse(empty(response().description(a("foo"))));
		assertFalse(empty(response().ex(a("foo"))));
		assertFalse(empty(response().example(a("foo"))));
		assertFalse(empty(response().examples(a("foo"))));
		assertFalse(empty(response().exs(a("foo"))));
		assertFalse(empty(response().headers(new ResponseHeader[]{responseHeader().$ref("foo")})));
		assertFalse(empty(response().parser(OpenApiParser.class)));
		assertFalse(empty(response().schema(schema().$ref("foo"))));
		assertFalse(empty(response().serializer(OpenApiSerializer.class)));
		assertFalse(empty(response().value(a(0))));
	}

	@Test
	public void a12_ResponseBody() throws Exception {
		assertObject(responseBody().annotationType()).json().contains("ResponseBody");
	}

	@Test
	public void a13_ResponseHeader() throws Exception {
		assertObject(responseHeader().annotationType()).json().contains("ResponseHeader");

		assertTrue(empty(A1.class.getAnnotation(ResponseHeader.class)));
		assertTrue(empty(A2.class.getAnnotation(ResponseHeader.class)));

		assertFalse(empty(responseHeader()._default(a("foo"))));
		assertFalse(empty(responseHeader()._enum(a("foo"))));
		assertFalse(empty(responseHeader().api(a("foo"))));
		assertFalse(empty(responseHeader().code(a(0))));
		assertFalse(empty(responseHeader().cf("foo")));
		assertFalse(empty(responseHeader().collectionFormat("foo")));
		assertFalse(empty(responseHeader().d(a("foo"))));
		assertFalse(empty(responseHeader().description(a("foo"))));
		assertFalse(empty(responseHeader().df(a("foo"))));
		assertFalse(empty(responseHeader().e(a("foo"))));
		assertFalse(empty(responseHeader().emax(true)));
		assertFalse(empty(responseHeader().emin(true)));
		assertFalse(empty(responseHeader().ex(a("foo"))));
		assertFalse(empty(responseHeader().example(a("foo"))));
		assertFalse(empty(responseHeader().exclusiveMaximum(true)));
		assertFalse(empty(responseHeader().exclusiveMinimum(true)));
		assertFalse(empty(responseHeader().f("foo")));
		assertFalse(empty(responseHeader().format("foo")));
		assertFalse(empty(responseHeader().items(items().$ref("foo"))));
		assertFalse(empty(responseHeader().max("foo")));
		assertFalse(empty(responseHeader().maxi(0)));
		assertFalse(empty(responseHeader().maximum("foo")));
		assertFalse(empty(responseHeader().maxItems(0)));
		assertFalse(empty(responseHeader().maxl(0)));
		assertFalse(empty(responseHeader().maxLength(0)));
		assertFalse(empty(responseHeader().min("foo")));
		assertFalse(empty(responseHeader().mini(0)));
		assertFalse(empty(responseHeader().minimum("foo")));
		assertFalse(empty(responseHeader().minItems(0)));
		assertFalse(empty(responseHeader().minl(0)));
		assertFalse(empty(responseHeader().minLength(0)));
		assertFalse(empty(responseHeader().mo("foo")));
		assertFalse(empty(responseHeader().multipleOf("foo")));
		assertFalse(empty(responseHeader().n("foo")));
		assertFalse(empty(responseHeader().name("foo")));
		assertFalse(empty(responseHeader().p("foo")));
		assertFalse(empty(responseHeader().pattern("foo")));
		assertFalse(empty(responseHeader().serializer(OpenApiSerializer.class)));
		assertFalse(empty(responseHeader().t("foo")));
		assertFalse(empty(responseHeader().type("foo")));
		assertFalse(empty(responseHeader().ui(true)));
		assertFalse(empty(responseHeader().uniqueItems(true)));
		assertFalse(empty(responseHeader().value("foo")));
	}

	@Test
	public void a14_ResponseStatus() throws Exception {
		assertObject(responseStatus().annotationType()).json().contains("ResponseStatus");
	}

	@Test
	public void a15_Tag() throws Exception {
		assertObject(tag().annotationType()).json().contains("Tag");

		assertObject(tag().description(a("foo")).description()).json().is("['foo']");
		assertObject(tag().externalDocs(externalDocs().url("foo")).externalDocs().url()).json().is("'foo'");
		assertObject(tag().name("foo").name()).json().is("'foo'");
		assertObject(tag().value(a("foo")).value()).json().is("['foo']");
	}

	@Test
	public void a16_ExternalDocs() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(externalDocs().annotationType()).json().contains("ExternalDocs");

		assertTrue(empty(x.externalDocs()));
		assertTrue(empty((ExternalDocs)null));

		assertFalse(empty(externalDocs().description(a("foo"))));
		assertFalse(empty(externalDocs().url("foo")));
		assertFalse(empty(externalDocs().value(a("foo"))));
	}

	@Test
	public void a17_Schema() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(schema().annotationType()).json().contains("Schema");

		assertTrue(empty(x.schema()));
		assertTrue(empty((Schema)null));

		assertFalse(empty(schema()._default(a("foo"))));
		assertFalse(empty(schema()._enum(a("foo"))));
		assertFalse(empty(schema().$ref("foo")));
		assertFalse(empty(schema().additionalProperties(a("foo"))));
		assertFalse(empty(schema().allOf(a("foo"))));
		assertFalse(empty(schema().cf("foo")));
		assertFalse(empty(schema().collectionFormat("foo")));
		assertFalse(empty(schema().d(a("foo"))));
		assertFalse(empty(schema().description(a("foo"))));
		assertFalse(empty(schema().df(a("foo"))));
		assertFalse(empty(schema().discriminator("foo")));
		assertFalse(empty(schema().e(a("foo"))));
		assertFalse(empty(schema().emax(true)));
		assertFalse(empty(schema().emin(true)));
		assertFalse(empty(schema().ex(a("foo"))));
		assertFalse(empty(schema().example(a("foo"))));
		assertFalse(empty(schema().examples(a("foo"))));
		assertFalse(empty(schema().exclusiveMaximum(true)));
		assertFalse(empty(schema().exclusiveMinimum(true)));
		assertFalse(empty(schema().exs(a("foo"))));
		assertFalse(empty(schema().externalDocs(externalDocs().url("foo"))));
		assertFalse(empty(schema().f("foo")));
		assertFalse(empty(schema().format("foo")));
		assertFalse(empty(schema().ignore(true)));
		assertFalse(empty(schema().items(items().$ref("foo"))));
		assertFalse(empty(schema().max("foo")));
		assertFalse(empty(schema().maxi(0)));
		assertFalse(empty(schema().maximum("foo")));
		assertFalse(empty(schema().maxItems(0)));
		assertFalse(empty(schema().maxl(0)));
		assertFalse(empty(schema().maxLength(0)));
		assertFalse(empty(schema().maxp(0)));
		assertFalse(empty(schema().maxProperties(0)));
		assertFalse(empty(schema().min("foo")));
		assertFalse(empty(schema().mini(0)));
		assertFalse(empty(schema().minimum("foo")));
		assertFalse(empty(schema().minItems(0)));
		assertFalse(empty(schema().minl(0)));
		assertFalse(empty(schema().minLength(0)));
		assertFalse(empty(schema().minp(0)));
		assertFalse(empty(schema().minProperties(0)));
		assertFalse(empty(schema().mo("foo")));
		assertFalse(empty(schema().multipleOf("foo")));
		assertFalse(empty(schema().on("foo")));
		assertFalse(empty(schema().p("foo")));
		assertFalse(empty(schema().pattern("foo")));
		assertFalse(empty(schema().properties(a("foo"))));
		assertFalse(empty(schema().r(true)));
		assertFalse(empty(schema().readOnly(true)));
		assertFalse(empty(schema().required(true)));
		assertFalse(empty(schema().ro(true)));
		assertFalse(empty(schema().t("foo")));
		assertFalse(empty(schema().title("foo")));
		assertFalse(empty(schema().type("foo")));
		assertFalse(empty(schema().ui(true)));
		assertFalse(empty(schema().uniqueItems(true)));
		assertFalse(empty(schema().value(a("foo"))));
		assertFalse(empty(schema().xml(a("foo"))));
	}

	@Test
	public void a18_SubItems() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(subItems().annotationType()).json().contains("SubItems");

		assertTrue(empty(x.subItems()));
		assertTrue(empty((SubItems)null));

		assertFalse(empty(subItems()._default(a("foo"))));
		assertFalse(empty(subItems()._enum(a("foo"))));
		assertFalse(empty(subItems().$ref("foo")));
		assertFalse(empty(subItems().cf("foo")));
		assertFalse(empty(subItems().collectionFormat("foo")));
		assertFalse(empty(subItems().df(a("foo"))));
		assertFalse(empty(subItems().e(a("foo"))));
		assertFalse(empty(subItems().emax(true)));
		assertFalse(empty(subItems().emin(true)));
		assertFalse(empty(subItems().exclusiveMaximum(true)));
		assertFalse(empty(subItems().exclusiveMinimum(true)));
		assertFalse(empty(subItems().f("foo")));
		assertFalse(empty(subItems().format("foo")));
		assertFalse(empty(subItems().items(a("foo"))));
		assertFalse(empty(subItems().max("foo")));
		assertFalse(empty(subItems().maxi(0)));
		assertFalse(empty(subItems().maximum("foo")));
		assertFalse(empty(subItems().maxItems(0)));
		assertFalse(empty(subItems().maxl(0)));
		assertFalse(empty(subItems().maxLength(0)));
		assertFalse(empty(subItems().min("foo")));
		assertFalse(empty(subItems().mini(0)));
		assertFalse(empty(subItems().minimum("foo")));
		assertFalse(empty(subItems().minItems(0)));
		assertFalse(empty(subItems().minl(0)));
		assertFalse(empty(subItems().minLength(0)));
		assertFalse(empty(subItems().mo("foo")));
		assertFalse(empty(subItems().multipleOf("foo")));
		assertFalse(empty(subItems().p("foo")));
		assertFalse(empty(subItems().pattern("foo")));
		assertFalse(empty(subItems().t("foo")));
		assertFalse(empty(subItems().type("foo")));
		assertFalse(empty(subItems().ui(true)));
		assertFalse(empty(subItems().uniqueItems(true)));
		assertFalse(empty(subItems().value(a("foo"))));
	}

	@Test
	public void a19_Items() throws Exception {
		X1 x = A1.class.getAnnotation(X1.class);

		assertObject(items().annotationType()).json().contains("Items");

		assertTrue(empty(x.items()));
		assertTrue(empty((Items)null));
		assertFalse(empty(items()._default(a("foo"))));
		assertFalse(empty(items()._enum(a("foo"))));
		assertFalse(empty(items().$ref("foo")));
		assertFalse(empty(items().cf("foo")));
		assertFalse(empty(items().collectionFormat("foo")));
		assertFalse(empty(items().df(a("foo"))));
		assertFalse(empty(items().e(a("foo"))));
		assertFalse(empty(items().emax(true)));
		assertFalse(empty(items().emin(true)));
		assertFalse(empty(items().exclusiveMaximum(true)));
		assertFalse(empty(items().exclusiveMinimum(true)));
		assertFalse(empty(items().f("foo")));
		assertFalse(empty(items().format("foo")));
		assertFalse(empty(items().items(subItems().$ref("foo"))));
		assertFalse(empty(items().max("foo")));
		assertFalse(empty(items().maxi(0)));
		assertFalse(empty(items().maximum("foo")));
		assertFalse(empty(items().maxItems(0)));
		assertFalse(empty(items().maxl(0)));
		assertFalse(empty(items().maxLength(0)));
		assertFalse(empty(items().min("foo")));
		assertFalse(empty(items().mini(0)));
		assertFalse(empty(items().minimum("foo")));
		assertFalse(empty(items().minItems(0)));
		assertFalse(empty(items().minl(0)));
		assertFalse(empty(items().minLength(0)));
		assertFalse(empty(items().mo("foo")));
		assertFalse(empty(items().multipleOf("foo")));
		assertFalse(empty(items().p("foo")));
		assertFalse(empty(items().pattern("foo")));
		assertFalse(empty(items().t("foo")));
		assertFalse(empty(items().type("foo")));
		assertFalse(empty(items().ui(true)));
		assertFalse(empty(items().uniqueItems(true)));
		assertFalse(empty(items().value(a("foo"))));
	}


	@Test
	public void b01_allEmpty() {
		assertTrue(allEmpty(new String[0]));
		assertTrue(allEmpty(""));
		assertTrue(allEmpty(null,""));
		assertFalse(allEmpty(null,"","x"));
		assertTrue(allEmpty(new String[0],new String[0]));
		assertTrue(allEmpty(null,new String[0]));
		assertFalse(allEmpty(null,new String[]{""}));
		assertFalse(allEmpty(null,new String[]{"x"}));
	}

	@Test
	public void b02_allTrue() {
		assertTrue(allTrue());
		assertTrue(allTrue(true));
		assertTrue(allTrue(true,true));
		assertFalse(allTrue(false,true));
		assertFalse(allTrue(false));
	}

	@Test
	public void b03_allFalse() {
		assertTrue(allFalse());
		assertTrue(allFalse(false));
		assertTrue(allFalse(false,false));
		assertFalse(allFalse(false,true));
		assertFalse(allFalse(true));
	}

	@Test
	public void b04_allMinusOne() {
		assertTrue(allMinusOne());
		assertTrue(allMinusOne(-1));
		assertTrue(allMinusOne(-1,-1));
		assertFalse(allMinusOne(-1,0));
		assertFalse(allMinusOne(0));
	}

	@Test
	public void b05_allMinusOneLongs() {
		assertTrue(allMinusOne(-1l));
		assertTrue(allMinusOne(-1l,-1l));
		assertFalse(allMinusOne(-1l,0l));
		assertFalse(allMinusOne(0l));
	}

	@Test
	public void b06_other() throws Exception {
		new AnnotationUtils();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static BodyAnnotation body() {
		return new BodyAnnotation();
	}

	private static ContactAnnotation contact() {
		return new ContactAnnotation();
	}

	private static FormDataAnnotation formData() {
		return new FormDataAnnotation();
	}

	private static HasFormDataAnnotation hasFormData() {
		return new HasFormDataAnnotation();
	}

	private static QueryAnnotation query() {
		return new QueryAnnotation();
	}

	private static HasQueryAnnotation hasQuery() {
		return new HasQueryAnnotation();
	}

	private static HeaderAnnotation header() {
		return new HeaderAnnotation();
	}

	private static LicenseAnnotation license() {
		return new LicenseAnnotation();
	}

	private static PathAnnotation path() {
		return new PathAnnotation();
	}

	private static RequestAnnotation request() {
		return new RequestAnnotation();
	}

	private static ResponseAnnotation response() {
		return new ResponseAnnotation();
	}

	private static ResponseBodyAnnotation responseBody() {
		return new ResponseBodyAnnotation();
	}

	private static ResponseHeaderAnnotation responseHeader() {
		return new ResponseHeaderAnnotation();
	}

	private static ResponseStatusAnnotation responseStatus() {
		return new ResponseStatusAnnotation();
	}

	private static TagAnnotation tag() {
		return new TagAnnotation();
	}

	private static SchemaAnnotation schema() {
		return new SchemaAnnotation();
	}

	private static ItemsAnnotation items() {
		return new ItemsAnnotation();
	}

	private static SubItemsAnnotation subItems() {
		return new SubItemsAnnotation();
	}

	private static ExternalDocsAnnotation externalDocs() {
		return new ExternalDocsAnnotation();
	}

	private static String[] a(String...s) {
		return s;
	}

	private static int[] a(int...i) {
		return i;
	}
}