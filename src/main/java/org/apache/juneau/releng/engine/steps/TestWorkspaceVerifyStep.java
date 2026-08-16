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

/**
 * §5.6 test-workspace-verify: unzip the reactor's workspace-template {@code -bin.zip} artifacts; human confirms
 * clean Eclipse import. Skippable.
 *
 * <p>The four modules below (decision S10) are every reactor module whose {@code pom.xml} wires the
 * {@code maven-assembly-plugin} to a {@code src/assembly/bin.xml} descriptor producing a workspace-import
 * template zip — derived from the 10.0 reactor's actual module poms (not just {@code juneau-release.sh}'s
 * older two-zip list, which predates {@code juneau-examples-mcp} and {@code juneau-sc-server}):
 * {@code juneau-examples/juneau-examples-core}, {@code juneau-examples/juneau-examples-mcp},
 * {@code juneau-petstore/juneau-petstore-jetty}, and {@code juneau-sc/juneau-sc-server}. Each assembly's
 * {@code finalName} is {@code <artifactId>-${project.version}}, so at this point in the pipeline (run
 * <b>before</b> {@code release-prepare} bumps the pom versions) the produced file is
 * {@code <artifactId>-<version>-SNAPSHOT-bin.zip} under that module's {@code target/}. {@code juneau-distrib}
 * also produces a {@code bin.zip}, but that's the full source+binary distribution artifact handled by
 * {@code binary-artifacts-stage} (§5.12), not a workspace-import template, so it's excluded here.
 */
public class TestWorkspaceVerifyStep implements ReleaseStep {

	/** {@code modulePath} relative to the staging clone root; {@code artifactId} is also the unzip target name. */
	private record WorkspaceZip(String modulePath, String artifactId) {
	}

	private static final List<WorkspaceZip> ZIPS = List.of(
			new WorkspaceZip("juneau-examples/juneau-examples-core", "juneau-examples-core"),
			new WorkspaceZip("juneau-examples/juneau-examples-mcp", "juneau-examples-mcp"),
			new WorkspaceZip("juneau-petstore/juneau-petstore-jetty", "juneau-petstore-jetty"),
			new WorkspaceZip("juneau-sc/juneau-sc-server", "juneau-sc-server"));

	@Override
	public String id() {
		return "test-workspace-verify";
	}

	@Override
	public String title() {
		return "Test-workspace verify";
	}

	@Override
	public boolean reviewGate() {
		return true;
	}

	@Override
	public boolean skippable() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), false);
		for (var z : ZIPS)
			p.line("Unzip: " + zipPath(ctx, z) + " -> target/workspace/" + z.artifactId());
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var base = ctx.stagingRepo.toString();
		for (var z : ZIPS)
			ctx.exec(List.of("unzip", "-o", zipPath(ctx, z), "-d", base + "/target/workspace/" + z.artifactId()));
		return StepResult.ok("Workspaces unzipped — confirm they import cleanly into Eclipse.");
	}

	/** The pre-{@code release-prepare} SNAPSHOT version drives the assembly's {@code finalName}. */
	private static String zipPath(StepContext ctx, WorkspaceZip z) {
		var snapshot = ctx.run.version + "-SNAPSHOT";
		return ctx.stagingRepo + "/" + z.modulePath() + "/target/" + z.artifactId() + "-" + snapshot + "-bin.zip";
	}
}
