CREATE TABLE updates
(
    id     BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    update JSONB,
    CONSTRAINT pk_updates PRIMARY KEY (id)
);