# --- !Ups

create table "user" (
    id                      serial primary key,
    email                   varchar not null
);

-- Multiple login providers could be associated with a single user.
create table "user_login_info" (
    id                      serial primary key,
    user_id                 integer not null references "user"(id),
    login_provider_id       varchar,
    login_provider_key      varchar,

    unique (user_id, login_provider_id)
);

-- Hashed password for password credentials associated with a login info.
create table "user_password_info" (
    login_info_id         integer primary key references "user_login_info"(id),
    hasher                varchar not null,
    password              varchar not null,
    salt                  varchar
);

# --- !Downs

drop table "user_password_info";
drop table "user_login_info";
drop table "user";
