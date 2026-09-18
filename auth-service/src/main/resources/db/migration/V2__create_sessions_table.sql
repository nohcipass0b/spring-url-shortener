-- refresh token sessions; only the sha-256 hash of a token is stored
create table sessions (
    id         uuid                        not null primary key,
    user_id    uuid                        not null references users (id) on delete cascade,
    token_hash varchar(64)                 not null unique,
    device     varchar(255),
    ip_address varchar(45),
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    revoked_at timestamp(6) with time zone
);

create index idx_sessions_user_id on sessions (user_id);
