create table if not exists patient (
    id uuid primary key default uuid_generate_v4 (),
    surname text,
    name text,
    patronymic text,
    sex sex_type,
    birth_date date,
    oms_policy text
);
--;;
create unique index patient_surname on patient (surname);
--;;
create unique index patient_surname_name on patient (surname, name);
--;;
create unique index patient_name on patient (name);
--;;
create unique index patient_oms on patient (oms_policy);