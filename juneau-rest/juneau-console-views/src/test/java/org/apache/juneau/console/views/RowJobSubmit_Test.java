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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * {@link RowJobSubmit}: envelope parse, verify (action first, then the ledger binding) and the refusal.
 */
class RowJobSubmit_Test extends TestBase {

	private static JsonMap body(String json5) {
		return JsonMap.of(Json5.to(json5, Map.class));
	}

	@Test void a01_parse_fullEnvelope() {
		var s = RowJobSubmit.parse(body("{action:'restock',targetId:'dog',idempotencyKey:'k',fields:{count:3}}"));
		assertEquals("restock", s.action());
		assertEquals("dog", s.targetId());
		assertEquals("k", s.idempotencyKey());
		assertEquals(3, s.fields().getInt("count"));
	}

	@Test void a02_parse_missingFields_readsEmpty() {
		assertTrue(RowJobSubmit.parse(body("{action:'restock',targetId:'dog',idempotencyKey:'k'}")).fields().isEmpty());
	}

	@Test void a03_parse_nullBody() {
		assertEquals("Expected a JSON object body", assertThrows(BadRequest.class, () -> RowJobSubmit.parse(null)).getMessage());
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', quoteCharacter='"', value={
		"{targetId:'dog',idempotencyKey:'k'}|Missing 'action'",
		"{action:' ',targetId:'dog',idempotencyKey:'k'}|Missing 'action'",
		"{action:'restock',idempotencyKey:'k'}|Missing 'targetId'",
		"{action:'restock',targetId:3,idempotencyKey:'k'}|Missing 'targetId'",
		"{action:'restock',targetId:'dog'}|Missing 'idempotencyKey'",
		"{action:'restock',targetId:'dog',idempotencyKey:'k',fields:[1]}|'fields' must be an object",
	})
	void a04_parse_errors(String json, String message) {
		var b = body(json);
		assertEquals(message, assertThrows(BadRequest.class, () -> RowJobSubmit.parse(b)).getMessage());
	}

	@Test void b01_verify_matchingKey() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog", "alice");
		var s = RowJobSubmit.parse(body("{action:'restock',targetId:'dog',idempotencyKey:'" + k.value() + "'}"));
		assertTrue(s.verify(l, "restock", "alice").isPresent());
		assertTrue(s.verify(l, "restock", "bob").isEmpty());
	}

	@Test void b02_verify_wrongAction_isEmptyBeforeTheLedger() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "dog");
		var s = RowJobSubmit.parse(body("{action:'restock',targetId:'dog',idempotencyKey:'" + k.value() + "'}"));
		assertTrue(s.verify(l, "restock").isPresent());
		assertTrue(s.verify(l, "groom").isEmpty());
	}

	@Test void b03_verify_keyForAnotherRow_isEmpty() {
		var l = JobKeyLedger.create();
		var k = l.issue("restock", "cat");
		var s = RowJobSubmit.parse(body("{action:'restock',targetId:'dog',idempotencyKey:'" + k.value() + "'}"));
		assertTrue(s.verify(l, "restock").isEmpty());
	}

	@Test void c01_bindingRefusal_codeAndMessage() {
		var r = RowJobSubmit.bindingRefusal();
		assertEquals(JobKeyLedger.REFUSAL_CODE, r.refusalCode);
		assertEquals("This key is not valid for this row.", r.message);
		assertEquals(409, RowJobSubmit.REFUSAL_STATUS);
	}

	@Test void c02_toString_redactsKey() {
		var s = RowJobSubmit.parse(body("{action:'restock',targetId:'dog',idempotencyKey:'secret'}"));
		assertFalse(s.toString().contains("secret"));
	}
}
