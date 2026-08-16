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

/** §5.7 deploy-snapshot: mvn deploy; primes gpg-agent once (passphrase on stdin, never argv). Mutating. */
public class DeploySnapshotStep implements ReleaseStep {
	@Override
	public String id() {
		return "deploy-snapshot";
	}

	@Override
	public String title() {
		return "Deploy snapshot";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), true)
				.line("Will prime gpg-agent (key " + ctx.gpgKeyId + ") then run: mvn deploy in " + ctx.stagingRepo);
	}

	@Override
	public StepResult apply(StepContext ctx) {
		// Prime gpg-agent once: throwaway loopback sign, passphrase via stdin (mirrors GpgValidator).
		ctx.dryRunOr(
				List.of("gpg", "--batch", "--yes", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
						"--local-user", ctx.gpgKeyId, "--sign", "--output", "/dev/null", "-"),
				ctx.gpgPassphrase + "\n", null);
		var res = ctx.dryRunOr(List.of("mvn", "-f", ctx.stagingRepo.toString() + "/pom.xml", "deploy"));
		return res.ok() ? StepResult.ok("SNAPSHOT deployed.") : StepResult.fail("mvn deploy failed.");
	}
}
