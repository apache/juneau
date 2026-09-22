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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.io.*;
import java.util.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The capture-only console slot directives: {@code <@head>}, {@code <@scripts>}, {@code <@brand>},
 * {@code <@actions>}, {@code <@title>}, {@code <@footer>}, and {@code <@body>}. One parameterized instance is
 * registered per slot name (its {@link ConsoleContext} field), so the seven directives share this one class.
 *
 * <p>
 * Each is authored inside {@code <@console>}. During the console's body pass this directive <b>captures</b> its
 * rendered body into the matching {@link ConsoleContext} field and emits <b>nothing</b> at the author's position;
 * {@code <@console>} then places each captured slot into the fixed document at its correct spot (chrome-level
 * {@code <@head>}/{@code <@scripts>} in the head/end-of-body pass-throughs; {@code <@brand>}/{@code <@actions>}/
 * {@code <@title>} in the header; {@code <@footer>} as the single footer; {@code <@body>} spliced onto the
 * {@code <body>} tag). A slot authored outside a {@code <@console>} (no {@link ConsoleContext} in scope) is
 * rejected fail-closed.
 *
 * <h5 class='section'>{@code phase=} (head/scripts only):</h5>
 * <p>
 * {@code <@head>} and {@code <@scripts>} accept an optional {@code phase=} that steers the capture to a distinct
 * cascade position: {@code <@head phase="before-page-css">} lands app CSS <b>before</b> the page-local
 * {@code pageCss} (so a page {@code css=} rule still wins), and {@code <@scripts phase="after-toolkit">} lands
 * scripts <b>between</b> the toolkit JS and {@code pageInit}. Any other slot rejects {@code phase=}; an
 * unrecognized value is rejected fail-closed.
 *
 * @since 10.0.0
 */
public final class ConsoleSlotDirectiveModel implements TemplateDirectiveModel {

	/** Slots that accept a {@code phase=} attribute. */
	private static final Set<String> PHASED_SLOTS = Set.of(ConsoleContext.HEAD, ConsoleContext.SCRIPTS);

	private static final String PHASE = "phase";

	private final String slot;

	/**
	 * @param slot One of the {@link ConsoleContext} slot-name constants this directive captures into &mdash; also the
	 * 	shared-variable name it is registered under.
	 */
	ConsoleSlotDirectiveModel(String slot) {
		this.slot = slot;
	}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@" + slot + "> has no format= attribute.");
		var allowed = PHASED_SLOTS.contains(slot) ? Set.of(PHASE) : Set.<String>of();
		FtlAttrLists.rejectUnknown(p, slot, allowed);

		var phase = FtlAttrLists.scalar(p, PHASE);
		if (! phase.isEmpty()) {
			var ok = (ConsoleContext.HEAD.equals(slot) && ConsoleContext.PHASE_BEFORE_PAGE_CSS.equals(phase))
				|| (ConsoleContext.SCRIPTS.equals(slot) && ConsoleContext.PHASE_AFTER_TOOLKIT.equals(phase));
			if (! ok)
				throw FtlAttrLists.reject("<@" + slot + "> unknown phase '" + phase + "'.");
		}

		var ctx = (ConsoleContext) env.getCustomState(ConsoleContext.KEY);
		if (ctx == null)
			throw FtlAttrLists.reject("<@" + slot + "> must be nested inside <@console>.");

		ctx.setSlot(slot, phase, CardDirectiveModel.capture(body));
	}
}
