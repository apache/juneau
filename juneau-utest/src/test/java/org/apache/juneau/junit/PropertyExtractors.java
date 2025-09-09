package org.apache.juneau.junit;

import static java.lang.Integer.*;
import static org.apache.juneau.junit.Utils.*;

import java.lang.reflect.*;
import java.util.*;

public class PropertyExtractors {

	public static class ObjectPropertyExtractor implements PropertyExtractor {

		@Override
		public boolean canExtract(BeanConverter converter, Object o, String name) {
			return true;
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String name) {
			return
				safe(() -> {
					if (o == null)
						return null;
					var f = (Field)null;
					var c = o.getClass();
					var n = Character.toUpperCase(name.charAt(0)) + name.substring(1);
					var m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("is"+n) && x.getParameterCount() == 0).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o);
					}
					m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get"+n) && x.getParameterCount() == 0).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o);
					}
					m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get") && x.getParameterCount() == 1 && x.getParameterTypes()[0] == String.class).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o, name);
					}
					var c2 = c;
					while (f == null && c2 != null) {
						f = Arrays.stream(c2.getDeclaredFields()).filter(x -> x.getName().equals(name)).findFirst().orElse(null);
						c2 = c2.getSuperclass();
					}
					if (f != null) {
						f.setAccessible(true);
						return f.get(o);
					}
					m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals(name) && x.getParameterCount() == 0).findFirst().orElse(null);
					if (m != null) {
						m.setAccessible(true);
						return m.invoke(o);
					}
					throw new RuntimeException(f("Property {0} not found on object of type {1}", name, o.getClass().getSimpleName()));
				});
		}
	}

	public static class ListPropertyExtractor extends ObjectPropertyExtractor {

		@Override
		public boolean canExtract(BeanConverter converter, Object o, String name) {
			return converter.canListify(o);
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String name) {
			var l = converter.listify(o);
			if (name.matches("-?\\d+"))
				return l.get(parseInt(name));
			if ("length".equals(name)) return l.size();
			if ("size".equals(name)) return l.size();
			return super.extract(converter, o, name);
		}
	}

	public static class MapPropertyExtractor extends ObjectPropertyExtractor {

		@Override
		public boolean canExtract(BeanConverter converter, Object o, String name) {
			return o instanceof Map;
		}

		@Override
		public Object extract(BeanConverter converter, Object o, String name) {
			var m = (Map<?,?>)o;
			if (m.containsKey(name)) return m.get(name);
			if ("size".equals(name)) return m.size();
			return super.extract(converter, o, name);
		}
	}
}