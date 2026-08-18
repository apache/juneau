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

package org.apache.juneau.releng.credential;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.juneau.releng.util.ProcessRunner;

/**
 * Validates a GitHub token via {@code GH_TOKEN=<token> gh api user}.
 *
 * <p>Neither path carries {@code gh}'s raw output into the message. On failure the output is read only to choose
 * among fixed reasons; on success the login is shown, but only after it is checked against GitHub's own username
 * rules, so an unexpected line from {@code gh} cannot become interface text. See {@link GpgValidator} for why an
 * unbounded channel from a third-party tool's stderr into the UI is worth closing even absent a demonstrated leak.
 */
public class GithubTokenValidator implements Validator {

	/** GitHub usernames: alphanumerics and single inner hyphens, 39 characters at most. */
	private static final Pattern LOGIN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}");

	private final ProcessRunner runner;

	public GithubTokenValidator(ProcessRunner runner) {
		this.runner = runner;
	}

	@Override
	public ValidationResult validate(String token, String account) {
		var r = runner.run(List.of("gh", "api", "user", "-q", ".login"), null, Map.of("GH_TOKEN", token));
		if (!r.ok())
			return ValidationResult.fail(reason(r));
		var login = r.output() == null ? "" : r.output().strip();
		return ValidationResult.ok(LOGIN.matcher(login).matches() ? "GitHub token OK (user " + login + ")."
				: "GitHub token OK.");
	}

	/**
	 * The enumerated reason the token was not accepted.
	 *
	 * <p>{@code gh} reports the HTTP status in its output rather than in its exit code, so the output is inspected
	 * to classify — and then discarded rather than displayed.
	 */
	private static String reason(ProcessRunner.ProcResult r) {
		var out = r.output() == null ? "" : r.output().toLowerCase();
		if (r.exitCode() == 127 || out.contains("command not found") || out.contains("no such file or directory"))
			return "gh is not installed or not on the PATH.";
		if (out.contains("bad credentials") || out.contains("401"))
			return "GitHub rejected the token (401 — not accepted).";
		if (out.contains("403") || out.contains("insufficient") || out.contains("scope"))
			return "The token was accepted but lacks the required scope (403).";
		if (out.contains("404"))
			return "GitHub returned 404 for the identity call — the token may be for a different account type.";
		if (out.contains("could not resolve") || out.contains("connection refused") || out.contains("timeout")
				|| out.contains("dial tcp") || out.contains("network is unreachable"))
			return "GitHub was unreachable — check the network.";
		return "GitHub rejected the token (gh exit code " + r.exitCode() + ").";
	}
}
