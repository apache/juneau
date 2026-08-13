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
 * Pluggable query wire-protocol adapter over the shared {@link ObjectSearcher}/{@link ObjectViewer}/{@link ObjectSorter}/{@link ObjectPaginator}
 * query engine.
 *
 * <p>
 * A {@code QueryProtocol} decouples the query <i>wire-protocol</i> (the request parameter names and the response
 * envelope shape) from the query <i>engine</i> (search / view / sort / page).  It has two responsibilities:
 * <ul class='spaced-list'>
 * 	<li>{@link #parse(RestRequest)} &mdash; read the protocol's request parameters and produce normalized
 * 		{@link QueryArgs} the engine consumes.
 * 	<li>{@link #wrap(RestRequest, QueryResult)} &mdash; shape the filtered {@link QueryResult} (data plus
 * 		pre-paging counts) into the protocol's response.  The returned value is a plain POJO/bean rendered by the
 * 		normal negotiated serializer &mdash; a protocol must <b>not</b> hardcode a serialization format here.
 * </ul>
 *
 * <p>
 * The engine sits between the two:
 * <p class='bcode'>
 * 	RestRequest ──[parse]──▶ QueryArgs ──▶ engine (+ recordsTotal / recordsFiltered) ──▶ QueryResult ──[wrap]──▶ response POJO
 * </p>
 *
 * <p>
 * Two adapters ship with Juneau:
 * <ul class='javatreec'>
 * 	<li class='jc'>{@link NativeQueryProtocol} &mdash; the Juneau-native {@code s/v/o/p/l} parameters with a bare
 * 		filtered result.  This is the default protocol.
 * 	<li class='jc'>{@code DataTablesQueryProtocol} (in {@code juneau-rest-server-datatables}) &mdash; the DataTables
 * 		server-side-processing contract.
 * </ul>
 *
 * <p>
 * A resource selects the active protocol for the {@link ProtocolQueryable} converter by registering a
 * {@link QueryableSettings} bean in its bean store (see {@link QueryableSettings}).  {@link Queryable} always uses
 * {@link NativeQueryProtocol} regardless of settings.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link QueryArgs}
 * 	<li class='jc'>{@link QueryResult}
 * 	<li class='jc'>{@link ProtocolQueryable}
 * 	<li class='jc'>{@link QueryableSettings}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface QueryProtocol {

	/**
	 * Parses this protocol's request parameters into normalized query arguments.
	 *
	 * @param req The incoming REST request.  Never <jk>null</jk>.
	 * @return
	 * 	The normalized query arguments the engine consumes.  Never <jk>null</jk> &mdash; return
	 * 	{@link QueryArgs#EMPTY} (or an empty builder result) when the request carries no query parameters.
	 */
	QueryArgs parse(RestRequest req);

	/**
	 * Shapes the filtered engine result into this protocol's response.
	 *
	 * <p>
	 * The returned value is a plain POJO/bean that the normal negotiated serializer renders &mdash; implementations
	 * must not hardcode a serialization format (e.g. JSON).
	 *
	 * @param req The incoming REST request (available for echoing protocol-specific values such as a request id).  Never <jk>null</jk>.
	 * @param result The filtered/paged data plus the pre-paging counts.  Never <jk>null</jk>.
	 * @return The response POJO/bean to serialize.  Can be <jk>null</jk>.
	 */
	Object wrap(RestRequest req, QueryResult result);

	/**
	 * Returns the Swagger/OpenAPI parameter descriptors for this protocol's query parameters.
	 *
	 * <p>
	 * The returned string is a Simplified-JSON array fragment suitable for a {@code @RestOp(swagger=@OpSwagger(parameters=...))}
	 * value, letting a resource document the protocol's query parameters without hardcoding them.  The default
	 * implementation returns an empty string (no documented parameters); {@link NativeQueryProtocol} returns the
	 * historical {@link Queryable#SWAGGER_PARAMS native s/v/o/p/l descriptors}.
	 *
	 * @return The Swagger parameter fragment, or an empty string if the protocol documents no query parameters.  Never <jk>null</jk>.
	 */
	default String swaggerParams() {
		return "";
	}
}
