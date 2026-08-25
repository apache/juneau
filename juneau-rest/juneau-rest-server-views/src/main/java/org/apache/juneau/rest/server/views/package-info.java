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

/**
 * A typed, declarative "rich DataTables view" toolkit that serializes to the frozen {@code VIEW_META} JSON wire
 * contract consumed by the first-party client runtime.
 *
 * <p>
 * This optional module builds on {@code juneau-rest-server-datatables} (the server-side query protocol):
 * an app author declares one {@link org.apache.juneau.rest.server.views.ViewDef} &mdash; columns, ribbon actions,
 * row-decorator rules, and named cell renderers &mdash; as ordinary Juneau beans, and the model serializes to the
 * {@code VIEW_META} JSON contract that the shipped {@code juneau-views.js}/{@code juneau-ribbon.js}/
 * {@code juneau-renders.js} runtime consumes to wire up a fully-featured DataTable.
 *
 * <p>
 * The module owns a dependency-free base {@code .tag} chip stylesheet and takes <b>no</b> hard dependency on
 * {@code juneau-rest-server-console-ui}: the base chip keeps client-rendered views legible standalone, while
 * console-ui's palette themes the shared {@code .tag.<domain>.<value>} class-name contract when present.
 *
 * <p>
 * Alongside the DataTables view it also hosts the emitter for the reusable calendar widget:
 * {@link org.apache.juneau.rest.server.views.CalendarTable#of(org.apache.juneau.rest.server.widgets.CalendarDef)}
 * paints a {@code data-juneau-calendar} month grid (server-rendered seed chips for true progressive enhancement,
 * plus the {@code <template>} skeletons and optional {@code escapeForScript}-encoded seed sidecar) that the
 * shipped {@code juneau-calendar.js}/{@code juneau-calendar.css} runtime hydrates.  The
 * {@link org.apache.juneau.rest.server.widgets.CalendarDef} bean itself lives in {@code juneau-rest-server-widgets}
 * (bean-only, no dependency on views); views <b>composes</b> it here.
 *
 * <h5 class='section'>Complex dialogs and nested popups (v1):</h5>
 * <p>
 * A {@link org.apache.juneau.rest.server.views.RowAction} whose presentation is {@code DIALOG} opens a declarative
 * popup dialog described by {@link org.apache.juneau.rest.server.widgets.ModalDef}.  The dialog may carry a
 * {@link org.apache.juneau.rest.server.widgets.FormDef} whose
 * {@link org.apache.juneau.rest.server.widgets.FormDef.Input}
 * controls are the frozen six-type vocabulary &mdash; {@code text}, {@code textarea}, {@code checkbox},
 * {@code toggle} (a {@code role=switch}), {@code select} (single-select), and {@code action} (a button holding an
 * {@link org.apache.juneau.rest.server.widgets.ActionRef} that opens a further dialog for a named row action, the
 * composition trigger).  Typed inputs are always painted from the model with {@code createElement} + {@code .value} /
 * {@code .checked} / {@code textContent} &mdash; never as markup &mdash; so a live-data or hostile field value can
 * never become an element.  The client adds advisory inline validation (required, {@code maxLength}, {@code pattern})
 * that blocks the confirm while the <b>server submit stays fully authoritative</b>; a {@code pattern} that a browser
 * regex engine rejects fails <i>open</i> rather than blocking a legitimate submit.
 *
 * <p>
 * At runtime, dialogs, cell popovers, and row-action menus share a single popup layer stack that owns focus trapping,
 * Escape-dismiss of the top layer, focus restore on close, and z-ordering.  Cell-anchored surfaces are portalled to
 * {@code document.body} as {@code position:fixed} so an overflow ancestor cannot clip them; the page-size chooser
 * stays {@code position:absolute}.  The v1 stack caps <b>dialog-kind</b> layers at two: a third dialog is a visible
 * refusal painted into the current top dialog rather than an unbounded stack.
 *
 * <p>
 * Both beans {@code implements Widget}: each declares a {@code CONTRACT_VERSION} and a {@link
 * org.apache.juneau.rest.server.widgets.FormDef#validate() validate()} invariant check, and the serving path stamps
 * the instance version via {@link org.apache.juneau.rest.server.widgets.FormDef#checked() checked()}.  When a form is
 * present the client enforces a fail-loud handshake &mdash; BOTH the modal and the nested form version must match the
 * runtime's baked-in version or the dialog refuses to open; a confirm-only dialog (no form) stays unversioned.
 *
 * <h5 class='section'>Relocated: the dialog beans now live in the widgets module</h5>
 * <p>
 * {@code ModalDef} and {@code FormDef} were general enough to belong to the shared widget vocabulary, so they have
 * moved from this package to {@code org.apache.juneau.rest.server.widgets}.  This is a <b>breaking package
 * relocation with no shim</b>: a deprecated forwarding class left behind here would re-create the views&rarr;widgets
 * dependency in reverse, so applications update their imports instead.  Only the dialog beans moved &mdash; the
 * table/row-action types they are used with ({@link org.apache.juneau.rest.server.views.RowAction},
 * {@link org.apache.juneau.rest.server.views.IdempotencyKey},
 * {@link org.apache.juneau.rest.server.views.ActionResult},
 * {@link org.apache.juneau.rest.server.views.WritePermit}) stay here.  The dialog/form <b>runtime</b> also stays
 * here, inside {@code juneau-views.js}: after the move a widgets-module bean is driven by a views-module script,
 * which is deliberate rather than accidental (splitting that monolith is a separate concern).
 *
 * <h5 class='section'>Follow-ons (not in this cut):</h5>
 * <ul>
 * 	<li>A second in-tree dialog-form consumer is a 10.0-destination follow-on.
 * 	<li>Richer controls (cron editor, rich text) are deferred.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.views.CalendarTable}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.datatables.DataTablesQueryProtocol}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.datatables.DataTablesColumns}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/manual/server-side">DataTables Server-Side Processing</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.views;
