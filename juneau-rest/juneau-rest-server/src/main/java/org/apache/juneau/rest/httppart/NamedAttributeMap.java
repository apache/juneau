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
package org.apache.juneau.rest.httppart;

import java.util.*;

/**
 * A list of {@link NamedAttribute} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HttpParts">HTTP Parts</a>
 * </ul>
 */
public class NamedAttributeMap extends LinkedHashMap<String,NamedAttribute> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @return An empty list.
	 */
	public static NamedAttributeMap create() {
		return new NamedAttributeMap();
	}

	/**
	 * Static creator.
	 *
	 * @param values The initial contents of this list.
	 * @return An empty list.
	 */
	public static NamedAttributeMap of(NamedAttribute...values) {
		return create().add(values);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public NamedAttributeMap() {
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The list to copy from.
	 */
	public NamedAttributeMap(NamedAttributeMap copyFrom) {
		super();
		putAll(copyFrom);
	}

	/**
	 * Creates a copy of this list.
	 *
	 * @return A new copy of this list.
	 */
	public NamedAttributeMap copy() {
		return new NamedAttributeMap(this);
	}

	//-------------------------------------------------------------------------------------------------------------
	// Properties
	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Appends the specified rest matcher classes to the list.
	 *
	 * @param values The values to add.
	 * @return This object.
	 */
	public NamedAttributeMap add(NamedAttribute...values) {
		for (NamedAttribute v : values)
			put(v.getName(), v);
		return this;
	}
}
