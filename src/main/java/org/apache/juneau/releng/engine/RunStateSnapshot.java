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

package org.apache.juneau.releng.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * A compact projection of a {@link RunState} (plus the engine-level {@code mode}/armed posture that isn't
 * part of {@code RunState} itself) pushed to every connected New-Release tab over the run-state SSE
 * channel. Deliberately NOT the whole {@code RunState} — the rail only needs each step's status, not its
 * timestamps/log refs/error text, which stay on the per-step console channel and the initial page render.
 */
public class RunStateSnapshot {

	public String version;
	public RunStatus status;
	public int rc;
	public ExecutionMode mode;
	public boolean armed;
	public List<StepSnapshot> steps = new ArrayList<>();

	public RunStateSnapshot() {
		// No-arg constructor required so the JSON parser can instantiate this bean before populating fields.
	}

	/** Projects {@code rs} down to the fields the rail needs, folding in the engine-level mode/armed posture. */
	public static RunStateSnapshot of(RunState rs, ExecutionMode mode, boolean armed) {
		var s = new RunStateSnapshot();
		s.version = rs.version;
		s.status = rs.status;
		s.rc = rs.rc;
		s.mode = mode;
		s.armed = armed;
		for (var step : rs.steps)
			s.steps.add(new StepSnapshot(step.id, step.status));
		return s;
	}

	/** One step's id + status — the only two fields the rail patches live. */
	public static class StepSnapshot {
		public String stepId;
		public StepStatus status;

		public StepSnapshot() {
			// No-arg constructor required so the JSON parser can instantiate this bean before populating fields.
		}

		public StepSnapshot(String stepId, StepStatus status) {
			this.stepId = stepId;
			this.status = status;
		}
	}
}
