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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.config.*;

/**
 * Listener that can be used to listen for change events for a specific section in a config file.
 * 
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-config.Listeners'>Overview &gt; juneau-config &gt; Listeners</a>
 * </ul>
 */
public class SectionListener extends ConfigListener {

	private boolean isDefault;
	private String prefix;

	/**
	 * Constructor.
	 * 
	 * @param section The name of the section in the config file to listen to.
	 */
	public SectionListener(String section) {
		isDefault = isEmpty(section);
		prefix = isDefault ? null : (section + '/');
	}

	@Override /* ConfigListener */
	public void onChange(ConfigFile cf, Set<String> changes) {
		for (String c : changes) {
			if (isDefault) {
				if (c.indexOf('/') == -1) {
					onChange(cf);
					return;
				}
			} else {
				if (c.startsWith(prefix)) {
					onChange(cf);
					return;
				}
			}
		}
	}

	/**
	 * Signifies that the config file entry changed.
	 * 
	 * @param cf The config file being modified.
	 */
	public void onChange(ConfigFile cf) {}
}
