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

import java.net.*;

import org.apache.juneau.*;

/**
 * Convenience class for representing a schema reference such as <js>"{'$ref':'/url/to/ref'}"</js>.
 *
 * <p>
 * An instance of this object is equivalent to calling...
 *
 * <p class='bjava'>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema().setRef(<jv>uri</jv>);
 * </p>
 */
public class JsonSchemaRef extends JsonSchema {

	/**
	 * Constructor.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the target reference.  Can be <jk>null</jk>.
	 */
	public JsonSchemaRef(Object uri) {
		this.setRef(uri);
	}
}
