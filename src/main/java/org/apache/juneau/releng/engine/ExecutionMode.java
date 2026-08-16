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

/**
 * Global box-wide execution posture for the release engine, and the cap on per-run mode.
 *
 * <p>{@code SAFE} (the default) rehearses a release with zero canonical side effects: the Nexus staging
 * callouts run their real client flow against an in-app loopback mock, and every other mutating callout is
 * command-logged rather than executed. {@code LIVE} executes everything for real and additionally requires
 * the run to be armed before any mutating step is allowed.
 *
 * <p>Each run stores its own mode (Dry-run vs Actual on the start form). A LIVE run is only possible when
 * this box was started with {@code rm.mode=live}; a SAFE box always caps the run to SAFE.
 */
public enum ExecutionMode {
	SAFE, LIVE;

	/** Case-insensitive parse; a null/blank/unknown value fails safe to {@link #SAFE}. */
	public static ExecutionMode fromConfig(String value) {
		if (value == null)
			return SAFE;
		return "live".equalsIgnoreCase(value.strip()) ? LIVE : SAFE;
	}
}
