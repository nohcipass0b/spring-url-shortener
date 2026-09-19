-- auth-service gets POSTGRES_DB; url-service needs its own database
CREATE USER urluser WITH PASSWORD 'urlpass';
CREATE DATABASE urldb OWNER urluser;
GRANT ALL PRIVILEGES ON DATABASE urldb TO urluser;
