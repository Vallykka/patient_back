create table if not exists city (
    id uuid primary key default uuid_generate_v4 (),
    name text unique
);