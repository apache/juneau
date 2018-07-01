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

import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Converter for enabling of {@link PojoQuery} support on response objects returned by a <code>@RestMethod</code> method.
 *
 * <p>
 * When enabled, objects in a POJO tree can be filtered using the functionality described in the {@link PojoQuery}
 * class.
 *
 * <p>
 * The following HTTP request parameters are available for tabular data (e.g. {@code Collections} of {@code Maps},
 * arrays of beans, etc...):
 * <ul class='spaced-list'>
 * 	<li>
 * 		<code>&amp;s=</code> Search arguments.
 * 		<br>Comma-delimited list of key/value pairs representing column names and search tokens.
 * 		<br>Example:
 * 		<p class='bcode'>
 * 	&amp;s=name=Bill*,birthDate&gt;2000
 * 		</p>
 * 	<li>
 * 		<code>&amp;v=</code> Visible columns.
 * 		<br>Comma-delimited list of column names to display.
 * 		<br>Example:
 * 		<p class='bcode'>
 * 	&amp;v=name,birthDate
 * 		</p>
 * 	<li>
 * 		<code>&amp;o=</code> Sort commands.
 * 		<br>Comma-delimited list of columns to sort by.
 * 		<br>Column names can be suffixed with <js>'+'</js> or <js>'-'</js> to indicate ascending or descending order.
 * 		<br>The default is ascending order.
 * 		<br>Example:
 * 		<p class='bcode'>
 * 	&amp;o=name,birthDate-
 * 		</p>
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
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PojoQuery} - Additional information on filtering POJO models.
 * 	<li class='jf'>{@link RestContext#REST_converters} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.Converters">Overview &gt; juneau-rest-server &gt; Converters</a>
 * </ul>
 */
public final class Queryable implements RestConverter {

	/**
	 * Swagger parameters for this converter.
	 */
	public static final String SWAGGER_PARAMS= ""
		+ "{in:'query',name:'s',description:'Search.\nComma-delimited list of key/value pairs representing column names and search tokens.\n\\'*\\' and \\'?\\' can be used as meta-characters.',schema:{type:'string'},x-examples:{example:'?s=Bill*,birthDate>2000'}},"
		+ "{in:'query',name:'v',description:'View.\nComma-delimited list of column names to display.',schema:{type:'string'},x-examples:{example:'?v=name,birthDate'}},"
		+ "{in:'query',name:'o',description:'Order by.\nComma-delimited list of columns to sort by.\nColumn names can be suffixed with \\'+\\' or \\'-\\' to indicate ascending or descending order.\nThe default is ascending order.',schema:{type:'string'},x-examples:{example:'?o=name,birthDate-'}},"
		+ "{in:'query',name:'i',description:'Case-insensitive.\nBoolean flag for case-insensitive matching on the search parameters.',schema:{type:'boolean'},x-examples:{example:'?i=true'}},"
		+ "{in:'query',name:'p',description:'Position.\nOnly return rows starting at the specified index position (zero-indexed).\nDefault is 0',schema:{type:'integer'},x-examples:{example:'?p=100'}},"
		+ "{in:'query',name:'l',description:'Limit.\nOnly return the specified number of rows.\nDefault is 0 (meaning return all rows).',schema:{type:'integer'},x-examples:{example:'?l=100'}}"
	;

	@Override /* RestConverter */
	public Object convert(RestRequest req, Object o) {
		if (o == null)
			return null;
		SearchArgs searchArgs = req.getQuery().getSearchArgs();
		if (searchArgs == null)
			return o;
		return new PojoQuery(o, req.getBeanSession()).filter(searchArgs);
	}
}
