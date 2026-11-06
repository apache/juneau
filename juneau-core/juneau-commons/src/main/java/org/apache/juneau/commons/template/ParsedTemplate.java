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
package org.apache.juneau.commons.template;

import org.apache.juneau.commons.svl.*;

/**
 * A template parsed by {@link TemplateResolver#parse(String)}.
 *
 * <p>
 * Immutable and thread-safe, so it can be cached and rendered concurrently.  It is bound to the resolver that parsed it.
 */
public final class ParsedTemplate {

	private final VarTemplate template;

	private final TemplateResolver owner;

	ParsedTemplate(VarTemplate template, TemplateResolver owner) {
		this.template = template;
		this.owner = owner;
	}

	TemplateResolver owner() { return owner; }

	VarTemplate template() { return template; }

	/**
	 * Returns the template text this was parsed from.
	 *
	 * @return The source text.
	 */
	public String getSource() { return template.getSource(); }

	@Override /* Overridden from Object */
	public String toString() { return "ParsedTemplate[" + template.getSource() + "]"; }
}
