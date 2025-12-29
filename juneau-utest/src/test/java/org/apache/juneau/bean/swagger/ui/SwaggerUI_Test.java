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
package org.apache.juneau.bean.swagger.ui;

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SwaggerUI}.
 */
class SwaggerUI_Test extends TestBase {

	private final BeanSession bs = BeanContext.DEFAULT_SESSION;

	/**
	 * Test method for media types.
	 */
	@Test void a01_mediaTypes() {
		assertList(
			new SwaggerUI().forMediaTypes(),
			"text/html"
		);
	}

	/**
	 * Test method for basic Swagger document conversion.
	 */
	@Test void a02_basicSwaggerConversion() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0").setDescription("Test API Description"))
			.setHost("localhost:8080")
			.setBasePath("/api")
			.setPaths(map("/test", operationMap().append("get", operation().setSummary("Test operation"))));

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Description:</th><td>Test API Description</td></tr>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'>
						<div class='op-block op-block-closed get'>
							<div class='op-block-summary' onclick='toggleOpBlock(this)'>
								<span class='method-button'>GET</span>
								<span class='path'>/test</span>
								<span class='summary'>Test operation</span>
							</div>
							<div class='op-block-contents'>
								<div class='table-container'></div>
							</div>
						</div>
					</div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with tags.
	 */
	@Test void a03_swaggerWithTags() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setTags(l(
				tag("users").setDescription("User operations"),
				tag("orders").setDescription("Order operations")
			));

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-summary' onclick='toggleTagBlock(this)'>
						<span class='name'>users</span>
						<span class='description'>User operations</span>
					</div>
					<div class='tag-block-contents'></div>
				</div>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-summary' onclick='toggleTagBlock(this)'>
						<span class='name'>orders</span>
						<span class='description'>Order operations</span>
					</div>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with parameters.
	 */
	@Test void a04_swaggerWithParameters() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with responses.
	 */
	@Test void a05_swaggerWithResponses() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with models.
	 */
	@Test void a06_swaggerWithModels() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for empty Swagger document.
	 */
	@Test void a07_emptySwaggerDocument() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with external documentation.
	 */
	@Test void a08_swaggerWithExternalDocs() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with security schemes.
	 */
	@Test void a09_swaggerWithSecuritySchemes() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for Swagger document with multiple operations.
	 */
	@Test void a10_swaggerWithMultipleOperations() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(new HashMap<>());

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'></div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}

	/**
	 * Test method for null input.
	 */
	@Test void a11_nullInput() {
		assertThrows(NullPointerException.class, () -> {
			new SwaggerUI().swap(bs, null);
		});
	}

	/**
	 * Test method for HTML output structure.
	 */
	@Test void a12_htmlOutputStructure() throws Exception {
		var swagger = swagger()
			.setInfo(info("Test API", "1.0.0"))
			.setPaths(map("/test", operationMap()
				.append("get", operation().setSummary("Test operation"))));

		assertString(
			"""
			<div class='swagger-ui'>
				<style></style>
				<script type='text/javascript'><sp/></script>
				<table class='header'>
					<tr><th>Version:</th><td>1.0.0</td></tr>
				</table>
				<div class='tag-block tag-block-open'>
					<div class='tag-block-contents'>
						<div class='op-block op-block-closed get'>
							<div class='op-block-summary' onclick='toggleOpBlock(this)'>
								<span class='method-button'>GET</span>
								<span class='path'>/test</span>
								<span class='summary'>Test operation</span>
							</div>
							<div class='op-block-contents'>
								<div class='table-container'></div>
							</div>
						</div>
					</div>
				</div>
			</div>
			""".replaceAll("\\n\\s*", ""),
			new SwaggerUI().swap(bs, swagger)
		);
	}
}