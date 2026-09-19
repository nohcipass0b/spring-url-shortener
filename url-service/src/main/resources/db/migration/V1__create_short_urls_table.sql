-- There is no foreign key to users: that table lives in auth-service's own
-- database. user_id is the UUID from the JWT subject, and ownership is enforced
-- in the service layer.
create table short_urls (
    id             uuid                        not null primary key,
    code           varchar(16)                 not null unique,
    original_url   varchar(2048)               not null,
    user_id        uuid                        not null,
    active         boolean                     not null default true,
    click_count    bigint                      not null default 0,
    created_at     timestamp(6) with time zone not null,
    deactivated_at timestamp(6) with time zone
);

-- this one for listing a user's own URLs
create index idx_short_urls_user_id on short_urls (user_id);
