# --- !Ups

-- Types

-- Maps java.time.Duration type.
create domain duration as bigint;

-- A amount of money.
create domain amount as numeric
    check (value >= 0);

-- A decimal coordinate value.
create domain coordinate as numeric
    check (value >= -180 and value <= 180);

-- Maps a java.time.java_localtime as an ISO 8601 string.
create domain java_localtime as varchar
    check (value similar to '\d{2}:\d{2}(:\d{2}(.\d+)?)?');

-- Maps a java.time.ZoneId as a string.
create domain java_zoneid as varchar;

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
    login_info_id           integer primary key
        references "user_login_info"(id),
    hasher                  varchar not null,
    password                varchar not null,
    salt                    varchar
);

-- Payout method

create table "payout_method" (
    id                      serial primary key,
    created_at              timestamp not null,

    owner_id                integer not null references "user"(id),

    country_code            char(2) not null,

    -- Full address as required for ABA and Australian accounts.
    address1                varchar,
    address2                varchar,
    zipcode                 varchar,
    city                    varchar,
    state_code              char(2),

    recipient_type          varchar not null
        check (recipient_type in ('business', 'private')),
    recipient_name          varchar not null,

    account_type            varchar not null
        check (account_type in (
            'iban', 'aba', 'canadian', 'australian', 'new-zealand')),

    -- IBAN
    iban_bic                varchar,
    iban_iban               varchar, -- IBAN account number

    -- ABA account
    aba_routing_number      varchar,
    aba_account_number      varchar,
    aba_account_type        varchar
        check (aba_account_type in ('checking', 'savings')),

    -- Canadian account
    canadian_institution_number varchar,
    canadian_transit_number     varchar,
    canadian_account_number     varchar,
    canadian_account_type       varchar
        check (canadian_account_type in ('checking', 'savings')),

    -- Australian account
    australian_bsb_code         varchar,
    australian_business_number  varchar,
    australian_account_number   varchar,

    -- New Zealand account
    new_zealand_account_number  varchar,

    -- Constraints

    check (account_type != 'iban' or (
        iban_iban is not null)),

    check (account_type != 'aba' or (
        aba_routing_number is not null and
        aba_account_number is not null and
        aba_account_type is not null and
        address1 is not null and
        zipcode is not null and
        city is not null and
        state_code is not null)),

    check (account_type != 'canadian' or (
        canadian_institution_number is not null and
        canadian_transit_number is not null and
        canadian_account_number is not null and
        canadian_account_type is not null)),

    check (account_type != 'australian' or (
        australian_bsb_code is not null and
        australian_business_number is not null and
        australian_account_number is not null and
        address1 is not null and
        zipcode is not null and
        city is not null and
        state_code is not null)),

    check (account_type != 'new-zealand' or (
        new_zealand_account_number is not null))
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
    state_code              varchar,
    country_code            char(2),

    -- Coordinates
    long                    coordinate not null,
    lat                     coordinate not null,

    timezone                java_zoneid,

    -- Opening schedule

    monday_is_open          boolean not null,
    monday_opens_at         java_localtime,
    monday_closes_at        java_localtime,

    tuesday_is_open         boolean not null,
    tuesday_opens_at        java_localtime,
    tuesday_closes_at       java_localtime,

    wednesday_is_open       boolean not null,
    wednesday_opens_at      java_localtime,
    wednesday_closes_at     java_localtime,

    thursday_is_open        boolean not null,
    thursday_opens_at       java_localtime,
    thursday_closes_at      java_localtime,

    friday_is_open          boolean not null,
    friday_opens_at         java_localtime,
    friday_closes_at        java_localtime,

    saturday_is_open        boolean not null,
    saturday_opens_at       java_localtime,
    saturday_closes_at      java_localtime,

    sunday_is_open          boolean not null,
    sunday_opens_at         java_localtime,
    sunday_closes_at        java_localtime,

    -- Pricing policy

    currency_code           char(3) not null,

    price_per_hour          amount not null,

    has_evening_pricing     boolean not null,
    evening_begins_at       java_localtime,
    evening_price_per_hour  amount,

    has_weekend_pricing     boolean not null,
    weekend_price_per_hour  amount,

    -- Booking policy

    min_booking_duration    duration not null
        check (min_booking_duration >= 0),
    automatic_approval      boolean not null,

    can_cancel              boolean not null,
    cancellation_notice     duration
        check (cancellation_notice >= 0),

    -- Payment policy

    has_online_payment      boolean not null,
    payout_method_id        integer references "payout_method"(id),
    has_onsite_payment      boolean not null,

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

    check (cancellation_notice is not null = can_cancel),

    check (payout_method_id is not null = has_online_payment)
);

-- Pictures and uploads

create table "picture" (
    id                  bytea primary key,
    created_at          timestamp,
    format              varchar not null
        check (format in ('png', 'gif', 'jpeg')),
    content             bytea not null
);

create table "studio_picture" (
    id                  serial primary key,
    studio_id           integer not null references "studio"(id),
    picture_id          bytea not null references "picture"(id)
);

# --- !Downs

drop table "studio_picture";
drop table "picture";

drop table "studio";

drop table "payout_method";

drop table "user_password_info";
drop table "user_login_info";
drop table "user";

drop domain "java_zoneid";
drop domain "java_localtime";
drop domain "coordinate";
drop domain "amount";
drop domain "duration";
