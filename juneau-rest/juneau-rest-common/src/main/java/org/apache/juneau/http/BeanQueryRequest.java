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
package org.apache.juneau.http;

import org.apache.juneau.commons.Schema;
import org.apache.juneau.commons.beanquery.*;

/**
 * The HTTP-binding {@link Request @Request} bean for {@link BeanQuery}-style list queries.
 *
 * <p>
 * This is a concrete {@link BeanQuery} subclass &mdash; not an interface &mdash; so it <b>is</b> the carrier the query
 * engine consumes.  Binding a list REST operation to a single {@link BeanQueryRequest} argument populates the inherited
 * {@code search} / {@code view} / {@code sort} / {@code position} / {@code limit} / {@code opts} fields straight from
 * the request, with no adapter step.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>private static final</jk> InMemoryBeanQueryContext&lt;Row&gt; <jsf>ROWS</jsf> = InMemoryBeanQueryContext
 * 		.<jsm>create</jsm>(Row.<jk>class</jk>)
 * 		.build();
 *
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> List&lt;Row&gt; getList(BeanQueryRequest <jv>q</jv>) {
 * 		<jk>try</jk> (InMemoryBeanQuerySession&lt;Row&gt; <jv>s</jv> = <jsf>ROWS</jsf>.getSession(<jv>rows</jv>)) {
 * 			<jk>return</jk> <jv>s</jv>.find(<jv>q</jv>).rows();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>How it binds</h5>
 * <p>
 * The type is annotated {@link Request @Request}, so a parameter of this type needs no second <ja>@Request</ja> on the
 * parameter.  Because it is a concrete class, the framework instantiates it with its public no-arg constructor and
 * calls the JavaBeans setter matching each annotated getter (so {@link #getOpts()} is populated via
 * {@link BeanQuery#setOpts(String) setOpts}).  Each getter is a single-valued {@link Query @Query}: exactly one
 * {@code search} parameter and one {@code opts} parameter bind &mdash; the {@code $}-language and the
 * {@code key=value} opts bag live <b>inside</b> those single strings and are decoded by the session, not on the wire.
 *
 * <h5 class='section'>Wire names</h5>
 * <p>
 * Every {@code @Query} name equals its Java property name: {@code search}, {@code view}, {@code sort},
 * {@code position}, {@code limit}, {@code opts}.  A resource that wants a different name (or extra parameters)
 * subclasses this bean and redeclares the getter with its own {@link Query @Query} name.
 *
 * <h5 class='section'>Swagger</h5>
 * <p>
 * Each getter carries a {@link Schema @Schema} description, so an operation taking this bean documents all six
 * parameters automatically; {@code position} also declares {@code minimum=0}, so a negative value fails part
 * validation (HTTP&nbsp;400) before the query runs.  {@code limit} has no minimum: the context clamps it instead.
 *
 * <h5 class='section'>Saved-view clash</h5>
 * <p>
 * {@link #getView()} is a <b>column projection</b> (field names), not a saved-view id.  Saved-view CRUD endpoints must
 * <b>not</b> take {@link BeanQueryRequest}; they keep their own {@link Query @Query}(<js>"view"</js>) id parameter, or
 * the wrong meaning of {@code view} would bind.
 *
 * <h5 class='section'>Errors</h5>
 * <p>
 * A malformed {@code search} (or any other clause the session rejects) throws {@link BeanQuerySyntaxException},
 * which {@code RestContext.convertThrowable} maps to an HTTP&nbsp;400 carrying an {@value #ERROR_HEADER} header and,
 * with problem details on, a {@code "code"} member:
 * </p>
 * <p class='bcode'>
 * 	GET /people?search=pasword=x&amp;sort=name,age:desc&amp;view=name,age&amp;position=20&amp;limit=10
 *
 * 	HTTP/1.1 400 Bad Request
 * 	X-BeanQuery-Error: UNKNOWN_COLUMN
 *
 * 	{
 * 	  "status": 400,
 * 	  "title": "Bad Request",
 * 	  "detail": "Unknown column 'pasword' in search.",
 * 	  "code": "UNKNOWN_COLUMN"
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanQuery}
 * 	<li class='ja'>{@link Request}
 * 	<li class='ja'>{@link Query}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Request">@Request</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsBeanQuery">BeanQuery</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Request
public class BeanQueryRequest extends BeanQuery {

	/**
	 * The response header {@code RestContext.convertThrowable} sets to a {@link BeanQuerySyntaxException}'s
	 * {@link BeanQuerySyntaxException#code() code()} name on every 400 it produces from that exception (with or without
	 * problem details).
	 */
	public static final String ERROR_HEADER = "X-BeanQuery-Error";

	@Override /* BeanQuery */
	@Query(schema=@Schema(d="Search expression: comma-separated column=expression clauses, e.g. "
		+ "name=$contains(smith),age=$gte(18). See the BeanQuery search language docs: "
		+ "https://juneau.apache.org/docs/topics/JuneauCommonsBeanQuery"))
	public String getSearch() {
		return super.getSearch();
	}

	@Override /* BeanQuery */
	@Query(schema=@Schema(d="Comma-separated columns to return, in order."))
	public String getView() {
		return super.getView();
	}

	@Override /* BeanQuery */
	@Query(schema=@Schema(d="Comma-separated sort columns; append \":desc\" (or \"-\") for descending."))
	public String getSort() {
		return super.getSort();
	}

	@Override /* BeanQuery */
	@Query(schema=@Schema(d="0-based index of the first row.", minimum="0"))
	public Integer getPosition() {
		return super.getPosition();
	}

	@Override /* BeanQuery */
	@Query(schema=@Schema(d="Maximum rows to return. Defaults and caps are set by the server."))
	public Integer getLimit() {
		return super.getLimit();
	}

	@Override /* BeanQuery */
	@Query(schema=@Schema(d="Options as key=value clauses, e.g. counts=true."))
	public String getOpts() {
		return super.getOpts();
	}
}
