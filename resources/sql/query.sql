-- :name patients :? :*
-- :doc Get list of patients
/* :require [clojure.string :as s] */
select p.id, p.surname, p.name, p.patronymic, p.sex, p.birth_date, p.oms_policy,
       count(*) OVER() AS full_count,
       pa.address_id as address_id, co.name as country, r.name as region, ci.name as city, s.name as street, a.house as house, a.flat as flat
from patient p
    left join patient_address pa on pa.patient_id = p.id
    left join address a on a.id = pa.address_id
    left join country co on co.id = a.country_id
    left join region r on r.id = a.region_id
    left join city ci on ci.id = a.city_id
    left join street s on s.id = a.street_id
/*~
(def fields {:surname "p.surname" :name "p.name" :patronymic "p.patronymic" :sex "p.sex", :birth-date "p.birth_date", :oms-policy "p.oms_policy"})
(when-let [params (filter (fn [[k v]] (not-any? #(= k %) (list :limit :offset))) params)]
  (when (not-empty params)
    (str "where "
      (s/join " and "
           (map (fn [[field val]]
              (if (string? val)
                (str "lower(" (get fields field) ") like lower('" val "%')")
                (str (get fields field) " = " val))) params))))) ~*/
order by p.surname
limit :limit offset :offset


-- :name patients-by-name :? :*
-- :doc Get list of patients by name
select id, name from patient where lower(name) like lower(:name)

-- :name patient-by-id :? :1
-- :doc Get selected patient
select p.id, p.surname, p.name, p.patronymic, p.sex, p.birth_date, p.oms_policy,
       pa.address_id as address_id, co.name as country, r.name as region, ci.name as city, s.name as street, a.house as house, a.flat as flat
from patient p
    left join patient_address pa on pa.patient_id = p.id
    left join address a on a.id = pa.address_id
    left join country co on co.id = a.country_id
    left join region r on r.id = a.region_id
    left join city ci on ci.id = a.city_id
    left join street s on s.id = a.street_id
where p.id = :id

-- :name insert-patient! :<!
-- :doc Inserts patient
insert into patient (surname, name, patronymic, sex, birth_date, oms_policy) values (:surname, :name, :patronymic, :sex, :birth-date, :oms-policy) returning id

-- :name update-patient! :<!
-- :doc Updates patient
update patient set surname = :surname, name = :name, patronymic = :patronymic, sex = :sex, birth_date = :birth-date, oms_policy = :oms-policy where id = :id returning id

-- :name delete-patient! :<! :1
-- :doc Deletes patient
delete from patient where id = :id

-- :name insert-country! :<!
-- :doc Inserts country
with input_country (name) as (values (:name)),
     insert_country as (insert into country (name) select * from input_country on conflict (name) do nothing returning id)
select 'i' as source, id
from insert_country union all
select 's' as source, c.id
    from input_country
    join country c using (name)


-- :name insert-region! :<!
-- :doc Inserts region
with input_region (name) as (values (:name)),
     insert_region as (insert into region (name) select * from input_region on conflict (name) do nothing returning id)
select 'i' as source, id
from insert_region union all
select 's' as source, c.id
     from input_region
     join region c using (name)

-- :name insert-city! :<!
-- :doc Inserts city
with input_city (name) as (values (:name)),
     insert_city as (insert into city (name) select * from input_city on conflict (name) do nothing returning id)
select 'i' as source, id
from insert_city union all
select 's' as source, c.id
    from input_city
    join city c using (name)

-- :name insert-street! :<!
-- :doc Inserts street
with input_street (name) as (values (:name)),
     insert_street as (insert into street (name) select * from input_street on conflict (name) do nothing returning id)
select 'i' as source, id
from insert_street union all
select 's' as source, c.id
     from input_street
     join street c using (name)

-- :name insert-address! :<!
-- :doc Inserts address
with input_addr (country_id, region_id, city_id, street_id, house, flat) as (values (:country-id, :region-id, :city-id, :street-id, :house, :flat)),
     insert_addr as (insert into address (country_id, region_id, city_id, street_id, house, flat) select * from input_addr
         on conflict (country_id, region_id, city_id, street_id, house, flat) do nothing returning id)
select 'i' as source, id
from insert_addr union all
select 's' as source, c.id
     from input_addr
     join address c using (country_id, region_id, city_id, street_id, house, flat)

-- :name insert-patient-address! :! :n
-- :doc Inserts patient's address
insert into patient_address (patient_id, address_id) values (:patient-id, :address-id)

-- :name delete-patient-address-by-patient! :<! :1
-- :doc Deletes patient's addresses
delete from patient_address where patient_id = :patient-id
