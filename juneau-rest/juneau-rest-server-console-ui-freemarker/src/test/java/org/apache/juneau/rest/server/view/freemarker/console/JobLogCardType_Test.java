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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class JobLogCardType_Test extends TestBase {

	private static Map<String,Object> frag(String body) {
		var b = CardSource.create("job-log", "j");
		if (body != null)
			b.body(body);
		return new JobLogCardType().toFragment(b.build());
	}

	@Test void a01_type_andNoBusRoles() {
		var h = new JobLogCardType();
		assertEquals("job-log", h.type());
		assertEquals(Set.of(), h.acceptedRoles());
		assertEquals(Set.of(), h.acceptedOps());
	}

	@Test void a02_fullBody_passesThrough() {
		var body = "{submit:{endpoint:'/jobs/rebuild',method:'PUT',fields:[{name:'reason',type:'text',required:true}],submitLabel:'Rebuild'},"
			+ "height:320,urlParam:'job',clearOnSubmit:false}";
		assertEquals(Json5.to(body, Map.class), Json5.to(Json.of(frag(body)), Map.class));
	}

	@Test void a03_visibleWhenNotEchoed() {
		assertFalse(frag("{submit:{endpoint:'/a'},visibleWhen:{field:'f',op:'present'}}").containsKey("visibleWhen"));
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"<null>|<@card id='j'> type='job-log' requires a JSON5 body with a 'submit' object.",
		"{submit:{endpoint:'/a'},colour:'red'}|<@card id='j'> type='job-log' unknown key 'colour'; allowed: 'submit, height, urlParam, clearOnSubmit, visibleWhen'.",
		"{submit:{method:'POST'}}|<@card id='j'> type='job-log' requires submit.endpoint.",
		"{submit:'x'}|<@card id='j'> type='job-log' requires submit.endpoint.",
		"{submit:{endpoint:' '}}|<@card id='j'> type='job-log' requires submit.endpoint.",
		"{submit:{endpoint:'https://evil.test/x'}}|<@card id='j'> type='job-log' submit.endpoint must be a same-origin path; got 'https://evil.test/x'.",
		"{submit:{endpoint:'//evil.test/x'}}|<@card id='j'> type='job-log' submit.endpoint must be a same-origin path; got '//evil.test/x'.",
		"{submit:{endpoint:'/a',method:'GET'}}|<@card id='j'> type='job-log' submit.method must be POST or PUT; got 'GET'.",
		"{submit:{endpoint:'/a'},height:40}|<@card id='j'> type='job-log' height must be an integer of at least '80'; got '40'.",
		"{submit:{endpoint:'/a'},height:'tall'}|<@card id='j'> type='job-log' height must be an integer of at least '80'; got 'tall'.",
		"{submit:{endpoint:'/a'},urlParam:'1x'}|<@card id='j'> type='job-log' urlParam must match '[A-Za-z][A-Za-z0-9_-]*'; got '1x'.",
		"{submit:{endpoint:'/a',fields:[{name:'idempotencyKey'}]}}|<@card id='j'> type='job-log' submit.fields name 'idempotencyKey' is reserved; the runtime sends it with every submit.",
		"{submit:{endpoint:'/a',verb:'POST'}}|<@card id='j'> type='job-log' submit unknown key 'verb'; allowed: 'endpoint, method, fields, submitLabel'.",
		"{submit:{endpoint:'/a',submitLabel:3}}|<@card id='j'> type='job-log' submit.submitLabel must be a string; got '3'.",
		"{submit:{endpoint:'/a',fields:'x'}}|<@card id='j'> type='job-log' submit.fields must be an array; got 'x'.",
		"{submit:{endpoint:'/a',fields:['x']}}|<@card id='j'> type='job-log' submit.fields[0] must be an object; got 'x'.",
		"{submit:{endpoint:'/a',fields:[{label:'L'}]}}|<@card id='j'> type='job-log' submit.fields[0] requires a non-blank 'name'.",
		"{submit:{endpoint:'/a',fields:[{name:3}]}}|<@card id='j'> type='job-log' submit.fields[0] requires a non-blank 'name'.",
		"{submit:{endpoint:'/a',fields:[{name:'n'},{name:'m',size:3}]}}|<@card id='j'> type='job-log' submit.fields[1] unknown key 'size'; allowed: 'name, label, type, placeholder, required, default'.",
		"{submit:{endpoint:'/a',fields:[{name:'n',type:'date'}]}}|<@card id='j'> type='job-log' submit.fields[0] type must be 'text', 'textarea' or 'number'; got 'date'.",
		"{submit:{endpoint:'/a',fields:[{name:'n',label:3}]}}|<@card id='j'> type='job-log' submit.fields[0] label must be a string; got '3'.",
		"{submit:{endpoint:'/a',fields:[{name:'n',placeholder:true}]}}|<@card id='j'> type='job-log' submit.fields[0] placeholder must be a string; got 'true'.",
		"{submit:{endpoint:'/a',fields:[{name:'n',required:'yes'}]}}|<@card id='j'> type='job-log' submit.fields[0] required must be a boolean; got 'yes'.",
		"{submit:{endpoint:'/a'},clearOnSubmit:'no'}|<@card id='j'> type='job-log' clearOnSubmit must be a boolean; got 'no'.",
		"{submit:{endpoint:'/a'},clearOnSubmit:0}|<@card id='j'> type='job-log' clearOnSubmit must be a boolean; got '0'.",
	}, nullValues="<null>")
	void a04_errors(String body, String message) {
		assertEquals(message, assertThrows(IllegalArgumentException.class, () -> frag(body)).getMessage());
	}

	@Test void a05_everyFieldKeyAndType_passes() {
		for (var t : List.of("text", "textarea", "number"))
			assertNotNull(frag("{submit:{endpoint:'/a',fields:[{name:'n',label:'N',type:'" + t + "',placeholder:'p',required:false,default:1}]},clearOnSubmit:true}"));
	}
}
