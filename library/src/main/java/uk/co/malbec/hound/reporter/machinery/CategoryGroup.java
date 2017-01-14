package uk.co.malbec.hound.reporter.machinery;

import java.util.List;

public interface CategoryGroup<CATEGORY, COLLECTOR> extends Referenceable<COLLECTOR> {

    List<CATEGORY> getKeys();

    boolean apply(CATEGORY key);

    COLLECTOR get(CATEGORY key);
}
