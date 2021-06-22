create table if not exists address (
    id uuid primary key default uuid_generate_v4 (),
    country_id uuid,
    region_id uuid,
    city_id uuid,
    street_id uuid,
    house text,
    flat text,
    foreign key (country_id) references country (id),
    foreign key (region_id) references region (id),
    foreign key (city_id) references city (id),
    foreign key (street_id) references street (id)
);
--;;
create unique index address_cols on address (country_id, region_id, city_id, street_id, house, flat);