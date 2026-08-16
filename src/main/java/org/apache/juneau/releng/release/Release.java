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

package org.apache.juneau.releng.release;

/** One row in the Releases tab. */
public class Release {
	public String version; // "9.2.1"
	public String stage; // "Awaiting vote" | "Distributed" | "Cancelled"
	public String rc; // "RC1" or "—"
	public String status; // "VOTING" | "RELEASED" | "DROPPED" | "DRAFT"
	public String voteCloses; // ISO datetime or "—"
	public String released; // date or "—"
	public String milestoneUrl; // may be null
	public String githubReleaseUrl; // may be null
	public String source; // "github" | "tag" | "state"

	public Release() {
	}

	public Release(String version, String status, String source) {
		this.version = version;
		this.status = status;
		this.source = source;
		this.stage = "—";
		this.rc = "—";
		this.voteCloses = "—";
		this.released = "—";
	}
}
