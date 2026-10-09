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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.view.freemarker.*;

import freemarker.core.*;
import freemarker.template.*;
import freemarker.template.utility.*;

/**
 * {@code <@bridge id transport session downstream upstream? maxAttempts?/>}: declares a server bridge in the
 * contract's {@code bridges} list.  The FTL twin of {@code PageSpec.bridge(BridgeDecl)}.  {@code session} may be a
 * {@code servlet:} / {@code context:} URI; it is resolved against the current request.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@topic name="ops.jobs" retain=true publisher="server"/&gt;
 * 	&lt;@bridge id="ops" transport="sse" session="servlet:/juneau-bus/session" downstream="ops.jobs"/&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class BridgeDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "bridge";

	static final Set<String> ATTRS = Set.of("id", "transport", "session", "downstream", "upstream", "maxAttempts");

	BridgeDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		var cap = PageCapture.get(env);
		if (cap == null || ! (cap.consoleOpen || cap.pageOpen))
			throw FtlAttrLists.reject("<@bridge> must be inside <@console> or <@page>");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);
		var id = FtlAttrLists.scalar(p, "id");
		var transport = FtlAttrLists.scalar(p, "transport");
		var session = FtlAttrLists.scalar(p, "session");
		try {
			var b = switch (transport) {
				case "sse" -> BridgeDecl.sse(id, session);
				case "websocket" -> BridgeDecl.websocket(id, session);
				default -> throw iaex("bridge '%s': transport '%s' must be sse or websocket", id, transport);
			};
			b.downstream(FtlAttrLists.list(p, NAME, "downstream").toArray(String[]::new));
			var up = FtlAttrLists.list(p, NAME, "upstream");
			if (! up.isEmpty())
				b.upstream(up.toArray(String[]::new));
			if (p.containsKey("maxAttempts"))
				b.maxAttempts(maxAttempts(id, DeepUnwrap.unwrap(p.get("maxAttempts"))));
			cap.addBridge(PageSpec.resolvedBridge(b, FreemarkerRenderScope.request()));
		} catch (IllegalArgumentException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
	}

	private static int maxAttempts(String id, Object v) {
		if (v instanceof Number n && n.doubleValue() == n.intValue())
			return n.intValue();
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw iaex("bridge '%s': maxAttempts must be an integer; got '%s'", id, v);
		}
	}
}
