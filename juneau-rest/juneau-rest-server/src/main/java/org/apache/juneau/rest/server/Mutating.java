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
package org.apache.juneau.rest.server;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Declares that a REST operation changes state, so that binding it to an HTTP method defined as safe becomes a
 * startup failure instead of a silent mistake.
 *
 * <h5 class='section'>The mistake this exists to catch</h5>
 * <p>
 * RFC 9110 defines {@code GET}, {@code HEAD}, {@code OPTIONS} and {@code TRACE} as <i>safe</i>: a client, a proxy,
 * a crawler or a browser prefetcher may issue them freely, repeat them, and follow them without asking, precisely
 * because they are defined not to change anything.  Everything downstream of a request is built on that promise
 * &mdash; caches store the response, link prefetchers fire the request before the user clicks, and every CSRF
 * defence in existence, including {@link org.apache.juneau.rest.server.filter.LoopbackBoundary}, applies its checks
 * only to the methods that are <i>not</i> safe.
 * <p>
 * So a handler that mutates state behind {@code GET} does not merely misuse a verb.  It sits behind whichever of
 * those checks the application relies on, having quietly opted out of all of them &mdash; not by disabling
 * anything, but by being classified as harmless.  Nothing about it looks wrong at the call site, and nothing
 * reports it at runtime, because from the boundary's point of view a {@code GET} arriving without a CSRF token is
 * an ordinary read.
 * <p>
 * That is the failure mode this annotation exists to convert into a boot error:
 * <p class='bjava'>
 * 	<jc>// Fails at startup: this mutates, and @RestOp with no method= and a name that starts with none of
 * 	// get/put/post/delete resolves to GET.</jc>
 * 	<ja>@Mutating</ja>
 * 	<ja>@RestOp</ja>
 * 	<jk>public</jk> Result <jsm>armRelease</jsm>(...) {...}
 * </p>
 * <p>
 * The {@code @RestOp} case above is worth singling out, because the developer never typed {@code GET} anywhere:
 * with no {@code method=} and a Java method name whose prefix is not one of {@code get}/{@code put}/{@code post}/
 * {@code delete}, the resolved method <b>defaults to {@code GET}</b>.  A mutating operation therefore lands on a
 * safe method through nobody's decision.  The check resolves the method the same way dispatch does, so it sees the
 * inferred {@code GET} rather than the absent annotation.
 *
 * <h5 class='section'>Why this is declared and not detected</h5>
 * <p>
 * The obvious alternative is for the framework to work out for itself which handlers mutate, and it was rejected
 * because no sound version of it exists.  "Mutates" is a claim about effects on state the <i>application</i> cares
 * about, and that has no syntactic signature:
 * <p class='bjava'>
 * 	<jv>runner</jv>.run(List.<jsm>of</jsm>(<js>"svn"</js>, <js>"commit"</js>, <jv>path</jv>));  <jc>// mutates the world</jc>
 * 	<jv>runner</jv>.run(List.<jsm>of</jsm>(<js>"svn"</js>, <js>"info"</js>, <jv>path</jv>));    <jc>// mutates nothing</jc>
 * </p>
 * <p>
 * Those are the same call to the same method with the same argument type, and telling them apart requires knowing
 * what {@code svn} does.  No analysis of the Java program can supply that.  Any analysis strong enough to see
 * through the indirection real applications use &mdash; an interface for the process runner, a supplier for the
 * credential, a method reference for the callback, an HTTP client whose URL is chosen at runtime &mdash; is a
 * whole-program points-to analysis, and it would still be wrong on the two lines above.
 * <p>
 * A naming heuristic ({@code set*}, {@code save*}, {@code delete*}) fails in both directions
 * &mdash; {@code getOrCreateSession} mutates, {@code applyFilter} does not &mdash; and has a worse property than
 * being unreliable: it would make renaming a Java method change the application's security posture.
 * <p>
 * Inspecting the signature fares no better.  A mutating operation need take nothing but a {@code @Path}: deleting
 * a record by id, or validating a stored credential and caching the verdict, both mutate while taking no body at
 * all.
 * <p>
 * The decisive argument is about the cost of being wrong.  This check's failure mode is <b>the application does
 * not start</b>.  A check that can refuse to start an application it merely suspects will be switched off, and a
 * check that has been switched off protects nothing &mdash; including against the real contradictions it would
 * have caught.  So the rule here is one a machine can evaluate with certainty: the developer stated that this
 * operation mutates, and the operation is bound to a method defined as safe.  Those cannot both be intended, and
 * no inference is involved in noticing it.
 *
 * <h5 class='section'>What this does not catch</h5>
 * <p>
 * <b>An operation that mutates and says nothing is invisible to this check.</b>  The check finds contradictions,
 * not omissions, and a developer who does not reach for the annotation gets no protection from it.  That limit is
 * inherent in the previous section: the alternative to a declaration that can be forgotten is an inference that
 * can be wrong, and at a gate that stops the application from booting, wrong is worse.
 * <p>
 * What makes the limit acceptable is that the annotation is <b>strictly additive</b>.  It can only ever cause a
 * boot failure; it never relaxes a runtime check, and there is deliberately no inverse annotation declaring a
 * {@code POST} to be safe.  An operation that omits it is therefore treated exactly as it would be if this
 * annotation did not exist &mdash; the boundary still classifies by HTTP method, still treats an unknown method as
 * state-changing, and still applies every write check to every non-safe method.  Adopting the annotation can move
 * an application from "wrong and silent" to "refuses to start"; it cannot move one from "protected" to
 * "unprotected".
 * <p>
 * For the same reason this is <b>method-level only</b>.  A class-level form meaning "everything here mutates"
 * would be convenient and would immediately produce false failures on the page-rendering {@code GET} that nearly
 * every such resource also has &mdash; reintroducing exactly the false-positive problem that ruled out inference.
 *
 * <h5 class='section'>When it runs</h5>
 * <p>
 * When the resource's operation table is assembled: at startup under {@link Rest#eagerInit()}, and otherwise on
 * the first request to that resource.  In both cases it runs <b>before any operation on that resource can be
 * dispatched</b>, so a contradiction cannot be reached by a request.  An application wanting the failure at boot
 * literally should set {@code eagerInit}.
 * <p>
 * The check is unconditional and needs no wiring.  It is not part of any filter, because HTTP method safety is not
 * a property of a security filter &mdash; it is a property of the resource, which the filter then depends on.  An
 * application with no {@code @Mutating} anywhere is unaffected.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Correct: declared mutating, bound to a method that is not safe.  Boots, and the boundary's write
 * 	// checks apply to it because POST is not in the safe set.</jc>
 * 	<ja>@Mutating</ja>
 * 	<ja>@RestPost</ja>(<js>"/{version}/arm"</js>)
 * 	<jk>public</jk> ArmResult <jsm>arm</jsm>(<ja>@Path</ja>(<js>"version"</js>) String <jv>version</jv>, <ja>@Content</ja> ArmRequest <jv>body</jv>) {...}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.filter.LoopbackBoundary}
 * 	<li class='jm'>{@link MethodSafety#check(java.util.List)}
 * </ul>
 *
 * @since 10.0.0
 */
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface Mutating {

	/**
	 * Optional note describing what this operation changes.
	 *
	 * <p>
	 * Included in the startup failure message when this operation is bound to a safe method, so the error can say
	 * what is at stake rather than only which method is wrong.  Has no other effect.
	 *
	 * @return The description of what this operation changes.
	 */
	String value() default "";
}
