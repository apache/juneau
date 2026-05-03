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
package org.apache.juneau.bean.mcp;

import org.apache.juneau.annotation.*;

/**
 * Server logging capability marker (structure may grow with the spec).
 */
@Bean
public class LoggingCapability {

	private String level;

	/**
	 * Optional default log level hint.
	 *
	 * @return The level, or {@code null} if not set.
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * Sets the default log level hint.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public LoggingCapability setLevel(String value) {
		level = value;
		return this;
	}
}
