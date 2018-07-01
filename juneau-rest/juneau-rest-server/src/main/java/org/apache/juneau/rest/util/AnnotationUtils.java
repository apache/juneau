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
package org.apache.juneau.rest.util;

import org.apache.juneau.rest.annotation.*;

/**
 * Various reusable utility methods when working with annotations.
 */
public class AnnotationUtils extends org.apache.juneau.http.annotation.AnnotationUtils {

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ResourceSwagger a) {
		if (a == null)
			return true;
		return
			allEmpty(a.version())
			&& allEmpty(a.title(), a.description(), a.value())
			&& empty(a.contact())
			&& empty(a.license())
			&& empty(a.externalDocs())
			&& a.tags().length == 0;
	}
}
