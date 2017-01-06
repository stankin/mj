CREATE TABLE Users (
  id       BIGSERIAL PRIMARY KEY,
  login    CHARACTER VARYING(255) UNIQUE,
  email    CHARACTER VARYING(255) UNIQUE,
  initials CHARACTER VARYING(255),
  name     CHARACTER VARYING(255),
  patronym CHARACTER VARYING(255),
  surname  CHARACTER VARYING(255)
);


INSERT INTO users (id, login, email) SELECT
                                       id,
                                       username,
                                       email
                                     FROM adminuser;

INSERT INTO users (id, login, name, patronym, surname, initials)
  SELECT
    id,
    cardid,
    name,
    patronym,
    surname,
    initials
  FROM student;

SELECT pg_catalog.setval('users_id_seq', max(id))
FROM users;

ALTER TABLE ONLY adminuser
  DROP CONSTRAINT adminuser_id_pk,
  ADD CONSTRAINT adminuser_id_pk
FOREIGN KEY (id) REFERENCES Users (id) ON DELETE CASCADE,
  DROP COLUMN username,
  DROP COLUMN email,
  DROP COLUMN isadmin;


ALTER TABLE ONLY groupshistory
  DROP CONSTRAINT fk33m2o1sr3icyn6rlqlmvnm4l3,
  DROP CONSTRAINT groupshistory_student_id_fkey,
  ADD CONSTRAINT groupshistory_student_id_fkey
FOREIGN KEY (student_id) REFERENCES Users (id) ON DELETE CASCADE;

ALTER TABLE ONLY modules
  DROP CONSTRAINT fkhtwh5vnigumkyiwtb5vk0ywle,
  DROP CONSTRAINT modules_student__fk,
  ADD CONSTRAINT modules_student__fk
FOREIGN KEY (student_id) REFERENCES Users (id) ON DELETE CASCADE;


ALTER TABLE ONLY student
  DROP CONSTRAINT student_id_pk,
  ADD CONSTRAINT student_id_pk
FOREIGN KEY (id) REFERENCES Users (id) ON DELETE CASCADE,
  DROP COLUMN cardid,
  DROP COLUMN name,
  DROP COLUMN patronym,
  DROP COLUMN surname,
  DROP COLUMN initials;


