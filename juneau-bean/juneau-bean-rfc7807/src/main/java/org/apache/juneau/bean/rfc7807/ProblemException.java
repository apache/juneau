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
package org.apache.juneau.bean.rfc7807;

/**
 * A {@link RuntimeException} that carries an
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a> {@link Problem} payload.
 *
 * <p>
 * Lets handlers throw a custom problem without manually building a {@code BasicHttpException}:
 * <p class='bjava'>
 * 	<jk>throw new</jk> ProblemException(
 * 		Problem.<jsm>fromStatus</jsm>(403, <js>"Insufficient credit"</js>, <js>"Balance 30 &lt; cost 50"</js>)
 * 			.set(<js>"balance"</js>, 30)
 * 	);
 * </p>
 *
 * <p>
 * The wrapped {@link Problem} is recovered via {@link #getProblem()}. Lives in the bean module
 * (no {@code juneau-rest-server} dep); the server-side processor recognises {@code ProblemException} and unwraps it
 * for serialization.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Problem}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanRfc7807">juneau-bean-rfc7807</a>
 * </ul>
 */
public class ProblemException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Problem problem;

	/**
	 * Constructor.
	 *
	 * @param problem The problem payload to carry. May be <jk>null</jk>, though callers should normally supply a populated
	 * 	{@link Problem}.
	 */
	public ProblemException(Problem problem) {
		super(messageOf(problem));
		this.problem = problem;
	}

	/**
	 * Convenience factory.
	 *
	 * @param problem The problem payload to carry. May be <jk>null</jk>.
	 * @return A new {@link ProblemException} wrapping {@code problem}.
	 */
	public static ProblemException of(Problem problem) {
		return new ProblemException(problem);
	}

	/**
	 * Returns the wrapped {@link Problem} payload.
	 *
	 * @return The problem payload, or <jk>null</jk> if none was supplied at construction.
	 */
	public Problem getProblem() {
		return problem;
	}

	private static String messageOf(Problem p) {
		if (p == null)
			return null;
		var detail = p.getDetail();
		if (detail != null)
			return detail;
		return p.getTitle();
	}
}
