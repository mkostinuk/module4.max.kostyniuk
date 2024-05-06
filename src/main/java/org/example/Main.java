package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.example.dao.CityDao;
import org.example.dao.CountryDao;
import org.example.entities.CityEntity;
import org.example.entities.CountryEntity;
import org.example.entities.CountryLanguageEntity;
import org.example.redis.CityCountry;
import org.example.redis.Language;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final ObjectMapper mapper;
    private final CityDao cityDao;
    private final CountryDao countryDao;

    public Main() {
        mapper = new ObjectMapper();
        this.sessionFactory = prepareRelationalDb();
        cityDao = new CityDao(sessionFactory);
        countryDao = new CountryDao(sessionFactory);
        redisClient = prepareRedisClient();
    }

    public static void main(String[] args) {
        Main main = new Main();
        List<CityEntity> allCities = main.fetchData(main);
        List<CityCountry> preparedData = main.transformData(allCities);
        main.pushToRedis(preparedData);

        main.sessionFactory.getCurrentSession().close();

        List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

        long startRedis = System.currentTimeMillis();
        main.testRedisData(ids);
        long stopRedis = System.currentTimeMillis();

        long startMysql = System.currentTimeMillis();
        main.testMysqlData(ids);
        long stopMysql = System.currentTimeMillis();

        System.out.printf("%s:\t%d ms\n", "Redis", (stopRedis - startRedis));
        System.out.printf("%s:\t%d ms\n", "MySQL", (stopMysql - startMysql));

        main.shutDown();
    }


    private List<CityCountry> transformData(List<CityEntity> allCities) {
        return allCities.stream().map(city -> {
            CityCountry res = new CityCountry();
            res.setId(city.getId());
            res.setName(city.getName());
            res.setPopulation(city.getPopulation());
            res.setDistrict(city.getDistrict());

            CountryEntity country = city.getCountry();
            res.setContinent(country.getContinent());
            res.setCountryCode(country.getCode());
            res.setCountryPopulation(country.getPopulation());
            res.setCountryCode(country.getCode());
            res.setSecondCountryCode(country.getSecondCode());
            res.setCountrySurfaceArea(country.getSurfaceArea());
            res.setCountryRegion(country.getRegion());
            Set<CountryLanguageEntity> countryLanguages = country.getLanguages();
            Set<Language> languages = countryLanguages.stream().map(cl -> {
                Language language = new Language();
                language.setLanguage(cl.getLanguage());
                language.setPercentage(cl.getPercentage());
                language.setIsOfficial(cl.getIsOfficial());
                return language;
            }).collect(Collectors.toSet());
            res.setLanguages(languages);
            return res;

        }).collect(Collectors.toList());

    }

    private void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                try {
                    mapper.readValue(value, CityCountry.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                CityEntity city = cityDao.getById(id);
                Set<CountryLanguageEntity> languages = city.getCountry().getLanguages();

            }
            session.getTransaction().commit();
        }
    }

    private SessionFactory prepareRelationalDb() {
        final SessionFactory sessionFactory;
        Properties properties = new Properties();
        properties.put(Environment.USER, System.getenv("user"));
        properties.put(Environment.PASS, System.getenv("pass"));
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3306/world");
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.HBM2DDL_AUTO, "validate");
        properties.put(Environment.STATEMENT_BATCH_SIZE, "100");
        sessionFactory = new Configuration().addAnnotatedClass(CityEntity.class).addAnnotatedClass(CountryEntity.class).addAnnotatedClass(CountryLanguageEntity.class).setProperties(properties).buildSessionFactory();
        return sessionFactory;
    }

    private void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connect = redisClient.connect()) {
            RedisCommands<String, String> sync = connect.sync();
            for (CityCountry cityCountry : data) {
                sync.set(String.valueOf(cityCountry.getId()), mapper.writeValueAsString(cityCountry));

            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();

        }
    }

    private void shutDown() {
        if (nonNull(sessionFactory)) {
            sessionFactory.close();
        }
        if (nonNull(redisClient)) {
            redisClient.close();
        }
    }

    private RedisClient prepareRedisClient() {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("Connected to Redis \n");
        }
        return redisClient;
    }

    private List<CityEntity> fetchData(Main main) {
        try (Session session = main.sessionFactory.getCurrentSession()) {
            List<CityEntity> allCities = new ArrayList<>();
            session.beginTransaction();
            List<CountryEntity> allCountries = countryDao.getAll();
            int totalCount = main.cityDao.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(main.cityDao.getItems(i, step));
            }
            session.getTransaction().commit();
            return allCities;
        }

    }
}