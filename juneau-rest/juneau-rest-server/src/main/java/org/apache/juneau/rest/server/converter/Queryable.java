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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;

/**
 * Converter for enabling search/view/sort/paging support on response objects returned by a
 * <c>@RestOp</c>-annotated method, backed by the BeanQuery engine.
 *
 * <p>
 * When enabled, a response that's a {@link Collection} or object array of beans, or of {@link Map Maps}, can be
 * filtered with the BeanQuery search language via the following request parameters (see {@link BeanQueryRequest}
 * for the per-parameter Swagger docs):
 * <ul class='spaced-list'>
 * 	<li><c>&amp;search=</c> Comma-separated <c>column=expression</c> clauses.
 * 	<li><c>&amp;view=</c> Comma-separated columns to return, in order.
 * 	<li><c>&amp;sort=</c> Comma-separated sort columns; append <js>":desc"</js> (or <js>"-"</js>) for descending.
 * 	<li><c>&amp;position=</c> 0-based index of the first row.
 * 	<li><c>&amp;limit=</c> Maximum rows to return.
 * 	<li><c>&amp;opts=</c> Options as <c>key=value</c> clauses (e.g. <c>counts=true</c>).
 * </ul>
 *
 * <p>
 * A response that isn't tabular &mdash; not a {@link Collection} or object array, or one whose first non-null element
 * is a plain string, number, date or other scalar &mdash; is returned <b>unchanged</b>; the query parameters are
 * ignored, exactly as they were for non-tabular results before 10.0.  An empty or all-null collection is likewise
 * returned unchanged (there's no element to derive columns from).
 *
 * <p>
 * <b>Bean rows</b> get one {@link InMemoryBeanQueryContext} per element class, built once (with any
 * {@link QueryableSettings} bean applied) and cached for the lifetime of this converter instance, so
 * {@link QueryableSettings} is read once per element class, not on every request.  <b>{@link Map} rows</b> get one
 * column per key of the first non-null row (type inferred from that row's value by the same rule as bean
 * properties, see {@link QueryProperty#searchTypeOf(Class)}: a {@link Number} &rarr; {@link SearchType#NUMERIC}, a
 * {@link Boolean} &rarr; {@link SearchType#BOOLEAN}, an enum &rarr; {@link SearchType#ENUM}, a
 * date/calendar/temporal &rarr; {@link SearchType#TIMESTAMP}, anything else (or a <jk>null</jk>) &rarr;
 * {@link SearchType#TEXT}); this context is built on every
 * request, since the key set can differ per response.  Keys that appear only in later rows are not queryable.
 *
 * <p>
 * <c>&amp;opts=counts=true</c> adds {@link #TOTAL_HEADER} / {@link #MATCHED_HEADER} response headers carrying the
 * session's <c>total</c>/<c>matched</c> row counts &mdash; each header is set only when the corresponding count
 * is present.  Without it, neither header is set.  The response body itself stays a bare list either way.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=Queryable.<jk>class</jk>)
 * 	<jk>public class</jk> PeopleResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> QueryableSettings queryableSettings() {
 * 			<jk>return</jk> QueryableSettings.<jsm>create</jsm>().maxLimit(500).build();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/people"</js>)
 * 		<jk>public</jk> List&lt;Person&gt; getPeople() {
 * 			<jk>return</jk> <jf>people</jf>;
 * 		}
 * 	}
 * </p>
 * <p>
 * 	<c>GET /people?search=age=$gte(18)&amp;sort=name,age:desc&amp;view=name,age&amp;position=20&amp;limit=10</c>
 * </p>
 * <p>
 * 	A bad column or option (e.g. <c>?sort=noSuchColumn</c>) throws {@link BeanQuerySyntaxException}, which
 * 	{@code RestContext.convertThrowable} maps to an HTTP&nbsp;400 carrying an <c>X-BeanQuery-Error</c> header and,
 * 	with problem details on, a <js>"code"</js> member.
 * </p>
 * <p>
 * 	<c>GET /people?search=age=$gte(18)&amp;opts=counts=true</c> adds count headers to the (still bare-list) body:
 * </p>
 * <p class='bcode'>
 * 	X-BeanQuery-Total: 42
 * 	X-BeanQuery-Matched: 7
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link QueryableSettings} - Per-resource limits and defaults for this converter.
 * 	<li class='jc'>{@link BeanQueryRequest} - The request parameters this converter binds.
 * 	<li class='jc'>{@link InMemoryBeanQueryContext} - The engine this converter drives.
 * 	<li class='ja'>{@link RestOp#converters()} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"unchecked" // convertBeanRows() casts the runtime element Class<?> to Class<Object> to build the in-memory query context
})
public class Queryable implements RestConverter {

	/**
	 * Response header carrying the total (unfiltered) row count.
	 *
	 * <p>
	 * Set only when {@code &opts=counts=true} is requested and the session reports a {@code total()} count.
	 */
	public static final String TOTAL_HEADER = "X-BeanQuery-Total";

	/**
	 * Response header carrying the filtered (matched) row count.
	 *
	 * <p>
	 * Set only when {@code &opts=counts=true} is requested and the session reports a {@code matched()} count.
	 */
	public static final String MATCHED_HEADER = "X-BeanQuery-Matched";

	/**
	 * Swagger parameters for this converter.
	 */
	public static final String SWAGGER_PARAMS = """
\t\t{
\t\t\tin:'query',
\t\t\tname:'search',
\t\t\tdescription:'Search expression: comma-separated column=expression clauses, e.g.
\t\t\t\tname=$contains(smith),age=$gte(18). See the BeanQuery search language docs:
\t\t\t\thttps://juneau.apache.org/docs/topics/JuneauCommonsBeanQuery',
\t\t\ttype:'string',
\t\t\texamples:{example:'?search=name=$contains(smith),age=$gte(18)'}
\t\t},{
\t\t\tin:'query',
\t\t\tname:'view',
\t\t\tdescription:'Comma-separated columns to return, in order.',
\t\t\ttype:'string',
\t\t\texamples:{example:'?view=name,age'}
\t\t},{
\t\t\tin:'query',
\t\t\tname:'sort',
\t\t\tdescription:'Comma-separated sort columns.
\t\t\t\tAppend \\':desc\\' (or \\'-\\') for descending.
\t\t\t\tThe default is ascending order.',
\t\t\ttype:'string',
\t\t\texamples:{example:'?sort=name,age:desc'}
\t\t},{
\t\t\tin:'query',
\t\t\tname:'position',
\t\t\tdescription:'0-based index of the first row.',
\t\t\ttype:'integer',
\t\t\tminimum:0,
\t\t\texamples:{example:'?position=20'}
\t\t},{
\t\t\tin:'query',
\t\t\tname:'limit',
\t\t\tdescription:'Maximum rows to return.
\t\t\t\tDefaults and caps are set by the server.',
\t\t\ttype:'integer',
\t\t\texamples:{example:'?limit=10'}
\t\t},{
\t\t\tin:'query',
\t\t\tname:'opts',
\t\t\tdescription:'Options as key=value clauses, e.g. counts=true.',
\t\t\ttype:'string',
\t\t\texamples:{example:'?opts=counts=true'}
\t\t}""";

	private static final List<Class<?>> SIMPLE_ELEMENT_TYPES = List.of(
		CharSequence.class, Number.class, Boolean.class, Character.class, Enum.class,
		Date.class, Calendar.class, Temporal.class
	);

	private final ConcurrentHashMap<Class<?>,Optional<InMemoryBeanQueryContext<Object>>> contextCache = new ConcurrentHashMap<>();

	@Override /* Overridden from RestConverter */
	public Object convert(RestRequest req, Object o) {
		if (o == null)
			return null;

		List<Object> rows;
		if (o instanceof Collection<?> c)
			rows = nonNull(c);
		else if (o instanceof Object[] a)
			rows = nonNull(Arrays.asList(a));
		else
			return o;

		if (rows.isEmpty())
			return o;

		var first = rows.get(0);
		Optional<Page<?>> page;
		if (first instanceof Map<?,?> m)
			page = convertMapRows(req, rows, m);
		else if (isSimpleType(first.getClass()))
			page = Optional.empty();
		else
			page = convertBeanRows(req, rows, first.getClass());

		return page.isPresent() ? page.get().rows() : o;
	}

	/**
	 * Returns the cached bean context for an element class, or <jk>null</jk> if none has been built (package-private, for tests).
	 *
	 * @param elementClass The row class.
	 * @return The cached context, or <jk>null</jk>.
	 */
	InMemoryBeanQueryContext<Object> cachedContext(Class<?> elementClass) {
		return contextCache.getOrDefault(elementClass, Optional.empty()).orElse(null);
	}

	private static List<Object> nonNull(Collection<?> c) {
		return addAllNn(new ArrayList<Object>(c.size()), c);
	}

	private Optional<Page<?>> convertBeanRows(RestRequest req, List<Object> rows, Class<?> elementClass) {
		var ctx = contextCache.computeIfAbsent(elementClass, c -> {
			if (QueryModel.of(c).getReadableProperties().isEmpty())
				return Optional.empty();
			var b = InMemoryBeanQueryContext.create((Class<Object>)c);
			return Optional.of(settingsOf(req).applyTo(b).build());
		});
		if (ctx.isEmpty())
			return Optional.empty();
		return Optional.of(run(req, ctx.get(), rows));
	}

	private Optional<Page<?>> convertMapRows(RestRequest req, List<Object> rows, Map<?,?> first) {
		var b = InMemoryBeanQueryContext.<Object>create();
		var columns = 0;
		for (var e : first.entrySet()) {
			var name = String.valueOf(e.getKey());
			if (! name.isBlank()) {
				b.column(name, e.getValue() == null ? SearchType.TEXT : QueryProperty.searchTypeOf(e.getValue().getClass()));
				columns++;
			}
		}
		if (columns == 0)
			return Optional.empty();
		return Optional.of(run(req, settingsOf(req).applyTo(b).build(), rows));
	}

	private static Page<?> run(RestRequest req, InMemoryBeanQueryContext<Object> ctx, List<Object> rows) {
		var query = req.getRequest(BeanQueryRequest.class);
		try (var s = ctx.getSession(rows)) {
			var view = query.getView();
			var page = view == null || view.isBlank() ? s.find(query) : s.findValues(query);
			setCountHeaders(req, page);
			return page;
		}
	}

	private static void setCountHeaders(RestRequest req, Page<?> page) {
		page.total().ifPresent(v -> req.getResponse().setHeader(TOTAL_HEADER, String.valueOf(v)));
		page.matched().ifPresent(v -> req.getResponse().setHeader(MATCHED_HEADER, String.valueOf(v)));
	}

	private static boolean isSimpleType(Class<?> c) {
		for (var t : SIMPLE_ELEMENT_TYPES)
			if (t.isAssignableFrom(c))
				return true;
		return false;
	}

	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this only borrows a bean and must not close it.
	})
	private static QueryableSettings settingsOf(RestRequest req) {
		return req.getContext().getBeanStore().getBean(QueryableSettings.class).orElse(QueryableSettings.DEFAULT);
	}
}
