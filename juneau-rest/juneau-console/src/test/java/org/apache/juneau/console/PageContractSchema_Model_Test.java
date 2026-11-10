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
package org.apache.juneau.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/** Schema coverage for the client object model additions: {@code model}, {@code handlerRef}, {@code scripts}, handler subscriptions. */
class PageContractSchema_Model_Test extends TestBase {

	private static String page(String extraRoot, String cards) {
		return "{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[]," + extraRoot + "\"cards\":[" + cards + "]}";
	}

	private static String html(String extra) {
		return "{\"id\":\"c1\",\"type\":\"html\",\"src\":\"/x\"" + extra + "}";
	}

	private static List<String> errors(String json) {
		return PageContractSchema.get().validate(json);
	}

	private static void valid(String json) {
		var e = errors(json);
		assertTrue(e.isEmpty(), () -> e + " for " + json);
	}

	private static void invalid(String json) {
		assertFalse(errors(json).isEmpty(), () -> "expected errors for " + json);
	}

	@Test void root_accepts_modelVersion_scripts_allowInline_model() {
		valid(page("\"modelVersion\":\"1\",\"scripts\":[\"/a.js\"],\"allowInlineHandlers\":true,\"model\":{\"on\":{\"loaded\":\"app.hello\"}},", ""));
	}

	@Test void root_rejects_modelVersion_other_than_1() {
		invalid(page("\"modelVersion\":\"2\",", ""));
	}

	@Test void root_model_page_only_on() {
		invalid(page("\"model\":{\"actions\":[{\"type\":\"handler\",\"id\":\"a\",\"handler\":\"app.x\"}]},", ""));
		invalid(page("\"model\":{\"subscribes\":[{\"topic\":\"data:x\",\"handler\":\"app.x\"}]},", ""));
	}

	@ParameterizedTest
	@ValueSource(strings = { "loaded", "rendered", "expanded", "collapsed", "activated" })
	void model_on_event_names_enum(String ev) {
		valid(page("", html(",\"model\":{\"on\":{\"" + ev + "\":\"app.x\"}}")));
	}

	@Test void model_on_unknown_event_rejected() {
		invalid(page("", html(",\"model\":{\"on\":{\"clicked\":\"app.x\"}}")));
	}

	@Test void model_unknown_key_rejected() {
		invalid(page("", html(",\"model\":{\"bogus\":1}")));
	}

	@Test void handlerAction_requires_type_id_handler() {
		invalid(page("", html(",\"model\":{\"actions\":[{\"id\":\"a\",\"handler\":\"app.x\"}]}")));
		invalid(page("", html(",\"model\":{\"actions\":[{\"type\":\"handler\",\"handler\":\"app.x\"}]}")));
		invalid(page("", html(",\"model\":{\"actions\":[{\"type\":\"handler\",\"id\":\"a\"}]}")));
		valid(page("", html(",\"model\":{\"actions\":[{\"type\":\"handler\",\"id\":\"a\",\"handler\":\"app.x\",\"icon\":\"i\",\"payload\":{}}]}")));
	}

	@Test void handlerAction_type_const_handler() {
		invalid(page("", html(",\"model\":{\"actions\":[{\"type\":\"link\",\"id\":\"a\",\"handler\":\"app.x\"}]}")));
	}

	@Test void handlerRef_name_or_inline() {
		valid(page("", html(",\"model\":{\"on\":{\"loaded\":{\"inline\":\"return 1;\"}}}")));
		invalid(page("", html(",\"model\":{\"on\":{\"loaded\":{\"inline\":\"\"}}}")));
		invalid(page("", html(",\"model\":{\"on\":{\"loaded\":{\"inline\":\"x\",\"extra\":1}}}")));
	}

	@Test void handlerRef_bad_name_rejected() {
		invalid(page("", html(",\"model\":{\"on\":{\"loaded\":\"BadName\"}}")));
		invalid(page("", html(",\"model\":{\"on\":{\"loaded\":\"nodot\"}}")));
	}

	@Test void subscription_roleSubscription_ok() {
		valid(page("", html(",\"subscribes\":[{\"topic\":\"selection:t\",\"as\":\"sel\"}]")));
	}

	@Test void subscription_handlerSubscription_ok() {
		valid(page("", html(",\"subscribes\":[{\"topic\":\"selection:t\",\"handler\":\"app.x\"}]")));
	}

	@Test void subscription_both_rejected() {
		invalid(page("", html(",\"subscribes\":[{\"topic\":\"selection:t\",\"as\":\"sel\",\"handler\":\"app.x\"}]")));
	}

	@Test void subscription_neither_rejected() {
		invalid(page("", html(",\"subscribes\":[{\"topic\":\"selection:t\"}]")));
	}

	@Test void templatedTopic_allows_placeholders() {
		valid(page("", html(",\"model\":{\"subscribes\":[{\"topic\":\"detail:{rowKey}\",\"handler\":\"app.x\"}]}")));
		valid(page("", html(",\"model\":{\"subscribes\":[{\"topic\":\"detail:{cardId}.{tabId}\",\"handler\":\"app.x\"}]}")));
		invalid(page("", html(",\"model\":{\"subscribes\":[{\"topic\":\"detail:{bogus}\",\"handler\":\"app.x\"}]}")));
	}

	@Test void every_closed_card_def_accepts_model() {
		var m = ",\"model\":{\"on\":{\"loaded\":\"app.x\"}}";
		valid(page("", html(m)));
		valid(page("", "{\"id\":\"d\",\"type\":\"datatables\",\"src\":\"/x\"" + m + "}"));
		valid(page("", "{\"id\":\"c\",\"type\":\"chart\",\"src\":\"/x\",\"kind\":\"line\"" + m + "}"));
		valid(page("", "{\"id\":\"j\",\"type\":\"job-log\",\"submit\":{\"endpoint\":\"/j\"}" + m + "}"));
		valid(page("", "{\"id\":\"tv\",\"type\":\"terminal-view\",\"terminalView\":{\"poll\":false}" + m + "}"));
		valid(page("", "{\"id\":\"t\",\"type\":\"terminal\",\"terminal\":{\"bytesUrl\":\"/b\",\"eventsUrl\":\"/e\"}" + m + "}"));
	}

	@Test void tableCatalog_rowDetail_ribbonItem_accept_model() {
		var m = "\"model\":{\"on\":{\"loaded\":\"app.x\"}}";
		valid(page("", "{\"id\":\"d\",\"type\":\"datatables\",\"table\":{\"dataUrl\":\"/x\",\"columns\":[{\"key\":\"id\"}]," + m
			+ ",\"detail\":{\"endpoint\":\"/d\"," + m + "},\"ribbon\":[{\"type\":\"publish\"," + m + "}]}}"));
	}

	@Test void tableCatalog_rowDetail_ribbonItem_reject_bad_model() {
		var t = "{\"id\":\"d\",\"type\":\"datatables\",\"table\":{\"dataUrl\":\"/x\",\"columns\":[{\"key\":\"id\"}],";
		invalid(page("", t + "\"detail\":{\"endpoint\":\"/d\",\"model\":{\"bogus\":1}}}}"));
		invalid(page("", t + "\"ribbon\":[{\"type\":\"publish\",\"model\":{\"on\":{\"clicked\":\"app.x\"}}}]}}"));
	}

	@Test void chart_on_actions_untouched() {
		valid(page("", "{\"id\":\"c\",\"type\":\"chart\",\"src\":\"/x\",\"kind\":\"line\",\"on\":{\"click\":\"app.x\"}}"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://x/y.js", "//x/y.js", "javascript:alert(1)", "relative.js", "/\\\\evil.com/x.js", " /a.js", "/a b.js", "/a\\u0001.js", "/a.js\\n" })
	void scripts_rejects_absolute_and_protocol_relative_urls(String url) {
		invalid(page("\"scripts\":[\"" + url + "\"],", ""));
	}

	@Test void scripts_accepts_same_origin_paths() {
		valid(page("\"scripts\":[\"/a.js\",\"/b/c.js\"],", ""));
	}

	@Test void existing_pages_unchanged() {
		valid(page("", ""));
	}
}
