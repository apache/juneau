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
package org.apache.juneau.rest.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.juneau.bean.rfc7807.Problem;
import org.apache.juneau.bean.rfc7807.ProblemException;
import org.apache.juneau.bean.rfc7807.ProblemLocalizationStrategy;
import org.apache.juneau.bean.rfc7807.ProblemMapper;
import org.apache.juneau.bean.rfc7807.ProblemMapperList;
import org.apache.juneau.bean.rfc7807.adapter.ProblemAdapters;
import org.apache.juneau.http.header.ContentType;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.rest.RestOpContext;
import org.apache.juneau.rest.RestOpSession;
import org.apache.juneau.serializer.SerializeException;

/**
 * Response processor that serializes
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807 / 9457</a> {@link Problem} payloads as
 * {@code application/problem+json}.
 *
 * <p>
 * Fires when the response content is one of the following (in order):
 * <ul class='spaced-list'>
 * 	<li>A {@link Problem} bean &mdash; serialized directly. Honors the client {@code Accept} header.
 * 	<li>A {@link ProblemException} &mdash; unwrapped via {@link ProblemException#getProblem()}. Honors the
 * 		client {@code Accept} header.
 * 	<li>A {@link BasicHttpException} &mdash; adapted via a registered {@link ProblemMapper} (if any matches),
 * 		else via {@link ProblemAdapters#fromException(BasicHttpException)}. This branch handles the common throw
 * 		path where the operation handler throws a {@code BasicHttpException} and the framework sets it as the
 * 		response content. <b>Gated by {@code isProblemDetails()}</b> on the {@link RestOpContext} (per-op
 * 		{@code @RestGet(problemDetails)} / {@code @RestPost(problemDetails)} / etc., falling back to the
 * 		resource-level {@code @Rest(problemDetails)} opt-in via tri-state inheritance). When opted-in, the
 * 		{@code Accept} header is ignored on this branch (errors always emit {@code application/problem+json}; see
 * 		RFC 7807).
 * 	<li>Any other {@link Throwable} &mdash; only when an explicit {@link ProblemMapper} matches its class
 * 		hierarchy. Gated by the same opt-in. Mappers that return {@code null} are skipped and the chain
 * 		continues; if no mapper matches, the processor returns {@code NEXT}.
 * </ul>
 *
 * <h5 class='section'>Mapper discovery</h5>
 * <p>
 * The processor pulls registered {@link ProblemMapper}s from the resource's bean store in this order:
 * <ul class='spaced-list'>
 * 	<li>A {@link ProblemMapperList} bean &mdash; treated as the authoritative ordered registry. Recommended
 * 		when more than one mapper is registered, since the underlying {@code @Bean} bean-store walk pairs each
 * 		factory method with its return type and so collapses multiple
 * 		{@code @Bean public ProblemMapper foo()} entries on the same resource into a single slot.
 * 	<li>A single {@link ProblemMapper} bean (via {@code BeanStore.getBean(ProblemMapper.class)}) &mdash; the
 * 		conventional single-mapper registration pattern.
 * </ul>
 * <p>
 * For a given thrown exception the processor walks the collected mappers and picks the one whose
 * {@link ProblemMapper#getExceptionType()} sits closest to the thrown class in the hierarchy (most-specific
 * wins; ties broken by registration order). The chosen mapper's {@link ProblemMapper#map(Throwable)} return
 * value replaces the default adaptation; a {@code null} return falls through to the next-most-specific mapper,
 * and ultimately to the built-in {@link ProblemAdapters#fromException(BasicHttpException)} fallback for
 * {@link BasicHttpException} subclasses.
 *
 * <h5 class='section'>Localization seam</h5>
 * <p>
 * Before serializing, the processor consults the bean store for a {@link ProblemLocalizationStrategy} bean and
 * passes the resolved {@link Problem} (and {@code RestRequest.getLocale()}) through it. The default behavior is
 * {@link ProblemLocalizationStrategy#IDENTITY} (pass-through) &mdash; this is a future-work seam intentionally
 * shipped without a built-in {@code Messages}/resource-bundle implementation. A {@code null} strategy return is
 * treated as &quot;no opinion&quot; and the pre-localization {@link Problem} is used instead.
 *
 * <p>
 * <b>Status code policy.</b> When {@link Problem#getStatus()} is non-{@code null}, the processor calls
 * {@code RestResponse.setStatus(int)} with that value. Otherwise it leaves the existing status alone &mdash;
 * {@code RestSession.run()} normalizes {@code 0} to {@code 200} so per-op default codes (e.g. {@code @RestPost}
 * default of 200) still flow through.
 *
 * <p>
 * <b>{@code type} field policy.</b> Serialized as-is. A {@code null} {@code type} is omitted from the JSON output
 * rather than synthesized as {@code "type":"about:blank"} on the wire (preserves the absent-vs-explicit distinction).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Problem}
 * 	<li class='jc'>{@link ProblemException}
 * 	<li class='jc'>{@link ProblemMapper}
 * 	<li class='jc'>{@link ProblemLocalizationStrategy}
 * 	<li class='jc'>{@link ProblemAdapters}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class ProblemDetailsProcessor implements ResponseProcessor {

	private static final String PROBLEM_JSON = ContentType.APPLICATION_PROBLEM_JSON.getValue();

	@Override /* Overridden from ResponseProcessor */
	@SuppressWarnings({
		"resource"  // negotiated output stream owned by the response; closed by the container/RestCall
	})
	public int process(RestOpSession opSession) throws IOException {

		var res = opSession.getResponse();
		var raw = res.getContent(Object.class);

		if (raw == null)
			return NEXT;

		Problem problem;
		boolean isErrorPath;

		if (raw instanceof Problem p) {
			problem = p;
			isErrorPath = false;
		} else if (raw instanceof ProblemException pe) {
			problem = pe.getProblem();
			isErrorPath = opSession.getContext().isProblemDetails();
		} else if (raw instanceof BasicHttpException e) {
			if (! opSession.getContext().isProblemDetails())
				return NEXT;
			problem = mapException(opSession, e);
			isErrorPath = true;
		} else if (raw instanceof Throwable t) {
			if (! opSession.getContext().isProblemDetails())
				return NEXT;
			problem = mapException(opSession, t);
			if (problem == null)
				return NEXT;
			isErrorPath = true;
		} else {
			return NEXT;
		}

		if (problem == null)
			return NEXT;

		// Q5: error path ignores Accept (RFC 7807 §3); success path honors Accept strictly.
		if (! isErrorPath) {
			var accept = opSession.getRequest().getHeaderParam("Accept").orElse("");
			if (! acceptsProblemJson(accept))
				return NEXT;
		}

		problem = localize(opSession, problem);

		var status = problem.getStatus();
		if (status != null)
			res.setStatus(status.intValue());

		res.setHeader(ContentType.APPLICATION_PROBLEM_JSON);

		try {
			var os = res.getNegotiatedOutputStream();
			JsonSerializer.DEFAULT.serialize(problem, os);
			os.flush();
			os.finish();
		} catch (SerializeException e) {
			throw new IOException("Failed to serialize Problem payload", e);
		}

		return FINISHED;
	}

	/**
	 * Resolves a {@link Problem} from a thrown exception by consulting registered {@link ProblemMapper}s in
	 * most-specific-first order, falling back to {@link ProblemAdapters#fromException(BasicHttpException)} for
	 * {@link BasicHttpException} subclasses when no mapper matches (or all matching mappers return {@code null}).
	 *
	 * @return The {@link Problem} to emit, or {@code null} when no mapper opined and the exception is not a
	 * 	{@link BasicHttpException}.
	 */
	@SuppressWarnings({
		"unchecked", // ProblemMapper#map is parameterized over the matched throwable subtype.
		"rawtypes"   // ProblemMapper#map is parameterized over the matched throwable subtype.
	})
	private static Problem mapException(RestOpSession opSession, Throwable thrown) {
		for (var mapper : resolveMappers(opSession, thrown.getClass())) {
			var result = ((ProblemMapper) mapper).map(thrown);
			if (result != null)
				return result;
		}
		if (thrown instanceof BasicHttpException e)
			return ProblemAdapters.fromException(e);
		return null;
	}

	/**
	 * Returns all registered {@link ProblemMapper}s whose {@link ProblemMapper#getExceptionType()} is assignable
	 * from {@code thrownClass}, sorted most-specific-first. A {@link ProblemMapperList} bean (if present) is
	 * consulted first; a single {@link ProblemMapper} bean (if no list is registered) is consulted as a
	 * fallback. Registration order serves as the tiebreaker for mappers at the same hierarchy depth.
	 */
	@SuppressWarnings({
		"resource" // RestOpSession owns this bean store lifecycle.
	})
	private static List<ProblemMapper<?>> resolveMappers(RestOpSession opSession, Class<?> thrownClass) {
		var bs = opSession.getBeanStore();
		var candidates = new ArrayList<ProblemMapper<?>>();
		bs.getBean(ProblemMapperList.class).map(ProblemMapperList::asList).ifPresent(candidates::addAll);
		if (candidates.isEmpty())
			bs.getBean(ProblemMapper.class).ifPresent(candidates::add);
		if (candidates.isEmpty())
			return List.of();
		var matches = new ArrayList<ProblemMapper<?>>(candidates.size());
		for (var m : candidates) {
			if (m.getExceptionType().isAssignableFrom(thrownClass))
				matches.add(m);
		}
		matches.sort((a, b) -> hierarchyDepth(a.getExceptionType(), thrownClass) - hierarchyDepth(b.getExceptionType(), thrownClass));
		return matches;
	}

	/**
	 * Returns the number of superclass hops from {@code thrownClass} up to {@code targetType}; lower is closer.
	 * A {@code 0} return means {@code targetType} equals {@code thrownClass} (exact match); larger values mean
	 * the mapper sits higher up the class hierarchy and is therefore less specific. Sorting ascending by this
	 * value puts the most-specific mapper first.
	 */
	private static int hierarchyDepth(Class<?> targetType, Class<?> thrownClass) {
		var depth = 0;
		var c = thrownClass;
		while (c != null && c != targetType) {
			c = c.getSuperclass();
			depth++;
		}
		return c == null ? Integer.MAX_VALUE : depth;
	}

	private static Problem localize(RestOpSession opSession, Problem problem) {
		@SuppressWarnings({
			"resource" // RestOpSession owns this bean store lifecycle.
		})
		var strategy = opSession.getBeanStore().getBean(ProblemLocalizationStrategy.class).orElse(ProblemLocalizationStrategy.IDENTITY);
		var locale = opSession.getRequest().getLocale();
		var result = strategy.localize(problem, locale);
		return result == null ? problem : result;
	}

	private static boolean acceptsProblemJson(String accept) {
		if (accept == null || accept.isEmpty())
			return true;
		return accept.contains(PROBLEM_JSON) || accept.contains("*/*");
	}
}
