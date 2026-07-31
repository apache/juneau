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
package org.apache.juneau.rest.server.mcp.v20260728;

import static java.util.concurrent.TimeUnit.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.commons.utils.JsonValueSafety;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpSchema;

/**
 * Bounded, no-fetch validation of {@code tools/call} arguments against a tool's declared input schema
 * (Resolution B2).
 *
 * <p>
 * The neutral {@link McpSchema} is an unconstrained JSON object carrier, and a client can send an
 * arbitrarily large or deep argument object. Before any validation runs, both the schema graph and the
 * argument graph are checked against the shared {@link JsonValueSafety} depth/node/deadline bounds,
 * so a hostile or accidentally-huge input is rejected up front rather than blowing the stack or spinning
 * indefinitely. The two checks share a single deadline, so a slow schema graph cannot buy the argument
 * graph extra time (or vice versa).
 *
 * <p>
 * <b>No external fetches.</b> The schema is converted to a {@link JsonSchema} bean and validated as-is. No
 * schema-resolution map is ever installed on the schema, so an external {@code $ref} is inert data:
 * {@link JsonSchemaValidator} does not resolve references, and with no resolution map installed there is no
 * code path that opens a network connection or a file to dereference one.
 *
 * <p>
 * Validation itself runs on a fixed-size pool of daemon threads and is bounded by the same deadline: if a
 * pathological schema (for example a catastrophically-backtracking {@code pattern}) fails to complete in
 * time, the task is cancelled and a {@code -32602} error is raised instead of hanging the request thread.
 */
final class McpSchemaSafety {

	/** Maximum nesting depth permitted in either the schema graph or the argument graph. */
	static final int MAX_DEPTH = 64;

	/** Maximum number of nodes permitted in either the schema graph or the argument graph. */
	static final int MAX_NODES = 10_000;

	/** Maximum wall-clock time permitted for a single schema validation, in milliseconds. */
	static final long MAX_VALIDATION_MILLIS = 100;

	private static final ExecutorService VALIDATION_POOL = Executors.newFixedThreadPool(
		Math.max(2, Runtime.getRuntime().availableProcessors()),
		r -> {
			var t = new Thread(r, "mcp-2026-07-28-schema-validation");
			t.setDaemon(true);
			return t;
		});

	private McpSchemaSafety() {}

	/**
	 * Validates a {@code tools/call} argument object against a tool's declared input schema, subject to the
	 * B2 bounds.
	 *
	 * @param schema The tool's neutral input schema, or <jk>null</jk> to skip validation entirely.
	 * @param args The argument object to validate. Never <jk>null</jk> (an absent argument object is an empty map).
	 * @throws McpException {@code -32602} if either graph exceeds {@link #MAX_DEPTH}/{@link #MAX_NODES}, if the
	 * 	arguments do not satisfy the schema, or if validation exceeds {@link #MAX_VALIDATION_MILLIS}.
	 */
	static void validateInput(McpSchema schema, Map<String,Object> args) {
		if (schema == null)
			return;
		var schemaMap = schema.toJsonMap();
		var deadline = JsonValueSafety.deadlineNanos();
		try {
			JsonValueSafety.check(schemaMap, "Tool input schema", deadline);
			JsonValueSafety.check(args, "Tool input", deadline);
		} catch (IllegalArgumentException e) {
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, e.getMessage());
		}
		var jsonSchema = Json.to(Json.of(schemaMap), JsonSchema.class);
		validateBounded(jsonSchema, args, deadline);
	}

	/**
	 * Runs {@link JsonSchemaValidator} on a daemon thread and enforces the remaining share of the
	 * shared {@link JsonValueSafety} deadline.
	 */
	private static void validateBounded(JsonSchema<?> schema, Object value, long deadlineNanos) {
		var remaining = JsonValueSafety.remainingNanos(deadlineNanos);
		if (remaining == 0)
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, "Tool input schema validation exceeded " + MAX_VALIDATION_MILLIS + " ms");
		var future = VALIDATION_POOL.submit(() -> {
			JsonSchemaValidator.of(schema).validate(value);
			return null;
		});
		try {
			future.get(remaining, NANOSECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, "Tool input schema validation exceeded " + MAX_VALIDATION_MILLIS + " ms");
		} catch (ExecutionException e) {
			var cause = e.getCause();
			if (cause instanceof McpException me)
				throw me;
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, cause == null ? e.getMessage() : cause.getMessage());
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new McpException(McpRevision.CODE_INVALID_PARAMS, "Tool input schema validation was interrupted");
		}
	}
}
