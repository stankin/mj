package ru.stankin.mj;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Created by nickl-mac on 12.09.15.
 */
public class UserGenerator {

    static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    private static String phone(Random random){

        java.util.function.Supplier<String> d = () -> random.nextInt(10) + "";

        return Stream.of(
                "(", d.get(),d.get(),d.get(),") ",
                d.get(),d.get(),d.get(), "-", d.get(),d.get(),"-",d.get(),d.get()).collect(Collectors.joining());


    }

    private static String birth(Random random){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        ZonedDateTime zonedDateTime = ZonedDateTime.of(1994, 5, 5, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime maxDate = ZonedDateTime.of(1998, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        while (true) {

            int v = (int) (random.nextGaussian() * 1000);
            ZonedDateTime birthDate = zonedDateTime.plusDays(v);
            if(birthDate.isAfter(maxDate))
                continue;

            return formatter.format(birthDate);
        }




    }


    public static void main(String[] args) throws Exception {


        Random random = new Random(1);

        AtomicInteger studnum = new AtomicInteger(114000);

        try(Stream<RandusEntry> randuss = savedRanduses().limit(1754).limit(50)) {

            randuss.forEach(randus-> {

                System.out.println(
                        Stream.of(
                                randus.lname,
                                randus.fname,
                                randus.patronymic,
                                Student.initialsFromNames(randus.fname, randus.patronymic),
                                studnum.addAndGet(random.nextInt(3) + 1),
                                phone(random),
                                phone(random),
                                randus.gender(),
                                birth(random)
                        ).map(Object::toString).collect(Collectors.joining(", "))

                );

            });
        }

    }

    private static Stream<RandusEntry> savedRanduses() throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader("users.json"));
        return reader.lines().onClose(() -> {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).map(line -> {
            try {
                return mapper.readValue(line, RandusEntry.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void genUsers() throws IOException {
        try (PrintWriter out = new PrintWriter("users.json")) {

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

}
