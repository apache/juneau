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
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * {@link JobLogRegion}: the builder, {@link JobLogRegion#fromMap(String, Map)}, validation and the compiled
 * {@link RegionDef}.
 */
class JobLogRegion_Test extends TestBase {

	private static JobLogRegion.Submit submit() {
		return JobLogRegion.Submit.create("/jobs/species/{id}/restock", "restock-species", "/jobs/species/{id}/key");
	}

	@SuppressWarnings({
		"unchecked" // A JSON5 object literal parses to a string-keyed map.
	})
	private static Map<String,Object> json(String json5) {
		return Json5.to(json5, Map.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder and compiled region
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_toRegion_isAnOptedInRowDetailRegion() {
		var r = JobLogRegion.create("restock").submit(submit()).toRegion();
		assertEquals(RegionDef.TYPE_ROW_DETAIL, r.type);
		assertEquals("job-log", r.populate);
		assertEquals(Set.of("job-log"), Set.copyOf(r.allowedPopulators));
		assertEquals("action", ((Map<?,?>)r.config.get("submit")).get("envelope"));
	}

	@Test void a02_fullBuilder_emitsCanonicalConfig() {
		var r = JobLogRegion.create("restock")
			.output(JobLogRegion.Output.field("available").from("row.available").when("any").mode("replace"))
			.clearOnSubmit(true)
			.height(120)
			.detail(JobLogRegion.Detail.of("fieldGrid", RegionDef.Field.of("available").label("Available")))
			.submit(submit().submitLabel("Restock").method("POST")
				.field(JobLogRegion.Field.of("count").label("How many").type("number").required(true).defaultValue(1))
				.field(JobLogRegion.Field.of("species").type("hidden").source("row.species")))
			.toRegion();
		assertEquals(
			"{submit:{endpoint:'/jobs/species/{id}/restock',method:'POST',envelope:'action',action:'restock-species',keyUrl:'/jobs/species/{id}/key',"
				+ "fields:[{name:'count',label:'How many',type:'number',required:true,'default':1},{name:'species',type:'hidden',source:'row.species'}],submitLabel:'Restock'},"
				+ "detail:{renderer:'fieldGrid',fields:[{data:'available',label:'Available'}]},height:120,clearOnSubmit:true,"
				+ "output:{target:{field:'available'},mode:'replace',from:'row.available',when:'any'}}",
			Json5.of(r.config));
	}

	@Test void a03_fromMap_matchesBuilder() {
		var built = JobLogRegion.create("restock").height(120).submit(submit().field(JobLogRegion.Field.of("count").type("number"))).toMap();
		var parsed = JobLogRegion.fromMap("restock", json(
			"{height:120,submit:{keyUrl:'/jobs/species/{id}/key',fields:[{type:'number',name:'count'}],action:'restock-species',envelope:'action',endpoint:'/jobs/species/{id}/restock'}}"))
			.validate().toMap();
		assertEquals(Json5.of(built), Json5.of(parsed));
	}

	@Test void a04_contractMap_carriesConfig() {
		var m = JobLogRegion.create("restock").submit(submit()).toRegion().toContractMap();
		assertTrue(m.get("config") instanceof Map<?,?> c && c.containsKey("submit"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_fromMap_unknownKey() {
		var arg = json("{colour:'red'}");
		var e = assertThrows(IllegalArgumentException.class, () -> JobLogRegion.fromMap("r", arg));
		assertEquals("JobLogRegion 'r' unknown key 'colour'; allowed: [submit, detail, height, clearOnSubmit, output].", e.getMessage());
	}

	@Test void b02_fromMap_urlParam_hasItsOwnRefusal() {
		var arg2 = json("{urlParam:'job'}");
		var e = assertThrows(IllegalArgumentException.class, () -> JobLogRegion.fromMap("r", arg2));
		assertEquals("JobLogRegion 'r' urlParam is not allowed in a row-detail region; a page reload does not reattach.", e.getMessage());
	}

	@Test void b03_blankId() {
		var d = JobLogRegion.create(" ").submit(submit());
		assertEquals("JobLogRegion id must not be blank.", assertThrows(IllegalArgumentException.class, d::validate).getMessage());
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"{}|JobLogRegion 'r' requires a 'submit' object.",
		"{submit:{action:'a',keyUrl:'/k'}}|JobLogRegion 'r' submit.endpoint is required.",
		"{submit:{endpoint:'/e',action:'a'}}|JobLogRegion 'r' submit.keyUrl is required.",
		"{submit:{endpoint:'https://evil.test/e',action:'a',keyUrl:'/k'}}|JobLogRegion 'r' submit.endpoint must be a same-origin path (no scheme, no leading '//', no '..' segment); got 'https://evil.test/e'.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'//evil.test/k'}}|JobLogRegion 'r' submit.keyUrl must be a same-origin path (no scheme, no leading '//', no '..' segment); got '//evil.test/k'.",
		"{submit:{endpoint:'/e/{name}',action:'a',keyUrl:'/k'}}|JobLogRegion 'r' submit.endpoint may only use the '{id}' token; got '{name}'.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'/k',envelope:'flat'}}|JobLogRegion 'r' submit.envelope must be 'action' in a row-detail region (the row-bound key is verified against targetId); got 'flat'.",
		"{submit:{endpoint:'/e',keyUrl:'/k'}}|JobLogRegion 'r' submit.action is required with envelope 'action'.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'/k',fields:[{name:'n',source:'row.a.b'}]}}|JobLogRegion 'r' submit.fields[0] source must match 'row\\.[A-Za-z_][A-Za-z0-9_]{0,63}'; got 'row.a.b'.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'/k'},height:20}|JobLogRegion 'r' height must be an integer of at least '80'; got '20'.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'/k'},detail:{renderer:'fieldGrid'}}|JobLogRegion 'r' detail.fields must be an array.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'/k'},detail:{fields:[]}}|JobLogRegion 'r' detail.renderer must be a non-blank string.",
		"{submit:{endpoint:'/e',action:'a',keyUrl:'/k'},output:{}}|JobLogRegion 'r' output.target must be an object with exactly one of 'field' or 'selector'.",
	})
	void b04_validate_errors(String json, String message) {
		var d = JobLogRegion.fromMap("r", json(json));
		assertEquals(message, assertThrows(IllegalArgumentException.class, d::validate).getMessage());
	}

	@Test void b05_idempotencyKeyField_allowedUnderActionEnvelope() {
		JobLogRegion.create("r").submit(submit().field(JobLogRegion.Field.of("idempotencyKey"))).validate();
	}
}
