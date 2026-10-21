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
package org.apache.juneau.releng.run;

/**
 * One release.py attempt, from {@code run.json} and/or JRM's {@code jrm-attempt.json} sidecar.
 */
public class Attempt {

	/** The attempt number within its run, or 0 for a sidecar-only attempt. */
	public int n;

	/** The History label: {@code #<n>} for a run-file attempt, else the attempt directory name. */
	public String label;

	/** The terminal id, {@code attempt-<dir name>}. */
	public String id;

	/** The attempt directory. */
	public String dir;

	/** {@code start}, {@code resume}, {@code start-step} or {@code revert}. */
	public String command;

	/** {@code ui} or {@code cli}. */
	public String origin;

	/** The status as recorded: {@code running}, {@code ok}, {@code paused}, {@code failed} or {@code cancelled}. */
	public String status;

	/** The status as shown, with interruptions derived. */
	public String outcome;

	/** The step the attempt stopped in. */
	public String step;

	/** {@code YYYYMMDD-HHMMSS}. */
	public String started;

	/** {@code YYYYMMDD-HHMMSS}. */
	public String ended;

	/** The script's process id, or {@code null}. */
	public Long pid;

	/** The exit code, or {@code null}. */
	public Integer exit;

	/** Whether this was a dry run. */
	public boolean dryRun;

	/** Whether {@code terminal.log} exists (a UI attempt), else the replay is {@code full.log}. */
	public boolean hasTerminalLog;
}
