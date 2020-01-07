// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import java.time.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

/**
 * A snapshot of execution statistics for REST resource classes.
 */
@Bean(bpi="startTime,upTime,methodStats")
public class RestContextStats {
	private final Instant startTime;
	private final List<MethodExecStats> methodStats;

	RestContextStats(Instant startTime, List<MethodExecStats> methodStats) {
		this.startTime = startTime;
		this.methodStats = methodStats;
	}

	/**
	 * Returns the time this REST resource class was started.
	 *
	 * @return The time this REST resource class was started.
	 */
	@Swap(TemporalSwap.IsoInstant.class)
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * Returns the time in milliseconds that this REST resource class has been running.
	 *
	 * @return The time in milliseconds that this REST resource class has been running.
	 */
	public String getUpTime() {
		long s = Duration.between(startTime, Instant.now()).getSeconds();
		return String.format("%dh:%02dm:%02ds", s / 3600, (s % 3600) / 60, (s % 60));
	}

	/**
	 * Returns statistics on all method executions.
	 *
	 * @return Statistics on all method executions.
	 */
	public Collection<MethodExecStats> getMethodStats() {
		return methodStats;
	}
}
