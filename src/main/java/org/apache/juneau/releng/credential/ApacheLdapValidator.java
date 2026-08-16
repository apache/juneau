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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Validates Apache LDAP creds by Basic-authenticating to Nexus (repository.apache.org) status. */
public class ApacheLdapValidator implements Validator {

	static final String NEXUS_STATUS = "https://repository.apache.org/service/local/status";

	private final HttpClient http;
	private final String url;

	public ApacheLdapValidator() {
		this(HttpClient.newHttpClient(), NEXUS_STATUS);
	}

	ApacheLdapValidator(HttpClient http, String url) {
		this.http = http;
		this.url = url;
	}

	@Override
	public ValidationResult validate(String password, String availid) {
		try {
			var basic = Base64.getEncoder().encodeToString((availid + ":" + password).getBytes(StandardCharsets.UTF_8));
			var req = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Basic " + basic).GET().build();
			var resp = http.send(req, HttpResponse.BodyHandlers.discarding());
			var code = resp.statusCode();
			if (code == 200)
				return ValidationResult.ok("Nexus authentication OK (200)");
			if (code == 401 || code == 403)
				return ValidationResult.fail("Nexus rejected credentials (" + code + ")");
			return ValidationResult.fail("Unexpected Nexus status " + code);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ValidationResult.fail("Nexus check error: " + e.getMessage());
		} catch (Exception e) {
			return ValidationResult.fail("Nexus check error: " + e.getMessage());
		}
	}
}
