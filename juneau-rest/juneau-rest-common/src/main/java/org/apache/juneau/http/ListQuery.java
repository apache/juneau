/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.collections.*;

/**
 * Stock {@link Request @Request} bean for list-fetch query parameters.
 *
 * <p>
 * Bind a typical list REST operation to a single method argument instead of repeating
 * <c>search</c> / <c>view</c> / <c>sort</c> / <c>position</c> / <c>limit</c> / <c>opt</c>
 * as individual {@link Query @Query} parameters.  Apps extend this interface with extra named getters
 * and/or use {@link #getOpts()} for unstructured extras.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> List&lt;Row&gt; getList(ListQuery <jv>q</jv>) {
 * 		<jk>var</jk> <jv>search</jv> = <jv>q</jv>.getSearch().orElse(<jk>null</jk>);
 * 		<jk>var</jk> <jv>view</jv> = <jv>q</jv>.getView().orElse(<js>"*"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * The type is annotated {@link Request @Request}, so a method parameter of type {@link ListQuery} does not
 * need a second <ja>@Request</ja> on the parameter.
 *
 * <h5 class='section'>Wire names</h5>
 * <p>
 * Default query names are the Java property names (<c>search</c>, <c>view</c>, <c>sort</c>,
 * <c>position</c>, <c>limit</c>) except {@link #getOpts()}, which binds <c>opt</c> (singular).
 * A resource that wants a different name redeclares the getter on a subtype:
 * </p>
 * <p class='bjava'>
 * 	<jk>public interface</jk> AppListQuery <jk>extends</jk> ListQuery {
 * 		<ja>@Query</ja>(<js>"v"</js>)
 * 		<ja>@Override</ja>
 * 		Optional&lt;String&gt; getView();
 * 	}
 * </p>
 *
 * <h5 class='section'>Saved-view clash</h5>
 * <p>
 * {@link #getView()} is a <b>column projection</b> (field names), not a saved-view id.
 * Saved-view CRUD endpoints must <b>not</b> take {@link ListQuery}; they keep their own
 * {@link Query @Query}(<js>"view"</js>) id parameter.  Using this type there would bind the
 * wrong meaning of <c>view</c>.
 *
 * <h5 class='section'>MULTI <c>search</c></h5>
 * <p>
 * {@link #getSearch()} uses <c>collectionFormat=multi</c> (repeated <c>?search=a&amp;search=b</c>,
 * not a comma-delimited string).  Values are not joined by this type.
 * </p>
 * <ul>
 * 	<li>Missing (zero occurrences) → {@link Optional#empty()}
 * 	<li>Present but every value is blank → {@link Optional#of Optional.of(new String[0])}
 * 	<li>Present with values → {@link Optional#of Optional.of} the collected array in query order
 * </ul>
 *
 * <h5 class='section'>MULTI <c>opt</c></h5>
 * <p>
 * {@link #getOpts()} binds repeated <c>opt</c> parameters.  Each occurrence is one top-level
 * <c>key=uonValue</c> pair (same MULTI shape as search).  Values are UON, so they may be strings,
 * numbers, lists, or nested maps.  Later keys win on overlap.  Missing <c>opt</c> →
 * {@link Optional#empty()}; one or more present → {@link Optional#of} the merged {@link JsonMap}.
 * </p>
 * <p class='bjava'>
 * 	<jc>// ?opt=environment=prod&amp;opt=period=7d</jc>
 * 	<jc>// → {environment:'prod', period:'7d'}</jc>
 *
 * 	<jc>// ?opt=flags=(a,b,c)</jc>
 * 	<jc>// → {flags:['a','b','c']}</jc>
 *
 * 	<jc>// ?opt=nested=(x=1,y=2)</jc>
 * 	<jc>// → {nested:{x:1, y:2}}</jc>
 * </p>
 * <p>
 * A parenthesized UON object as a single value (<c>?opt=(environment=prod,period=7d)</c>) is still
 * parsed and merged; the intended authoring is repeated <c>opt=key=value</c>.
 *
 * <p>
 * This type is HTTP-binding only.  It is not the converter engine <c>QueryArgs</c> type
 * (<c>s</c>/<c>v</c>/<c>o</c>/<c>p</c>/<c>l</c> / DataTables protocol).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Request}
 * 	<li class='ja'>{@link Query}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Request">@Request</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Request
public interface ListQuery {

	/**
	 * Filter terms.
	 *
	 * <p>
	 * Repeated <c>?search=</c> query parameters (<c>collectionFormat=multi</c>).
	 * Missing → {@link Optional#empty()}; present-but-empty → {@link Optional#of Optional.of(new String[0])}.
	 *
	 * @return The search terms in query order, or empty if the parameter is absent.
	 */
	@Query(schema=@Schema(collectionFormat="multi"))
	Optional<String[]> getSearch();

	/**
	 * Column projection.
	 *
	 * <p>
	 * Field names to return, <b>not</b> a saved-view id.  Saved-view endpoints must not use
	 * {@link ListQuery}; they keep a separate {@link Query @Query}(<js>"view"</js>) id parameter.
	 *
	 * @return The view expression, or empty if absent.
	 */
	@Query
	Optional<String> getView();

	/**
	 * Sort columns.
	 *
	 * @return The sort expression, or empty if absent.
	 */
	@Query
	Optional<String> getSort();

	/**
	 * Zero-based page offset.
	 *
	 * @return The position, or empty if absent.
	 */
	@Query
	Optional<Integer> getPosition();

	/**
	 * Page size.
	 *
	 * @return The limit, or empty if absent.
	 */
	@Query
	Optional<Integer> getLimit();

	/**
	 * App extras bag.
	 *
	 * <p>
	 * Repeated <c>opt</c> query parameters (singular wire name; this getter stays {@code getOpts()}).
	 * Each occurrence is <c>key=uonValue</c>, merged into one {@link JsonMap} (later key wins).
	 *
	 * @return The merged extras map, or empty if <c>opt</c> is absent.
	 */
	@Query(name="opt", schema=@Schema(collectionFormat="multi"))
	Optional<JsonMap> getOpts();
}
