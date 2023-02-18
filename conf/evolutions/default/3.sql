# --- !Ups

alter table "picture"
    drop constraint picture_format_check,
    add constraint picture_format_check
        check (format in ('gif', 'jpeg', 'png', 'webp', 'webp-lossless'));

# --- !Downs

alter table "picture"
    drop constraint picture_format_check,
    add constraint picture_format_check
        check (format in ('png', 'gif', 'jpeg'));
