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

import java.util.*;

import javax.xml.bind.*;

/**
 * Static utility methods for ATOM marshalling code.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-dto.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * </ul>
 */
class Utils {

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
