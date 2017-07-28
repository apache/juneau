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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;

/**
 * Class for listening for certain parse events during a document parse.
 */
public class ParserListener {

	/**
	 * Gets called when an unknown bean property is detected in a document.
	 *
	 * <p>
	 * This method only gets called if {@link BeanContext#BEAN_ignoreUnknownBeanProperties} setting is <jk>true</jk>.
	 * Otherwise, the parser will throw a {@link ParseException}.
	 *
	 * @param <T> The class type of the bean.
	 * @param session The parser session.
	 * @param pipe
	 * 	The parser input.
	 * 	Note that if {@link BeanContext#BEAN_debug} is enabled on the parser, you can get the input as a string through
	 * 	{@link ParserPipe#getInputAsString()}.
	 * @param propertyName The property name encountered in the document.
	 * @param beanClass The bean class.
	 * @param bean The bean.
	 * @param line
	 * 	The line number where the unknown property was found (-1 if parser doesn't support line/column indicators).
	 * @param col
	 * 	The column number where the unknown property was found (-1 if parser doesn't support line/column indicators).
	 */
	public <T> void onUnknownBeanProperty(ParserSession session, ParserPipe pipe, String propertyName, Class<T> beanClass, T bean, int line, int col) {
		onError(session, pipe, null,
			format("Unknown property ''{0}'' encountered while trying to parse into class ''{1}'' at line {2} column {3}",
				propertyName, beanClass, line, col)
		);
	}

	/**
	 * Called when an error occurs during parsing but is ignored.
	 *
	 * @param session The parser session.
	 * @param pipe
	 * 	The parser input.
	 * 	Note that if {@link BeanContext#BEAN_debug} is enabled on the parser, you can get the input as a string through
	 * 	{@link ParserPipe#getInputAsString()}.
	 * @param t The throwable that was thrown by the getter method.
	 * @param msg The error message.
	 */
	public void onError(ParserSession session, ParserPipe pipe, Throwable t, String msg) {
		// Do something with this information.
	}
}
