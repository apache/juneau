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
package org.apache.juneau.swaps;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Transforms {@link XMLGregorianCalendar XMLGregorianCalendars} to ISO8601 date-time {@link String Strings}.
 *
 * <p>
 * Objects are converted to strings using {@link XMLGregorianCalendar#toXMLFormat()}.
 *
 * <p>
 * Strings are converted to objects using {@link DatatypeFactory#newXMLGregorianCalendar(String)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class XMLGregorianCalendarSwap extends StringSwap<XMLGregorianCalendar> {

	private DatatypeFactory dtf;

	/**
	 * Constructor.
	 */
	public XMLGregorianCalendarSwap() {
		try {
			this.dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Converts the specified <c>XMLGregorianCalendar</c> to a {@link String}.
	 */
	@Override /* ObjectSwap */
	public String swap(BeanSession session, XMLGregorianCalendar b) throws Exception {
		return b.toXMLFormat();
	}

	/**
	 * Converts the specified {@link String} to an <c>XMLGregorianCalendar</c>.
	 */
	@Override /* ObjectSwap */
	public XMLGregorianCalendar unswap(BeanSession session, String s, ClassMeta<?> hint) throws Exception {
		if (isEmpty(s))
			return null;
		return dtf.newXMLGregorianCalendar(s);
	}
}