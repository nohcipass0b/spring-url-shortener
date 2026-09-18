-- create simple user table
create table users (
    id            uuid                        not null primary key,
    email         varchar(255)                not null unique,
    password_hash varchar(60)                 not null,
    created_at    timestamp(6) with time zone not null
);
