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

import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.16 tally-vote-result: record outcome (passed/rejected) + tally text. rejected -> Drop-RC (engine handles). */
public class TallyVoteResultStep implements ReleaseStep {
	@Override
	public String id() {
		return "tally-vote-result";
	}

	@Override
	public String title() {
		return "Tally vote result";
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), false).line("Submit outcome (passed|rejected) + tally summary.");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var outcome = ctx.formInputs.getOrDefault("voteOutcome", "");
		if (!outcome.equals("passed") && !outcome.equals("rejected"))
			return StepResult.fail("voteOutcome must be 'passed' or 'rejected'.");
		ctx.log.accept("Vote outcome recorded: " + outcome);
		// 'rejected' is routed to Drop-RC by ReleaseEngine.applyStep, not here.
		return StepResult.ok("Vote outcome: " + outcome + ".");
	}
}
