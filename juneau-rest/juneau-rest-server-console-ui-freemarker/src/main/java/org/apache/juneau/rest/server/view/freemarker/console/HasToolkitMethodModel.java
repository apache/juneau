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

import java.util.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code jcHasToolkit(name)} FreeMarker method: whether the current {@code <@page toolkit=…>} lists
 * {@code name}. Lets a chrome check which toolkits are active so it can emit toolkit-specific markup.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;#if jcHasToolkit("views")&gt;
 * 	  &lt;link rel="stylesheet" href="/app/views-extras.css"&gt;
 * 	&lt;/#if&gt;
 * </p>
 *
 * <p>
 * Only {@code toolkit=} counts.  Packs that cards require (for example {@code "datatables-glue"} for a
 * {@code type="datatables"} card) are loaded but don't make {@code jcHasToolkit} return <jk>true</jk>.
 *
 * @since 10.0.0
 */
public final class HasToolkitMethodModel implements TemplateMethodModelEx {

	/** The shared-variable name this method registers under. */
	public static final String NAME = "jcHasToolkit";

	HasToolkitMethodModel() {}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
		if (args.size() != 1)
			throw FtlAttrLists.reject(f("%s(name) takes exactly one argument; got '%s'.", NAME, args.size()));
		var a = args.get(0);
		if (! (a instanceof TemplateScalarModel s))
			throw FtlAttrLists.reject(f("%s(name) takes a string; got '%s'.", NAME, a));
		var cap = PageCapture.get(Environment.getCurrentEnvironment());
		return nn(cap) && cap.toolkits().contains(s.getAsString().trim()) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
	}
}
