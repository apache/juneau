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

/**
 * Possible event types for the {@link ConfigEvent} class.
 */
public enum ConfigEventType {

	/**
	 * Set an individual entry value in a config.
	 */
	SET_ENTRY,

	/**
	 * Removes an entry value from a config.
	 */
	REMOVE_ENTRY,

	/**
	 * Adds or replaces a section in a config.
	 */
	SET_SECTION,

	/**
	 * Removes a section from a config.
	 */
	REMOVE_SECTION;
}
