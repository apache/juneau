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

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.rest.server.widgets.*;

/**
 * A {@link CardBody} that hosts a whole {@link ViewDef} &mdash; a live view table inside a card.
 *
 * <h5 class='section'>Why this type lives in views, not with the other card beans:</h5>
 * <p>
 * Every other card bean ({@link Card}, {@link CardGrid}, {@link CardFieldList}) lives in
 * {@code juneau-rest-server-widgets}, but this one wraps a {@link ViewDef}, which is a views type.  The dependency
 * edge between the two modules is one-way &mdash; views composes the widget beans and widgets never reaches back
 * &mdash; so a body that names a {@link ViewDef} can only be declared here, implementing the widgets
 * {@link CardBody} interface from the far side of that edge.  That is precisely why {@link CardBody} is a plain,
 * non-sealed interface: the extension point has to stay open to the module above it.
 *
 * <h5 class='section'>It brings its own data path:</h5>
 * <p>
 * A {@link CardFieldList} is re-filled by the card runtime from a card-level refresh endpoint.  This body has no such
 * wire and deliberately carries no {@code refreshEndpoint} / {@code pollIntervalMs} of its own: the hosted table is
 * emitted by {@link ViewTable}, so it arrives with the table's own ajax, paging, sorting and refresh behavior
 * already attached.  A card hosting one therefore carries none of the {@code data-juneau-card-refresh} attributes,
 * and there is no way to declare a card-level refresh wire that would then compete with the table's own.
 *
 * <h5 class='section'>A request is required:</h5>
 * <p>
 * The hosted table is emitted through {@link ViewTable#of(jakarta.servlet.http.HttpServletRequest,ViewDef)}, so the
 * enclosing card must be emitted by {@link CardGridTable#of(jakarta.servlet.http.HttpServletRequest,Card)}.  The
 * request-free {@link CardGridTable#of(CardGrid)} overload has no request to hand down and <b>fails closed</b> on
 * this body rather than emitting a table that would silently lose its CSRF token and server-value resolution.
 *
 * <h5 class='section'>Visibility:</h5>
 * <p>
 * A card is an ordinary element, not a {@code <template>}, so a hosted table is initialized by the views runtime's
 * page-load pass like any other {@code data-juneau-view} table &mdash; <b>including</b> when its card starts out
 * hidden (inside an inactive tab panel, say).  It is live, exactly as a table in a hidden tab is live today.  Only
 * the poll timers of a refreshable {@link CardFieldList} sibling are suspended while a card is hidden.
 *
 * <h5 class='section'>Nesting:</h5>
 * <p>
 * A hosted view may declare a nested table wherever {@link NestedTableDef} already allows one.  Hosting a view in a
 * card grants no additional nesting depth.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link CardGridTable}
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
public class ViewCardBody implements CardBody {

	/**
	 * The frozen contract version for this body type.
	 *
	 * <p>
	 * Independent of {@link CardFieldList#CONTRACT_VERSION}, which versions the card <i>refresh envelope</i> and is
	 * the value the card runtime handshakes against.  This body has no refresh envelope, so a revision here can
	 * never disturb that handshake &mdash; and vice versa.
	 */
	public static final String CONTRACT_VERSION = "1";

	/** The hosted view.  Required. */
	public ViewDef view;

	/**
	 * Creates a body hosting the given view.
	 *
	 * @param value The view to host.  Must not be <jk>null</jk>.
	 * @return A new {@link ViewCardBody}.
	 */
	public static ViewCardBody of(ViewDef value) {
		var b = new ViewCardBody();
		b.view = value;
		return b;
	}

	/**
	 * Fail-closed bean validation; fans out to {@link ViewDef#validate()} so a card can never carry a view the
	 * emitter would have rejected on its own.
	 *
	 * @throws IllegalArgumentException If this body is not well-formed.
	 */
	@Override /* CardBody */
	public void validate() {
		if (view == null)
			throw iaex("ViewCardBody must declare a view.");
		view.validate();
	}
}
