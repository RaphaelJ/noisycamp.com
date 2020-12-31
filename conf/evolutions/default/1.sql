# --- !Ups

-- Types

-- Maps java.time.Duration type.
create domain duration as bigint
    check (value >= 0);

-- Maps squants.Currency to its 3-chars ISO code.
create domain currency as char(3);

-- A amount of money.
create domain amount as numeric
    check (value >= 0);

-- Country as its 2-chars ISO code.
create domain country as char(2);

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
    email                   varchar not null unique,
    avatar_id               integer,

    plan                    varchar not null
        check (plan in ('free', 'premium')),

    stripe_account_id       varchar,
    stripe_completed        boolean not null,

    check (not stripe_completed or stripe_account_id is not null)
);

create index "idx_user_stripe_account_id" on "user" ("stripe_account_id");

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

    published               boolean,

    name                    varchar not null,
    description             varchar not null,
    phone                   varchar,

    use_practice            boolean,
    use_recording           boolean,
    use_live                boolean,
    use_lessons             boolean,

    -- Address
    address1                varchar not null,
    address2                varchar,
    zipcode                 varchar,
    city                    varchar not null,
    state_code              varchar,
    country                 char(2),

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

create index "idx_studio_owner_id" on "studio" ("owner_id");
create index "idx_studio_long" on "studio" ("long");
create index "idx_studio_lat" on "studio" ("lat");

create index "idx_studio_monday_is_open" on "studio" using hash ("monday_is_open");
create index "idx_studio_tuesday_is_open" on "studio" using hash ("tuesday_is_open");
create index "idx_studio_wednesday_is_open" on "studio" using hash ("wednesday_is_open");
create index "idx_studio_thursday_is_open" on "studio" using hash ("thursday_is_open");
create index "idx_studio_friday_is_open" on "studio" using hash ("friday_is_open");
create index "idx_studio_saturday_is_open" on "studio" using hash ("saturday_is_open");
create index "idx_studio_sunday_is_open" on "studio" using hash ("sunday_is_open");

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

    price_type          varchar -- null if free/included in the session price
        check (price_type in ('per-hour', 'per-session')),
    price               amount,

    check ((price is not null) = (price_type is not null))
);

create table "studio_equipment" (
    id                  serial primary key,
    studio_id           integer not null references "studio"(id),
    equipment_id        integer not null references "equipment"(id),

    unique ("studio_id", "equipment_id")
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
        check (status in (
            'payment-processing', 'payment-failure', 'pending-validation', 'valid', 'rejected',
            'cancelled-by-customer', 'cancelled-by-owner')),

    can_cancel                  boolean not null,
    cancellation_notice         duration,

    cancelled_at                timestamp
        check (
            (status in ('cancelled-by-customer', 'cancelled-by-owner')) =
            (cancelled_at is not null)),
    cancellation_reason         varchar
        check (
            cancellation_reason is null
            or status in ('cancelled-by-customer', 'cancelled-by-owner')),

    -- Booking time and duration

    begins_at                   java_localdatetime not null,
    duration                    duration not null,

    -- Not strictly required, but makes booking time SQL queries easier and more efficient.
    ends_at                     java_localdatetime not null,

    duration_regular            duration not null,
    duration_evening            duration not null,
    duration_weekend            duration not null,

    -- Booking's pricing at the time of booking

    currency                    currency not null,
    total                       amount not null,

    -- Pricing policy at the time of booking
    price_per_hour              amount not null,
    evening_price_per_hour      amount,
    weekend_price_per_hour      amount,

    -- Payment

    transaction_fee_rate        numeric,

    payment_method              varchar not null
        check (payment_method in ('online', 'onsite')),

    stripe_checkout_session_id  varchar unique,
    stripe_payment_intent_id    varchar unique,
    stripe_refund_id            varchar unique,

    check (duration = (duration_regular + duration_evening + duration_weekend)),
    check (stripe_checkout_session_id is not null = (payment_method = 'online')),
    check (stripe_payment_intent_id is not null = (payment_method = 'online')),
    check (stripe_refund_id is null or (payment_method = 'online'))
);

create index "idx_studio_booking_studio_id_begins_at"
    on "studio_booking" ("studio_id", "begins_at");
create index "idx_studio_booking_studio_id_ends_at"
    on "studio_booking" ("studio_id", "ends_at");

create index "idx_studio_booking_customer_id_begins_at"
    on "studio_booking" ("customer_id", "begins_at");

create table "studio_booking_equipment" (
    id                  serial primary key,
    booking_id          integer not null references "studio_booking"(id),
    equipment_id        integer not null references "equipment"(id),

    unique ("booking_id", "equipment_id")
);

create index "idx_studio_booking_equipment_booking_id" 
    on "studio_booking_equipment" ("booking_id");
create index "idx_studio_booking_equipment_equipment_id"
    on "studio_booking_equipment" ("equipment_id");

-- Payouts

create table "payout" (
    id                          serial primary key,
    created_at                  timestamp not null,

    customer_id                 integer not null references "user"(id),

    stripe_payout_id            varchar unique not null,

    currency                    currency not null,
    amount                      amount not null
);

create index "idx_payout_customer_id" on "payout" ("customer_id");

# --- !Downs

drop table "payout";

drop table "studio_booking_equipment";
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
drop domain "country";
drop domain "amount";
drop domain "currency";
drop domain "duration";
