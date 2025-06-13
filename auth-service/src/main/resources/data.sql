-- Default Authorities
INSERT INTO authorities (name) VALUES ('OP_CREATE_SURVEY');
INSERT INTO authorities (name) VALUES ('OP_VIEW_OWN_SURVEY');
INSERT INTO authorities (name) VALUES ('OP_EDIT_OWN_SURVEY');
INSERT INTO authorities (name) VALUES ('OP_DELETE_OWN_SURVEY');
INSERT INTO authorities (name) VALUES ('OP_SHARE_SURVEY');
INSERT INTO authorities (name) VALUES ('OP_MANAGE_USERS');
INSERT INTO authorities (name) VALUES ('OP_MANAGE_ROLES');
INSERT INTO authorities (name) VALUES ('OP_VIEW_ALL_SURVEYS');

-- Default Roles
INSERT INTO roles (name) VALUES ('ROLE_SYSTEM_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_USER_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_SURVEY_CREATOR');
INSERT INTO roles (name) VALUES ('ROLE_USER');

-- Assign Authorities to ROLE_SYSTEM_ADMIN
-- Assuming IDs: ROLE_SYSTEM_ADMIN=1, and authorities 1-8
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 1);-- OP_CREATE_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 2);-- OP_VIEW_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 3);-- OP_EDIT_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 4);-- OP_DELETE_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 5);-- OP_SHARE_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 6);-- OP_MANAGE_USERS
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 7);-- OP_MANAGE_ROLES
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 8);-- OP_VIEW_ALL_SURVEYS

-- Assign Authorities to ROLE_USER_ADMIN
-- Assuming IDs: ROLE_USER_ADMIN=2, OP_MANAGE_USERS=6
INSERT INTO role_authorities (role_id, authority_id) VALUES (2, 6);-- OP_MANAGE_USERS

-- Assign Authorities to ROLE_SURVEY_CREATOR
-- Assuming IDs: ROLE_SURVEY_CREATOR=3, and authorities 1-5
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 1);-- OP_CREATE_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 2);-- OP_VIEW_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 3);-- OP_EDIT_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 4);-- OP_DELETE_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 5);-- OP_SHARE_SURVEY
