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
package org.apache.juneau.server.config.repository;

import org.apache.juneau.json.annotation.*;

/**
 * Configuration item representing a named value (e.g., a config file section).
 */
@Json
public class ConfigItem {

	private String value;

	/**
	 * Constructor.
	 *
	 * @param value The configuration value.
	 */
	public ConfigItem(String value) {
		this.value = value;
	}

	/**
	 * Returns the configuration value.
	 *
	 * @return The value.
	 */
	public String getValue() { return value; }

	/**
	 * Sets the configuration value.
	 *
	 * @param value The value to set.
	 */
	public void setValue(String value) { this.value = value; }
}