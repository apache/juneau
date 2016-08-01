/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.labels;

import static java.lang.String.*;

import java.util.*;

import com.ibm.juno.server.converters.*;

/**
 * Reusable label constructs for REST OPTIONS requests.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class DefaultLabels {

	private static String
		bundleName = DefaultLabels.class.getPackage().getName() + ".nls.DefaultLabels",
		typeKey = "%s.%s.type",	// "{category}.{name}.type"
		descKey = "%s.%s.desc";	// "{catetory}.{name}.desc"

	private static Map<Locale,ParamDescription[]> queryableParams = new HashMap<Locale,ParamDescription[]>();
	private static Map<Locale,ParamDescription[]> headerParams = new HashMap<Locale,ParamDescription[]>();

	/**
	 * OPTIONS request labels for query/view/sort/paging features on POJOs
	 * 	when {@link Queryable} filter is associated with a resource method.
	 * @param locale The client locale.
	 * @return A list of localized parameter descriptions.
	 */
	public static ParamDescription[] getQueryableParamDescriptions(Locale locale) {
		if (! queryableParams.containsKey(locale)) {
			ResourceBundle rb = ResourceBundle.getBundle(bundleName, locale);
			String category = "QueryableParam";
			String[] p = {"q","v","s","g","i","p","l"};
			ParamDescription[] l = new ParamDescription[p.length];
			for (int i = 0; i < p.length; i++)
				l[i] = new ParamDescription(p[i],
					rb.getString(format(typeKey, category, p[i])),
					rb.getString(format(descKey, category, p[i]))
				);
			queryableParams.put(locale, l);
		}
		return queryableParams.get(locale);
	}

	/**
	 * OPTIONS request labels for header values that can be specified as GET parameters.
	 * @param locale The client locale.
	 * @return A list of localized parameter descriptions.
	 */
	public static ParamDescription[] getHeaderParamDescriptions(Locale locale) {
		if (! headerParams.containsKey(locale)) {
			ResourceBundle rb = ResourceBundle.getBundle(bundleName, locale);
			String category = "HeaderParam";
			String[] p = {"Accept","Accept-Encoding","Method","Content","plainText"};
			ParamDescription[] l = new ParamDescription[p.length];
			for (int i = 0; i < p.length; i++)
				l[i] = new ParamDescription(p[i],
					rb.getString(format(typeKey, category, p[i])),
					rb.getString(format(descKey, category, p[i]))
				);
			headerParams.put(locale, l);
		}
		return headerParams.get(locale);
	}
}
