package org.unitedlands.wars.utils;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.GeopolObject;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;

public class GeopolUtils {

    public static GeopolObject getGeopolObject(String name) {
        GeopolObject obj = null;
        obj = UnitedLandsDataManager.instance().getSettlement(name);
        if (obj != null)
            return obj;
        obj = UnitedLandsDataManager.instance().getCountry(name);
        if (obj != null)
            return obj;
        obj = UnitedLandsDataManager.instance().getRegion(name);
        if (obj != null)
            return obj;
        return null;
    }

    public static GeopolObject getGeopolObject(UUID id) {
        GeopolObject obj = null;
        obj = UnitedLandsDataManager.instance().getSettlement(id);
        if (obj != null)
            return obj;
        obj = UnitedLandsDataManager.instance().getCountry(id);
        if (obj != null)
            return obj;
        obj = UnitedLandsDataManager.instance().getRegion(id);
        if (obj != null)
            return obj;
        return null;
    }

}
