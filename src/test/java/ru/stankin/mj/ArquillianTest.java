package ru.stankin.mj;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import ru.stankin.mj.http.HttpApi2;
import ru.stankin.mj.model.*;
import ru.stankin.mj.model.user.User;
import ru.stankin.mj.rested.security.ShiroConfiguration;
import ru.stankin.mj.testutils.InWeldTest;
import ru.stankin.mj.utils.FlywayMigrations;
import ru.stankin.mj.view.AccountWindow;
import ru.stankin.mj.model.user.UserDAO;

import javax.inject.Inject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .addPackage(Package.getPackage("ru.stankin.mj.model.user"))
                .addPackage(AccountWindow.class.getPackage())
                .addPackage(HttpApi2.class.getPackage())
                .addPackage(ShiroConfiguration.class.getPackage())
                .addPackage(FlywayMigrations.class.getPackage())
                .addAsResource("log4j2-test.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
                .addAsWebResource(new File("src/main/webapp/index.html"))
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new StringAsset("flyway.cleandb = true"), "flyway.properties");

        Files.list(Paths.get("src/main/resources/sql"))
                .forEach(p -> jar.addAsResource(p.toFile(), "/sql/" + p.getFileName()));


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
    Sql2o sql2;

    @Inject
    AuthenticationsStore auths;


    private <T> T connect(Function<Connection, T> op) {
        try (Connection connection = sql2.open().setRollbackOnException(false)) {
            return op.apply(connection);
        }
    }

    private long connectLong(ToLongFunction<Connection> op) {
        try (Connection connection = sql2.open().setRollbackOnException(false)) {
            return op.applyAsLong(connection);
        }
    }

    private void refresh(Student s) {
        Student student = storage.getStudentById(s.id, s.getModules().get(0).getSubject().getSemester());
        s.cardid = student.cardid;
        s.name = student.name;
        s.initials = student.initials;
        s.surname = student.surname;
        s.stgroup = student.stgroup;
        s.patronym = student.patronym;
        s.setGroups(student.getGroups());
        s.setModules(student.getModules());
    }


    @Test
    @InSequence(1)
    public void uploadWrongEtalon() throws Exception {
        //        sql2.joinTransaction();
        mj.updateStudentsFromExcel("2014-осень", loadResource("/newEtalon-joinedGroups.xls"));

        try (Stream<Student> students = storage.getStudents()) {
            Assert.assertEquals(1750, students.count());
        }
        Assert.assertEquals(1750, connectLong(c -> c.createQuery("SELECT count(g) FROM groupshistory g").executeScalar(Integer.class)));
        Assert.assertEquals(1750, connectLong(c -> c.createQuery("SELECT count(g) FROM groupshistory g WHERE g.semestr = '2014-осень'").executeScalar(Long.class)));
    }

    @Test
    @InSequence(1)
    public void uploadRightEtalon() throws Exception {
        //        sql2.joinTransaction();
        mj.updateStudentsFromExcel("2014-осень", loadResource("/newEtalon.xls"));
        //        sql2.joinTransaction();
        //mj.updateStudentsFromExcel(ModuleJournalUploaderTest.class.getResourceAsStream("/Эталон на 21.10.2014.xls"));

        try (Stream<Student> students = storage.getStudents()) {
            Assert.assertEquals(1753, students.count());
        }
        Assert.assertEquals(1753, connectLong(c -> c.createQuery("SELECT count(g) FROM groupshistory g").executeScalar(Long.class)));
        Assert.assertEquals(1753, connectLong(c -> c.createQuery("SELECT count(g) FROM groupshistory g WHERE g.semestr = '2014-осень'").executeScalar(Long.class)));
    }

    @Test
    @InSequence(3)
    public void uploadSemesterModules() throws Exception {
        //        sql2.joinTransaction();
        mj.updateMarksFromExcel("2014-осень", loadResource("/information_items_property_2349.xls"));
        Assert.assertEquals(4067, connectLong(c -> c.createQuery("SELECT count(m) FROM modules m").executeScalar(Long.class)));
        Assert.assertEquals(4067, connectLong(c -> c.createQuery("SELECT count(m) FROM modules m JOIN subjects s ON m.subject_id = s.id WHERE s.semester = '2014-осень'").executeScalar(Long.class)));
        {
            Student s1 = storage.getStudentByGroupSurnameInitials("2014-осень", "ИДБ-13-14", "Наумова", "Р.В.");
            s1 = storage.getStudentById(s1.id, "2014-осень");
            Assert.assertEquals(new TreeSet<>(asList("2014-осень")), storage.getStudentSemestersWithMarks(s1.id));
            Assert.assertEquals(30, s1.getModules().size());
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-осень")).count());
        }
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

        User idb1316Student = userDAO.getUserBy("114513"); // Богданова	Устина	Кирилловна

        auths.updatePassword(idb1316Student.getId(),"nonDefaultPassword" );
        userDAO.saveUser(idb1316Student);

        User idb1316StudentWithNewPassword = userDAO.getUserBy("114513", "nonDefaultPassword");
        Assert.assertEquals(((Student) idb1316Student).id, ((Student) idb1316StudentWithNewPassword).id);
        Assert.assertNull(userDAO.getUserBy("114513", "114513"));

        mj.updateStudentsFromExcel("2014-осень", loadResource("/newEtalon.xls"));

        User idb1316StudentWithNewPasswordAfterEtalonUpdate = userDAO.getUserBy("114513", "nonDefaultPassword");
        Assert.assertEquals(((Student) idb1316Student).id, ((Student) idb1316StudentWithNewPasswordAfterEtalonUpdate).id);


    }

    @Test
    @InSequence(6)
    public void testNewEtalon() throws Exception {


        Student studentWithOldGroup = (Student) userDAO.getUserBy("114513");
        Assert.assertEquals("ИДБ-13-16", studentWithOldGroup.stgroup);

        Assert.assertEquals(1, storage.getStudentSemestersWithMarks(studentWithOldGroup.id).size());
        Student oldSemestrStudent = storage.getStudentById(studentWithOldGroup.id, "2014-осень");
        int EXPECTED_MODULES_IN_2014_1 = 31;
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrStudent.getModules().size());
        Assert.assertEquals(1, oldSemestrStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());
        Assert.assertEquals("Student has his group in history: " + oldSemestrStudent.getGroups(),
                new TreeSet<>(asList("ИДБ-13-16")),
                oldSemestrStudent.getGroups().stream().map(s -> s.groupName).collect(Collectors.toCollection(TreeSet<String>::new)));


        // Student switches group
        mj.updateStudentsFromExcel("2014-2", loadResource("/newEtalon-joinedGroups.xls"));

        Student studentWithNewGroup = (Student) userDAO.getUserBy("114513");
        Assert.assertEquals("ИДБ-13-15", studentWithNewGroup.stgroup);

        Assert.assertEquals("Students have the same id", studentWithNewGroup.id, studentWithOldGroup.id);
         Assert.assertTrue("And nonDefaultPassword", auths.acceptPassword(studentWithNewGroup.id, "nonDefaultPassword"));
        Assert.assertEquals("Student have both groups in history",
                new TreeSet<>(asList("ИДБ-13-15", "ИДБ-13-16")),
                studentWithNewGroup.getGroups().stream().map(s -> s.groupName).collect(Collectors.toCollection(TreeSet<String>::new)));

        Assert.assertEquals(1, storage.getStudentSemestersWithMarks(studentWithNewGroup.id).size());
        Student oldSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-осень");
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrNewStudent.getModules().size());
        Assert.assertEquals("expect one \"54\" mark in previous journal", 1, oldSemestrNewStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());

        // Uploading updated Marks for previous and new semester
        List<String> updateMarksFromExcelReport = mj.updateMarksFromExcel("2014-осень", loadResource("/information_items_property_2349_idb-13-16-updated.xls"));

        Optional<String> reportAboutStudent = updateMarksFromExcelReport.stream().filter(s -> s.contains(oldSemestrStudent.surname)).findAny();
        Assert.assertFalse("not expect any errors about " + oldSemestrStudent.surname + " but got: " + reportAboutStudent, reportAboutStudent.isPresent());
        mj.updateMarksFromExcel("2014-2", loadResource("/2 курс II семестр 2014-2015.xls"));

        Assert.assertEquals("old semester uploaded but without some students", 3977, connectLong(c -> c.createQuery("SELECT count(m) FROM modules m WHERE m.subject_id IN (SELECT id FROM subjects WHERE semester = '2014-осень')").executeScalar(Long.class)));
        Assert.assertEquals("new semester uploaded", 3573, connectLong(c -> c.createQuery("SELECT count(m) FROM modules m WHERE m.subject_id IN (SELECT id FROM subjects WHERE semester = '2014-2')").executeScalar(Long.class)));


        // Expect updating marks for previous semestr
        oldSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-осень");
        Assert.assertEquals(EXPECTED_MODULES_IN_2014_1, oldSemestrNewStudent.getModules().size());
        // Uploading Marks for previous semester with updates for this student
        Assert.assertEquals("expect ten \"54\" marks in updated prev semestr journal", 10, oldSemestrNewStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());


        Assert.assertEquals("Expect new marks for new semestr in database for " + studentWithNewGroup.id, 29,
                connectLong(c -> c.createQuery("SELECT count(m) FROM modules m WHERE m.subject_id IN (SELECT id FROM subjects WHERE semester = '2014-2') AND m.student_id = :student").addParameter("student", studentWithNewGroup.id).executeScalar(Long.class))
        );

        Student newSemestrNewStudent = storage.getStudentById(studentWithNewGroup.id, "2014-2");
        Assert.assertEquals("Expect new marks for new semestr", 29, newSemestrNewStudent.getModules().size());
        // Uploading Marks for previous semester with updates for this student
        Assert.assertEquals("expect 3 \"54\" marks in new semestr journal", 3, newSemestrNewStudent.getModules().stream()
                .filter(m -> m.getValue() == 54).count());

    }

    @Test
    @InSequence(7)
    public void testUploadNewSemesterModules() throws Exception {
        storage.deleteAllModules("2014-2");
//        sql2.joinTransaction();
        {
            Student s0 = storage.getStudentByGroupSurnameInitials("2014-осень", "ИДБ-13-14", "Наумова", "Р.В.");
            Assert.assertNotNull(s0);
            Student s1 = storage.getStudentById(s0.id, "2014-осень");
            List<Module> allModules = connect(c -> c.createQuery("SELECT m.*, m.student_id as studentId FROM modules m").throwOnMappingFailure(false).executeAndFetch(Module.class));
            Assert.assertFalse("",allModules.stream().anyMatch(m -> m.studentId == 0));
            Assert.assertEquals(3977, allModules.size());
            Assert.assertEquals(30, allModules.stream().filter(m -> m.studentId == s1.id).count());
            refresh(s1);
            Assert.assertEquals(30, s1.getModules().stream().filter(m -> m.getSubject().getSemester().equals("2014-осень")).count());
        }

        //        sql2.joinTransaction();
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
            connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=utf-8");
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
            Assert.assertEquals(2428, connection.getContentLength());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF8"));

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }

            connection.disconnect();
        }


    }

    public InputStream loadResource(String name) {
        return ArquillianTest.class.getResourceAsStream(name);
    }

}
