/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.jaxrs;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Annotations applicable to subclasses of {@link BaseProvider}.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Used to associate serializers, parsers, filters, and properties with instances of {@link BaseProvider}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface JunoProvider {

	/**
	 * Provider-level POJO filters.
	 * <p>
	 * 	These filters are applied to all serializers and parsers being used by the provider.
	 * <p>
	 * 	If the specified class is an instance of {@link Filter}, then that filter is added.
	 * 	Any other classes are wrapped in a {@link BeanFilter} to indicate that subclasses should
	 * 		be treated as the specified class type.
	 */
	Class<?>[] filters() default {};

	/**
	 * Provider-level properties.
	 * <p>
	 * 	Any of the following property names can be specified:
	 * <ul>
	 * 	<li>{@link RestServletProperties}
	 * 	<li>{@link BeanContextProperties}
	 * 	<li>{@link SerializerProperties}
	 * 	<li>{@link ParserProperties}
	 * 	<li>{@link JsonSerializerProperties}
	 * 	<li>{@link XmlSerializerProperties}
	 * 	<li>{@link XmlParserProperties}
	 * </ul>
	 * <p>
	 * 	Property values will be converted to the appropriate type.
	 * <p>
	 * 	These properties can be augmented/overridden through the {@link RestMethod#properties()} annotation on the REST method.
	 */
	Property[] properties() default {};

	/**
	 * Specifies a list of {@link Serializer} classes to add to the list of serializers available for this provider.
	 * <p>
	 * 	This annotation can only be used on {@link Serializer} classes that have no-arg constructors.
	 */
	Class<? extends Serializer<?>>[] serializers() default {};

	/**
	 * Specifies a list of {@link Parser} classes to add to the list of parsers available for this provider.
	 * <p>
	 * 	This annotation can only be used on {@link Parser} classes that have no-arg constructors.
	 */
	Class<? extends Parser<?>>[] parsers() default {};
}
