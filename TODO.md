# TODO

- Document "juneau.enableVerboseExceptions" setting.
- Change field names that start with underscore so that they end with underscore (e.g. "_enum"->"enum_")
- Find places where we defined fields and methods as _foobar and convert them to foobar_
- Investigate navlinks URL generation issue: Either "request:?Accept=text/json&plainText=true" should be supported, or "request:/?Accept=text/json&plainText=true" should not append '/' to the request URL. Currently, "request:/?Accept=..." generates URLs like "http://localhost:5000/rest/db/request:?Accept=..." which is incorrect.
- Update REST server API to use new BeanStore2.
- ClassInfo should have a findGetter(String propertyName) convenience method.
- Make sure @Beanp("*") works on plain fields.



