/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import com.ibm.juno.core.*;

/**
 * Class for listening for certain parse events during a document parse.
 * <p>
 * 	Listeners can be registered with parsers through the {@link Parser#addListener(ParserListener)} method.
 * </p>
 * 	It should be noted that listeners are not automatically copied over to new parsers when a parser is cloned.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ParserListener {

	/**
	 * Gets called when an unknown bean property is detected in a document.
	 * <p>
	 * 	This method only gets called if {@link BeanContextProperties#BEAN_ignoreUnknownBeanProperties} setting is <jk>true</jk>.
	 * 	Otherwise, the parser will throw a {@link ParseException}.
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
