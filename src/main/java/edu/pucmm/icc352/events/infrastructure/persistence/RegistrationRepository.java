package edu.pucmm.icc352.events.infrastructure.persistence;

import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.domain.model.RegistrationStatus;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public final class RegistrationRepository {
    public Optional<Registration> findByEventIdAndUserId(Session session, Long eventId, Long userId) {
        return session.createQuery(
                        """
                        select r
                        from Registration r
                        join fetch r.event e
                        join fetch e.createdBy
                        join fetch r.user u
                        where e.id = :eventId
                          and u.id = :userId
                        """,
                        Registration.class
                )
                .setParameter("eventId", eventId)
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }

    public Optional<Registration> findByValidationToken(Session session, String validationToken) {
        return session.createQuery(
                        """
                        select r
                        from Registration r
                        join fetch r.event e
                        join fetch e.createdBy
                        join fetch r.user u
                        where r.validationToken = :validationToken
                        """,
                        Registration.class
                )
                .setParameter("validationToken", validationToken)
                .uniqueResultOptional();
    }

    public List<Registration> findByEventId(Session session, Long eventId) {
        return session.createQuery(
                        """
                        select r
                        from Registration r
                        join fetch r.event e
                        join fetch e.createdBy
                        join fetch r.user u
                        where e.id = :eventId
                        order by r.createdAt asc
                        """,
                        Registration.class
                )
                .setParameter("eventId", eventId)
                .list();
    }

    public List<Registration> findActiveByUserId(Session session, Long userId) {
        return session.createQuery(
                        """
                        select r
                        from Registration r
                        join fetch r.event e
                        join fetch e.createdBy
                        join fetch r.user u
                        where u.id = :userId
                          and r.status = :status
                        order by e.startsAt asc
                        """,
                        Registration.class
                )
                .setParameter("userId", userId)
                .setParameter("status", RegistrationStatus.ACTIVE)
                .list();
    }

    public long countActiveByEventId(Session session, Long eventId) {
        return session.createQuery(
                        """
                        select count(r.id)
                        from Registration r
                        where r.event.id = :eventId
                          and r.status = :status
                        """,
                        Long.class
                )
                .setParameter("eventId", eventId)
                .setParameter("status", RegistrationStatus.ACTIVE)
                .getSingleResult();
    }

    public void save(Session session, Registration registration) {
        if (registration.getId() == null) {
            session.persist(registration);
            return;
        }
        session.merge(registration);
    }
}
