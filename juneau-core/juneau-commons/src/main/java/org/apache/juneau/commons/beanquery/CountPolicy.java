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
package org.apache.juneau.commons.beanquery;

/**
 * Which counts a {@link BeanQuerySession#find(BeanQuery) find} / {@link BeanQuerySession#findValues(BeanQuery)
 * findValues} call computes alongside its page of rows.
 *
 * <p>
 * Set on {@link BeanQueryContext.Builder#countPolicy(CountPolicy)} and overridable per session with
 * {@link BeanQuerySession.Builder#countPolicy(CountPolicy)}.  Counts can cost extra queries (two {@code COUNT(*)}
 * statements on a SQL engine), so the default {@link #IF_REQUESTED} lets the caller opt in while the server keeps
 * the final say.  {@link BeanQuerySession#count(BeanQuery)} always computes both counts regardless of the policy.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Always return the matched count, whatever the caller asks for.</jc>
 * 	InMemoryBeanQueryContext&lt;Person&gt; <jv>context</jv> = InMemoryBeanQueryContext
 * 		.<jsm>create</jsm>(Person.<jk>class</jk>)
 * 		.countPolicy(CountPolicy.<jsf>MATCHED</jsf>)
 * 		.build();
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.getSession(<jv>people</jv>)) {
 * 		<jk>long</jk> <jv>matched</jv> = <jv>session</jv>.find(<jv>query</jv>).matched().getAsLong();
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public enum CountPolicy {

	/** Never compute counts; {@link Page#total()} and {@link Page#matched()} are empty. */
	NONE,

	/** Compute only the matched count (rows that pass the guards and the search). */
	MATCHED,

	/** Compute both the total count (rows that pass the guards) and the matched count. */
	BOTH,

	/**
	 * Let the query decide through its {@code opts}: {@code counts=matched} &rarr; {@link #MATCHED};
	 * {@code counts=both} or {@code counts=true} &rarr; {@link #BOTH}; anything else &rarr; {@link #NONE}.
	 *
	 * <p>
	 * A {@link ResolvedQuery#counts() resolved query} never carries this value; it is always replaced by one of the
	 * other three.
	 */
	IF_REQUESTED
}
