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
package org.apache.juneau.beanquery.sql;

import org.apache.juneau.commons.beanquery.*;

/**
 * Maps a logical column name to its dialect-quoted SQL reference, for {@link SqlSearchCompiler}.
 *
 * <p>
 * Only columns that passed validation reach it: column types and operator sets travel in the {@link ResolvedQuery},
 * so a resolver never has to guess about an unknown name.  {@link SqlBeanQueryContext} is the usual implementation.
 * The interface is small so the compiler can be tested without a context or a database.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Logical names map one-to-one to quoted SQL names.</jc>
 * 	ColumnResolver <jv>columns</jv> = <jv>c</jv> -&gt; <jv>dialect</jv>.quote(<jv>c</jv>);
 * 	SqlFragment <jv>where</jv> = SqlSearchCompiler.<jsm>create</jsm>(<jv>dialect</jv>, <jv>columns</jv>).compile(<jv>context</jv>.resolve(<jv>query</jv>).filter());
 * </p>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface ColumnResolver {

	/**
	 * Returns the dialect-quoted SQL reference for a column (for example {@code "age"} for column {@code age}).
	 *
	 * @param column A validated logical column name.  Never <jk>null</jk>.
	 * @return The quoted SQL reference.
	 */
	String columnSql(String column);
}
