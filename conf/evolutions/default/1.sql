# --- !Ups

create table "user" (
    "id"                    serial primary key,
    "email"                 varchar not null,
    "login_provider_id"     varchar,
    "login_provider_key"    varchar,

    unique ("login_provider_id", "login_provider_key")
);

# --- !Downs

drop table "user";
