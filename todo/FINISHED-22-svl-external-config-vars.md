# FINISHED-22: SVL external config vars

Implemented additional SVL vars for `.env`-style external configuration sources in `juneau-commons`.

## Shipped

- Added `DotenvVar` (`$DE{key[,default]}`) backed by `DotenvPropertySource`.
- Added `EnvFileVar` (`$EF{key[,default]}`) as a generic env-file alias, also backed by `DotenvPropertySource`.
- Added both vars to `VarList.addDefault()`, so they are included in `VarResolver.DEFAULT` / `defaultVars()`.
- Updated `VarResolver` and `VarList` Javadocs to include the new vars.
- Added unit tests in `PropertyVars_Test` for:
  - Direct var usage (`DotenvVar.create(path)` and `EnvFileVar.create(path)`).
  - Default fallback behavior.
  - Inclusion in `defaultVars()`.

## Notes

- `DotenvPropertySource` path resolution behavior is reused as-is:
  - `juneau.dotenv.path` system property
  - `JUNEAU_DOTENV_PATH` environment variable
  - fallback `.env`
- File-glob and JSON-pointer vars remain future enhancements if needed.
