package uk.co.malbec.hound.reporter.machinery;

import java.util.*;
import java.util.function.Supplier;

public class DynamicCategoryGroup<CATEGORY, COLLECTOR> implements CategoryGroup<CATEGORY, COLLECTOR> {

    private Map<CATEGORY, COLLECTOR> categories = new HashMap<>();
    private List<CATEGORY> keys = new ArrayList<>();

    private COLLECTOR activeCollector;

    private Supplier<COLLECTOR> creator;

    public DynamicCategoryGroup(Supplier<COLLECTOR> creator){
        this.creator = creator;
    }

    @Override
    public List<CATEGORY> getKeys() {
        return keys;
    }

    @Override
    public boolean apply(CATEGORY key) {
        activeCollector = categories.get(key);
        if (activeCollector == null){
            activeCollector = creator.get();
            categories.put(key, activeCollector);
            keys.add(key);
        }

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
