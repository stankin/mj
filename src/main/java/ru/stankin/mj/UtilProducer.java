package ru.stankin.mj;

import com.sun.org.apache.xpath.internal.SourceTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.sql2o.Sql2o;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by nickl on 07.01.15.
 */
@ApplicationScoped
public class UtilProducer {

    Logger log = LogManager.getLogger(UtilProducer.class);


    ExecutorService executorService = Executors.newCachedThreadPool();


    @PostConstruct
    public void initDatabase() throws IOException {
        log.info("Running Flyway to init database ");


        Flyway flyway = new Flyway();
        flyway.setLocations("classpath:/sql");
        flyway.setDataSource(dataSource);

        Properties properties = new Properties();
        InputStream flywayProps = getClass().getClassLoader().getResourceAsStream("flyway.properties");
        if (flywayProps != null) {
            properties.load(flywayProps);
            String cleanup = properties.getProperty("flyway.cleandb");
            log.debug("cleanDbattr = {}", cleanup);
            if ("true".equalsIgnoreCase(cleanup))
                {
                    log.info("cleaningdb");
                    flyway.clean();
                }

        }


        flyway.migrate();
        log.info("Flyway database migration is done");

    }


    @Resource(lookup = "java:jboss/datasources/mj2")
    DataSource dataSource;


    @Produces
    public ExecutorService defaultExecutorService(){
        System.out.println("getting executor service "+ this);
        return executorService;
    }

    @Produces
    @Default
    public DataSource defaultDataSource() {
        return dataSource;
    }

    @Produces
    public Sql2o defaultSql2o() {
        return new Sql2o(dataSource);
    }


}
