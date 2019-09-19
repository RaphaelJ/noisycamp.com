# --- !Ups

-- Types

create domain duration as bigint; -- Maps Joda Duration type.

-- Users and account

create table "user" (
    id                      serial primary key,
    created_at              timestamp not null,
    first_name              varchar,
    last_name               varchar,
    email                   varchar not null,
    avatar_id               integer
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
    login_info_id           integer primary key references "user_login_info"(id),
    hasher                  varchar not null,
    password                varchar not null,
    salt                    varchar
);

-- Studios

create table "studio" (
    id                      serial primary key,
    created_at              timestamp not null,

    owner_id                integer not null references "user"(id),

    name                    varchar not null,
    description             varchar not null,

    -- Address
    address1                varchar not null,
    address2                varchar,
    zipcode                 varchar not null,
    city                    varchar not null,
    state_code              char(2),
    country_code            char(2),

    -- Coordinates
    long                    double precision not null
        check (long >= -90 and long <= 90),
    lat                     double precision not null
        check (lat >= -90 and lat <= 90),

    -- Opening schedule

    monday_is_open          boolean not null,
    monday_opens_at         time without time zone,
    monday_closes_at        time without time zone,

    tuesday_is_open         boolean not null,
    tuesday_opens_at        time without time zone,
    tuesday_closes_at       time without time zone,

    wednesday_is_open       boolean not null,
    wednesday_opens_at      time without time zone,
    wednesday_closes_at     time without time zone,

    thursday_is_open        boolean not null,
    thursday_opens_at       time without time zone,
    thursday_closes_at      time without time zone,

    friday_is_open          boolean not null,
    friday_opens_at         time without time zone,
    friday_closes_at        time without time zone,

    saturday_is_open        boolean not null,
    saturday_opens_at       time without time zone,
    saturday_closes_at      time without time zone,

    sunday_is_open          boolean not null,
    sunday_opens_at         time without time zone,
    sunday_closes_at        time without time zone,

    -- Pricing policy

    currency_code           char(3) not null,

    price_per_hour          numeric not null,

    has_evening_pricing     boolean not null,
    evening_begins_at       time without time zone,
    evening_price_per_hour  numeric not null,

    has_weekend_pricing     boolean not null,
    weekend_price_per_hour  numeric not null,

    -- Booking policy

    min_booking_duration    duration not null,
    automatic_approval      boolean not null,

    can_cancel              boolean not null,
    cancellation_notice     duration,

    -- Constraints

    check (monday_opens_at is not null = monday_is_open),
    check (monday_closes_at is not null = monday_is_open),
    check (tuesday_opens_at is not null = tuesday_is_open),
    check (tuesday_closes_at is not null = tuesday_is_open),
    check (wednesday_opens_at is not null = wednesday_is_open),
    check (wednesday_closes_at is not null = wednesday_is_open),
    check (thursday_opens_at is not null = thursday_is_open),
    check (thursday_closes_at is not null = thursday_is_open),
    check (friday_opens_at is not null = friday_is_open),
    check (friday_closes_at is not null = friday_is_open),
    check (saturday_opens_at is not null = saturday_is_open),
    check (saturday_closes_at is not null = saturday_is_open),
    check (sunday_opens_at is not null = sunday_is_open),
    check (sunday_closes_at is not null = sunday_is_open),

    check (evening_begins_at is not null = has_evening_pricing),
    check (evening_price_per_hour is not null = has_evening_pricing),

    check (weekend_price_per_hour is not null = has_weekend_pricing),

    check (cancellation_notice is not null = can_cancel)
);

-- Pictures and uploads

create table "picture" (
    id                  bytea primary key,
    created_at          timestamp,
    format              varchar not null,
    content             bytea not null
);

# --- !Downs

drop table "picture";

drop table "studio";

drop table "user_password_info";
drop table "user_login_info";
drop table "user";

drop domain "duration";
