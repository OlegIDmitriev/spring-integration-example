create table if not exists request (
   id              bigserial PRIMARY KEY,
   integration_id  uuid,
   created_at      timestamp with time zone
);