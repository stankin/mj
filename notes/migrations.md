# Migration 04.11.15 

    ALTER TABLE MODULES DROP CONSTRAINT FK_5OTVK9YXTO52RGFKJAN0QIB4O
    
## Восстановление паролей:

    UPDATE Student s SET (Password) = (SELECT PASSWORD FROM CSVREAD('/Users/nickl-mac/Downloads/newpasswords.txt') i WHERE i.cardid = s.cardid) where (SELECT count(*) FROM CSVREAD('/Users/nickl-mac/Downloads/newpasswords.txt') i WHERE i.cardid = s.cardid) >0