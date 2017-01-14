import uk.co.malbec.hound.reporter.HtmlReporter;
import uk.co.malbec.hound.sampler.HybridSampler;

import java.io.File;
import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {
        new HybridSampler()
                .setDataDir(new File("/Users/robindevilliers/Documents/development/malbec/hound/clients/reports/1484342586431/data")
                )
                .stream().forEach(sample -> {
            System.out.println(sample.getEnd() - sample.getStart());
        });


        new HtmlReporter().setReportsDirectory(new File("/Users/robindevilliers/Documents/development/malbec/hound/clients/reports/test"))
                .generate(new HybridSampler()
                        .setDataDir(new File("/Users/robindevilliers/Documents/development/malbec/hound/clients/reports/1484342586431/data")));

        new ProcessBuilder("open", new File("/Users/robindevilliers/Documents/development/malbec/hound/clients/reports/test").getAbsolutePath() + "/index.html").start();
    }
}
