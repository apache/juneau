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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.15 vote-gate: hold the pipeline open ~72h. Entering it is the gate; only tally-vote-result advances. */
public class VoteGateStep implements ReleaseStep {
	@Override
	public String id() {
		return "vote-gate";
	}

	@Override
	public String title() {
		return "Awaiting vote";
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), false).line("Sets status AWAITING_VOTE; deadline = now + 72h.");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		ctx.run.voteDeadline = Instant.now().plus(72, ChronoUnit.HOURS).toString();
		ctx.log.accept("Vote opened; closes " + ctx.run.voteDeadline);
		return StepResult.ok("Awaiting vote until " + ctx.run.voteDeadline + ".");
	}
}
