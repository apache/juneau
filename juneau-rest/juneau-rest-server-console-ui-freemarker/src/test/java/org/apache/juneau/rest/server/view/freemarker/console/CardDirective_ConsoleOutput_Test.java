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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * The {@code <@card type="console-output">} bridge card: a JSON5 {@code {contractVersion, output}} body, validated
 * through {@code ConsoleOutputDef} at render time and emitted as a {@code cards[]} entry with an {@code output}
 * object.
 */
class CardDirective_ConsoleOutput_Test extends TestBase {

	/** The contract entry for card {@code id}. */
	private static JsonMap card(String body, String id) {
		for (var o : assertPage(body).isValid().contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "' in " + body);
	}

	@Test void a01_fullCard() {
		var body = render("co-card");
		assertPage(body).isValid().hasCard("run-output", "console-output");
		assertEquals(
			Json5.to("{id:'run-output',type:'console-output',title:'Output',output:{linesUrl:'/runs/r-7/lines',"
				+ "downloadUrl:'/runs/r-7/download',refreshMs:1000,tail:5000,rows:20,anchorPrefix:'raw-L',markers:'dim',"
				+ "title:'Build output'}}", Map.class),
			Json5.to(Json.of(card(body, "run-output")), Map.class));
		assertFalse(body.contains("<template data-card=\"run-output\">"), () -> body);
	}

	@Test void a02_minimalCard_hasNoTitleAndOnlyLinesUrl() {
		assertEquals(
			Json5.to("{id:'log',type:'console-output',output:{linesUrl:'/logs/1/lines'}}", Map.class),
			Json5.to(Json.of(card(render("co-card-minimal"), "log")), Map.class));
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"co-card-noid|<@card type=\"console-output\"> requires id=.",
		"co-card-src|<@card id='log'> type='console-output' takes its options as the body; src= and template= are not allowed.",
		"co-card-empty|<@card id='log'> type='console-output' requires a JSON5 body { contractVersion: '1', output: {...} }.",
		"co-card-noversion|<@card id='log'> type='console-output' requires contractVersion: '1'; got 'null'.",
		"co-card-extra|<@card id='log'> type='console-output' unknown key 'poll'; allowed: contractVersion, output.",
		"co-card-nooutput|<@card id='log'> type='console-output' requires an output object.",
		"co-card-badkey|ConsoleOutputDef 'log' unknown key 'colour'",
		"co-card-unsafe|linesUrl must be a same-origin path",
		"co-card-idurl|{id}",
	})
	void a03_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
