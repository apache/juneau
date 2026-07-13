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
package org.apache.juneau.bean.openapi3.ui;

import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link RedocUI}.
 */
class RedocUI_Test extends TestBase {

	private static final RedocUI UI = new RedocUI();
	private static final MarshallingSession SESSION = MarshallingContext.DEFAULT_SESSION;

	// Tests forMediaTypes() returns text/html.
	@Test void a01_forMediaTypes() {
		var mediaTypes = UI.forMediaTypes();
		assertNotNull(mediaTypes);
		assertEquals(1, mediaTypes.length);
		assertEquals(org.apache.juneau.commons.http.MediaType.HTML, mediaTypes[0]);
	}

	// Tests swap() with a fully-populated OpenApi document covering most branches.
	@Test void a02_swap_fullDocument() throws Exception {
		var doc = openApi()
			.setInfo(
				info()
					.setTitle("Test API")
					.setVersion("1.0")
					.setDescription("This is a test API.")
			)
			.setPaths(m(
				"/pets", pathItem()
					.setGet(operation().setSummary("List all pets"))
					.setPut(operation().setSummary("Update a pet"))
					.setPost(operation().setSummary("Create a pet"))
					.setDelete(operation().setSummary("Delete a pet"))
					.setPatch(operation().setSummary("Patch a pet"))
					.setHead(operation().setSummary("Head a pet"))
					.setOptions(operation().setSummary("Options a pet"))
					.setTrace(operation().setSummary("Trace a pet")),
				"/pets/{id}", pathItem()
					.setGet(operation().setDescription("Pet description but no summary"))
			))
			.setComponents(
				components()
					.setSchemas(m(
						"Pet", schemaInfo().setType("object").setDescription("A pet object"),
						"Error", schemaInfo().setType("object")
					))
			);

		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		assertInstanceOf(Div.class, result);
		var html = result.toString();
		assertNotNull(html);
		assertTrue(html.contains("redoc-ui"));
		assertTrue(html.contains("redoc-container"));
	}

	// Tests swap() with a minimal OpenApi (no info, no paths, no components).
	@Test void a03_swap_minimal() throws Exception {
		var doc = openApi().setOpenapi("3.0.0");
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		assertInstanceOf(Div.class, result);
		var html = result.toString();
		assertTrue(html.contains("redoc-ui"));
	}

	// Tests info present but no title -> covers nn(info.getTitle())=false in sidebar+content.
	@Test void a04_swap_infoNoTitle() throws Exception {
		var doc = openApi().setInfo(info().setVersion("2.0"));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info with only title (no description, no version) -> covers false branches in content().
	@Test void a05_swap_infoTitleOnly() throws Exception {
		var doc = openApi().setInfo(info().setTitle("Title Only"));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info with description but no version/title -> covers nn(info.getDescription())=true alone.
	@Test void a06_swap_infoDescriptionOnly() throws Exception {
		var doc = openApi().setInfo(info().setDescription("Description only"));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests info with version only -> covers nn(info.getVersion())=true with title and description false.
	@Test void a07_swap_infoVersionOnly() throws Exception {
		var doc = openApi().setInfo(info().setVersion("3.1.0"));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests components without schemas -> covers nn(getSchemas())=false.
	@Test void a08_swap_componentsNoSchemas() throws Exception {
		var doc = openApi().setComponents(components());
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests components with schemas where schema has no description -> covers nn(schema.getDescription())=false.
	@Test void a09_swap_componentsSchemaNoDescription() throws Exception {
		var doc = openApi()
			.setComponents(
				components().setSchemas(m(
					"BareModel", schemaInfo().setType("string")
				))
			);
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests operation with summary -> covers nn(op.getSummary())=true branch in label.
	@Test void a10_swap_operationWithSummary() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/users", pathItem().setGet(operation().setSummary("List users"))
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		var html = result.toString();
		assertTrue(html.contains("List users"));
	}

	// Tests operation without summary -> covers nn(op.getSummary())=false branch in label.
	@Test void a11_swap_operationNoSummary() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/users", pathItem().setGet(operation().setDescription("Just a description"))
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		var html = result.toString();
		assertTrue(html.contains("GET /users"));
	}

	// Tests path with special characters -> exercises replaceAll regex in anchor generation.
	@Test void a12_swap_pathWithSpecialChars() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/users/{id}/posts", pathItem().setGet(operation().setSummary("Get user posts"))
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests operation with description but no summary -> covers content() rendering of opDesc.
	@Test void a13_swap_operationDescOnly() throws Exception {
		var doc = openApi()
			.setPaths(m(
				"/test", pathItem().setGet(operation().setDescription("Operation description"))
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		var html = result.toString();
		assertTrue(html.contains("Operation description"));
	}

	// Tests fully empty PathItem (no operations) -> covers all op==null branches in renderPathOperations/appendOperations.
	@Test void a14_swap_emptyPathItem() throws Exception {
		var doc = openApi()
			.setPaths(m("/empty", pathItem()));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
	}

	// Tests path with all 8 HTTP verbs -> exercises every branch in renderPathOperations and appendOperations.
	@Test void a15_swap_allVerbsWithSummaryAndDescription() throws Exception {
		var doc = openApi()
			.setInfo(info().setTitle("API").setVersion("1.0").setDescription("Test desc"))
			.setPaths(m(
				"/all", pathItem()
					.setGet(operation().setSummary("Get").setDescription("Get desc"))
					.setPut(operation().setSummary("Put").setDescription("Put desc"))
					.setPost(operation().setSummary("Post").setDescription("Post desc"))
					.setDelete(operation().setSummary("Delete").setDescription("Delete desc"))
					.setPatch(operation().setSummary("Patch").setDescription("Patch desc"))
					.setHead(operation().setSummary("Head").setDescription("Head desc"))
					.setOptions(operation().setSummary("Options").setDescription("Options desc"))
					.setTrace(operation().setSummary("Trace").setDescription("Trace desc"))
			));
		var result = UI.swap(SESSION, doc);
		assertNotNull(result);
		var html = result.toString();
		assertTrue(html.contains("redoc-method-get"));
		assertTrue(html.contains("redoc-method-trace"));
	}
}
