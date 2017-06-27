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
package org.apache.juneau.rest.widget;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.converters.*;

/**
 * Adds a <code>QUERY</code> link to the page that allows you to perform search/view/sort/paging on the page data.
 * <p>
 * A typical usage of the query widget is to include it as a navigation link as shown in the example below
 * pulled from the <code>PetStoreResource</code> example:
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(
 * 		widgets={
 * 			QueryWidget.<jk>class</jk>
 * 		},
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			links=<js>"{up:'...',options:'...',query:'$W{query}',source:'...'}"</js>
 * 		)
 * 	)
 * </p>
 * <p>
 * In the above example, this adds a <code>QUERY</code> that displays a search popup that can be used for filtering the
 * page results...
 * <p>
 * <img class='bordered' src='doc-files/PetStore_Query.png'>
 * <p>
 * Tooltips are provided by hovering over the field names.
 * <p>
 * <img class='bordered' src='doc-files/PetStore_Query_tooltip.png'>
 * <p>
 * When submitted, the form submits a GET request against the current URI with special GET search API query parameters.
 * (e.g. <js>"?s=column1=Foo*&amp;v=column1,column2&amp;o=column1,column2-&amp;p=100&amp;l=100"</js>).
 * <p>
 * The search arguments can be retrieved programmatically using {@link RequestQuery#getSearchArgs()}.
 * <p>
 * Typically, the search functionality is implemented by applying the predefined {@link Queryable} converter on the
 * method that's returning a 2-dimensional table of POJOs that you wish to filter:
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"GET"</js>,
 * 		path=<js>"/"</js>,
 * 		converters=Queryable.<jk>class</jk>
 * 	)
 * 	<jk>public</jk> Collection&lt;Pet&gt; getPets() {
 * </p>
 * <p>
 * The following shows various search arguments and their results on the page:
 * <table style='width:auto'>
 * 	<tr>
 * 		<th>Search type</th><th>Query arguments</th><th>Query results</th>
 * 	</tr>
 * 	<tr>
 * 		<td>No arguments</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q1.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r1.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>String search</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q2.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r2.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Numeric range</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q3.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r3.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>ANDed terms</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q4.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r4.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Date range (entire year)</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q8.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r8.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Date range</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q9.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r9.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Date range</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q10.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r10.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Hide columns</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q5.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r5.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Sort</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q6.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r6.png'></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Sort descending</td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_q7.png'></td>
 * 		<td><img class='bordered' src='doc-files/PetStore_Query_r7.png'></td>
 * 	</tr>
 * </table>
 *
 */
public class QueryWidget extends Widget {

	@Override
	public String getName() {
		return "query";
	}

	@Override /* Widget */
	public String resolve(RestRequest req) throws Exception {
		// Note we're stripping off the license header.
		return IOUtils.read(getClass().getResourceAsStream("QueryWidget.html")).replaceFirst("(?s)<!--(.*?)-->\\s*", "");
	}
}
