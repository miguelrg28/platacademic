package edu.pucmm.icc352.events.infrastructure.persistence;

import edu.pucmm.icc352.events.domain.model.User;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public final class UserRepository {
    public Optional<User> findById(Session session, Long id) {
        return session.createQuery(
                        "select distinct u from User u left join fetch u.roles where u.id = :id",
                        User.class
                )
                .setParameter("id", id)
                .uniqueResultOptional();
    }

    public Optional<User> findByUsername(Session session, String username) {
        return session.createQuery(
                        "select distinct u from User u left join fetch u.roles where u.username = :username",
                        User.class
                )
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public Optional<User> findByEmail(Session session, String email) {
        return session.createQuery(
                        "select distinct u from User u left join fetch u.roles where u.email = :email",
                        User.class
                )
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    public List<User> findAll(Session session) {
        return session.createQuery(
                        "select distinct u from User u left join fetch u.roles order by u.createdAt desc",
                        User.class
                )
                .list();
    }

    public void save(Session session, User user) {
        if (user.getId() == null) {
            session.persist(user);
            return;
        }
        session.merge(user);
    }
}
