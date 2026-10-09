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
import org.apache.juneau.rest.server.console.TopicDecl.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * {@code <@topic name="..." retain=true|false publisher="script|server|ribbon"/>}: declares a page-level custom topic
 * in the contract's {@code topics} list.  The FTL twin of {@code PageSpec.topic(TopicDecl)}; the two union.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@page&gt;
 * 	  &lt;@topic name="app.region-picked" retain=true publisher="script"/&gt;
 * 	  &lt;@card type="html" id="region" subscribes="app.region-picked"&gt;...&lt;/@card&gt;
 * 	&lt;/@page&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class TopicDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "topic";

	static final Set<String> ATTRS = Set.of("name", "retain", "publisher");

	TopicDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		var cap = PageCapture.get(env);
		if (cap == null || ! (cap.consoleOpen || cap.pageOpen))
			throw FtlAttrLists.reject("<@topic> must be inside <@console> or <@page>");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);
		var name = FtlAttrLists.scalar(p, "name");
		if (! p.containsKey("retain"))
			throw FtlAttrLists.reject(f("<@topic name='%s'> needs retain=true or retain=false", name));
		var retain = FtlAttrLists.strictBoolean(p, NAME, "retain", false);
		var publisher = publisher(name, FtlAttrLists.scalar(p, "publisher"));
		try {
			var d = TopicDecl.of(name).retain(retain);
			if (publisher != null)
				d.publisher(publisher);
			cap.addTopic(d.toMap(), "<@topic>");
		} catch (IllegalArgumentException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
	}

	private static Publisher publisher(String name, String v) throws TemplateModelException {
		if (v.isEmpty())
			return null;  // TopicDecl.toMap() reports the missing publisher with the same text as Java.
		return switch (v) {
			case "script" -> Publisher.SCRIPT;
			case "server" -> Publisher.SERVER;
			case "ribbon" -> Publisher.RIBBON;
			default -> throw FtlAttrLists.reject(f("<@topic name='%s'> publisher='%s' must be script, server or ribbon", name, v));
		};
	}
}
