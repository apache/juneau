# TODO

- Find all places where we have system properties defined for juneau and convert them to use the new Settings class.
- Document "juneau.enableVerboseExceptions" setting.
- Change field names that start with underscore so that they end with underscore (e.g. "_enum"->"enum_")
- Find places where we defined fields and methods as _foobar and convert them to foobar_
- Investigate navlinks URL generation issue: Either "request:?Accept=text/json&plainText=true" should be supported, or "request:/?Accept=text/json&plainText=true" should not append '/' to the request URL. Currently, "request:/?Accept=..." generates URLs like "http://localhost:5000/rest/db/request:?Accept=..." which is incorrect.
- Determine if it's possible to add a "short" field to @Schema for AI purposes.
- Ensure Juneau support record types for serializing/parsing.
- JsonSchemaParser should be able to produce JsonSchema beans.
- JsonSchemaGenerator should return JsonSchema beans.
- Create full-fledged CSV serializer/parser support.
- Add YAML serializer/parser support.
- Update REST server API to use new BeanStore2.
- ClassInfo should have a findGetter(String propertyName) convenience method.
- Make sure @Beanp("*") works on plain fields.
- Add schema validation to beans during parsing.
- Duration objects should be supported for serialization by default.

