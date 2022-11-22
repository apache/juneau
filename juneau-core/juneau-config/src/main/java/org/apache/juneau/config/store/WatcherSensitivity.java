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
package org.apache.juneau.config.store;

/**
 * Determines how often the file system is polled by the watcher in {@link FileStore}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This relies on internal Sun packages and may not work on all JVMs.
 * </ul>
 */
public enum WatcherSensitivity {

	/** 30 seconds */
	LOW,

	/** 10 seconds */
	MEDIUM,

	/** 2 seconds */
	HIGH;
}
