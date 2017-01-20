ALTER TABLE modules DROP COLUMN id;

ALTER TABLE modules ADD PRIMARY KEY (student_id, subject_id, num);