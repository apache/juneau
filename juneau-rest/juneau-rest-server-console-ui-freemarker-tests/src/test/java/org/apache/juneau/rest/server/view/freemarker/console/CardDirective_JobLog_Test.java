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

/** The {@code <@card type="job-log">} card and its {@code joblog} toolkit pack. */
class CardDirective_JobLog_Test extends TestBase {

	private static JsonMap card(String body, String id) {
		for (var o : assertPage(body).isValid().contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "' in " + body);
	}

	@Test void a01_card() {
		assertEquals(
			Json5.to("{id:'rebuild',type:'job-log',title:'Rebuild index',submit:{endpoint:'/console/jobs/rebuild',"
				+ "fields:[{name:'reason',label:'Reason',type:'text'}],submitLabel:'Rebuild'},urlParam:'job'}", Map.class),
			Json5.to(Json.of(card(render("jl-card"), "rebuild")), Map.class));
	}

	@Test void a02_jobLogScriptEmittedOnce() {
		var body = render("jl-card");
		var s = "/juneau-console/juneau-joblog.js?v=";
		assertTrue(body.contains(s), () -> body);
		assertEquals(body.indexOf(s), body.lastIndexOf(s), () -> body);
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"jl-card-src|<@card id='rebuild'> type='job-log' takes its options as the body; src= is not allowed.",
		"jl-card-template|<@card id='rebuild'> type='job-log' takes its options as the body; template= is not allowed.",
		"jl-card-both|<@card id='rebuild'> type='job-log' takes its options as the body; src= and template= are not allowed.",
		"jl-card-noendpoint|<@card id='rebuild'> type='job-log' requires submit.endpoint.",
	})
	void a03_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
