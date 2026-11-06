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

import java.util.*;

/**
 * Thrown by {@link TemplateResolver#validate(String, TemplateCatalog)} when a template has problems.
 */
public class TemplateValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final transient List<TemplateProblem> problems;

	/**
	 * Constructor.
	 *
	 * @param problems The problems, in source order.  Must not be empty.
	 */
	public TemplateValidationException(List<TemplateProblem> problems) {
		super(message(problems));
		this.problems = List.copyOf(problems);
	}

	/**
	 * Returns the problems.
	 *
	 * @return An unmodifiable list, in source order.
	 */
	public List<TemplateProblem> problems() { return problems; }

	private static String message(List<TemplateProblem> problems) {
		var sb = new StringBuilder("Template has ").append(problems.size()).append(" problem(s)");
		for (var p : problems)
			sb.append("\n  at ").append(p.start()).append('-').append(p.end()).append(": ").append(p.message());
		return sb.toString();
	}
}
