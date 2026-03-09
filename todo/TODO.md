# TODO


- Document "juneau.enableVerboseExceptions" setting.
- Change field names that start with underscore so that they end with underscore (e.g. "_enum"->"enum_")
- Find places where we defined fields and methods as _foobar and convert them to foobar_
- Investigate navlinks URL generation issue: Either "request:?Accept=text/json&plainText=true" should be supported, or "request:/?Accept=text/json&plainText=true" should not append '/' to the request URL. Currently, "request:/?Accept=..." generates URLs like "http://localhost:5000/rest/db/request:?Accept=..." which is incorrect.
- Update REST server API to use new BeanStore2.
- ClassInfo should have a findGetter(String propertyName) convenience method.
- Make sure @Beanp("*") works on plain fields.
- Rename ResettableSupplier to Memoizer
- Need an easier way to specify this header:
Content-Disposition: attachment; filename="example.pdf"

Figure out why this needs a cast:
	private static final Json5 JSON5_LENIENT = new Json5(Json5Serializer.DEFAULT, (Json5Parser)Json5Parser.create().ignoreUnknownBeanProperties().build());

- A comprehensive plan for handling large data sets using Suppliers and Consumers?

- CSV format supports property names in headers (make sure it can work with Suppliers and Consumers above)
  - prop1,prop2,prop3
- Need a better way to define serializer config values (e.g. useWhitespace) arguments through REST (Additional headers?  Content-Type modifications?)

- RestResponse needs a setSerializer() command.

- Upgrade to Jena 6.0