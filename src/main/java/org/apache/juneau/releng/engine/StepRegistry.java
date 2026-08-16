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

import java.util.List;
import org.apache.juneau.releng.engine.steps.*;

/** The ordered list of the 24 pipeline steps. */
public class StepRegistry {

	private final List<ReleaseStep> steps;

	public StepRegistry(List<ReleaseStep> steps) {
		this.steps = List.copyOf(steps);
	}

	/** The canonical 24-step pipeline in spec order. */
	public static StepRegistry standard(BranchResolver branches) {
		return new StepRegistry(List.of(new PreflightStep(branches), // 1
				new ComposeProposeEmailStep(), // 2
				new WorkspaceSetupStep(), // 3
				new BuildVerifyStep(), // 4
				new JavadocVerifyStep(), // 5
				new TestWorkspaceVerifyStep(), // 6
				new DeploySnapshotStep(), // 7
				new ReleasePrepareStep(), // 8
				new ReleaseDiffReviewStep(), // 9
				new ReleasePerformStep(), // 10
				new NexusStagingCloseStep(), // 11
				new BinaryArtifactsStageStep(), // 12
				new DevDistVerifyStep(), // 13
				new ComposeVoteEmailStep(), // 14
				new VoteGateStep(), // 15
				new TallyVoteResultStep(), // 16
				new ComposeResultEmailStep(), // 17
				new NexusReleaseStep(), // 18
				new DistPromoteStep(), // 19
				new GithubReleaseCreateStep(), // 20
				new MilestoneCloseStep(), // 21
				new ManualFollowupChecklistStep(), // 22
				new ComposeAnnouncementEmailStep(), // 23
				new FinalizeRunStep() // 24
		));
	}

	public List<ReleaseStep> steps() {
		return steps;
	}

	public List<String> ids() {
		return steps.stream().map(ReleaseStep::id).toList();
	}

	public ReleaseStep byId(String id) {
		for (var s : steps)
			if (s.id().equals(id))
				return s;
		return null;
	}

	/** The 0-based index of the first step reset by Drop-RC: {@code workspace-setup}. */
	public static final String DROP_RC_RESET_FROM = "workspace-setup";
}
