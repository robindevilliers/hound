package uk.co.malbec.hound.reporter.machinery;


import java.util.*;
import java.util.function.Supplier;

public class LiteralCategoryGroup<CATEGORY, COLLECTOR> implements CategoryGroup<CATEGORY, COLLECTOR> {

    private Map<CATEGORY, COLLECTOR> categories = new HashMap<>();
    private List<CATEGORY> keys = new ArrayList<>();

    private COLLECTOR activeCollector;

    public LiteralCategoryGroup(Supplier<COLLECTOR> creator, CATEGORY... breakpoints){
        for (CATEGORY breakpoint : breakpoints){
            keys.add(breakpoint);
            categories.put(breakpoint, creator.get());
        }
    }

    @Override
    public List<CATEGORY> getKeys() {
        return keys;
    }

    @Override
    public boolean apply(CATEGORY key) {
        activeCollector = categories.get(key);
        return activeCollector != null;
    }

    @Override
    public COLLECTOR current() {
        return activeCollector;
    }

    public COLLECTOR get(CATEGORY key){
        return categories.get(key);
    }
}
