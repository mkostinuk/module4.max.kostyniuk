package org.example.repositories;

import org.example.entities.CountryEntity;
import org.hibernate.SessionFactory;

import java.util.List;

public class CountryRepo {
    private final SessionFactory sessionFactory;

    public CountryRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<CountryEntity> getAll() {
        return sessionFactory.getCurrentSession().
                createQuery("select c from CountryEntity c join fetch c.languages", CountryEntity.class)
                .list();
    }
}
