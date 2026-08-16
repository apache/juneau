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

/** One discrete, resumable, idempotent pipeline step. */
public interface ReleaseStep {

	/** Stable id used in run state + REST paths (e.g. "workspace-setup"). */
	String id();

	/** Human title for the left-rail. */
	String title();

	/** Does apply() mutate remote state (git remote / SVN / Nexus / GitHub)? Drives the confirm gate. */
	default boolean mutating() {
		return false;
	}

	/** Human-review gate (run then require explicit "looks good") rather than a mutation gate. */
	default boolean reviewGate() {
		return false;
	}

	/** May the run mark this step SKIPPED? */
	default boolean skippable() {
		return false;
	}

	/** Compute the dry-run preview without mutating anything. */
	Preview preview(StepContext ctx);

	/** Execute the step (idempotent/safely re-runnable per each step's §5 notes). */
	StepResult apply(StepContext ctx);
}
