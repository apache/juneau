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
package org.apache.juneau.petstore.console.dev;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.widgets.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Dialogs (builder only): a permanent demo of the two dialog navigation styles, a {@code ModalDef.steps} wizard and a
 * {@code nav:"folder"} sectioned form, each opened from a ribbon button.
 *
 * <p>
 * Nothing is stored: the submit endpoint accepts the dialog envelope and answers with a success toast.  The page
 * exists so the screenshot sweep and a person can see both dialogs in every palette and scheme.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	GET /console/dev/dialogs
 * 	GET /console/dev/dialogs/wizard-form
 * 	GET /console/dev/dialogs/folder-form
 * </p>
 */
@Rest(path="/dialogs", title="Dialogs")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class DialogsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * Renders the Dialogs page.
	 *
	 * @param req The request.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return twin(req, DevPageBuilders.dialogs());
	}

	/**
	 * The rows the page's table draws: one per dialog style.
	 *
	 * @return One row per style.
	 */
	@RestGet(path="/rows")
	public List<Map<String,Object>> rows() {
		return List.of(Map.of("style", "wizard", "steps", 3), Map.of("style", "folder", "steps", 3));
	}

	/**
	 * The wizard dialog: three steps with display fields, Back and Next, and a step rail.
	 *
	 * @return The dialog.
	 */
	@RestGet(path="/wizard-form")
	public ModalDef wizardForm() {
		return ModalDef.create("Register a pet")
			.step(ModalDef.Step.of("species").title("Pick a species").field("Species", "Dog").field("Breed", "Beagle"))
			.step(ModalDef.Step.of("owner").title("Owner").field("Name", "Mary Watson").field("Phone", "555-0100"))
			.step(ModalDef.Step.of("review").title("Review").confirmLabel("Register").field("Fee", "$25.00"));
	}

	/**
	 * The folder-tab dialog: a sectioned form drawn with square-topped folder tabs.
	 *
	 * @return The dialog.
	 */
	@RestGet(path="/folder-form")
	public ModalDef folderForm() {
		var form = FormDef.create()
			.section(FormDef.Section.of("basics", "Basics")
				.field(FormDef.Input.of("name", "Name", "text").required())
				.field(FormDef.Input.of("notes", "Notes", "textarea")))
			.section(FormDef.Section.of("options", "Options")
				.field(FormDef.Input.of("notify", "Notify me", "toggle")))
			.section(FormDef.Section.of("danger", "Advanced")
				.field(FormDef.Input.of("code", "Code", "text")));
		return ModalDef.create("Pet settings").nav(ModalDef.NAV_FOLDER).form(form);
	}

	/**
	 * Accepts either dialog's submit and stores nothing.
	 *
	 * @param body The dialog envelope; ignored.
	 * @return A success toast.
	 */
	@RestPost(path="/submit")
	public ActionResult submit(@Content JsonMap body) {
		return ActionResult.ok().message("Saved (demo only, nothing is stored)");
	}
}
