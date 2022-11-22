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
package org.apache.juneau.rest.converter;

import org.apache.juneau.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Converter for enabling of search/view/sort/page support on response objects returned by a <c>@RestOp</c>-annotated method.
 *
 * <p>
 * When enabled, objects in a POJO tree can be filtered using the functionality described in the {@link ObjectSearcher},
 * {@link ObjectViewer}, {@link ObjectSorter}, and {@link ObjectPaginator} classes.
 *
 * <p>
 * The following HTTP request parameters are available for tabular data (e.g. {@code Collections} of {@code Maps},
 * arrays of beans, etc...):
 * <ul class='spaced-list'>
 * 	<li>
 * 		<c>&amp;s=</c> Search arguments.
 * 		<br>Comma-delimited list of key/value pairs representing column names and search tokens.
 * 		<br>Example:
 * 		<p class='burlenc'>
 * 	&amp;s=name=Bill*,birthDate&gt;2000
 * 		</p>
 * 	<li>
 * 		<c>&amp;v=</c> Visible columns.
 * 		<br>Comma-delimited list of column names to display.
 * 		<br>Example:
 * 		<p class='burlenc'>
 * 	&amp;v=name,birthDate
 * 		</p>
 * 	<li>
 * 		<c>&amp;o=</c> Sort commands.
 * 		<br>Comma-delimited list of columns to sort by.
 * 		<br>Column names can be suffixed with <js>'+'</js> or <js>'-'</js> to indicate ascending or descending order.
 * 		<br>The default is ascending order.
 * 		<br>Example:
 * 		<p class='burlenc'>
 * 	&amp;o=name,birthDate-
 * 		</p>
 * 	<li>
 * 		<c>&amp;i=</c> Case-insensitive parameter.
 * 		<br>Boolean flag for case-insensitive matching on the search parameters.
 * 	<li>
 * 		<c>&amp;p=</c> - Position parameter.
 * 		<br>Only return rows starting at the specified index position (zero-indexed).
 * 		<br>Default is {@code 0}.
 * 	<li>
 * 		<c>&amp;l=</c> Limit parameter.
 * 		<br>Only return the specified number of rows.
 * 		<br>Default is {@code 0} (meaning return all rows).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ObjectSearcher} - Additional information on searching POJO models.
 * 	<li class='jc'>{@link ObjectViewer} - Additional information on filtering POJO models.
 * 	<li class='jc'>{@link ObjectSorter} - Additional information on sorting POJO models.
 * 	<li class='jc'>{@link ObjectPaginator} - Additional information on paginating POJO models.
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#converters()} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Converters">Converters</a>
 * </ul>
 */
public final class Queryable implements RestConverter {

	/**
	 * Swagger parameters for this converter.
	 */
	public static final String SWAGGER_PARAMS=""
		+ "{"
			+ "in:'query',"
			+ "name:'s',"
			+ "description:'"
				+ "Search.\n"
				+ "Key/value pairs representing column names and search tokens.\n"
				+ "\\'*\\' and \\'?\\' can be used as meta-characters in string fields.\n"
				+ "\\'>\\', \\'>=\\', \\'<\\', and \\'<=\\' can be used as limits on numeric and date fields.\n"
				+ "Date fields can be matched with partial dates (e.g. \\'2018\\' to match any date in the year 2018)."
			+ "',"
			+ "type:'array',"
			+ "collectionFormat:'csv',"
			+ "examples:{example:'?s=Bill*,birthDate>2000'}"
		+ "},"
		+ "{"
			+ "in:'query',"
			+ "name:'v',"
			+ "description:'"
				+ "View.\n"
				+ "Column names to display."
			+ "',"
			+ "type:'array',"
			+ "collectionFormat:'csv',"
			+ "examples:{example:'?v=name,birthDate'}"
		+ "},"
		+ "{"
			+ "in:'query',"
			+ "name:'o',"
			+ "description:'"
				+ "Order by.\n"
				+ "Columns to sort by.\n"
				+ "Column names can be suffixed with \\'+\\' or \\'-\\' to indicate ascending or descending order.\n"
				+ "The default is ascending order."
			+ "',"
			+ "type:'array',"
			+ "collectionFormat:'csv',"
			+ "examples:{example:'?o=name,birthDate-'}"
		+ "},"
		+ "{"
			+ "in:'query',"
			+ "name:'p',"
			+ "description:'"
				+ "Position.\n"
				+ "Only return rows starting at the specified index position (zero-indexed).\n"
				+ "Default is 0"
			+ "',"
			+ "type:'integer',"
			+ "examples:{example:'?p=100'}"
		+ "},"
		+ "{"
			+ "in:'query',"
			+ "name:'l',"
			+ "description:'"
				+ "Limit.\n"
				+ "Only return the specified number of rows.\n"
				+ "Default is 0 (meaning return all rows)."
			+ "',"
			+ "type:'integer',"
			+ "examples:{example:'?l=100'}"
		+ "}"
	;

	@Override /* RestConverter */
	public Object convert(RestRequest req, Object o) {
		if (o == null)
			return null;

		Value<Object> v = Value.of(o);
		RequestQueryParams params = req.getQueryParams();
		BeanSession bs = req.getBeanSession();

		params.getSearchArgs().ifPresent(x -> v.set(ObjectSearcher.create().run(bs, v.get(), x)));
		params.getSortArgs().ifPresent(x -> v.set(ObjectSorter.create().run(bs, v.get(), x)));
		params.getViewArgs().ifPresent(x -> v.set(ObjectViewer.create().run(bs, v.get(), x)));
		params.getPageArgs().ifPresent(x -> v.set(ObjectPaginator.create().run(bs, v.get(), x)));
		return v.get();
	}
}
