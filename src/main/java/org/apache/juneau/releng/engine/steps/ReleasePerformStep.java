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

/** §5.10 release-perform: build from tag + deploy to a new Nexus staging repo. Mutating. */
public class ReleasePerformStep implements ReleaseStep {
	@Override
	public String id() {
		return "release-perform";
	}

	@Override
	public String title() {
		return "release:perform";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), true).line("Will run: mvn release:perform (builds juneau-" + ctx.run.version + "-RC"
				+ ctx.run.rc + " from target/checkout)");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var res = ctx.dryRunOr(List.of("mvn", "-f", ctx.stagingRepo.toString() + "/pom.xml", "release:perform"));
		return res.ok() ? StepResult.ok("release:perform complete; staging repo populated.")
				: StepResult.fail("mvn release:perform failed.");
	}
}
