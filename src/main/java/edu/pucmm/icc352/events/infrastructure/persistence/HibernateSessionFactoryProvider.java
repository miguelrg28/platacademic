package edu.pucmm.icc352.events.infrastructure.persistence;

import edu.pucmm.icc352.events.config.DatabaseSettings;
import edu.pucmm.icc352.events.domain.model.Event;
import edu.pucmm.icc352.events.domain.model.Registration;
import edu.pucmm.icc352.events.domain.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public final class HibernateSessionFactoryProvider {
    public SessionFactory build(DatabaseSettings settings) {
        Properties properties = new Properties();
        properties.put(AvailableSettings.DRIVER, "org.h2.Driver");
        properties.put(AvailableSettings.URL, settings.jdbcUrl());
        properties.put(AvailableSettings.USER, settings.username());
        properties.put(AvailableSettings.PASS, settings.password());
        properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
        properties.put(AvailableSettings.HBM2DDL_AUTO, "validate");
        properties.put(AvailableSettings.SHOW_SQL, "false");
        properties.put(AvailableSettings.FORMAT_SQL, "false");
        properties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "25");
        properties.put(AvailableSettings.ORDER_INSERTS, "true");
        properties.put(AvailableSettings.ORDER_UPDATES, "true");

        Configuration configuration = new Configuration();
        configuration.setProperties(properties);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Event.class);
        configuration.addAnnotatedClass(Registration.class);
        return configuration.buildSessionFactory();
    }
}
