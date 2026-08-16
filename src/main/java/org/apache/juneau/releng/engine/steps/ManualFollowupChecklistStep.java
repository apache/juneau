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

package org.apache.juneau.releng.engine.steps;

import java.util.List;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.22 manual-followup-checklist: reminder gate for the intentionally-manual tasks. No automation. */
public class ManualFollowupChecklistStep implements ReleaseStep {
	static final List<String> ITEMS = List.of("Update juneau-docs release-notes page + site",
			"Publish aggregate Javadoc", "Update the download page", "Edit the Confluence release wiki");

	@Override
	public String id() {
		return "manual-followup-checklist";
	}

	@Override
	public String title() {
		return "Manual follow-up checklist";
	}

	@Override
	public boolean reviewGate() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), false);
		for (var i : ITEMS)
			p.line("[ ] " + i);
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var checked = ctx.formInputs.getOrDefault("checklist", "");
		for (var i : ITEMS)
			if (!checked.contains(i))
				return StepResult.fail("All items must be checked.");
		return StepResult.ok("All manual follow-ups acknowledged.");
	}
}
