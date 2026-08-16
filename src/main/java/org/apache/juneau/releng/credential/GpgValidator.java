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

/** Validates a GPG key + passphrase: key must exist, and a discard test-sign must succeed. */
public class GpgValidator implements Validator {

	private final ProcessRunner runner;

	public GpgValidator(ProcessRunner runner) {
		this.runner = runner;
	}

	@Override
	public ValidationResult validate(String passphrase, String keyId) {
		var present = runner.run(List.of("gpg", "--list-secret-keys", keyId), null, null);
		if (!present.ok())
			return ValidationResult.fail("No secret key for " + keyId);

		// Test-sign a tiny payload; passphrase on stdin, signature discarded.
		var sign = runner.run(List.of("gpg", "--batch", "--yes", "--pinentry-mode", "loopback", "--passphrase-fd", "0",
				"--local-user", keyId, "--sign", "--output", "/dev/null", "-"), passphrase + "\n", null);
		return sign.ok() ? ValidationResult.ok("GPG key " + keyId + " signs OK")
				: ValidationResult.fail("GPG test-sign failed (bad passphrase?): " + sign.output().strip());
	}
}
