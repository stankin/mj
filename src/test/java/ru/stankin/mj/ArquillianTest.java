package ru.stankin.mj;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.stankin.mj.model.*;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(Arquillian.class)
public class ArquillianTest {

    @Deployment
    public static Archive<?> createDeployment() throws IOException {

        // Import Maven runtime dependencies
        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .importRuntimeDependencies().resolve().withTransitivity().asFile();


        WebArchive jar = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(files)
                .addPackage(Package.getPackage("ru.stankin.mj"))
                .addPackage(Package.getPackage("ru.stankin.mj.model"))
                //.addPackage(Package.getPackage("ru.stankin.mj.view"))
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsResource("log4j2-test.xml")
                .addAsWebInfResource("jbossas-ds.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        Files.list(Paths.get("src/test/resources"))
                .filter(p -> p.toString().endsWith(".xls"))
                .forEach(p -> jar.addAsResource(p.toFile()));

//        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
//                .addPackage(Package.getPackage("ru.stankin.mj.model"))
//                //.addPackages(true, "ru.stankin.mj")
//                //.addClasses(ModuleJournalUploader.class)
//                .addAsManifestResource("test-persistence.xml", "persistence.xml")
//                .addAsManifestResource("jbossas-ds.xml")
//                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        //.addAsManifestResource(new File("src/main/webapp/WEB-INF/beans.xml"), "beans.xml");
        System.out.println(jar.toString(true));
        return jar;
    }

    @Inject
    ModuleJournalUploader mj;

    @Inject
    Storage storage;

    @Inject
    UserTransaction utx;

    @Test
    public void testLoadStudentsList() throws Exception {

        utx.begin();

        mj.updateStudentsFromExcel(loadResource("/newEtalon.xls"));

        utx.commit();
        utx.begin();
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        Assert.assertEquals(1753, storage.getStudents().count());
        utx.commit();
        utx.begin();
        mj.updateMarksFromExcel("2014-1", loadResource("/information_items_property_2349.xls"));
//        utx.commit();
//
//        utx.begin();
        Student s1 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Недашковская", "Н.Я.");

        Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());

        utx.commit();

    }

    public InputStream loadResource(String name) {
        return ModuleJournalUploaderTest.class.getResourceAsStream(name);
    }

}
