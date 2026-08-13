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

/**
 * A {@link org.apache.juneau.rest.server.converter.QueryProtocol} adapter implementing the
 * <a class="doclink" href="https://datatables.net/manual/server-side">DataTables server-side processing</a> wire
 * contract on top of Juneau's shared query engine.
 *
 * <p>
 * This is the optional companion to {@link org.apache.juneau.rest.server.converter.ProtocolQueryable} in
 * {@code juneau-rest-server}.  Adding this module to a service's classpath lets a resource speak the DataTables
 * server-side protocol instead of the Juneau-native {@code s/v/o/p/l} protocol, by registering a
 * {@link org.apache.juneau.rest.server.converter.QueryableSettings} bean selecting
 * {@link org.apache.juneau.rest.server.datatables.DataTablesQueryProtocol}.
 *
 * <p>
 * The SPI ({@code QueryProtocol}), the normalized types ({@code QueryArgs}/{@code QueryResult}), and the
 * {@code NativeQueryProtocol} all stay in {@code juneau-rest-server} core; only the DataTables-specific protocol and
 * its {@link org.apache.juneau.rest.server.datatables.DataTablesResults} response envelope live here.
 *
 * <h5 class='topic'>Protocol versioning</h5>
 *
 * <p>
 * The DataTables server-side wire contract is small and has stayed backward-compatible across DataTables 1.10 → 2.x,
 * so this module versions the protocol at the <b>class level</b> rather than shipping per-version Maven artifacts.
 * {@link org.apache.juneau.rest.server.datatables.DataTablesQueryProtocol} is the current protocol; a future breaking
 * DataTables wire change would be added as a sibling class (e.g. {@code DataTables2QueryProtocol}) in this same module.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.converter.QueryProtocol}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.converter.ProtocolQueryable}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.converter.QueryableSettings}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/manual/server-side">DataTables Server-Side Processing</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.datatables;
