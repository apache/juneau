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

/** §5.5 javadoc-verify: mvn javadoc:aggregate; human confirms cleanliness. Skippable. */
public class JavadocVerifyStep implements ReleaseStep {
	@Override
	public String id() {
		return "javadoc-verify";
	}

	@Override
	public String title() {
		return "Javadoc verify";
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
		return new Preview(id(), false).line("Will run: mvn javadoc:aggregate in " + ctx.stagingRepo);
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var res = ctx.exec(List.of("mvn", "-f", ctx.stagingRepo.toString() + "/pom.xml", "javadoc:aggregate"));
		// Non-zero-but-noisy javadoc historically continues; require human "looks clean" via the review gate.
		return res.ok() ? StepResult.ok("Javadoc generated — review the log, then confirm.")
				: StepResult.ok("Javadoc finished with warnings/errors — review the log carefully, then confirm.");
	}
}
