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

import org.apache.juneau.*;

/**
 * Class for listening for certain parse events during a document parse.
 * <p>
 * Listeners can be registered with parsers through the {@link Parser#addListener(ParserListener)} method.
 * <p>
 * It should be noted that listeners are not automatically copied over to new parsers when a parser is cloned.
 */
public class ParserListener {

	/**
	 * Gets called when an unknown bean property is detected in a document.
	 * <p>
	 * This method only gets called if {@link BeanContext#BEAN_ignoreUnknownBeanProperties} setting is <jk>true</jk>.
	 * Otherwise, the parser will throw a {@link ParseException}.
	 *
	 * @param <T> The class type of the bean.
	 * @param propertyName The property name encountered in the document.
	 * @param beanClass The bean class.
	 * @param bean The bean.
	 * @param line The line number where the unknown property was found (-1 if parser doesn't support line/column indicators).
	 * @param col The column number where the unknown property was found (-1 if parser doesn't support line/column indicators).
	 */
	public <T> void onUnknownProperty(String propertyName, Class<T> beanClass, T bean, int line, int col) {
		// Do something with information
	}
}
