create table "ai-rofl-bot".answers
(
    id         serial
        constraint answers_pk
            primary key,
    to_update integer not null
        references updates (id),
    text       text    not null
);
