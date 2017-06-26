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
package org.apache.juneau.dto.jsonschema;

import java.util.*;

/**
 * Represents a list of {@link Schema} objects.
 * <p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 	<ul>
 * 		<li class='sublink'><a class='doclink'
 * 			href='../../../../../overview-summary.html#DTOs.JsonSchema'>JSON-Schema</a>
 * 	</ul>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.jsonschema</a>
 * </ul>
 */
public class SchemaArray extends LinkedList<Schema> {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public SchemaArray() {}

	/**
	 * Constructor with predefined types to add to this list.
	 *
	 * @param schemas The list of schemas in this array.
	 */
	public SchemaArray(Schema...schemas) {
		addAll(schemas);
	}

	/**
	 * Convenience method for adding one or more {@link Schema} objects to this array.
	 *
	 * @param schemas The {@link Schema} objects to add to this array.
	 * @return This object (for method chaining).
	 */
	public SchemaArray addAll(Schema...schemas) {
		for (Schema s : schemas)
			add(s);
		return this;
	}
}
