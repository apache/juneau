// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.parser;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.common.internal.*;

/**
 * Identifies a position in a reader or input stream.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class Position {

	static final Position UNKNOWN = new Position(-1);

	int line, column, position;

	/**
	 * Constructor.
	 *
	 * @param line The current line number.
	 * @param column The current column number.
	 */
	public Position(int line, int column) {
		this.line = line;
		this.column = column;
		this.position = -1;
	}

	/**
	 * Constructor.
	 *
	 * @param position The current byte position.
	 */
	public Position(int position) {
		this.line = -1;
		this.column = -1;
		this.position = position;
	}

	@Override /* Object */
	public String toString() {
		List<String> l = list();
		if (line != -1)
			l.add("line " + line);
		if (column != -1)
			l.add("column " + column);
		if (position != -1)
			l.add("position " + position);
		if (l.isEmpty())
			l.add("unknown");
		return StringUtils.join(l, ", ");
	}

	/**
	 * Returns the current line.
	 *
	 * @return The current line, or <c>-1</c> if not specified.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the current column.
	 *
	 * @return The current column, or <c>-1</c> if not specified.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Returns the current byte position.
	 *
	 * @return The current byte position, or <c>-1</c> if not specified.
	 */
	public int getPosition() {
		return position;
	}
}
