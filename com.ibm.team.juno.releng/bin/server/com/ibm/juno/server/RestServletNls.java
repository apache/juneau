/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import java.util.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.annotation.*;

/**
 * Utility class for extracting messages from resource bundles identified through the {@link RestResource#messages()} annotation.
 * <p>
 * When mutiple servlets are using the same resource bundle, the key names can be prefixed with the servlet class name
 * 	to prevent name conflicts.
 * 
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RestServletNls {

	private List<ClassBundleDef> defs = new LinkedList<ClassBundleDef>();
	private Map<Locale,SafeResourceBundle> bundles = new HashMap<Locale,SafeResourceBundle>();

	/**
	 * Create an NLS bundle for the specified servlet.
	 *
	 * @param servletClass The servlet class.
	 * @param bundlePath The bundle path.
	 */
	protected RestServletNls(Class<?> servletClass, String bundlePath) {
		addDef(servletClass, bundlePath);
	}

	/**
	 * Add another bundle path to this NLS bundle.
	 * Order of property lookup is first-to-last.
	 *
	 * @param servletClass The servlet class.
	 * @param bundlePath The bundle path.
	 * @return This object (for method chaining).
	 */
	protected RestServletNls addDef(Class<?> servletClass, String bundlePath) {
		defs.add(new ClassBundleDef(servletClass, bundlePath.replace('/', '.')));
		return this;
	}

	/**
	 * A single entry of {@link RestResource#messages()}.
	 */
	static class ClassBundleDef {
	private Class<?> servletClass;
	private String bundlePath;
		private ClassBundleDef(Class<?> servletClass, String bundlePath) {
		this.servletClass = servletClass;
			this.bundlePath = bundlePath;
		}
	}

	String getMessage(Locale locale, String key, Object...args) {
		SafeResourceBundle rb = getBundle(locale);
		return rb.getString(key, args);
	}

	String findMessage(Locale locale, String...keys) {
		SafeResourceBundle rb = getBundle(locale);
		return rb.findFirstString(keys);
	}

	/**
	 * Returns the resource bundle for the specified locale.
	 *
	 * @param locale The client locale.
	 * @return The resource bundle for the specified locale.  Never <jk>null</jk>.
	 */
	protected SafeResourceBundle getBundle(Locale locale) {
		if (! bundles.containsKey(locale)) {
			List<SafeResourceBundle> l = new ArrayList<SafeResourceBundle>(defs.size());
			for (ClassBundleDef cbd : defs) {
			ResourceBundle rb = null;
				ClassLoader cl = cbd.servletClass.getClassLoader();
			try {
					rb = ResourceBundle.getBundle(cbd.bundlePath, locale, cl);
			} catch (MissingResourceException e) {
				try {
						rb = ResourceBundle.getBundle(cbd.servletClass.getPackage().getName() + '.' + cbd.bundlePath, locale, cl);
				} catch (MissingResourceException e2) {
				}
			}
				l.add(new SafeResourceBundle(rb, cbd.servletClass));
			}
			SafeResourceBundle b = (l.size() == 1 ? l.get(0) : new SafeResourceMultiBundle(l));
			bundles.put(locale, b);

		}
		return bundles.get(locale);
	}
}
