import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.transform.Field;
import org.apache.commons.collections.Factory;
import ru.stankin.mj.model.Student;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Field String randusLoadedFileName = "usersFromRanduss1.json"

@Field String outFileName = "additionalUsers.json"

public class RandusEntry {

    public String lname;
    public String fname;
    public String patronymic;
    public String date;
    public String city;
    public String street;
    public String phone;
    public int house;
    public int apartment;
    public String login;
    public String password;
    public String color;
    public String userpic;

    @Override
    public String toString() {
        return "RandusEntry{" +
                "apartment=" + apartment +
                ", lname='" + lname + '\'' +
                ", fname='" + fname + '\'' +
                ", patronymic='" + patronymic + '\'' +
                ", date='" + date + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", phone='" + phone + '\'' +
                ", house=" + house +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", color='" + color + '\'' +
                ", userpic='" + userpic + '\'' +
                '}';
    }

    public String gender() {
        char c = userpic.charAt(25);
        //System.err.println("c="+c + " "+userpic);
        switch (c) {
            case 'w': return "ж";
            case 'm': return "м";
        }
        throw new IllegalArgumentException("unknown char " + c);
    }

}


@Field ObjectMapper mapper = new ObjectMapper();

mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


String phone(Random random) {

    def d = { random.nextInt(10) + "" };

    return Stream.of(
            "(", d(), d(), d(), ") ",
            d(), d(), d(), "-", d(), d(), "-", d(), d()).collect(Collectors.joining());


}

String birth(Random random) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    ZonedDateTime zonedDateTime = ZonedDateTime.of(1994, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime maxDate = ZonedDateTime.of(1998, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    while (true) {

        int v = (int) (random.nextGaussian() * 1000);
        ZonedDateTime birthDate = zonedDateTime.plusDays(v);
        if (birthDate.isAfter(maxDate))
            continue;

        return formatter.format(birthDate);
    }


}


Stream<RandusEntry> savedRanduses() throws FileNotFoundException {
    BufferedReader reader = new BufferedReader(new FileReader(randusLoadedFileName));
    return reader.lines().onClose {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }.map { line ->
        try {
            return mapper.readValue(line, RandusEntry.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
}

private void genUsers() throws IOException {
    PrintWriter out = new PrintWriter(randusLoadedFileName)
    out.withCloseable {

        for (int i = 0; i < 3000; i++) {
            System.out.println("getting user " + i);
            URLConnection connection = new URL("http://randus.ru/api.php").openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            reader.close();

        }

    }
}

//genUsers()

Random random = new Random(1);

AtomicInteger studnum = new AtomicInteger(500000);

Stream<RandusEntry> randuss = savedRanduses()//.limit(1754).limit(50)

PrintWriter out = new PrintWriter(outFileName)
out.withCloseable {
    randuss.forEach { randus ->

        out.println(mapper.writeValueAsString([
                lname     : randus.lname,
                fname     : randus.fname,
                patronymic: randus.patronymic,
                initials  : Student.initialsFromNames(randus.fname, randus.patronymic),
                carid     : studnum.addAndGet(random.nextInt(3) + 1),
                homephone : phone(random),
                mobile    : phone(random),
                gender    : randus.gender(),
                birth     : birth(random)

        ]))

//    def stream = Stream.of(
//            randus.lname,
//            randus.fname,
//            randus.patronymic,
//            Student.initialsFromNames(randus.fname, randus.patronymic),
//            studnum.addAndGet(random.nextInt(3) + 1),
//            phone(random),
//            phone(random),
//            randus.gender(),
//            birth(random)
//    )
//
//    System.out.println(
//            stream.map { it.toString() }.collect(Collectors.joining(", "))
//
//    );

    };
    randuss.close()
    println("users saved to $outFileName")
}



