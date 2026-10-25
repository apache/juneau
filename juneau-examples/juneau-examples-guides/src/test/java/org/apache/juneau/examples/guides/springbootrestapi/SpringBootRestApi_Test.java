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
package org.apache.juneau.examples.guides.springbootrestapi;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;

import org.apache.juneau.*;
import org.apache.juneau.examples.guides.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.*;
import org.springframework.context.*;

/**
 * Backs the "Build a REST API on Spring Boot" guide ({@code pages/guides/01.SpringBootRestApi.md}).
 *
 * <p>
 * Starts {@link PetApplication} on a random port and sends the requests the page's "Run it" section shows with
 * {@code curl}, comparing each response with the output the page shows.
 */
class SpringBootRestApi_Test extends TestBase {

	private static final String OUTPUT = "SpringBootRestApi/run-output.txt";

	private static ConfigurableApplicationContext app;
	private static HttpClient http;
	private static String root;

	@BeforeAll
	static void startApp() {
		app = new SpringApplicationBuilder(PetApplication.class).properties("server.port=0").run();
		root = "http://localhost:" + app.getEnvironment().getProperty("local.server.port");
		http = HttpClient.newHttpClient();
	}

	@AfterAll
	static void stopApp() {
		app.close();
	}

	private static HttpResponse<String> send(HttpRequest.Builder req) throws Exception {
		return http.send(req.header("Accept", "application/json").build(), BodyHandlers.ofString());
	}

	@Test
	void a01_getPet() throws Exception {
		var resp = send(HttpRequest.newBuilder(URI.create(root + "/pets/1")));
		assertEquals(200, resp.statusCode());
		assertEquals(GuideRegion.read(OUTPUT, "get-pet"), resp.body());
	}

	@Test
	void a02_addPet() throws Exception {
		var resp = send(HttpRequest.newBuilder(URI.create(root + "/pets"))
			.header("Content-Type", "application/json")
			.POST(BodyPublishers.ofString("{\"name\":\"Rex\"}")));
		assertEquals(200, resp.statusCode());
		assertEquals(GuideRegion.read(OUTPUT, "add-pet"), resp.body());
	}

	@Test
	void a03_listPets() throws Exception {
		var resp = send(HttpRequest.newBuilder(URI.create(root + "/pets")));
		assertEquals(200, resp.statusCode());
		assertEquals(GuideRegion.read(OUTPUT, "list-pets"), resp.body());
	}

	@Test
	void a04_missingPet() throws Exception {
		var resp = send(HttpRequest.newBuilder(URI.create(root + "/pets/99")).header("No-Trace", "true"));
		assertEquals(GuideRegion.read(OUTPUT, "missing-pet"), String.valueOf(resp.statusCode()));
	}
}
