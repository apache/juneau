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
package org.apache.juneau.config.event;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

/**
 * Represents a list of {@link ConfigEvent} objects.
 */
public class ConfigEvents extends ArrayList<ConfigEvent> {
	private static final long serialVersionUID = 1L;

	/**
	 * Returns <jk>true</jk> if the specified section was modified in this list of events.
	 *
	 * @param name The section name.
	 * @return <jk>true</jk> if the specified section was modified in this list of events.
	 */
	public boolean isSectionModified(String name) {
		for (ConfigEvent ce : this)
			if (eq(name, ce.getSection()))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified key was modified in this list of events.
	 *
	 * @param section The section name.
	 * @param key The key name.
	 * @return <jk>true</jk> if the specified key was modified in this list of events.
	 */
	public boolean isKeyModified(String section, String key) {
		for (ConfigEvent ce : this)
			if (eq(section, ce.getSection()) && eq(key, ce.getKey()))
				return true;
		return false;
	}
}
