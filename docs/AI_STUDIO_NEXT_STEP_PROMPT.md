# Google AI Studio / coding-agent implementation prompt

Integrate the supplied SAGE project with the Public APIs + Supabase + Needle work already present in this repository.

## Required architecture
- Needle 3 is the local intent router.
- Gemini/Grok remain the conversational/reasoning layer.
- Android/Kotlin tools are deterministic executors.
- Public APIs are a catalogue and an allowlisted execution layer, never arbitrary URLs from model output.
- Supabase stores public API metadata, enabled tool registry entries, user-owned connection references, and non-sensitive execution logs.
- API credentials remain only in Android encrypted storage; never store third-party secrets in Supabase or logs.

## Public API directory
Use `PublicApiDirectoryClient` for the public catalogue service. Add a SAGE screen or search surface for:
- search by name/description
- category filter
- HTTPS/auth/CORS metadata
- documentation link
- sync to Supabase

## Supabase
Run `supabase/migrations/20260928_public_api_catalog.sql`.
Configure the app with SUPABASE_URL and SUPABASE_ANON_KEY via local.properties/BuildConfig. Never use a service-role key in the APK.

## Needle
The native Needle 3 bridge is already wired to the real header and static library:
- `native/needle/needle.h`
- `native/needle/libneedle.a`
- `app/src/main/assets/needle3.cact`
- `native/needle/NeedleJNI.cpp`
- `native/needle/CMakeLists.txt`

Keep the mutex because the Needle C runtime is process-global and non-thread-safe.

## Controlled API execution
Implement `call_registered_public_api` only for rows in `api_tool_registry` where `enabled=true`.
- Accept only HTTPS base URLs.
- Reject localhost, loopback, link-local and private-network destinations.
- Permit only methods registered by the row.
- Never allow the model to supply an arbitrary host.
- Require confirmation for state-changing methods.
- Resolve user credentials from Android encrypted storage using `credential_ref`.
- Log only API id, tool name, status code, success and timestamp.
- Never log Authorization headers, API keys, request bodies containing secrets, or raw responses that may contain credentials.

## Needle tool routing
Keep the static Needle toolset small. At minimum expose:
- `search_public_apis`
- `open_public_api_docs`
- `call_registered_public_api`

If the app has many API tools, use Needle's documented tool-index retrieval rather than declaring hundreds of independent tools.

## Chat orchestration
For each user message:
1. Try Needle for local/action intent.
2. If it returns no function call, send the request to the selected Gemini/Grok conversational model.
3. If Needle returns an API-discovery/action call, execute only the corresponding deterministic tool.
4. For calls/SMS, keep SAGE's existing confirmation guardrail.
5. For registered public APIs, show confirmation when the registry requires it.
6. Never claim an action completed until the executor returns success.
