--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.5
-- Dumped by pg_dump version 9.5.5

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

-- --
-- -- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
-- --
--
-- CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
--
--
-- --
-- -- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
-- --
--
-- COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;
CREATE TABLE adminuser (
    id bigint NOT NULL,
    email character varying(255),
    isadmin boolean NOT NULL,
    password character varying(255),
    username character varying(255),
    cookie character varying(255)
);



CREATE SEQUENCE adminuser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE adminuser_id_seq OWNED BY adminuser.id;

CREATE TABLE dbcopytest (
    subject_id character(10)
);



CREATE TABLE groupshistory (
    id integer NOT NULL,
    groupname character varying(255),
    semestr character varying(255),
    student_id integer
);



CREATE SEQUENCE groupshistory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE groupshistory_id_seq OWNED BY groupshistory.id;

CREATE SEQUENCE hibernate_sequence
    START WITH 8862864
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



CREATE TABLE modules (
    id integer NOT NULL,
    color integer NOT NULL,
    num character varying(255),
    value integer NOT NULL,
    student_id integer,
    subject_id integer
);



CREATE SEQUENCE modules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE modules_id_seq OWNED BY modules.id;

CREATE TABLE student (
    id integer NOT NULL,
    cardid character varying(255),
    initials character varying(255),
    name character varying(255),
    password character varying(255),
    patronym character varying(255),
    stgroup character varying(255),
    surname character varying(255)
);



CREATE SEQUENCE student_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE student_id_seq OWNED BY student.id;

CREATE TABLE subjects (
    id integer NOT NULL,
    factor real NOT NULL,
    stgroup character varying(255),
    title TEXT,
    semester character varying(255)
);



CREATE SEQUENCE subjects_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE subjects_id_seq OWNED BY subjects.id;

ALTER TABLE ONLY adminuser ALTER COLUMN id SET DEFAULT nextval('adminuser_id_seq'::regclass);

ALTER TABLE ONLY groupshistory ALTER COLUMN id SET DEFAULT nextval('groupshistory_id_seq'::regclass);

ALTER TABLE ONLY modules ALTER COLUMN id SET DEFAULT nextval('modules_id_seq'::regclass);

ALTER TABLE ONLY student ALTER COLUMN id SET DEFAULT nextval('student_id_seq'::regclass);

ALTER TABLE ONLY subjects ALTER COLUMN id SET DEFAULT nextval('subjects_id_seq'::regclass);

ALTER TABLE ONLY adminuser
    ADD CONSTRAINT adminuser_id_pk PRIMARY KEY (id);

ALTER TABLE ONLY groupshistory
    ADD CONSTRAINT groupshistory_id_pk PRIMARY KEY (id);

ALTER TABLE ONLY modules
    ADD CONSTRAINT modules_id_pk PRIMARY KEY (id);

ALTER TABLE ONLY student
    ADD CONSTRAINT student_id_pk PRIMARY KEY (id);

ALTER TABLE ONLY subjects
    ADD CONSTRAINT subjects_id_pk PRIMARY KEY (id);

ALTER TABLE ONLY groupshistory
    ADD CONSTRAINT uk5d4a1gfffpbuabky7sj4pb3po UNIQUE (student_id, semestr, groupname);

ALTER TABLE ONLY groupshistory
    ADD CONSTRAINT uk_5d4a1gfffpbuabky7sj4pb3po UNIQUE (student_id, semestr, groupname);

ALTER TABLE ONLY adminuser
    ADD CONSTRAINT uk_egxx66shwrpr03gkgw8yj4vwh UNIQUE (cookie);

CREATE INDEX fk_5otvk9yxto52rgfkjan0qib4o_index_7 ON modules USING btree (student_id);

CREATE INDEX fk_86oo8uuxdn2hr4dolbbgqgmpg_index_7 ON modules USING btree (subject_id);

CREATE INDEX idxqylfrw82tuigt0bugfxcern8u ON student USING btree (stgroup, surname, initials);

CREATE INDEX idxtew6ykyn1txtjb9xf8xp1wtft ON student USING btree (cardid);

CREATE UNIQUE INDEX primary_key_1 ON groupshistory USING btree (id);

CREATE UNIQUE INDEX primary_key_7 ON modules USING btree (id);

CREATE UNIQUE INDEX primary_key_a ON adminuser USING btree (id);

CREATE UNIQUE INDEX primary_key_b ON student USING btree (id);

CREATE UNIQUE INDEX primary_key_f ON subjects USING btree (id);

CREATE INDEX title_index ON subjects USING btree (stgroup, title);

CREATE UNIQUE INDEX uk_5d4a1gfffpbuabky7sj4pb3po_index_1 ON groupshistory USING btree (student_id, semestr, groupname);

CREATE INDEX uk_qylfrw82tuigt0bugfxcern8u ON student USING btree (stgroup, surname, initials);

CREATE UNIQUE INDEX uk_se1grcfbuoj9jhn4ycih7wl0c_index_a ON adminuser USING btree (username);

CREATE INDEX uk_tew6ykyn1txtjb9xf8xp1wtft ON student USING btree (cardid);

CREATE UNIQUE INDEX uk_tew6ykyn1txtjb9xf8xp1wtft_index_b ON student USING btree (cardid);

ALTER TABLE ONLY groupshistory
    ADD CONSTRAINT fk33m2o1sr3icyn6rlqlmvnm4l3 FOREIGN KEY (student_id) REFERENCES student(id);

ALTER TABLE ONLY modules
    ADD CONSTRAINT fkhtwh5vnigumkyiwtb5vk0ywle FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;

ALTER TABLE ONLY modules
    ADD CONSTRAINT fkkb5kuit0ks4bhh4qugw6t7wtj FOREIGN KEY (subject_id) REFERENCES subjects(id);

ALTER TABLE ONLY groupshistory
    ADD CONSTRAINT groupshistory_student_id_fkey FOREIGN KEY (student_id) REFERENCES student(id);

ALTER TABLE ONLY modules
    ADD CONSTRAINT modules_student__fk FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;

ALTER TABLE ONLY modules
    ADD CONSTRAINT modules_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES subjects(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

