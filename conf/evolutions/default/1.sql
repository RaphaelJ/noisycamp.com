# --- !Ups

-- Types

-- Maps java.time.Duration type.
create domain duration as bigint
    check (value >= 0);

-- A amount of money.
create domain amount as numeric
    check (value >= 0);

-- A decimal coordinate value.
create domain coordinate as numeric
    check (value >= -180 and value <= 180);

-- Maps a java.time.LocalTime as an ISO 8601 string.
create domain java_localtime as varchar
    check (value similar to '\d{2}:\d{2}(:\d{2}(.\d+)?)?');

-- Maps a java.time.LocalDateTime as an ISO 8601 string.
create domain java_localdatetime as varchar
    check (value similar to '\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(.\d+)?)?');

-- Maps a java.time.ZoneId as a string.
create domain java_zoneid as varchar;

-- Users and account

create table "user" (
    id                      serial primary key,
    created_at              timestamp not null,
    first_name              varchar,
    last_name               varchar,
    email                   varchar not null,
    avatar_id               integer,

    stripe_user_id          varchar
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
    zipcode                 varchar,
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

    price_per_hour          amount not null,

    has_evening_pricing     boolean not null,
    evening_begins_at       java_localtime,
    evening_price_per_hour  amount,

    has_weekend_pricing     boolean not null,
    weekend_price_per_hour  amount,

    -- Booking policy

    min_booking_duration    duration not null,
    automatic_approval      boolean not null,

    can_cancel              boolean not null,
    cancellation_notice     duration,

    -- Payment policy

    has_online_payment      boolean not null,
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

    check (cancellation_notice is not null = can_cancel)
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

create index "idx_studio_picture_studio_id" on "studio_picture" ("studio_id");
create index "idx_studio_picture_picture_id" on "studio_picture" ("picture_id");

-- Studio equipment

create table "equipment" (
    id                  serial primary key,

    category            varchar,
    details             varchar,
    price_per_hour      amount
);

create table "studio_equipment" (
    id                  serial primary key,
    studio_id           integer not null references "studio"(id),
    equipment_id        integer not null references "equipment"(id)
);

create index "idx_studio_equipment_studio_id" on "studio_equipment" ("studio_id");
create index "idx_studio_equipment_equipment_id" on "studio_equipment" ("equipment_id");

-- Bookings

create table "studio_booking" (
    id                          serial primary key,
    created_at                  timestamp not null,

    studio_id                   integer not null references "studio"(id),
    customer_id                 integer not null references "user"(id),

    status                      varchar not null
        check (status in ('processing', 'pending-validation', 'succeeded', 'failed', 'cancelled')),

    -- Booking time and duration

    begins_at                   java_localdatetime not null,

    duration_total              duration not null,
    duration_regular            duration not null,
    duration_evening            duration not null,
    duration_weekend            duration not null,

    -- Booking's pricing at the time of booking

    currency_code               char(3) not null,
    total                       amount not null,

    -- Pricing policy at the time of booking
    regular_price_per_hour      amount not null,
    evening_price_per_hour      amount,
    weekend_price_per_hour      amount,

    -- Payment

    payment_method              varchar not null
        check (payment_method in ('online', 'onsite')),

    stripe_checkout_session_id  varchar unique,
    stripe_payment_intent_id    varchar unique,

    check (duration_total = (
        duration_regular + duration_evening + duration_weekend
    )),
    check (stripe_checkout_session_id is not null = (payment_method = 'online')),
    check (stripe_payment_intent_id is not null = (payment_method = 'online'))
);

-- Payouts

create table "payout" (
    id                          serial primary key,
    created_at                  timestamp not null,

    customer_id                 integer not null references "user"(id),

    stripe_payout_id            varchar unique not null,

    currency_code               char(3) not null,
    amount                      amount not null
);

create index "idx_payout_customer_id" on "payout" ("customer_id");

# --- !Downs

drop table "payout";

drop table "studio_booking";

drop table "studio_equipment";
drop table "equipment";

drop table "studio_picture";
drop table "picture";

drop table "studio";

drop table "user_password_info";
drop table "user_login_info";
drop table "user";

drop domain "java_zoneid";
drop domain "java_localtime";
drop domain "java_localdatetime";
drop domain "coordinate";
drop domain "amount";
drop domain "duration";
