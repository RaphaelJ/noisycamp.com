# --- !Ups

-- Maps a java.time.LocalDate as an ISO 8601 string.
create domain java_localdate as varchar
    check (value similar to '\d{4}-\d{2}-\d{2}');

-- Splits some of `studio_booking`'s content into the new `studio_customer_booking` table, and
-- create a new empty `studio_manual_booking` table.

create table "studio_customer_booking" (
    id                          integer unique references "studio_booking"(id),

    customer_id                 integer not null references "user"(id),

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

    -- Booking cancellation

    can_cancel                  boolean not null,
    cancellation_notice         duration,

    -- Payment

    transaction_fee_rate        numeric,

    payment_method              varchar not null
        check (payment_method in ('online', 'onsite')),

    stripe_checkout_session_id  varchar unique,
    stripe_payment_intent_id    varchar unique,
    stripe_refund_id            varchar unique,

    check (stripe_checkout_session_id is not null = (payment_method = 'online')),
    check (stripe_payment_intent_id is not null = (payment_method = 'online')),
    check (stripe_refund_id is null or (payment_method = 'online'))
);

insert into "studio_customer_booking"
    select
        id,
        customer_id,
        duration_regular, duration_evening, duration_weekend,
        currency, total,
        price_per_hour, evening_price_per_hour, weekend_price_per_hour,
        can_cancel, cancellation_notice,
        transaction_fee_rate,
        payment_method,
        stripe_checkout_session_id, stripe_payment_intent_id, stripe_refund_id
    from "studio_booking";

create table "studio_manual_booking" (
    id                          integer unique references "studio_booking"(id),
    title                       varchar not null,
    customer_email              varchar
);

alter table "studio_booking"
    add column repeat_type      varchar
        check (repeat_type in ('repeat-count', 'repeat-until')),

    add column repeat_frequency varchar
        check (
            repeat_type in ('repeat-count', 'repeat-until')
            and repeat_frequency in ('daily', 'weekly', 'monthly', 'yearly')),

    add column repeat_count     integer
        check (
            (repeat_type = 'repeat-count') = (repeat_count is not null)
            and repeat_count >= 1),
    add column repeat_until     java_localdate
        check (
            (repeat_type = 'repeat-until') = (repeat_until is not null)
            and repeat_until >= begins_at),

    add column booking_type     varchar not null default('customer')
        check (booking_type in ('customer', 'manual')),

    drop column can_cancel,
    drop column cancellation_notice,
    drop column customer_id,
    drop column duration_regular,
    drop column duration_evening,
    drop column duration_weekend,
    drop column currency,
    drop column total,
    drop column price_per_hour,
    drop column evening_price_per_hour,
    drop column weekend_price_per_hour,
    drop column transaction_fee_rate,
    drop column payment_method,
    drop column stripe_checkout_session_id,
    drop column stripe_payment_intent_id,
    drop column stripe_refund_id;

create domain plan_type as varchar
    check (value in ('free', 'standard', 'premium'));

create table "user_subscription" (
    id                          serial primary key,
    created_at                  timestamp not null,

    user_id                     integer not null references "user"(id),

    plan                        plan_type not null,

    stripe_checkout_session_id  varchar not null unique,
    stripe_customer_id          varchar unique,
    stripe_subscription_id      varchar unique,

    status                      varchar
        check (status in (
            'trialing', 'active', 'incomplete', 'incomplete_expired', 'past_due', 'cancelled',
            'unpaid'))
);

alter table "user"
    add column subscription_id  integer references "user_subscription"(id)
        check (subscription_id is null or (plan in ('standard', 'premium'))),

    -- Defined if the user is currently upgrading its plan.
    add column next_subscription_id  integer references "user_subscription"(id),

    drop constraint user_plan_check,
    alter column plan type plan_type;

# --- !Downs

alter table "user"
    drop column "subscription_id",
    drop column "next_subscription_id";

drop table "user_subscription";

drop table "studio_manual_booking";
drop table "studio_customer_booking";

drop domain "java_localdate";
