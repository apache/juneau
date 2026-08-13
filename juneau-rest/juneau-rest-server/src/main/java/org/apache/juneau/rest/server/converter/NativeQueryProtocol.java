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
package org.apache.juneau.rest.server.converter;

import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.rest.server.*;

/**
 * The Juneau-native query protocol: the historical {@code s/v/o/p/l} query parameters with a bare filtered result.
 *
 * <p>
 * This adapter is the extraction of the parsing that used to live in {@code RequestQueryParamList} and the wrapping
 * that used to live in {@code Queryable} &mdash; it preserves the pre-10.0 wire behavior exactly and is the default
 * protocol for both {@link Queryable} and {@link ProtocolQueryable}.
 *
 * <p>
 * Request parameters (all for tabular data &mdash; collections of maps, arrays of beans, etc.):
 * <ul class='spaced-list'>
 * 	<li><c>&amp;s=</c> Search &mdash; comma-delimited <c>column=token</c> pairs (see {@link SearchArgs}).
 * 	<li><c>&amp;v=</c> View &mdash; comma-delimited column names to display (see {@link ViewArgs}).
 * 	<li><c>&amp;o=</c> Order &mdash; comma-delimited columns to sort by, {@code +}/{@code -} suffixed (see {@link SortArgs}).
 * 	<li><c>&amp;p=</c> Position &mdash; zero-indexed start row (see {@link PageArgs}).
 * 	<li><c>&amp;l=</c> Limit &mdash; number of rows to return (see {@link PageArgs}).
 * </ul>
 *
 * <p>
 * {@link #wrap(RestRequest, QueryResult)} returns the filtered value unchanged (a bare POJO the negotiated serializer
 * renders); the {@link QueryResult} counts are ignored.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link QueryProtocol}
 * 	<li class='jc'>{@link Queryable}
 * 	<li class='jc'>{@link ProtocolQueryable}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class NativeQueryProtocol implements QueryProtocol {

	/** Reusable stateless instance. */
	public static final NativeQueryProtocol INSTANCE = new NativeQueryProtocol();

	@Override /* Overridden from QueryProtocol */
	public QueryArgs parse(RestRequest req) {
		var p = req.getQueryParams();
		return QueryArgs.create()
			.search(SearchArgs.create(p.get("s").asString().orElse(null)))
			.sort(SortArgs.create(p.get("o").asString().orElse(null)))
			.view(ViewArgs.create(p.get("v").asString().orElse(null)))
			.page(PageArgs.create(p.get("p").asInteger().orElse(null), p.get("l").asInteger().orElse(null)))
			.build();
	}

	@Override /* Overridden from QueryProtocol */
	public Object wrap(RestRequest req, QueryResult result) {
		return result.getData();
	}

	@Override /* Overridden from QueryProtocol */
	public String swaggerParams() {
		return Queryable.SWAGGER_PARAMS;
	}
}
