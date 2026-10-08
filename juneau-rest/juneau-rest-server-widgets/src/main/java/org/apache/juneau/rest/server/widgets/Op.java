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
package org.apache.juneau.rest.server.widgets;

/**
 * The comparison operator of a declarative single-field rule.
 *
 * <p>
 * One vocabulary, one place.  Every rule in the toolkit that tests one row field against one value shares these
 * constants &mdash; {@link ActionRef#enabledWhen(String,Op,Object,String) the action-bar state rule} here, and the
 * {@code RowClassRule} row decorator in the views module, and the views module's {@code VisibilityRule}.  It lives in this module because the widgets&rarr;views
 * dependency does not exist, so the shared type has to sit on the side both can see.
 *
 * <p>
 * {@link #IN}/{@link #CONTAINS} are collection operators (the value is a list, or the tested field is) used by
 * visibility rules only.
 *
 * <p>
 * {@link #EQ}/{@link #NE} <b>require</b> a comparison value; {@link #PRESENT}/{@link #ABSENT} test only whether the
 * field is non-null/non-empty and therefore <b>omit</b> one.  {@link #requiresValue()} is the discriminator, and
 * every host is expected to reject the mismatched form rather than ignore the surplus value.
 *
 * <p>
 * Each constant also carries its lowercase wire token, for the hosts that do serialize a rule.
 *
 * @since 10.0.0
 */
public enum Op {

	/** Matches when the tested field equals the rule value. */
	EQ("eq"),

	/** Matches when the tested field does not equal the rule value. */
	NE("ne"),

	/** Matches when the tested field is non-null/non-empty (no value needed). */
	PRESENT("present"),

	/** Matches when the tested field is missing/null (no value needed). */
	ABSENT("absent"),

	/** Matches when the tested field equals one of the values in the rule's value list (the value is a list). */
	IN("in"),

	/** Matches when the tested field (itself a list) contains the rule's value. */
	CONTAINS("contains");

	private final String wire;

	Op(String wire) {
		this.wire = wire;
	}

	/**
	 * Returns the lowercase wire token for this operator.
	 *
	 * @return The wire token (e.g. <c>"eq"</c>).
	 */
	public String wire() {
		return wire;
	}

	/**
	 * Whether this operator requires a comparison value ({@link #EQ}/{@link #NE}).
	 *
	 * @return <jk>true</jk> for {@link #EQ} and {@link #NE}, <jk>false</jk> for {@link #PRESENT} and
	 * 	{@link #ABSENT}.
	 */
	public boolean requiresValue() {
		return this == EQ || this == NE || this == IN || this == CONTAINS;
	}

	/**
	 * Whether this operator is a collection operator ({@link #IN}/{@link #CONTAINS}).
	 *
	 * <p>
	 * Only the visibility rule evaluator understands these; the disable-with-reason rules accept the original four.
	 *
	 * @return <jk>true</jk> for {@link #IN} and {@link #CONTAINS}.
	 */
	public boolean isCollectionOp() {
		return this == IN || this == CONTAINS;
	}
}
