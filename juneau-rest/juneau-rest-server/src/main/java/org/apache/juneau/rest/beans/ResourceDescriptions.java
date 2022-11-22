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
package org.apache.juneau.rest.beans;

import java.util.*;

/**
 * A list of {@link ResourceDescription} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.UtilityBeans">Utility Beans</a>
 * </ul>
 *
 * @serial exclude
 */
public class ResourceDescriptions extends ArrayList<ResourceDescription> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @return A new {@link ResourceDescriptions} object.
	 */
	public static ResourceDescriptions create() {
		return new ResourceDescriptions();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a new {@link ResourceDescription} to this list.
	 *
	 * @param name The name of the child resource.
	 * @param description The description of the child resource.
	 * @return This object.
	 */
	public ResourceDescriptions append(String name, String description) {
		super.add(new ResourceDescription(name, description));
		return this;
	}
	/**
	 * Adds a new {@link ResourceDescription} to this list when the uri is different from the name.
	 *
	 * @param name The name of the child resource.
	 * @param uri The URI of the child resource.
	 * @param description The description of the child resource.
	 * @return This object.
	 */
	public ResourceDescriptions append(String name, String uri, String description) {
		super.add(new ResourceDescription(name, uri, description));
		return this;
	}
}
