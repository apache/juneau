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

import java.util.Map;
import org.apache.juneau.releng.email.EmailTemplate;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.17 compose-result-email: draft-and-open [RESULT] with the tally from §5.16. */
public class ComposeResultEmailStep implements ReleaseStep {
	@Override
	public String id() {
		return "compose-result-email";
	}

	@Override
	public String title() {
		return "Compose [RESULT] email";
	}

	private Map<String, String> vars(StepContext ctx) {
		return Map.of("outcome", ctx.formInputs.getOrDefault("voteOutcome", "passed"), "tally",
				ctx.formInputs.getOrDefault("tally", ""));
	}

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), false);
		for (var line : ctx.email.renderBody(EmailTemplate.RESULT, ctx.run, vars(ctx)).split("\n"))
			p.line(line);
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var path = ctx.email.compose(EmailTemplate.RESULT, ctx.run, vars(ctx));
		return StepResult.ok("Opened draft: " + path);
	}
}
