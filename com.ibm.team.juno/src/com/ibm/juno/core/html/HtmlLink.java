/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Used in conjunction with the {@link HtmlSerializer} class to define hyperlinks.
 * <p>
 * 	This annotation is applied to classes.
 * <p>
 * 	Annotation that can be used to specify that a class has a URL associated with it.
 * <p>
 * 	When rendered using the {@link com.ibm.juno.core.html.HtmlSerializer HtmlSerializer} class, this class will get rendered as a hyperlink like so...
 * <p class='code'>
 * 	<xt>&lt;a</xt> <xa>href</xa>=<xs>'hrefProperty'</xs><xt>&gt;</xt>nameProperty<xt>&lt;/a&gt;</xt>
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface HtmlLink {

	/**
	 * The bean property whose value becomes the name in the hyperlink.
	 */
	String nameProperty() default "";

	/**
	 * The bean property whose value becomes the url in the hyperlink.
	 */
	String hrefProperty() default "";
}
