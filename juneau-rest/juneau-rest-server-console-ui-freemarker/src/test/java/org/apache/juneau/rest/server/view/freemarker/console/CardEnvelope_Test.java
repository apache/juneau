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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link CardEnvelope#parse(String)}: the {@code type="json"} card body (and the
 * sugars that desugar to it) is parsed with {@code Json5Parser.DEFAULT} &mdash; comments, unquoted
 * keys, and single quotes are all first-class, so JSON5 parses where strict JSON would not. Strict
 * JSON is a subset (it still parses). A card body is exactly <b>one</b> object &mdash; not an
 * IRS-style {@code Json5l} multi-record stream: the parser reads a single top-level object and
 * stops, so only the first record is ever a card body.
 *
 * @since 10.0.0
 */
class CardEnvelope_Test extends TestBase {

	@Test void json5_comments_unquotedKeys_singleQuotes() {
		var m = CardEnvelope.parse("""
			{
				// title card
				type: 'html',
				content: '<h1>Title</h1>'
			}
			""");
		assertBean(m, "type,content", "html,<h1>Title</h1>");
	}

	@Test void strictJson_stillWorks() {
		var m = CardEnvelope.parse("{\"type\":\"html\",\"content\":\"<p>x</p>\"}");
		assertEquals("html", m.getString("type"));
	}

	@Test void cardBodyIsOneObject_notAJson5lStream() {
		// A card body is a single object; the parser reads exactly one top-level object and stops, so a
		// multi-record (IRS Json5l) stream never yields a merged/second record as the card body.
		var m = CardEnvelope.parse("{type:'html'}\n{type:'js'}");
		assertEquals("html", m.getString("type"));
	}

	private static final String BASE = "dataUrl:'/d', columns:[{key:'a',label:'A'}]";

	@Test void liftTable_viewLevelKeys_roundTrip() {
		var slot = CardEnvelope.liftTable("c1", CardEnvelope.parse("{" + BASE + """
			, copyLink:false, primary:true, columnConfig:true,
			rowActions:[{id:'del',label:'Delete'}],
			rowClassRules:{'is-bad':'status==\\'bad\\''},
			pausePollingWhileEditing:true
			}"""));
		assertBean(slot.getMap("view"), "id,copyLink,primary,columnConfig,pausePollingWhileEditing", "c1,false,true,true,true");
		assertBean(slot.getMap("view").getList("rowActions").get(0), "id,label", "del,Delete");
		assertTrue(slot.getMap("view").getMap("rowClassRules").containsKey("is-bad"));
	}

	@Test void liftTable_slotLevelKeys_selectionAndBulk() {
		var slot = CardEnvelope.liftTable("c1", CardEnvelope.parse("{" + BASE + """
			, selection:{rowIdField:'id', selectAll:false},
			bulk:{contractVersion:1, actions:[{id:'x'}]}
			}"""));
		assertBean(slot.getMap("selection"), "rowIdField,selectAll", "id,false");
		assertBean(slot.getMap("bulk"), "contractVersion", "1");
		assertFalse(slot.getMap("view").containsKey("selection"));
		assertFalse(slot.getMap("view").containsKey("bulk"));
	}

	@Test void liftTable_absentKeys_notEmitted() {
		var slot = CardEnvelope.liftTable("c1", CardEnvelope.parse("{" + BASE + "}"));
		for (var k : List.of("selection", "bulk"))
			assertFalse(slot.containsKey(k), k);
		for (var k : List.of("copyLink", "rowActions", "rowClassRules", "pausePollingWhileEditing"))
			assertFalse(slot.getMap("view").containsKey(k), k);
	}

	// The new v1 main sources live here; the test CWD is the module directory (surefire).
	private static final Path MAIN =
		Path.of("src/main/java/org/apache/juneau/rest/server/view/freemarker/console");

	@Test void dualHat_cardEnvelope_usesJson5ParserOnly() throws Exception {
		// The card body is parsed with the Juneau Json5Parser, never an IRS Json5l reader, and the file
		// carries no Salesforce package/SLDS markers.
		var src = Files.readString(MAIN.resolve("CardEnvelope.java"));
		assertTrue(src.contains("Json5Parser"), () -> src);
		assertFalse(src.contains("Json5l"), () -> src);
		assertFalse(src.contains("com.sfdc"), () -> src);
		assertFalse(src.contains("slds-"), () -> src);
	}

	@Test void dualHat_newMainSources_haveNoSalesforceMarkers() throws Exception {
		// Grep over the new v1 main sources (page/card/toolkit): no SLDS,
		// no lightning, no Salesforce Sans, no IRS Json5l anywhere in the Apache trees.
		var files = new ArrayList<Path>();
		for (var f : List.of("PageDirectiveModel.java", "CardDirectiveModel.java", "CardEnvelope.java",
				"ToolkitPackRegistry.java", "FtlAttrLists.java"))
			files.add(MAIN.resolve(f));
		for (var p : files) {
			assertTrue(Files.exists(p), () -> "missing source: " + p);
			var src = Files.readString(p);
			assertFalse(src.contains("slds-"), () -> p + " contains slds-");
			assertFalse(src.contains("lightning"), () -> p + " contains lightning");
			assertFalse(src.contains("Salesforce Sans"), () -> p + " contains Salesforce Sans");
			assertFalse(src.contains("Json5l"), () -> p + " contains Json5l");
		}
	}
}
