ALTER TABLE public.passwordrecovery
  DROP CONSTRAINT passwordrecovery_user_id_fkey,
  ADD CONSTRAINT passwordrecovery_user_id_fkey
FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;