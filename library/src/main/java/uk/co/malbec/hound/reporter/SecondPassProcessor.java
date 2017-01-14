package uk.co.malbec.hound.reporter;

import uk.co.malbec.hound.Sample;
import uk.co.malbec.hound.reporter.machinery.LiteralCategoryGroup;
import uk.co.malbec.hound.reporter.machinery.Machinery;
import uk.co.malbec.hound.reporter.machinery.Vector;

import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Collections.sort;
import static java.util.function.Function.identity;
import static uk.co.malbec.hound.Utils.interpolateList;
import static uk.co.malbec.hound.reporter.machinery.Machinery.*;
import static uk.co.malbec.hound.reporter.machinery.Machinery.updateScalarIf;

public class SecondPassProcessor implements Consumer<Sample> {

    private Consumer<Sample> secondPass;

    public SecondPassProcessor(Data data) {

        sort(data.getStatistics().getTimeDistribution().getValues());
        double percentile1 = interpolateList(1, data.getStatistics().getTimeDistribution().getValues());
        double percentile99 = interpolateList(99, data.getStatistics().getTimeDistribution().getValues());


        data.getStatistics().setTimeLineCategories(generateLiteralCategoryGroupForTimeLineCategories(data));

        for (String operationName : data.getOperationNameCategories().getKeys()) {
            data.getOperationNameCategories().get(operationName).setTimeLineCategories(generateLiteralCategoryGroupForTimeLineCategories(data));
        }

        secondPass = all(
                partitionBy(Sample::getOperationName, String::equals, data.getOperationNameCategories(),
                        all(
                                Machinery.map(HtmlReporter::time,
                                        all(
                                                difference(reference(data.getOperationNameCategories(), Statistics::getMean), square(sum(reference(data.getOperationNameCategories(), Statistics::getSumOfSquares)))),
                                                filter(l -> l > percentile1 && l < percentile99,
                                                        all(
                                                                limit(2000000, reference(data.getOperationNameCategories(), Statistics::getTimeDistributionExcludingOutliers))
                                                        )
                                                )
                                        )
                                ),
                                partitionBy(Sample::getStart, Machinery::lessThan, reference(data.getOperationNameCategories(), Statistics::getTimeLineCategories),
                                        (sample) -> {
                                            reference(reference(data.getOperationNameCategories(), Statistics::getTimeLineCategories).get()).get().accept(sample);
                                        }
                                )
                        )
                ),
                Machinery.map(HtmlReporter::time, all(
                        difference(data.getStatistics().getMean(), square(sum(reference(referenceable(data.getStatistics()), Statistics::getSumOfSquares)))),
                        filter(l -> l > percentile1 && l < percentile99,
                                all(
                                        updateScalarIf(identity(), Machinery::lessThan, data.getStatistics().getMinimumTime()),
                                        updateScalarIf(identity(), Machinery::greaterThan, data.getStatistics().getMaximumTime())
                                )
                        )
                        )
                ),
                partitionBy(Sample::getStart, Machinery::lessThan, data.getStatistics().getTimeLineCategories(),
                        (sample) -> {
                            reference(data.getStatistics().getTimeLineCategories()).get().accept(sample);
                        }

                )
        );

    }

    private LiteralCategoryGroup<Long, Vector<Sample>> generateLiteralCategoryGroupForTimeLineCategories(Data data) {
        long BUCKETS = 12;
        long bucketSize = Math.max((data.getLatestStartTime().getValue() - data.getEarliestStartTime().getValue()) / BUCKETS, 1);
        ArrayList<Long> breakpoints = new ArrayList<>();

        for (long i = data.getEarliestStartTime().getValue() + bucketSize; i <= data.getLatestStartTime().getValue(); i = i + bucketSize) {
            breakpoints.add(i);
        }

        return literalCategorization(Vector::new, breakpoints.toArray(new Long[0]));
    }

    @Override
    public void accept(Sample sample) {
        secondPass.accept(sample);
    }
}
