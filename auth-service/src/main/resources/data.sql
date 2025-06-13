-- Default Authorities
INSERT INTO authorities (name) VALUES ('OP_CREATE_SURVEY') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_VIEW_OWN_SURVEY') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_EDIT_OWN_SURVEY') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_DELETE_OWN_SURVEY') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_SHARE_SURVEY') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_MANAGE_USERS') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_MANAGE_ROLES') ON CONFLICT (name) DO NOTHING;
INSERT INTO authorities (name) VALUES ('OP_VIEW_ALL_SURVEYS') ON CONFLICT (name) DO NOTHING;

-- Default Roles
INSERT INTO roles (name) VALUES ('ROLE_SYSTEM_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_SURVEY_CREATOR') ON CONFLICT (name) DO NOTHING;

-- Assign Authorities to ROLE_SYSTEM_ADMIN
-- Assuming IDs: ROLE_SYSTEM_ADMIN=1, and authorities 1-8
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 1) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_CREATE_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 2) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_VIEW_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 3) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_EDIT_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 4) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_DELETE_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 5) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_SHARE_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 6) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_MANAGE_USERS
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 7) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_MANAGE_ROLES
INSERT INTO role_authorities (role_id, authority_id) VALUES (1, 8) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_VIEW_ALL_SURVEYS

-- Assign Authorities to ROLE_USER_ADMIN
-- Assuming IDs: ROLE_USER_ADMIN=2, OP_MANAGE_USERS=6
INSERT INTO role_authorities (role_id, authority_id) VALUES (2, 6) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_MANAGE_USERS

-- Assign Authorities to ROLE_SURVEY_CREATOR
-- Assuming IDs: ROLE_SURVEY_CREATOR=3, and authorities 1-5
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 1) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_CREATE_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 2) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_VIEW_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 3) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_EDIT_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 4) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_DELETE_OWN_SURVEY
INSERT INTO role_authorities (role_id, authority_id) VALUES (3, 5) ON CONFLICT (role_id, authority_id) DO NOTHING; -- OP_SHARE_SURVEY

-- Default User: admin / password
-- The password hash is for 'password' using BCrypt: {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
INSERT INTO users (username, email, password, is_active) VALUES ('admin', 'admin@example.com', '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', true) ON CONFLICT (username) DO NOTHING;

-- Assign ROLE_SYSTEM_ADMIN to admin user
-- Assuming IDs: admin_user_id=1 (if it's the first user), ROLE_SYSTEM_ADMIN=1
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1) ON CONFLICT (user_id, role_id) DO NOTHING;
