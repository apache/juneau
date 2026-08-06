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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the three Phase-3 pause/resume seam types: {@link McpInputRequiredSignal},
 * {@link McpMrtrResumeContext}, and {@link McpMrtrCapabilityContext}.
 */
class McpInputRequiredSignal_Test {

	// -------- McpInputRequiredSignal ---------

	@Test void a01_emptyInputRequestsThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpInputRequiredSignal(Map.of(), "cont"));
		assertEquals("inputRequests must not be null or empty", e.getMessage());
	}

	@Test void a02_nullInputRequestsThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpInputRequiredSignal(null, "cont"));
		assertEquals("inputRequests must not be null or empty", e.getMessage());
	}

	@Test void a03_validSignalStoresAndReturnsFieldsUnchanged() {
		var reqs = Map.<String,Object>of("q1", Map.of("type", "elicitation"));
		var a = new McpInputRequiredSignal(reqs, "cont-1");
		assertSame(reqs, a.getInputRequests());
		assertEquals("cont-1", a.getContinuation());
		assertEquals("Handler requested more input", a.getMessage());
	}

	@Test void a04_nullContinuationIsAllowed() {
		var a = new McpInputRequiredSignal(Map.of("q1", "x"), null);
		assertNull(a.getContinuation());
	}

	// -------- McpMrtrResumeContext ---------

	@Test void b01_recordAccessorsReturnConstructedValues() {
		var responses = Map.<String,Object>of("q1", "answer");
		var a = new McpMrtrResumeContext("cont", responses);
		assertEquals("cont", a.continuation());
		assertSame(responses, a.inputResponses());
	}

	@Test void b02_valueEquality() {
		var a = new McpMrtrResumeContext("c", Map.of("q1", "a"));
		var b = new McpMrtrResumeContext("c", Map.of("q1", "a"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test void b03_continuationAsNullReturnsNull() {
		var a = new McpMrtrResumeContext(null, Map.of());
		assertNull(a.continuationAs(B04_Continuation.class));
	}

	public static class B04_Continuation {
		private int step;
		private String note;

		public int getStep() { return step; }
		public B04_Continuation setStep(int value) { step = value; return this; }
		public String getNote() { return note; }
		public B04_Continuation setNote(String value) { note = value; return this; }
	}

	@Test void b04_continuationAsConvertsGenericJsonBackToBeanType() {
		// A resume continuation arrives as generic JSON (JsonMap), never the handler's original bean type.
		var generic = JsonMap.of("step", 2, "note", "resume");
		var a = new McpMrtrResumeContext(generic, Map.of());
		var typed = a.continuationAs(B04_Continuation.class);
		assertEquals(2, typed.getStep());
		assertEquals("resume", typed.getNote());
	}

	@Test void b05_continuationAsConvertsStringContinuation() {
		var a = new McpMrtrResumeContext("hello", Map.of());
		assertEquals("hello", a.continuationAs(String.class));
	}

	@Test void b06_continuationAsNullTypeThrows() {
		var a = new McpMrtrResumeContext(JsonMap.of("step", 1), Map.of());
		var e = assertThrows(IllegalArgumentException.class, () -> a.continuationAs(null));
		assertEquals("Argument 'type' cannot be null.", e.getMessage());
	}

	@Test void b07_continuationAsShapeMismatchThrows() {
		// A JSON-array continuation cannot be converted to a bean target: the marshaller throws (which the
		// dispatcher's generic branch surfaces as -32603). Documents the continuationAs failure contract.
		var a = new McpMrtrResumeContext(JsonList.of(1, 2), Map.of());
		assertThrows(RuntimeException.class, () -> a.continuationAs(B04_Continuation.class));
	}

	@Test void b08_continuationAsStringReturnsStringContinuation() {
		var a = new McpMrtrResumeContext("hello", Map.of());
		assertEquals("hello", a.continuationAsString());
	}

	@Test void b09_continuationAsStringNullContinuationReturnsNull() {
		var a = new McpMrtrResumeContext(null, Map.of());
		assertNull(a.continuationAsString());
	}

	@Test void b10_continuationAsStringNumericContinuationThrows() {
		// L-4: empirically, Json.to(Json.of(42), String.class) throws rather than stringifying - String
		// conversion requires an already-quoted JSON string literal, so a bare number does not convert.
		var a = new McpMrtrResumeContext(42, Map.of());
		assertThrows(RuntimeException.class, a::continuationAsString);
	}

	@Test void b11_continuationAsStringJsonMapContinuationThrows() {
		// L-4: a JsonMap continuation is likewise structurally incompatible with String.
		var a = new McpMrtrResumeContext(JsonMap.of("step", 1), Map.of());
		assertThrows(RuntimeException.class, a::continuationAsString);
	}

	// -------- McpMrtrCapabilityContext ---------

	@Test void c01_recordAccessorReturnsConstructedValue() {
		assertTrue(new McpMrtrCapabilityContext(true).elicitationSupported());
		assertFalse(new McpMrtrCapabilityContext(false).elicitationSupported());
	}

	@Test void c02_valueEquality() {
		assertEquals(new McpMrtrCapabilityContext(true), new McpMrtrCapabilityContext(true));
		assertNotEquals(new McpMrtrCapabilityContext(true), new McpMrtrCapabilityContext(false));
	}
}
