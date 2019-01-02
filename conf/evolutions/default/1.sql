# --- !Ups

create table "user" (
    "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT
    );

# --- !Downs

drop table "user";
