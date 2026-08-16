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
import org.apache.juneau.releng.util.ProcessRunner;

/** Validates a GitHub token via {@code GH_TOKEN=<token> gh api user}. */
public class GithubTokenValidator implements Validator {

	private final ProcessRunner runner;

	public GithubTokenValidator(ProcessRunner runner) {
		this.runner = runner;
	}

	@Override
	public ValidationResult validate(String token, String account) {
		var r = runner.run(List.of("gh", "api", "user", "-q", ".login"), null, Map.of("GH_TOKEN", token));
		return r.ok() ? ValidationResult.ok("GitHub token OK (user " + r.output().strip() + ")")
				: ValidationResult.fail("GitHub token rejected: " + r.output().strip());
	}
}
