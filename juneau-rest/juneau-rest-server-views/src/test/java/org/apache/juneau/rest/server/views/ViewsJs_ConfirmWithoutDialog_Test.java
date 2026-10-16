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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * A row action with {@link RowAction#confirm(String) confirm} (or {@link RowAction#confirmRenderer(String)
 * confirmRenderer}) prompts before it submits even without {@code present(DIALOG)}, from every surface that
 * dispatches a row action: the row-action menu, a cell pill, a detail-panel ActionBar button and
 * {@code JuneauViews.rowActions.run}.  Cancel sends nothing; Confirm sends exactly one request, carrying a {@code rowActions.run} caller's {@code extra}.  An action with no
 * confirm, or with {@link RowAction#noConfirm() noConfirm}, still submits directly.
 *
 * <p>Behavioral Node harness ({@code confirm-without-dialog.cjs}); skipped when {@code node} is not on {@code PATH}.
 */
class ViewsJs_ConfirmWithoutDialog_Test extends TestBase {

	private static final List<String> SURFACES = List.of("menu", "pill", "detail", "run");

	private static Map<?,?> report;

	@BeforeAll
	static void probe() {
		report = NodeHarness.report("confirm-without-dialog.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
	}

	private static Map<?,?> surface(String name) {
		assumeTrue(report != null, "node not available or confirm-without-dialog.cjs not found - behavioral layer skipped");
		return (Map<?,?>) report.get(name);
	}

	@Test void a01_cancelSendsNothing() {
		for (var s : SURFACES)
			assertBean(surface(s), "cancel{fetchesAtClick,title,cancelled,fetchesAfterCancel,closed}", "{0,Delete 'Rex'?,true,0,true}");
	}

	@Test void a02_confirmSendsExactlyOneRequest() {
		for (var s : SURFACES)
			assertBean(surface(s), "confirm{fetchesAtClick,title,confirmed,fetches,url,method,action}", "{0,Delete 'Rex'?,true,1,/x/7,DELETE,delete}");
	}

	@Test void a03_noConfirmStillSubmitsDirectly() {
		for (var s : SURFACES)
			assertBean(surface(s), "plain{fetches,dialogs},noConfirm{fetches,dialogs}", "{1,0},{1,0}");
	}

	@Test void a04_aNamedConfirmRendererGatesTheWrite() {
		assertBean(surface("renderer"), "no{asked,fetches},yes{asked,fetches},titles", "{1,0},{2,1},[Delete,Delete]");
	}

	@Test void a05_runExtraIsDeliveredAfterConfirm() {
		assertBean(surface("extra"),
			"builtin{before,fetches,fields,targetId},renderer{fetches,fields,targetId}",
			"{0,1,{reason=dup},7},{1,{reason=dup},7}");
	}

	@Test void a06_runExtraCannotOverrideTheInvokedAction() {
		assertBean(surface("reserved"),
			"direct{fetches,action,note},builtin{fetches,action,note},renderer{fetches,action,note}",
			"{1,delete,n},{1,delete,n},{1,delete,n}");
	}
}
