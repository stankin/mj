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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

    @PersistenceContext
    private EntityManager em;

    @Test
    public void testUploadNewSemesterModules() throws Exception {

        utx.begin();
//        em.joinTransaction();

        mj.updateStudentsFromExcel(loadResource("/newEtalon.xls"));

        utx.commit();
        utx.begin();
//        em.joinTransaction();
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        Assert.assertEquals(1753, storage.getStudents().count());
        utx.commit();

        utx.begin();
//        em.joinTransaction();
        mj.updateMarksFromExcel("2014-1", loadResource("/information_items_property_2349.xls"));
        Assert.assertEquals(10922, em.createQuery("select count(m) from Module m", Long.class).getSingleResult().intValue());
        {
            Student s1 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());
        }
        utx.commit();
        utx.begin();
//        em.joinTransaction();
        mj.updateMarksFromExcel("2014-1", loadResource("/information_items_property_2349.xls"));
        Assert.assertEquals(10922, em.createQuery("select count(m) from Module m", Long.class).getSingleResult().intValue());
        //storage.
        utx.commit();
        utx.begin();
//        em.joinTransaction();
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-1");
            List<Module> allModules = em.createQuery("select m from Module m", Module.class).getResultList();
            Assert.assertEquals(10922, allModules.size());
            Assert.assertEquals(30, allModules.stream().filter(m -> m.getStudent().equals(s1)).count());
            em.refresh(s1);
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());
        }
        utx.commit();

        utx.begin();
//        em.joinTransaction();
        mj.updateMarksFromExcel("2014-2", loadResource("/2 курс II семестр 2014-2015.xls"));
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-2");
            Assert.assertEquals(33, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-2")).count());
        }
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-1");
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());
        }

        utx.commit();

    }

    public InputStream loadResource(String name) {
        return ModuleJournalUploaderTest.class.getResourceAsStream(name);
    }

}
