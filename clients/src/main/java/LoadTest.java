import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.joda.time.DateTime;
import uk.co.malbec.hound.Hound;
import uk.co.malbec.hound.OperationException;
import uk.co.malbec.hound.OperationType;
import uk.co.malbec.hound.Transition;
import uk.co.malbec.hound.reporter.HtmlReporter;
import uk.co.malbec.hound.sampler.HybridSampler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import static java.lang.String.format;
import static java.util.stream.IntStream.range;
import static org.joda.time.DateTime.now;

public class LoadTest {

    private static Random randomGenerator = new Random();

    public enum GridFsOperationType implements OperationType {
        GOTO_INDEX_PAGE, UPLOAD_FILE
    }

    public static void main(String[] args) throws IOException {

        File reportsDirectory = new File(format("%s/reports/%s", System.getProperty("user.dir"), System.currentTimeMillis()));


        Hound<GridFsUser> hound = new Hound<GridFsUser>()
                .shutdownTime(now().plusMinutes(120));

        hound.configureSampler(HybridSampler.class)
                .setSampleDirectory(new File(reportsDirectory, "data"));

        hound.configureReporter(HtmlReporter.class)
                .setReportsDirectory(reportsDirectory)
                .setDescription("GridFs file upload test")
                .addBulletPoint("100 users, uploading files every 2 seconds")
                .addBulletPoint("No write concern set")
                .addBulletPoint("No replication")
                .addBulletPoint("Connection timeouts set at 5 seconds")
                .addBulletPoint("Running on 2.9 Ghz laptop with 16GB of memory and 2Gbit/s SSD disk")
                .addBulletPoint("Mongo, application server and test client on single machine")
                .setExecuteTime(now());

        configureOperations(hound);

        range(0, 100).forEach(i -> {

            Client client = JerseyClientBuilder.newBuilder()
                    .register(MultiPartFeature.class).build();

            client.property(ClientProperties.CONNECT_TIMEOUT, 5000);
            client.property(ClientProperties.READ_TIMEOUT, 5000);

            WebTarget target = client.target("http://localhost:8080");

            hound
                    .createUser(new GridFsUser(i))
                    .registerSupplier(WebTarget.class, () -> target)
                    .start("user" + i, new Transition(GridFsOperationType.GOTO_INDEX_PAGE, now()));
        });


        hound.waitFor();

        new ProcessBuilder("open", reportsDirectory.getAbsolutePath() + "/index.html").start();
    }


    private static void configureOperations(Hound<GridFsUser> hound) {
        hound
                .register(GridFsOperationType.GOTO_INDEX_PAGE, WebTarget.class, (gridFsWebTarget, context) -> {
                    try {
                        Response response = gridFsWebTarget.path("/ui/").request().get();
                        if (response.getStatus() != 200) {
                            throw new OperationException("upload file", "unexpected response code");
                        }

                    } finally{
                        context.schedule(new Transition(GridFsOperationType.UPLOAD_FILE, now().plusSeconds(1)));
                    }
                })
                .register(GridFsOperationType.UPLOAD_FILE, WebTarget.class, (gridFsWebTarget, context) -> {

                    try {
                        MultiPart multiPart = new MultiPart().bodyPart(new FileDataBodyPart(
                                "file",
                                new File("testData/" + randomGenerator.nextInt(10) + ".jpg"),
                                MediaType.APPLICATION_OCTET_STREAM_TYPE
                        ));

                        Response response = gridFsWebTarget.path("/files")
                                .request()
                                .post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA));

                        if (response.getStatus() != 200) {
                            String body = response.readEntity(String.class);
                            throw new OperationException("upload file", body);
                        }
                    } finally {
                        context.schedule(new Transition(GridFsOperationType.GOTO_INDEX_PAGE, now().plusSeconds(1)));
                    }

                });

    }

    public static class GridFsUser {

        private int index;

        public GridFsUser(int index) {
            this.index = index;
        }

    }
}
