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

/**
 * Convenience class for representing a property that's an array of simple types.
 * <p>
 * 	An instance of this object is equivalent to calling...
 *
 * <p class='bcode'>
 * 	SchemaProperty p = <jk>new</jk> SchemaProperty(name)
 * 		.setType(JsonType.<jsf>ARRAY</jsf>)
 * 		.setItems(
 * 			<jk>new</jk> Schema().setType(elementType)
 * 		);
 * </p>
 */
public class SchemaPropertySimpleArray extends SchemaProperty {

	/**
	 * Constructor.
	 *
	 * @param name The name of the schema property.
	 * @param elementType The JSON type of the elements in the array.
	 */
	public SchemaPropertySimpleArray(String name, JsonType elementType) {
		setName(name);
		setType(JsonType.ARRAY);
		setItems(
			new Schema().setType(elementType)
		);
	}
}
