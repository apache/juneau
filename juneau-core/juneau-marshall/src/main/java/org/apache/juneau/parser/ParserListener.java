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

import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.*;

/**
 * Class for listening for certain parse events during a document parse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class ParserListener {

	/**
	 * Represents no parser listener.
	 */
	public static final class Void extends ParserListener {}

	/**
	 * Gets called when an unknown bean property is detected in a document.
	 *
	 * <p>
	 * This method only gets called if {@link org.apache.juneau.BeanContext.Builder#ignoreUnknownBeanProperties()} setting is <jk>true</jk>.
	 * Otherwise, the parser will throw a {@link ParseException}.
	 *
	 * @param <T> The class type of the bean.
	 * @param session The parser session.
	 * @param propertyName The property name encountered in the document.
	 * @param beanClass The bean class.
	 * @param bean The bean.
	 */
	public <T> void onUnknownBeanProperty(ParserSession session, String propertyName, Class<T> beanClass, T bean) {
		onError(session, null,
			format("Unknown property ''{0}'' encountered while trying to parse into class ''{1}'' at location {2}",
				propertyName, beanClass, session.getPosition())
		);
	}

	/**
	 * Called when an exception is thrown when trying to call a bean setter method.
	 *
	 * @param session The serializer session.
	 * @param t The throwable that was thrown by the setter method.
	 * @param p The bean property we had an issue on.
	 */
	public void onBeanSetterException(ParserSession session, Throwable t, BeanPropertyMeta p) {
		onError(session, t, format("Could not call setValue() on property ''{0}'' of class ''{1}'', exception = {2}",
			p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage()));
	}

	/**
	 * Called when an error occurs during parsing but is ignored.
	 *
	 * @param session The parser session.
	 * @param t The throwable that was thrown by the getter method.
	 * @param msg The error message.
	 */
	public void onError(ParserSession session, Throwable t, String msg) {
		// Do something with this information.
	}
}
