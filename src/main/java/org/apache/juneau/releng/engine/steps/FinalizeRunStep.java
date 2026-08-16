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
import org.apache.juneau.releng.engine.RunStatus;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/**
 * finalize-run: mark RELEASED, freeze state, release the single-active-run lock. Terminal.
 *
 * <p>{@link org.apache.juneau.releng.engine.ReleaseEngine#apply} refuses to invoke this step's
 * {@code apply()} at all unless every required predecessor has already reached a terminal-success
 * state, so this method itself never needs to re-check prerequisites.
 */
public class FinalizeRunStep implements ReleaseStep {
	@Override
	public String id() {
		return "finalize-run";
	}

	@Override
	public String title() {
		return "Finalize run";
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), false).line("Mark run RELEASED and freeze state.");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		ctx.run.status = RunStatus.RELEASED;
		ctx.log.accept("Run finalized: " + ctx.run.version + " released.");
		return StepResult.ok("Run finalized.");
	}
}
