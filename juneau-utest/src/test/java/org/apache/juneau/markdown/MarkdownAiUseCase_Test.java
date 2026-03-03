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
package org.apache.juneau.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * AI/LLM integration tests demonstrating practical use cases for Markdown output.
 */
class MarkdownAiUseCase_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// h01 - Dataset metadata for LLM context
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_datasetDescription() throws Exception {
		var dataset = new DatasetMetadata();
		dataset.name = "customer_sales_2024";
		dataset.description = "Monthly sales by customer region";
		dataset.rowCount = 150_000;
		dataset.columnCount = 12;
		dataset.columns = List.of("customer_id", "region", "product", "revenue", "quantity");
		dataset.format = "Parquet";

		var md = MarkdownDocSerializer.create()
			.title("Dataset: " + dataset.name)
			.build()
			.serialize(dataset);

		assertTrue(md.contains(dataset.name), "Expected dataset name: " + md);
		assertTrue(md.contains(dataset.description), "Expected description: " + md);
		assertTrue(md.contains(String.valueOf(dataset.rowCount)), "Expected row count: " + md);
		assertTrue(md.contains("columns") || md.contains("customer_id"), "Expected column info: " + md);
	}

	public static class DatasetMetadata {
		public String name;
		public String description;
		public long rowCount;
		public int columnCount;
		public List<String> columns;
		public String format;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h02 - API response for chat display
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h02_apiResponse() throws Exception {
		var response = new ApiResponse<>();
		response.status = 200;
		response.message = "Success";
		response.data = List.of(
			new User(1, "alice"),
			new User(2, "bob")
		);

		var md = MarkdownSerializer.DEFAULT.serialize(response);

		assertTrue(md.contains("200") || md.contains("status"), "Expected status: " + md);
		assertTrue(md.contains("Success"), "Expected message: " + md);
		assertTrue(md.contains("alice") || md.contains("bob"), "Expected user data: " + md);
	}

	public static class ApiResponse<T> {
		public int status;
		public String message;
		public T data;
	}

	public static class User {
		public int id;
		public String name;
		public User() {}
		public User(int id, String name) { this.id = id; this.name = name; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h03 - Configuration report (readable document)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h03_configReport() throws Exception {
		var config = new AppConfig();
		config.appName = "MyApp";
		config.debug = false;
		config.timeout = 30;
		config.database = new DbConfig();
		config.database.host = "localhost";
		config.database.port = 5432;
		config.database.name = "mydb";

		var md = MarkdownDocSerializer.create()
			.title("Configuration Report")
			.addHorizontalRules(true)
			.build()
			.serialize(config);

		assertTrue(md.contains("# Configuration Report"), "Expected title: " + md);
		assertTrue(md.contains("MyApp"), "Expected app name: " + md);
		assertTrue(md.contains("database") || md.contains("## database"), "Expected database section: " + md);
		assertTrue(md.contains("localhost"), "Expected db host: " + md);
		assertTrue(md.contains("---"), "Expected horizontal rules: " + md);
	}

	public static class AppConfig {
		public String appName;
		public boolean debug;
		public int timeout;
		public DbConfig database;
	}

	public static class DbConfig {
		public String host;
		public int port;
		public String name;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h04 - Error report for debugging
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h04_errorReport() throws Exception {
		var error = new ErrorReport();
		error.code = "ERR_CONNECTION_TIMEOUT";
		error.message = "Connection to database timed out after 30s";
		error.timestamp = "2024-01-15T10:30:00Z";
		error.details = Map.of(
			"host", "db.example.com",
			"port", "5432",
			"retries", 3
		);

		var md = MarkdownDocSerializer.create()
			.title("Error Report")
			.build()
			.serialize(error);

		assertTrue(md.contains("ERR_CONNECTION_TIMEOUT"), "Expected error code: " + md);
		assertTrue(md.contains("timed out"), "Expected message: " + md);
		assertTrue(md.contains("host") || md.contains("db.example.com"), "Expected details: " + md);
	}

	public static class ErrorReport {
		public String code;
		public String message;
		public String timestamp;
		public Map<String, Object> details;
	}
}
