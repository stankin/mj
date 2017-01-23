CREATE TABLE public.TRANSACTIONS
(
  id     BIGSERIAL PRIMARY KEY,
  time   TIMESTAMP NOT NULL,
  author BIGINT    NOT NULL,
  CONSTRAINT TRANSACTIONS_users_id_fk FOREIGN KEY (author) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
);


ALTER TABLE public.modules
  ADD transaction BIGINT NULL;
ALTER TABLE public.modules
  ADD CONSTRAINT modules_transactions_id_fk
FOREIGN KEY (transaction) REFERENCES transactions (id) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE TABLE moduleshistory
(
  id          SERIAL PRIMARY KEY NOT NULL,
  color       INTEGER            NOT NULL,
  num         VARCHAR(255),
  value       INTEGER            NOT NULL,
  student_id  INTEGER,
  subject_id  INTEGER,
  transaction BIGINT,
  CONSTRAINT moduleshistory_student__fk FOREIGN KEY (student_id) REFERENCES users (id),
  CONSTRAINT moduleshistory_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES subjects (id),
  CONSTRAINT moduleshistory_transactions_id_fk FOREIGN KEY (transaction) REFERENCES transactions (id)
);


CREATE OR REPLACE FUNCTION copyModuleToHistory()
  RETURNS TRIGGER AS
$BODY$
BEGIN
  IF (SELECT count(*)
      FROM users
      WHERE id = OLD.student_id
      LIMIT 1) > 0
  THEN
    INSERT INTO moduleshistory (color, num, value, student_id, subject_id, transaction)
    VALUES (OLD.color, OLD.num, OLD.value, OLD.student_id, OLD.subject_id, OLD.transaction);
  END IF;
  IF (TG_OP = 'DELETE')
  THEN
    RETURN OLD;
  ELSE
    RETURN NEW;
  END IF;
END;
$BODY$ LANGUAGE PLPGSQL;


CREATE TRIGGER copyModuleToHistoryTrigger
BEFORE UPDATE OR DELETE ON modules
FOR EACH ROW EXECUTE PROCEDURE copyModuleToHistory();
