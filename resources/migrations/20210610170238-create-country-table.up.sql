create table if not exists country (
    id uuid primary key default uuid_generate_v4 (),
    name text unique
);