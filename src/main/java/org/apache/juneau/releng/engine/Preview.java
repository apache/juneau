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

import java.util.ArrayList;
import java.util.List;

/** A step's dry-run preview payload. */
public class Preview {
	public String stepId;
	public boolean mutating; // does apply() change remote state?
	public boolean reviewGate; // human-review gate (not a mutation gate)
	public List<String> lines = new ArrayList<>(); // human-readable preview text
	public String overrideField; // optional manual override (e.g. Nexus repo-id), else null

	public Preview() {
	}

	public Preview(String stepId, boolean mutating) {
		this.stepId = stepId;
		this.mutating = mutating;
	}

	public Preview line(String s) {
		lines.add(s);
		return this;
	}
}
