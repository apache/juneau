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
package org.apache.juneau.rest.converters;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Converter for enabling of {@link PojoQuery} support on response objects returned by a <code>@RestMethod</code> method.
 * <p>
 * When enabled, objects in a POJO tree can be filtered using the functionality described in the {@link PojoQuery}
 * class.
 * <p>
 * The following HTTP request parameters are available for tabular data (e.g. {@code Collections} of {@code Maps},
 * arrays of beans, etc...):
 * <ul class='spaced-list'>
 * 	<li>
 * 		<code>&amp;s=</code> Search arguments.
 * 		<br>Comma-delimited list of key/value pairs representing column names and search tokens.
 * 		<br>Example:  <js>"&amp;s=name=Bill*,birthDate&gt;2000"</js>
 * 	<li>
 * 		<code>&amp;v=</code> Visible columns.
 * 		<br>Comma-delimited list of column names to display.
 * 		<br>Example:  <js>"&amp;v=name,birthDate"</js>
 * 	<li>
 * 		<code>&amp;o=</code> Sort commands.
 * 		<br>Comma-delimited list of columns to sort by.
 * 		<br>Column names can be suffixed with <js>'+'</js> or <js>'-'</js> to indicate ascending or descending order.
 * 		<br>The default is ascending order.
 * 		<br>Example:  <js>"&amp;o=name,birthDate-"</js>
 * 	<li>
 * 		<code>&amp;i=</code> Case-insensitive parameter.
 * 		<br>Boolean flag for case-insensitive matching on the search parameters.
 * 	<li>
 * 		<code>&amp;p=</code> - Position parameter.
 * 		<br>Only return rows starting at the specified index position (zero-indexed).
 * 		<br>Default is {@code 0}.
 * 	<li>
 * 		<code>&amp;l=</code> Limit parameter.
 * 		<br>Only return the specified number of rows.
 * 		<br>Default is {@code 0} (meaning return all rows).
 * </ul>
 *
 * <p>
 * See {@link PojoQuery} for additional information on filtering POJO models.
 */
public final class Queryable implements RestConverter {

	@Override /* RestConverter */
	public Object convert(RestRequest req, Object o, ClassMeta<?> cm) {
		if (o == null)
			return null;
		SearchArgs searchArgs = req.getQuery().getSearchArgs();
		if (searchArgs == null)
			return o;
		return new PojoQuery(o, req.getBeanSession()).filter(searchArgs);
	}
}
