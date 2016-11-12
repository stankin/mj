package ru.stankin.mj;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.stankin.mj.model.*;
import ru.stankin.mj.model.user.User;
import ru.stankin.mj.view.AccountWindow;
import ru.stankin.mj.model.user.UserDAO;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

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
    public void uploadWrongEtalon() throws Exception {
        utx.begin();
//        em.joinTransaction();
        mj.updateStudentsFromExcel("2014-осень", loadResource("/newEtalon-joinedGroups.xls"));
        utx.commit();
        utx.begin();

        Assert.assertEquals(1750, storage.getStudents().count());
        Assert.assertEquals(1750, em.createQuery("select count(g) from StudentHistoricalGroup g", Long.class).getSingleResult().intValue());
        Assert.assertEquals(1750, em.createQuery("select count(g) from StudentHistoricalGroup g where g.semestr = '2014-осень'", Long.class).getSingleResult().intValue());
        utx.commit();
    }

    @Test
    @InSequence(1)
    public void uploadRightEtalon() throws Exception {
        utx.begin();
//        em.joinTransaction();
        mj.updateStudentsFromExcel("2014-осень", loadResource("/newEtalon.xls"));
        utx.commit();
        utx.begin();
//        em.joinTransaction();
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        Assert.assertEquals(1753, storage.getStudents().count());
        Assert.assertEquals(1753, em.createQuery("select count(g) from StudentHistoricalGroup g", Long.class).getSingleResult().intValue());
        Assert.assertEquals(1753, em.createQuery("select count(g) from StudentHistoricalGroup g where g.semestr = '2014-осень'", Long.class).getSingleResult().intValue());
        utx.commit();
    }

    @Test
    @InSequence(3)
    public void uploadSemesterModules() throws Exception {
        utx.begin();
//        em.joinTransaction();
        mj.updateMarksFromExcel("2014-осень", loadResource("/information_items_property_2349.xls"));
        utx.commit();
        utx.begin();
        Assert.assertEquals(4067, em.createQuery("select count(m) from Module m", Long.class).getSingleResult().intValue());
        Assert.assertEquals(4067, em.createQuery("select count(m) from Module m where m.subject.semester = '2014-осень'", Long.class).getSingleResult().intValue());
        {
            Student s1 = storage.getStudentByGroupSurnameInitials("2014-осень", "ИДБ-13-14", "Наумова", "Р.В.");
            s1 = storage.getStudentById(s1.id, "2014-осень");
            Assert.assertEquals(new TreeSet<>(asList("2014-осень")), storage.getStudentSemestersWithMarks(s1.id));
            Assert.assertEquals(30, s1.getModules().size());
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-осень")).count());
        }
        utx.commit();
    }

    @Test
    @InSequence(4)
    public void uploadSemesterModulesAgain() throws Exception {
        uploadSemesterModules();
    }

    @Inject
    UserDAO userDAO;

    @Test
    @InSequence(5)
    public void testPasswordChange() throws Exception {

        utx.begin();
        User idb1316Student = userDAO.getUserBy("114513", "114513"); // Богданова	Устина	Кирилловна
        idb1316Student.setPassword("nonDefaultPassword");
        userDAO.saveUser(idb1316Student);
        utx.commit();

        utx.begin();
        User idb1316StudentWithNewPassword = userDAO.getUserBy("114513", "nonDefaultPassword");
        Assert.assertEquals(((Student) idb1316Student).id, ((Student) idb1316StudentWithNewPassword).id);
        Assert.assertNull(userDAO.getUserBy("114513", "114513"));

        mj.updateStudentsFromExcel("2014-осень", loadResource("/newEtalon.xls"));
        utx.commit();

        utx.begin();
        User idb1316StudentWithNewPasswordAfterEtalonUpdate = userDAO.getUserBy("114513", "nonDefaultPassword");
        Assert.assertEquals(((Student) idb1316Student).id, ((Student) idb1316StudentWithNewPasswordAfterEtalonUpdate).id);
        utx.commit();


    }

    @Test
    @InSequence(6)
    public void testNewEtalon() throws Exception {


        utx.begin();
        Student studentWithOldGroup = (Student) userDAO.getUserBy("114513");
        Assert.assertEquals("ИДБ-13-16", studentWithOldGroup.stgroup);

        Assert.assertEquals(1, storage.getStudentSemestersWithMarks(studentWithOldGroup.id).size());
        Student oldSemestrStudent = storage.getStudentById(studentWithOldGroup.id, "2014-осень");
        int EXPECTED_MODULES_IN_2014_1 = 31;
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrStudent.getModules().size());
        Assert.assertEquals(1, oldSemestrStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());
        Assert.assertEquals("Student has his group in history",
                new TreeSet<>(asList("ИДБ-13-16")),
                oldSemestrStudent.getGroups().stream().map(s -> s.groupName).collect(Collectors.toCollection(TreeSet<String>::new)));



        // Student switches group
        mj.updateStudentsFromExcel("2014-2", loadResource("/newEtalon-joinedGroups.xls"));
        utx.commit();

        utx.begin();
        Student studentWithNewGroup = (Student) userDAO.getUserBy("114513");
        Assert.assertEquals("ИДБ-13-15", studentWithNewGroup.stgroup);

        Assert.assertEquals("Students have the same id", studentWithNewGroup.id, studentWithOldGroup.id);
        Assert.assertEquals("Students have the same password", studentWithNewGroup.password, studentWithOldGroup.password);
        Assert.assertEquals("And it is", "nonDefaultPassword", studentWithNewGroup.password);
        Assert.assertEquals("Student have both groups in history",
                new TreeSet<>(asList("ИДБ-13-15", "ИДБ-13-16")),
                studentWithNewGroup.getGroups().stream().map(s -> s.groupName).collect(Collectors.toCollection(TreeSet<String>::new)));

        Assert.assertEquals(1, storage.getStudentSemestersWithMarks(studentWithNewGroup.id).size());
        Student oldSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-осень");
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrNewStudent.getModules().size());
        Assert.assertEquals("expect one \"54\" mark in previous journal", 1, oldSemestrNewStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());
        utx.commit();

        // Uploading updated Marks for previous and new semester
        utx.begin();
        List<String> updateMarksFromExcelReport = mj.updateMarksFromExcel("2014-осень", loadResource("/information_items_property_2349_idb-13-16-updated.xls"));

        Optional<String> reportAboutStudent = updateMarksFromExcelReport.stream().filter(s -> s.contains(oldSemestrStudent.surname)).findAny();
        Assert.assertFalse("not expect any errors about " + oldSemestrStudent.surname + " but got: " + reportAboutStudent, reportAboutStudent.isPresent());
        mj.updateMarksFromExcel("2014-2", loadResource("/2 курс II семестр 2014-2015.xls"));
        utx.commit();

        Assert.assertEquals("old semester uploaded but without some students", 3977, em.createQuery("select count(m) from Module m where m.subject.semester = '2014-осень'", Long.class).getSingleResult().intValue());
        Assert.assertEquals("new semester uploaded", 3573, em.createQuery("select count(m) from Module m where m.subject.semester = '2014-2'", Long.class).getSingleResult().intValue());


        // Expect updating marks for previous semestr
        oldSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-осень");
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrNewStudent.getModules().size());
        // Uploading Marks for previous semester with updates for this student
        Assert.assertEquals("expect ten \"54\" marks in updated prev semestr journal", 10, oldSemestrNewStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());


        Assert.assertEquals("Expect new marks for new semestr in database for " + studentWithNewGroup.id, 29,
                em.createQuery("select count(m) from Module m where m.subject.semester = '2014-2' and m.student = :student", Long.class)
                        .setParameter("student", studentWithNewGroup).getSingleResult().intValue());

        Student newSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-2");
        Assert.assertEquals("Expect new marks for new semestr", 29, newSemestrNewStudent.getModules().size());
        // Uploading Marks for previous semester with updates for this student
        Assert.assertEquals("expect 3 \"54\" marks in new semestr journal", 3, newSemestrNewStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());

    }

    @Test
    @InSequence(7)
    public void testUploadNewSemesterModules() throws Exception {
        utx.begin();
        storage.deleteAllModules("2014-2");
//        em.joinTransaction();
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("2014-осень", "ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-осень");
            List<Module> allModules = em.createQuery("select m from Module m", Module.class).getResultList();
            Assert.assertEquals(3977, allModules.size());
            Assert.assertEquals(30, allModules.stream().filter(m -> m.getStudent().equals(s1)).count());
            em.refresh(s1);
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-осень")).count());
        }
        utx.commit();

        utx.begin();
//        em.joinTransaction();
        mj.updateMarksFromExcel("2014-2", loadResource("/2 курс II семестр 2014-2015.xls"));
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("2014-2", "ИДБ-13-14", "Наумова", "Р.В.");
            Student s2 = storage.getStudentByGroupSurnameInitials("2014-2", "ИДБ-13-14", "Новикова", "Х.Ф.");
            Student s1 = storage.getStudentById(s0.id, "2014-2");
            Assert.assertEquals(new TreeSet<>(asList("2014-осень", "2014-2")), storage.getStudentSemestersWithMarks(s1.id));
            Assert.assertEquals(new TreeSet<>(asList("2014-осень")), storage.getStudentSemestersWithMarks(s2.id));
            Assert.assertEquals(33, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-2")).count());
        }
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("2014-2", "ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-осень");
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-осень")).count());
        }
        storage.deleteAllModules("2014-2");
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("2014-осень", "ИДБ-13-14", "Наумова", "Р.В.");
            Student s1 = storage.getStudentById(s0.id, "2014-осень");
            Assert.assertEquals(new TreeSet<>(asList("2014-осень")), storage.getStudentSemestersWithMarks(s1.id));
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-осень")).count());
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
            Assert.assertEquals(3883, connection.getContentLength());
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

    @Test
    @InSequence(1002)
    public void httpApi() throws Exception {
        //System.out.println(url);

        {
            HttpURLConnection connection = (HttpURLConnection) new URL(url + "webapi/api2/marks").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type","application/x-www-form-urlencoded; charset=utf-8");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF8");
            writer.append(URLEncoder.encode("student", "UTF-8"));
            writer.append("=");
            writer.append(URLEncoder.encode("114513", "UTF-8"));
            writer.append("&");
            writer.append(URLEncoder.encode("password", "UTF-8"));
            writer.append("=");
            writer.append(URLEncoder.encode("nonDefaultPassword", "UTF-8"));
            writer.append("&");
            writer.append(URLEncoder.encode("semester", "UTF-8"));
            writer.append("=");
            writer.append(URLEncoder.encode("2014-осень", "UTF-8"));
            writer.close();
            Assert.assertEquals(200, connection.getResponseCode());
            Assert.assertEquals(2025, connection.getContentLength());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF8"));

            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                System.out.println(line);
            }

            connection.disconnect();
        }


    }

    public InputStream loadResource(String name) {
        return ModuleJournalUploaderTest.class.getResourceAsStream(name);
    }

}
