create table if not exists patient_address (
    patient_id uuid references  patient,
    address_id uuid references  address,
    primary key (patient_id, address_id)
);