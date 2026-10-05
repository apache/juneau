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
 * PostgreSQL support for the Juneau {@code BeanQuery} SQL engine.
 *
 * <p>
 * This package holds exactly one type, {@link org.apache.juneau.beanquery.postgres.PostgresDialect}, the
 * {@code SqlDialect} Juneau ships out of the box.  It lives in its own package (and its own artifact,
 * {@code juneau-beanquery-postgres}) rather than inside {@code org.apache.juneau.commons.beanquery} - the dialect-
 * agnostic engine and the commons search-expression model must never depend on a specific dialect.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	SqlBeanQueryContext&lt;Task&gt; <jv>ctx</jv> = SqlBeanQueryContext.<jsm>create</jsm>(Task.<jk>class</jk>)
 * 		.dialect(PostgresDialect.<jsf>INSTANCE</jsf>).table(<js>"tasks"</js>)
 * 		.column(<js>"name"</js>, SearchType.<jsf>TEXT</jsf>).column(<js>"age"</js>, SearchType.<jsf>NUMERIC</jsf>)
 * 		.connectionSupplier(<jv>supplier</jv>)
 * 		.build();
 * </p>
 *
 * @since 10.0.0
 */
package org.apache.juneau.beanquery.postgres;
