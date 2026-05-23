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
package org.apache.juneau.rest.health;

import static java.util.EnumSet.*;

import java.util.*;

/**
 * Functional SPI for reporting health of one component.
 *
 * @since 9.5.0
 */
@FunctionalInterface
public interface HealthIndicator {

	/**
	 * Performs the health check.
	 *
	 * @return Health result. Never <jk>null</jk>.
	 */
	Health check();

	/**
	 * Probe categories where this indicator should run.
	 *
	 * <p>
	 * Default is liveness + readiness.
	 *
	 * @return Probe categories. Never <jk>null</jk>.
	 */
	default EnumSet<HealthProbe> probes() {
		return of(HealthProbe.LIVE, HealthProbe.READY);
	}
}
