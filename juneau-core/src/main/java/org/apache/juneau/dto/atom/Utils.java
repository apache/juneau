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
package org.apache.juneau.dto.atom;

import java.net.*;
import java.util.*;

import javax.xml.bind.*;

/**
 * Static utility methods for ATOM marshalling code.
 */
class Utils {

	/**
	 * Converts a string to a URI without a {@link URISyntaxException}
	 *
	 * @param uri The URI string to convert.
	 * @return A new URI object.
	 */
	static final URI toURI(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts an ISO8601 date-time string to a {@link Calendar}.
	 *
	 * @param lexicalXSDDateTime The ISO8601 date-time string.
	 * @return A new {@link Calendar} object.
	 */
	static final Calendar parseDateTime(String lexicalXSDDateTime) {
		return DatatypeConverter.parseDateTime(lexicalXSDDateTime);
	}

}
