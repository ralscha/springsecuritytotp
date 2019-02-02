CREATE TABLE app_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    name    VARCHAR(50) NOT NULL,
    PRIMARY KEY(id)
);

INSERT INTO app_role(id, name) values(1, 'ADMIN'), (2, 'USER');


CREATE TABLE app_user (
	id              BIGINT NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL,
    enabled         BOOLEAN not null,
    expiration_date TIMESTAMP,
    failed_logins   INTEGER,
    first_name      VARCHAR(255),
    locale          VARCHAR(8),
    locked_out      TIMESTAMP,
    name            VARCHAR(255),
    password_hash   VARCHAR(80),
    secret          VARCHAR(16),
    user_name       VARCHAR(100) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE app_user_roles (
    app_user_id  BIGINT NOT NULL,
    app_role_id  BIGINT NOT NULL,
    PRIMARY KEY (app_user_id, app_role_id),
    FOREIGN KEY (app_user_id) REFERENCES app_user(id),
    FOREIGN KEY (app_role_id) REFERENCES app_role(id)
); 
