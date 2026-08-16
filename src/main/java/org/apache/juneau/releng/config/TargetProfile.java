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

package org.apache.juneau.releng.config;

/**
 * Centralizes the release target's endpoints in one config-backed, auditable object, replacing the endpoint
 * literals that used to be scattered across the pipeline steps and the Nexus client.
 *
 * <p>{@code nexusBaseUrl} is mode-derived by the application wiring: it points at the in-app loopback mock
 * under SAFE mode and at real Nexus under LIVE mode. Everything else defaults to the canonical Apache Juneau
 * production endpoints (see {@link #prodDefault()}). This slice ships prod defaults only; no fork target is
 * defined.
 */
public record TargetProfile(String cloneUrl, String repoSlug, String ghSlug, String nexusBaseUrl,
		String nexusProfileId, String distDevBase, String distReleaseBase) {

	/** The canonical Apache Juneau production endpoints (the former hardcoded literals). */
	public static TargetProfile prodDefault() {
		return new TargetProfile("https://gitbox.apache.org/repos/asf/juneau.git", "apache/juneau", "apache/juneau",
				"https://repository.apache.org", "1a24bc7f954a70", "https://dist.apache.org/repos/dist/dev/juneau",
				"https://dist.apache.org/repos/dist/release/juneau");
	}

	/** A copy of this profile with a different Nexus base URL (used to inject the mode-derived base). */
	public TargetProfile withNexusBaseUrl(String url) {
		return new TargetProfile(cloneUrl, repoSlug, ghSlug, url, nexusProfileId, distDevBase, distReleaseBase);
	}
}
