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
package org.apache.juneau.config.listener;

import java.util.*;

import org.apache.juneau.config.*;

/**
 * Listener that can be used to listen for change events in config files.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-config.Listeners'>Overview &gt; juneau-config &gt; Listeners</a>
 * </ul>
 */
public class ConfigListener {

	/**
	 * Gets called immediately after a config file has been loaded.
	 * 
	 * @param cf The config file being loaded.
	 */
	public void onLoad(ConfigFile cf) {}

	/**
	 * Gets called immediately after a config file has been saved.
	 * 
	 * @param cf The config file being saved.
	 */
	public void onSave(ConfigFile cf) {}

	/**
	 * Signifies that the specified values have changed.
	 * 
	 * @param cf The config file being modified.
	 * @param changes The full keys (e.g. <js>"Section/key"</js>) of entries that have changed in the config file.
	 */
	public void onChange(ConfigFile cf, Set<String> changes) {}
}
