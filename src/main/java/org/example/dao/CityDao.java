package org.example.dao;


import org.example.entities.CityEntity;
import org.hibernate.SessionFactory;

import java.util.List;


public class CityDao {
    private final SessionFactory sessionFactory;

    public CityDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<CityEntity> getItems(int offset, int limit) {
        return sessionFactory.getCurrentSession().
                createQuery("from CityEntity c ", CityEntity.class).
                setFirstResult(offset).
                setMaxResults(limit).list();
    }

    public int getTotalCount() {
        return Math.toIntExact(sessionFactory.getCurrentSession().
                createQuery("select count(c) from  CityEntity c ", Long.class).
                uniqueResult());
    }

    public CityEntity getById(Integer id) {
        return sessionFactory.getCurrentSession().
                createQuery("select c from CityEntity  c join fetch c.country where c.id = :ID", CityEntity.class).
                setParameter("ID", id).uniqueResult();
    }

}

