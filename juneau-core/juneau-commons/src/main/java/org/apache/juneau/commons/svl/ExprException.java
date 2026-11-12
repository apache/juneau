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
package org.apache.juneau.commons.svl;

/**
 * Thrown when an SVL <c>#{...}</c> expression cannot be lexed or parsed.
 *
 * <p>
 * The message reads <c>message at position N:</c>, followed by the source line containing the position and a caret line
 * (tabs copied, other characters as spaces) under the position.
 */
@SuppressWarnings({
	"java:S110" // Exception hierarchy mirrors the resolver exception layering
})
public class ExprException extends VarResolverException {
	private static final long serialVersionUID = 1L;

	private final String baseMessage;
	private final String source;
	private final int pos;

	/**
	 * Constructor.
	 *
	 * @param message The plain (not printf-style) description.
	 * @param source The expression source text.
	 * @param pos The zero-based character offset of the problem.
	 */
	public ExprException(String message, String source, int pos) {
		this(message, source, pos, null);
	}

	/**
	 * Constructor.
	 *
	 * @param message The plain (not printf-style) description.
	 * @param source The expression source text.
	 * @param pos The zero-based character offset of the problem.
	 * @param cause The underlying failure.  Can be <jk>null</jk>.
	 */
	public ExprException(String message, String source, int pos, Throwable cause) {
		super(cause, "%s", render(message, source, pos));
		this.baseMessage = message;
		this.source = source;
		this.pos = pos;
	}

	private static String render(String message, String source, int pos) {
		int p = Math.max(0, Math.min(pos, source.length()));
		int ls = source.lastIndexOf('\n', p - 1) + 1;
		int le = source.indexOf('\n', p);
		if (le < 0) le = source.length();
		var line = source.substring(ls, le);
		var pad = new StringBuilder();
		for (int i = ls; i < p; i++)
			pad.append(source.charAt(i) == '\t' ? '\t' : ' ');
		return message + " at position " + pos + ":\n" + line + "\n" + pad + "^";
	}

	/** @return The description without position or source. */
	public String getBaseMessage() { return baseMessage; }

	/** @return The expression source text. */
	public String getSource() { return source; }

	/** @return The zero-based character offset of the problem. */
	public int getPos() { return pos; }
}
