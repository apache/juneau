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

import org.apache.juneau.commons.*;

/**
 * A server-side failure while running a valid {@link BeanQuery}: a {@code SQLException}, a connection failure, or a
 * column accessor that throws.
 *
 * <p>
 * Contrast with {@link BeanQuerySyntaxException}, which reports bad caller input (HTTP 400).  This exception maps to
 * HTTP 500.  Its message is deliberately generic (for example {@code "Query execution failed for table 'person'."});
 * the underlying cause is attached, and engines log any SQL text instead of putting it in the message.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> (SqlBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.getSession()) {
 * 		<jk>return</jk> <jv>session</jv>.find(<jv>query</jv>);
 * 	} <jk>catch</jk> (BeanQuerySyntaxException <jv>e</jv>) {
 * 		<jk>throw new</jk> BadRequest(<jv>e</jv>.getMessage());          <jc>// The caller's fault.</jc>
 * 	} <jk>catch</jk> (BeanQueryExecutionException <jv>e</jv>) {
 * 		<jk>throw new</jk> InternalServerError(<jv>e</jv>, <js>"Search failed."</js>);  <jc>// Ours.</jc>
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public class BeanQueryExecutionException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param cause The underlying failure.  Can be <jk>null</jk>.
	 * @param message The printf-style message.
	 * @param args The message arguments.
	 */
	public BeanQueryExecutionException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}
}
