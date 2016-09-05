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
package org.apache.juneau.client;

import java.util.*;

import org.apache.http.client.utils.*;
import org.apache.http.message.*;

/**
 * Convenience class for setting date headers in RFC2616 format.
 * <p>
 * Equivalent to the following code:
 * <p class='bcode'>
 * 	Header h = <jk>new</jk> Header(name, DateUtils.<jsm>formatDate</jsm>(value));
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class DateHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a date request property in RFC2616 format.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	public DateHeader(String name, Date value) {
		super(name, DateUtils.formatDate(value));
	}
}
