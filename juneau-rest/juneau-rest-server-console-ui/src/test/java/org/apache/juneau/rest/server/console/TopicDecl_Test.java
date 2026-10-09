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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.TopicDecl.*;
import org.junit.jupiter.api.*;

class TopicDecl_Test extends TestBase {

	@Test void a01_pageLevel() {
		assertEquals("{topic:'app.region-picked',retain:true,publisher:'script'}",
			Json5.DEFAULT.write(TopicDecl.of("app.region-picked").retain(true).publisher(Publisher.SCRIPT).toMap()));
		assertEquals("{topic:'ssc.alert:*',retain:false,publisher:'server'}",
			Json5.DEFAULT.write(TopicDecl.of("ssc.alert:*").retain(false).publisher(Publisher.SERVER).toMap()));
		assertEquals("ribbon", TopicDecl.of("app.go-live").retain(false).publisher(Publisher.RIBBON).toMap().getString("publisher"));
	}

	@Test void a02_jobWildcard_onlyWithServer() {
		assertEquals("{topic:'job:*',retain:true,publisher:'server'}",
			Json5.DEFAULT.write(TopicDecl.of("job:*").retain(true).publisher(Publisher.SERVER).toMap()));
		var e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("job:*").retain(true).publisher(Publisher.SCRIPT).toMap());
		assertEquals("'job' is a framework family; framework topics are implicit and may not be declared", e.getMessage());
	}

	@Test void a03_publication() {
		assertEquals("{topic:'ssc.focus',retain:true}", Json5.DEFAULT.write(TopicDecl.of("ssc.focus").retain(true).toPublicationMap()));
		var e = assertThrows(IllegalArgumentException.class,
			() -> TopicDecl.of("ssc.focus").retain(true).publisher(Publisher.SCRIPT).toPublicationMap());
		assertEquals("topic 'ssc.focus': publisher is page-level only; a card's publishes may not set it", e.getMessage());
		e = assertThrows(IllegalArgumentException.class,
			() -> TopicDecl.of("job:*").retain(true).toPublicationMap());
		assertEquals("'job' is a framework family; framework topics are implicit and may not be declared", e.getMessage());
	}

	@Test void b01_frameworkFamily_isE45() {
		var e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("selection:changes"));
		assertEquals("'selection' is a framework family; framework topics are implicit and may not be declared", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("job:j-91"));
		assertEquals("'job' is a framework family; framework topics are implicit and may not be declared", e.getMessage());
	}

	@Test void b02_unnamespaced_isE45() {
		var e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("focus"));
		assertEquals("custom topic family 'focus' must be namespaced (e.g. 'app.focus')", e.getMessage());
	}

	@Test void b03_badSyntax_isE40() {
		var e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("App.Focus"));
		assertEquals("invalid topic 'App.Focus': expected family[:key] (see the topic syntax)", e.getMessage());
		assertThrows(IllegalArgumentException.class, () -> TopicDecl.of(null));
	}

	@Test void b04_retainRequired() {
		var e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("ssc.focus").publisher(Publisher.SCRIPT).toMap());
		assertEquals("topic 'ssc.focus' is declared with retain=unset here; call retain(true) or retain(false)", e.getMessage());
		assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("ssc.focus").toPublicationMap());
	}

	@Test void b05_pageLevelNeedsPublisher() {
		var e = assertThrows(IllegalArgumentException.class, () -> TopicDecl.of("ssc.focus").retain(true).toMap());
		assertEquals("topic 'ssc.focus': a page-level topic needs publisher(SCRIPT|SERVER|RIBBON)", e.getMessage());
	}
}
