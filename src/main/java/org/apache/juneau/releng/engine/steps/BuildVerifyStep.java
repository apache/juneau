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

/** §5.4 build-verify: mvn clean verify on the checked-out branch. Fully idempotent. */
public class BuildVerifyStep implements ReleaseStep {
	@Override
	public String id() {
		return "build-verify";
	}

	@Override
	public String title() {
		return "Build & verify";
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), false).line("Will run: mvn clean verify in " + ctx.stagingRepo);
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var res = ctx.exec(List.of("mvn", "-f", ctx.stagingRepo.toString() + "/pom.xml", "clean", "verify"));
		return res.ok() ? StepResult.ok("Build + tests passed.") : StepResult.fail("mvn clean verify failed.");
	}
}
