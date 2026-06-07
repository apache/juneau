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
package org.apache.juneau.config;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.config.event.*;
import org.junit.jupiter.api.*;

class ConfigEvent_Test extends TestBase {

	//====================================================================================================
	// Getters
	//====================================================================================================

	@Test void a01_getters() {
		var e = ConfigEvent.setEntry("myconfig.cfg", "MySection", "myKey", "myValue", "*", "myComment", l("line1"));
		assertBean(e, "config,section,key,value,modifiers,comment,preLines,type", "myconfig.cfg,MySection,myKey,myValue,*,myComment,[line1],SET_ENTRY");
	}

	@Test void a02_removeEntry_getters() {
		var e = ConfigEvent.removeEntry("cfg.cfg", "S", "k");
		assertBean(e, "config,section,key,value,modifiers,comment,preLines,type", "cfg.cfg,S,k,<null>,<null>,<null>,<null>,REMOVE_ENTRY");
	}

	@Test void a03_removeSection_getters() {
		var e = ConfigEvent.removeSection("cfg.cfg", "S");
		assertBean(e, "config,section,key,value,modifiers,comment,preLines,type", "cfg.cfg,S,<null>,<null>,<null>,<null>,<null>,REMOVE_SECTION");
	}

	@Test void a04_setSection_getters() {
		var e = ConfigEvent.setSection("cfg.cfg", "S", l("# comment"));
		assertBean(e, "config,section,key,value,modifiers,comment,preLines,type", "cfg.cfg,S,<null>,<null>,<null>,<null>,[# comment],SET_SECTION");
	}

	//====================================================================================================
	// toString() cases
	//====================================================================================================

	@Test void b01_toString_removeSection() {
		var e = ConfigEvent.removeSection("cfg.cfg", "MySection");
		assertEquals("REMOVE_SECTION(MySection)", e.toString());
	}

	@Test void b02_toString_removeEntry_withSection() {
		var e = ConfigEvent.removeEntry("cfg.cfg", "S", "key1");
		assertEquals("REMOVE_ENTRY(S/key1)", e.toString());
	}

	@Test void b03_toString_removeEntry_emptySection() {
		var e = ConfigEvent.removeEntry("cfg.cfg", "", "key1");
		assertEquals("REMOVE_ENTRY(key1)", e.toString());
	}

	@Test void b04_toString_setSection() {
		var e = ConfigEvent.setSection("cfg.cfg", "MySection", l("# pre-line"));
		assertEquals("SET_SECTION(MySection, preLines=# pre-line)", e.toString());
	}

	@Test void b05_toString_setSection_noPrelines() {
		var e = ConfigEvent.setSection("cfg.cfg", "S", l());
		assertEquals("SET_SECTION(S, preLines=)", e.toString());
	}

	@Test void b06_toString_setEntry_basic() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", "value", null, null, null);
		assertEquals("SET(S/key = value)", e.toString());
	}

	@Test void b07_toString_setEntry_emptySection() {
		var e = ConfigEvent.setEntry("cfg.cfg", "", "key", "value", null, null, null);
		assertEquals("SET(key = value)", e.toString());
	}

	@Test void b08_toString_setEntry_withModifiers() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", "value", "*", null, null);
		assertEquals("SET(S/key* = value)", e.toString());
	}

	@Test void b09_toString_setEntry_withComment() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", "value", null, "my comment", null);
		assertEquals("SET(S/key = value # my comment)", e.toString());
	}

	@Test void b10_toString_setEntry_valueWithNewline() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", "line1\nline2", null, null, null);
		var result = e.toString();
		assertTrue(result.startsWith("SET(S/key = line1\n\tline2)"), "Unexpected: " + result);
	}

	@Test void b11_toString_setEntry_valueWithHash() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", "val#ue", null, null, null);
		assertEquals("SET(S/key = val\\#ue)", e.toString());
	}

	@Test void b12_toString_setEntry_nullValue() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", null, null, null, null);
		assertEquals("SET(S/key = null)", e.toString());
	}

	@Test void b13_toString_setEntry_modifiersAndComment() {
		var e = ConfigEvent.setEntry("cfg.cfg", "S", "key", "val", "*", "the comment", null);
		assertEquals("SET(S/key* = val # the comment)", e.toString());
	}
}
