package edu.pucmm.icc352.events.infrastructure.persistence;

import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.EventStatus;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class EventRepository {
    public Optional<Event> findById(Session session, Long id) {
        return session.createQuery(
                        "select e from Event e join fetch e.createdBy where e.id = :id",
                        Event.class
                )
                .setParameter("id", id)
                .uniqueResultOptional();
    }

    public List<Event> findAll(Session session) {
        return session.createQuery(
                        "select e from Event e join fetch e.createdBy order by e.startsAt desc",
                        Event.class
                )
                .list();
    }

    public List<Event> findAvailable(Session session, LocalDateTime now) {
        return session.createQuery(
                        """
                        select e
                        from Event e
                        join fetch e.createdBy
                        where e.status = :status
                          and e.startsAt >= :now
                        order by e.startsAt asc
                        """,
                        Event.class
                )
                .setParameter("status", EventStatus.PUBLISHED)
                .setParameter("now", now)
                .list();
    }

    public void save(Session session, Event event) {
        if (event.getId() == null) {
            session.persist(event);
            return;
        }
        session.merge(event);
    }

    public void delete(Session session, Event event) {
        session.remove(event);
    }
}
