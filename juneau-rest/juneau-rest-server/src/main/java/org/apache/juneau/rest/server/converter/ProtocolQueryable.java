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

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.rest.server.*;

/**
 * Protocol-agnostic converter for enabling search/view/sort/page support on response objects, driven by a
 * pluggable {@link QueryProtocol}.
 *
 * <p>
 * This is the generic counterpart to {@link Queryable}: it resolves a {@link QueryProtocol}, calls
 * {@link QueryProtocol#parse(RestRequest) parse} to normalize the request into {@link QueryArgs}, runs the shared
 * {@link ObjectSearcher}/{@link ObjectSorter}/{@link ObjectViewer}/{@link ObjectPaginator} engine, then calls
 * {@link QueryProtocol#wrap(RestRequest, QueryResult) wrap} to shape the response.
 *
 * <p>
 * The active protocol is selected per-resource by registering a {@link QueryableSettings} bean in the bean store.
 * When none is registered, the {@linkplain NativeQueryProtocol native protocol} is used, so this converter behaves
 * identically to {@link Queryable} out of the box.
 *
 * <h5 class='section'>Example - DataTables protocol:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=ProtocolQueryable.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> QueryableSettings queryableSettings() {
 * 			<jk>return</jk> QueryableSettings.<jsm>create</jsm>().protocol(<jk>new</jk> DataTablesQueryProtocol()).build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Queryable} - The native-only converter (back-compatible entry point).
 * 	<li class='jc'>{@link QueryProtocol} - The pluggable protocol SPI.
 * 	<li class='jc'>{@link QueryableSettings} - Per-resource protocol selection.
 * 	<li class='ja'>{@link RestOp#converters()} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class ProtocolQueryable implements RestConverter {

	/**
	 * The protocol this converter is hard-wired to, or <jk>null</jk> to resolve one per-request from a
	 * {@link QueryableSettings} bean.
	 */
	private final QueryProtocol fixedProtocol;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Resolves the active {@link QueryProtocol} per-request from a {@link QueryableSettings} bean in the resource's
	 * bean store, falling back to {@link NativeQueryProtocol} when none is registered.
	 */
	public ProtocolQueryable() {
		this(null);
	}

	/**
	 * Constructor for a subclass that is hard-wired to a fixed protocol.
	 *
	 * <p>
	 * When a non-<jk>null</jk> protocol is supplied, this converter always uses it and never consults
	 * {@link QueryableSettings} &mdash; making the "fixed protocol, extension point disabled" intent explicit (see
	 * {@link Queryable}, which fixes {@link NativeQueryProtocol}).  A subclass wanting the default bean-store
	 * resolution should use the no-arg constructor instead.
	 *
	 * @param fixedProtocol The protocol to always use, or <jk>null</jk> to resolve per-request from settings.
	 */
	protected ProtocolQueryable(QueryProtocol fixedProtocol) {
		this.fixedProtocol = fixedProtocol;
	}

	@Override /* Overridden from RestConverter */
	public Object convert(RestRequest req, Object o) throws BasicHttpException {
		if (o == null)
			return null;
		var protocol = resolveProtocol(req);
		var args = protocol.parse(req);
		var result = QueryEngine.run(req, o, args);
		return protocol.wrap(req, result);
	}

	/**
	 * Resolves the {@link QueryProtocol} to use for this request.
	 *
	 * <p>
	 * When the converter was constructed with a fixed protocol (see {@link #ProtocolQueryable(QueryProtocol)}), that
	 * protocol is always returned.  Otherwise, the default implementation reads a {@link QueryableSettings} bean from
	 * the resource's bean store, falling back to {@link NativeQueryProtocol} when none is registered.
	 *
	 * @param req The incoming request.
	 * @return The protocol to use.  Never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this only borrows a bean and must not close it.
	})
	protected QueryProtocol resolveProtocol(RestRequest req) {
		if (fixedProtocol != null)
			return fixedProtocol;
		return req.getContext().getBeanStore().getBean(QueryableSettings.class).orElse(QueryableSettings.DEFAULT).protocol();
	}
}
