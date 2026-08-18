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
import org.apache.juneau.releng.util.ProcessRunner;

/**
 * Validates a GPG key + passphrase: key must exist, and a discard test-sign must succeed.
 *
 * <p>Failure messages are drawn from a fixed set and never include {@code gpg}'s own output. That output is a
 * third-party program's stderr heading for a JSON response, the credential card and a table column, and while
 * {@code gpg} is not known to echo a passphrase it was given, a rule that secrets stay out of the interface cannot
 * rest on another program's discretion about what it prints. The exit code is included because it is a bounded
 * integer and is the one detail that distinguishes otherwise identical failures; anyone needing more can run the
 * command themselves.
 */
public class GpgValidator implements Validator {

	private final ProcessRunner runner;

	public GpgValidator(ProcessRunner runner) {
		this.runner = runner;
	}

	@Override
	public ValidationResult validate(String passphrase, String keyId) {
		var present = runner.run(List.of("gpg", "--list-secret-keys", keyId), null, null);
		if (!present.ok())
			return ValidationResult.fail(missingTool(present) ? "gpg is not installed or not on the PATH."
					: "No secret key for " + keyId + ".");

		// Test-sign a tiny payload; passphrase on stdin, signature discarded.
		var sign = runner.run(List.of("gpg", "--batch", "--yes", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
				"--local-user", keyId, "--sign", "--output", "/dev/null", "-"), passphrase + "\n", null);
		return sign.ok() ? ValidationResult.ok("GPG key " + keyId + " signs OK.") : ValidationResult.fail(reason(sign));
	}

	/**
	 * The enumerated reason a test-sign failed.
	 *
	 * <p>{@code gpg}'s output is read here to choose among fixed strings, and is not carried into any of them.
	 */
	private static String reason(ProcessRunner.ProcResult r) {
		if (missingTool(r))
			return "gpg is not installed or not on the PATH.";
		var out = r.output() == null ? "" : r.output().toLowerCase();
		if (out.contains("bad passphrase") || out.contains("bad session key"))
			return "The passphrase was rejected by gpg.";
		if (out.contains("no secret key") || out.contains("no such key"))
			return "gpg has no secret key for that key ID.";
		if (out.contains("pinentry") || out.contains("inappropriate ioctl"))
			return "gpg could not read the passphrase non-interactively (pinentry).";
		if (out.contains("expired"))
			return "The signing key has expired.";
		if (out.contains("revoked"))
			return "The signing key has been revoked.";
		return "gpg refused to sign (exit code " + r.exitCode() + ").";
	}

	private static boolean missingTool(ProcessRunner.ProcResult r) {
		var out = r.output() == null ? "" : r.output().toLowerCase();
		return r.exitCode() == 127 || out.contains("command not found") || out.contains("no such file or directory");
	}
}
