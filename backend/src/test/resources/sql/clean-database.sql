SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM actor_title;
DELETE FROM title_definition;
DELETE FROM visit;
DELETE FROM verification_attempt;
DELETE FROM anonymous_session;
DELETE FROM actor;
DELETE FROM app_user;
DELETE FROM tourist_spot_image;
DELETE FROM tourist_spot;
DELETE FROM region;
DELETE FROM sync_error;
DELETE FROM tourist_spot_staging;
DELETE FROM sync_run;

SET FOREIGN_KEY_CHECKS = 1;
