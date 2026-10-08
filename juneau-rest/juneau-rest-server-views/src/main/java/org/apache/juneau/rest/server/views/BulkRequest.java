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
package org.apache.juneau.rest.server.views;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * The request body of an {@code aggregate}-mode bulk action ({@link RowAction.BulkMode#AGGREGATE}) &mdash;
 * every selected row's stable id, plus a client-generated idempotency key so a retried submit (e.g. after a
 * dropped connection) never double-applies.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(path=<js>"/changes/abort"</js>)
 * 	<jk>public</jk> BulkResult abortChanges(<ja>@Content</ja> BulkRequest <jv>request</jv>) {
 * 		<jk>if</jk> (<jv>idempotencyStore</jv>.seen(<jv>request</jv>.idempotencyKey))
 * 			<jk>return</jk> <jv>idempotencyStore</jv>.resultFor(<jv>request</jv>.idempotencyKey);
 * 		<jc>// ... apply, then record the result under request.idempotencyKey ...</jc>
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
@BeanType(properties="ids,idempotencyKey")
public final class BulkRequest {

	/** The stable ids of every row the client submitted this action for. */
	public List<String> ids;

	/** A client-generated token unique to this submit, so a retried request is never double-applied. */
	public String idempotencyKey;
}
