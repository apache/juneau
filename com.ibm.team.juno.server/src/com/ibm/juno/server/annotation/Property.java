/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.jena.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.xml.*;

/**
 * Property name/value pair used in the {@link RestResource#properties()} annotation.
 * <p>
 * 	Any of the following property names can be specified:
 * <ul>
 * 	<li>{@link BeanContextProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link ParserProperties}
 * 	<li>{@link JsonSerializerProperties}
 * 	<li>{@link RdfSerializerProperties}
 * 	<li>{@link RdfParserProperties}
 * 	<li>{@link RdfProperties}
 * 	<li>{@link XmlSerializerProperties}
 * 	<li>{@link XmlParserProperties}
 * </ul>
 * <p>
 * 	Property values types that are not <code>Strings</code> will automatically be converted to the
 * 		correct type (e.g. <code>Boolean</code>, etc...).
 * <p>
 * 	See {@link RestResource#properties} for more information.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Property {

	/**
	 * Property name.
	 */
	String name();

	/**
	 * Property value.
	 */
	String value();
}
