-- SAGE Public API catalogue + controlled execution registry.
-- Run this in Supabase SQL Editor. Never store provider/API secrets here.

create extension if not exists pgcrypto;

create table if not exists public.public_apis (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  description text not null default '',
  auth text,
  https boolean not null default false,
  cors text,
  link text not null,
  category text,
  source text not null default 'public-apis',
  synced_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists public_apis_category_idx on public.public_apis(category);
create index if not exists public_apis_name_idx on public.public_apis using gin(to_tsvector('simple', name || ' ' || description));

create table if not exists public.api_tool_registry (
  id uuid primary key default gen_random_uuid(),
  api_id uuid not null references public.public_apis(id) on delete cascade,
  tool_name text not null,
  method text not null check (method in ('GET','POST')),
  base_url text not null,
  path_template text not null default '/',
  enabled boolean not null default false,
  requires_user_confirmation boolean not null default true,
  created_at timestamptz not null default now(),
  unique(api_id, tool_name)
);

create index if not exists api_tool_registry_api_idx on public.api_tool_registry(api_id);

create table if not exists public.user_api_connections (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  api_id uuid not null references public.public_apis(id) on delete cascade,
  credential_ref text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, api_id)
);

create table if not exists public.api_execution_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  api_id uuid references public.public_apis(id) on delete set null,
  tool_name text not null,
  method text not null,
  status_code integer,
  success boolean not null,
  created_at timestamptz not null default now()
);

alter table public.public_apis enable row level security;
alter table public.api_tool_registry enable row level security;
alter table public.user_api_connections enable row level security;
alter table public.api_execution_logs enable row level security;

-- Catalogue is public read-only data.
drop policy if exists public_apis_read on public.public_apis;
create policy public_apis_read on public.public_apis for select using (true);

-- Only enabled registry entries are readable by authenticated clients.
drop policy if exists api_registry_read on public.api_tool_registry;
create policy api_registry_read on public.api_tool_registry for select to authenticated using (enabled = true);

-- Credentials/logs are user-owned.
drop policy if exists api_connections_owner on public.user_api_connections;
create policy api_connections_owner on public.user_api_connections for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists api_logs_owner on public.api_execution_logs;
create policy api_logs_owner on public.api_execution_logs for select to authenticated using (auth.uid() = user_id);
drop policy if exists api_logs_insert on public.api_execution_logs;
create policy api_logs_insert on public.api_execution_logs for insert to authenticated with check (auth.uid() = user_id);
