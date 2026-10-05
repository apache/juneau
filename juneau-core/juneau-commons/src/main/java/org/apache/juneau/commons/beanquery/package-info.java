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
 * A protocol-agnostic query API for filtering, sorting, and paging collections of beans (design §5).
 *
 * <p>
 * A {@link org.apache.juneau.commons.beanquery.BeanQuery} carries only raw strings: one {@code search} string of
 * {@code column=expression} clauses in the {@code $}-language, plus raw view/sort directives, {@code position}/
 * {@code limit}, and an open {@code opts} bag.  The clause grammar shared by {@code search} and {@code opts} is decoded
 * by {@link org.apache.juneau.commons.beanquery.ClauseParser}; the single public
 * {@link org.apache.juneau.commons.beanquery.SearchExpressionParser} then parses each clause's expression against a
 * {@link org.apache.juneau.commons.beanquery.SearchOperatorSet}, throwing
 * {@link org.apache.juneau.commons.beanquery.BeanQuerySyntaxException} on an unknown operator or a malformed
 * expression.
 *
 * <p>
 * A {@link org.apache.juneau.commons.beanquery.BeanQueryContext} is the immutable, thread-safe half: its columns,
 * operators and limits are fixed by a builder ({@code create()...build()}; {@code copy()} derives a variant).  Per
 * request it opens a {@link org.apache.juneau.commons.beanquery.BeanQuerySession}, which may narrow the columns,
 * override the limits and add a guard.  Every query is validated into a
 * {@link org.apache.juneau.commons.beanquery.ResolvedQuery} before any engine runs it; rule violations throw
 * {@link org.apache.juneau.commons.beanquery.BeanQuerySyntaxException} (client error) and engine failures throw
 * {@link org.apache.juneau.commons.beanquery.BeanQueryExecutionException} (server error).  Results come back as
 * {@link org.apache.juneau.commons.beanquery.Page}s with the counts the
 * {@link org.apache.juneau.commons.beanquery.CountPolicy} selects.
 *
 * <p>
 * The {@link org.apache.juneau.commons.beanquery.InMemoryBeanQueryContext} runs against an in-JVM collection.  SQL
 * engines live in separate modules: {@code juneau-beanquery-sql} supplies the shared JDBC context and the
 * {@code SqlDialect} hook under {@code org.apache.juneau.beanquery.sql}.  Commons
 * carries the query API only, not the SQL dialect types.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>static final</jk> InMemoryBeanQueryContext&lt;Person&gt; <jsf>PEOPLE</jsf> = InMemoryBeanQueryContext
 * 		.<jsm>create</jsm>(Person.<jk>class</jk>)
 * 		.exclude(<js>"password"</js>)
 * 		.maxLimit(500)
 * 		.build();
 *
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jsf>PEOPLE</jsf>.getSession(<jv>people</jv>)) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(BeanQuery.<jsm>create</jsm>()
 * 			.setSearch(<js>"age=$gt(21)"</js>)
 * 			.setSort(<js>"name"</js>)
 * 			.setLimit(20));
 * 	}
 * </p>
 */
package org.apache.juneau.commons.beanquery;
