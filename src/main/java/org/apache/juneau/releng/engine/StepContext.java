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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;

/**
 * Injected into every {@link ReleaseStep#preview}/{@link ReleaseStep#apply}.
 */
public class StepContext {
	public RunState run;
	public ProcessRunner runner;
	public Consumer<String> log; // This step's RunLog.lineSink(): tees to that step's own log file
									// and that step's own SSE broadcaster.
	public TargetProfile target = TargetProfile.prodDefault(); // stamped by the engine
	public Path stagingRepo; // rm.staging.dir/git/juneau
	public Path stateDir; // rm.state.dir
	public String repoDir; // rm.repo.dir (maintainer's working clone)
	public String committerEmail; // rm.git.committer.email
	public String availid; // Apache LDAP account (from CredentialService)
	public String ldapPassword; // resolved for the duration of a mutating step, then zeroed
	public String gpgKeyId;
	public String gpgPassphrase;
	public String githubToken; // for GH_TOKEN env
	public NexusStagingClient nexus;
	public EmailService email;
	public MilestoneService milestone;
	public Map<String, String> formInputs; // developmentVersion, voteOutcome, tally, repoIdOverride, checklist...

	/**
	 * Runs a subprocess with output teed to the SSE log (helper for step apply/preview implementations).
	 */
	public ProcessRunner.ProcResult exec(List<String> command) {
		return runner.run(command, null, null, log);
	}

	/**
	 * Runs a subprocess with explicit stdin/env, output teed to the SSE log.
	 */
	public ProcessRunner.ProcResult exec(List<String> command, String stdin, Map<String, String> env) {
		return runner.run(command, stdin, env, log);
	}

	/**
	 * Belt-and-suspenders scrub of any secret-shaped token before a command is logged. Secrets are already
	 * kept off argv (passed via stdin/env), so this only guards against a future call site accidentally
	 * placing a credential immediately after a {@code --password}/{@code --token}-style flag.
	 */
	public static List<String> redactArgv(List<String> command) {
		List<String> out = los(command.size());
		var redactNext = false;
		for (var token : command) {
			if (redactNext) {
				out.add("<redacted>");
				redactNext = false;
				continue;
			}
			out.add(token);
			var t = token.toLowerCase();
			redactNext = (t.endsWith("password") || t.endsWith("token") || t.endsWith("passphrase")
					|| t.endsWith("secret")) && !t.contains("from-stdin");
		}
		return out;
	}
}
