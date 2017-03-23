ALTER TABLE public.moduleshistory
  DROP CONSTRAINT moduleshistory_student__fk,
  ADD CONSTRAINT moduleshistory_student__fk
FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE;