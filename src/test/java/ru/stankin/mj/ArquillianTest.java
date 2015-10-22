package ru.stankin.mj;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.stankin.mj.model.*;
import ru.stankin.mj.view.AccountWindow;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
                .addPackage(AccountWindow.class.getPackage())
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsResource("log4j2-test.xml")
                .addAsWebInfResource("jbossas-ds.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
                .addAsWebResource(new File("src/main/webapp/index.html"))
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
    @InSequence(1)
    public void uploadEtalon() throws Exception {
        utx.begin();
//        em.joinTransaction();
        mj.updateStudentsFromExcel(loadResource("/newEtalon.xls"));
        utx.commit();
        utx.begin();
//        em.joinTransaction();
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        Assert.assertEquals(1753, storage.getStudents().count());
        utx.commit();
    }

    @Test
    @InSequence(2)
    public void uploadSemesterModules() throws Exception {
        utx.begin();
//        em.joinTransaction();
        mj.updateMarksFromExcel("2014-1", loadResource("/information_items_property_2349.xls"));
        Assert.assertEquals(4067, em.createQuery("select count(m) from Module m", Long.class).getSingleResult().intValue());
        {
            Student s1 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Assert.assertEquals(new TreeSet<>(Arrays.asList("2014-1")), storage.getStudentSemesters(s1.id));
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());
        }
        utx.commit();
    }

    @Test
    @InSequence(3)
    public void uploadSemesterModulesAgain() throws Exception {
        uploadSemesterModules();
    }

    @Inject
    UserDAO userDAO;

    @Test
    @InSequence(4)
    public void testPasswordChange() throws Exception {

        utx.begin();
        User idb1316Student = userDAO.getUserBy("114513", "114513");
        idb1316Student.setPassword("nonDefaultPassword");
        userDAO.saveUser(idb1316Student);
        utx.commit();

        utx.begin();
        User idb1316StudentWithNewPassword = userDAO.getUserBy("114513", "nonDefaultPassword");
        Assert.assertEquals(((Student) idb1316Student).id, ((Student) idb1316StudentWithNewPassword).id);
        Assert.assertNull(userDAO.getUserBy("114513", "114513"));

        mj.updateStudentsFromExcel(loadResource("/newEtalon.xls"));
        utx.commit();

        utx.begin();
        User idb1316StudentWithNewPasswordAfterEtalonUpdate = userDAO.getUserBy("114513", "nonDefaultPassword");
        Assert.assertEquals(((Student) idb1316Student).id, ((Student) idb1316StudentWithNewPasswordAfterEtalonUpdate).id);
        utx.commit();


    }

    @Test
    @InSequence(5)
    public void testNewEtalon() throws Exception {


        utx.begin();
        Student studentWithOldGroup = (Student) userDAO.getUserBy("114513");
        Assert.assertEquals("ИДБ-13-16", studentWithOldGroup.stgroup);

        Assert.assertEquals(1, storage.getStudentSemesters(studentWithOldGroup.id).size());
        Student oldSemestrStudent = storage.getStudentById(studentWithOldGroup.id, "2014-1");
        int EXPECTED_MODULES_IN_2014_1 = 31;
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrStudent.getModules().size());



        // Student switches group
        mj.updateStudentsFromExcel(loadResource("/newEtalon-joinedGroups.xls"));
        utx.commit();

        utx.begin();
        Student studentWithNewGroup = (Student) userDAO.getUserBy("114513");
        Assert.assertEquals("ИДБ-13-15", studentWithNewGroup.stgroup);

        Assert.assertEquals("Students have the same id", studentWithNewGroup.id, studentWithOldGroup.id);
        Assert.assertEquals("Students have the same password", studentWithNewGroup.password, studentWithOldGroup.password);
        Assert.assertEquals("And it is", "nonDefaultPassword", studentWithNewGroup.password);

        Assert.assertEquals(1, storage.getStudentSemesters(studentWithNewGroup.id).size());
        Student oldSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-1");
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrNewStudent.getModules().size());

        utx.commit();

    }

    @Test
    @InSequence(6)
    public void testUploadNewSemesterModules() throws Exception {
        utx.begin();
//        em.joinTransaction();
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-1");
            List<Module> allModules = em.createQuery("select m from Module m", Module.class).getResultList();
            Assert.assertEquals(3977, allModules.size());
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
            Student s2 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Новикова", "Х.Ф.");
            Student s1 = storage.getStudentById(s0.id, "2014-2");
            Assert.assertEquals(new TreeSet<>(Arrays.asList("2014-1", "2014-2")), storage.getStudentSemesters(s1.id));
            Assert.assertEquals(new TreeSet<>(Arrays.asList("2014-1")), storage.getStudentSemesters(s2.id));
            Assert.assertEquals(33, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-2")).count());
        }
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-1");
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());
        }
        storage.deleteAllModules("2014-2");
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-1");
            Assert.assertEquals(new TreeSet<>(Arrays.asList("2014-1")), storage.getStudentSemesters(s1.id));
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-1")).count());
        }

        utx.commit();

    }

    @ArquillianResource
    URL url;

    @Test
    @InSequence(1000)
    public void VaadinSmokeTest() throws Exception {
        //System.out.println(url);
        {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            Assert.assertEquals(3885, connection.getContentLength());
            connection.disconnect();
        }
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url + "/VAADIN/vaadinBootstrap.js").openConnection();
            Assert.assertEquals(1671, connection.getContentLength());
            connection.disconnect();
        }
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url + "/VAADIN/widgetsets/ru.stankin.mj.WidgetSet/ru.stankin.mj.WidgetSet.nocache.js").openConnection();
            Assert.assertEquals(1707, connection.getContentLength());
            connection.disconnect();
        }

    }

    public InputStream loadResource(String name) {
        return ModuleJournalUploaderTest.class.getResourceAsStream(name);
    }

}
