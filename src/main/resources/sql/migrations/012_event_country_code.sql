USE oripa;

ALTER TABLE event_place
    ADD COLUMN country_code CHAR(2) NULL AFTER event_type;
