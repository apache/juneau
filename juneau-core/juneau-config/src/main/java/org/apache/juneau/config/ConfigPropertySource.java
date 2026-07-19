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
package org.apache.juneau.config;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.commons.settings.*;

/**
 * {@link PropertySource} adapter for {@link Config}.
 */
public class ConfigPropertySource implements PropertySource {

	private final Config config;

	/**
	 * Constructor.
	 *
	 * @param config The wrapped config.
	 */
	public ConfigPropertySource(Config config) {
		this.config = config;
	}

	@Override
	public PropertyLookupResult get(String name) {
		if (config == null)
			return PropertyLookupResult.missing();
		try {
			var value = config.getString(name);
			return value == null ? PropertyLookupResult.missing() : PropertyLookupResult.present(o(value));
		} catch (@SuppressWarnings("unused") Exception unused) {
			// A lookup failure (bad key, unreadable config) is treated as an absent property so the property-source chain can fall through to the next source.
			return PropertyLookupResult.missing();
		}
	}
}
