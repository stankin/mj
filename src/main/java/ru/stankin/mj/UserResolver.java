package ru.stankin.mj;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stankin.mj.model.AdminUser;
import ru.stankin.mj.model.Storage;
import ru.stankin.mj.model.Student;
import ru.stankin.mj.model.User;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Created by nickl on 16.02.15.
 */
@Singleton
public class UserResolver implements UserDAO {

    private static final Logger logger = LogManager.getLogger(UserResolver.class);

    @Inject
    Storage storage;

    @PersistenceContext
    private EntityManager em;

    private volatile boolean initRequred = true;
    //TODO: Maybe @javax.annotation.PostConstruct ?
    private void initIfRequired(){
        if(initRequred) synchronized (this) {
            if(initRequred) {
                CriteriaBuilder b = em.getCriteriaBuilder();
                CriteriaQuery<AdminUser> query = b.createQuery(AdminUser.class);
                query.select(query.from(AdminUser.class));
                try {
                    em.createQuery(query).getSingleResult();
                } catch (NoResultException e) {
                    em.persist(new AdminUser("admin", "adminadmin", null));
                }
                em.flush();
                initRequred = false;
            }
        }

    }


    @Override
    @Transactional
    public User getUserBy(String username, String password) {
        initIfRequired();
        User result = getUserBy(username);

        if (result != null && !result.getPassword().equals(password))
            result = null;

        return result;
    }

    @Override
    public User getUserBy(String username) {
        User result = storage.getStudentByCardId(username);
        if (result == null)
            result = getAdminUser(username);
        return result;
    }

    public AdminUser getAdminUser(String username) {
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<AdminUser> query = b.createQuery(AdminUser.class);
        Root<AdminUser> from = query.from(AdminUser.class);
        query.where(b.equal(from.get("username"), username));

        try {
            TypedQuery<AdminUser> query1 = em.createQuery(query);
            query1.setFlushMode(FlushModeType.COMMIT);
            query1.setMaxResults(1);
            return query1.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }


    @Override
    @Transactional
    public boolean saveUser(User user) {
        logger.debug("saving user {}", user);
        if(user instanceof Student)
            storage.saveStudent((Student) user, null);
        else{
            em.merge(user);
        }
        return true;
    }

    @Override
    public List<User> getUsers() {
        throw new UnsupportedOperationException("getUsers");
    }



    @Override
    @Transactional
    public User getUserCookie(String cookie) {
        User result = storage.getStudentByCookie(cookie);
        if (result == null)
            result = getAdminByCookId(cookie);
        return result;
    }

    public AdminUser getAdminByCookId(String cookie) {
        CriteriaBuilder b = em.getCriteriaBuilder();
        CriteriaQuery<AdminUser> query = b.createQuery(AdminUser.class);
        Root<AdminUser> from = query.from(AdminUser.class);
        query.where(b.equal(from.get("cookie"), cookie));

        try {
            TypedQuery<AdminUser> query1 = em.createQuery(query);

            query1.setFlushMode(FlushModeType.COMMIT);
            query1.setMaxResults(1);
            return query1.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }


}
