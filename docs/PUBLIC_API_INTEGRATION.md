# SAGE Public APIs integration

## Three layers
1. **Directory**: SAGE searches the Public APIs catalogue through `https://api.publicapis.org`.
2. **Supabase**: catalogue metadata is mirrored into `public_apis`; only explicitly enabled API tools are registered in `api_tool_registry`.
3. **AI/action layer**: Needle can discover catalogue entries and open documentation. Actual API execution must use an enabled registry entry and a user-owned credential reference; arbitrary URLs from model output are not executable.

The uploaded `public-apis-master.zip` is retained as validation/contribution tooling. It does not contain the catalogue README/data, so it is not treated as the data source.

## Supabase
Run `supabase/migrations/20260928_public_api_catalog.sql`.

Do not put service-role keys or third-party API secrets in Supabase tables or the APK. Keep user credentials in SAGE's Android encrypted storage and store only a non-secret `credential_ref` in Supabase.

## Public catalogue source
The upstream project documents entries with API name, description, auth, HTTPS, CORS, link, and category. The companion directory service exposes `/entries`, `/categories`, `/random`, and `/health` without authentication over HTTPS.

## Needle
The integrated native bridge loads `needle3.cact`, calls the real C API (`needle_load`, `needle_init`, `needle_complete`, `needle_reset`) and serializes Needle's JSON response directly back to Kotlin. The native runtime is process-global and non-thread-safe, so the JNI wrapper serializes calls with a mutex.
