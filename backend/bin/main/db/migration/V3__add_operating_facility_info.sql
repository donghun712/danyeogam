ALTER TABLE tourist_spot
    ADD COLUMN operating_hours TEXT NULL AFTER detail_hydrated_at,
    ADD COLUMN closed_days TEXT NULL AFTER operating_hours,
    ADD COLUMN parking_note TEXT NULL AFTER closed_days,
    ADD COLUMN parking_fee_note TEXT NULL AFTER parking_note,
    ADD COLUMN stroller_rental_note TEXT NULL AFTER parking_fee_note,
    ADD COLUMN pet_allowed_note TEXT NULL AFTER stroller_rental_note,
    ADD COLUMN intro_hydrated_at DATETIME(6) NULL AFTER pet_allowed_note;
