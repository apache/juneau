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

/** One step's mutable state within a run. */
public class StepState {
	public String id;
	public StepStatus status = StepStatus.PENDING;
	public String startedAt; // ISO-8601 or null
	public String completedAt; // ISO-8601 or null
	public String error; // failure message + exit code, or null
	public Long logOffset; // byte offset into this step's own log file at failure, or null
	public String logRef; // path to this step's own log file, e.g.
							// "logs/9.2.1-RC2-release-prepare.log"; null until first run

	public StepState() {
	}

	public StepState(String id) {
		this.id = id;
	}
}
