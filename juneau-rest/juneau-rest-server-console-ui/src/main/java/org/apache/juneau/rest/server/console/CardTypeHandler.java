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
package org.apache.juneau.rest.server.console;

import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * Server half of a console card type: validates an authored card and returns its page-contract fragment.
 * Rendering is never done here; the JS handler registered under the same {@link #type()} renders the fragment.
 *
 * <p>
 * A type with no registered handler uses the generic passthrough (JSON5 body becomes the fragment, markup body
 * becomes a template), so most custom cards need only JS. Register a handler when the server must validate
 * or enrich the body.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jk>public final class</jk> KpiCardType <jk>implements</jk> CardTypeHandler {
 *     <ja>@Override</ja> <jk>public</jk> String type() { <jk>return</jk> <js>"kpi"</js>; }
 *     <ja>@Override</ja> <jk>public</jk> JsonMap toFragment(CardSource <jv>src</jv>) {
 *       JsonMap <jv>b</jv> = <jv>src</jv>.json();                                  <jc>// E-24 on bad JSON5</jc>
 *       <jk>if</jk> (! <jv>b</jv>.containsKey(<js>"value"</js>))
 *         <jk>throw</jk> <jv>src</jv>.error(<js>"type='kpi' requires 'value'."</js>);
 *       <jk>return</jk> <jv>b</jv>;
 *     }
 *   }
 *   <jc>// Registration (either):</jc>
 *   ConsoleFreemarkerMixin.<jsm>create</jsm>().cardType(<jk>new</jk> KpiCardType()).build();
 *   <jc>// or META-INF/services/org.apache.juneau.rest.server.console.CardTypeHandler</jc>
 * </p>
 *
 * @since 10.0.0
 */
public interface CardTypeHandler {

	/**
	 * The type name this handler serves.
	 *
	 * @return The type name; must match {@code ^[a-z][a-z0-9-]{0,31}$} (E-21).
	 */
	String type();

	/**
	 * Validates the authored card and returns its contract fragment.
	 *
	 * @param source The authored card (attributes plus body).
	 * @return The fragment merged flat onto the card's base keys; must not contain a reserved key (E-23).
	 * @throws IllegalArgumentException via {@link CardSource#error(String, Object...)} on invalid input.
	 */
	JsonMap toFragment(CardSource source);

	/**
	 * Topics this card publishes without declaring them, for R-10. Default: none (the shell adds card:/cmd:).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <ja>@Override</ja>
	 *   <jk>public</jk> List&lt;String&gt; implicitTopics(JsonMap <jv>card</jv>) {
	 *     <jk>return</jk> List.<jsm>of</jsm>(<js>"kpi.threshold-crossed"</js>);
	 *   }
	 * </p>
	 *
	 * @param card The card object from the page contract (base keys plus this type's fragment).
	 * @return The concrete topics (or {@code ns.name:*} patterns) the type's JS publishes for this card.
	 */
	default List<String> implicitTopics(JsonMap card) { return List.of(); }

	/**
	 * Roles this type accepts in {@code subscribes[].as}, beyond {@code refresh} and {@code params}.
	 * {@code null} (the default) means "unknown to Java": the Java validator skips E-44 and the JS check (E-JS-47) decides.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <ja>@Override</ja>
	 *   <jk>public</jk> Set&lt;String&gt; acceptedRoles() { <jk>return</jk> Set.<jsm>of</jsm>(<js>"highlight"</js>); }
	 * </p>
	 *
	 * @return The accepted role names, or {@code null} if unknown.
	 */
	default Set<String> acceptedRoles() { return null; }

	/**
	 * {@code cmd:<id>} ops this type handles, beyond the shell's {@code refresh}, for E-43 on a ribbon {@code target}.
	 * {@code null} (the default) means "unknown to Java": the Java validator skips the check and the JS shell reports
	 * an unhandled op at runtime (E-JS-48).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   <ja>@Override</ja>
	 *   <jk>public</jk> Set&lt;String&gt; acceptedOps() { <jk>return</jk> Set.<jsm>of</jsm>(<js>"set-target"</js>); }
	 * </p>
	 *
	 * @return The handled op names, or {@code null} if unknown.
	 */
	default Set<String> acceptedOps() { return null; }
}
