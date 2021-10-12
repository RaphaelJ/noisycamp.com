# --- !Ups

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
    title                       varchar not null
);

alter table "studio_booking"
    add column booking_type     varchar not null default('customer')
        check (booking_type in ('customer', 'manual')),

    add column repeat_type      varchar
        check (repeat_type in ('repeat-count', 'repeat-until')),
    add column repeat_count     integer
        check (
            (repeat_type = 'repeat-count') = (repeat_count is not null)
            and repeat_count >= 1),
    add column repeat_until     java_localdatetime
        check (
            (repeat_type = 'repeat-until') = (repeat_until is not null)
            and repeat_until >= begins_at),

    drop column customer_id,
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

# --- !Downs

drop table "studio_manual_booking";
drop table "studio_customer_booking";
