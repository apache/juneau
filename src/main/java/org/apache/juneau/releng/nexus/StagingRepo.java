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

package org.apache.juneau.releng.nexus;

/**
 * A Nexus staging repository (subset of fields). Profile id for {@code org.apache.juneau}:
 * {@code 1a24bc7f954a70}. {@code status} is normalized to lowercase open/closed/released regardless of
 * whether the raw JSON field is named {@code type} or {@code state}.
 */
public class StagingRepo {
	public String id; // repositoryId, e.g. "orgapachejuneau-1042"
	public String profileId;
	public String description;
	public String status; // normalized: "open" | "closed" | "released"
	public boolean transitioning; // true while Nexus is still processing a close/promote/drop call
	public String created;

	public StagingRepo() {
	}

	public StagingRepo(String id, String status) {
		this.id = id;
		this.status = status;
	}
}
