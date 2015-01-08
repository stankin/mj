package ru.stankin.mj;

import com.sun.org.apache.xpath.internal.SourceTree;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by nickl on 07.01.15.
 */
@ApplicationScoped
public class UtilProducer {


    ExecutorService executorService = Executors.newCachedThreadPool();


    @Produces
    public ExecutorService defaultExecutorService(){
        System.out.println("getting executor service "+ this);
        return executorService;
    }

}
