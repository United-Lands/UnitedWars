package org.unitedlands.wars.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.unitedlands.utils.United;

public class Serializers {

    public static <T> Set<T> deserializeUuidSet(String serialized, Function<UUID, T> lookup, String description) {
        Set<T> result = new HashSet<>();
        if (serialized == null || serialized.isBlank())
            return result;

        List<String> unresolved = new ArrayList<>();
        for (var part : serialized.split("#")) {
            try {
                var value = lookup.apply(UUID.fromString(part));
                if (value != null)
                    result.add(value);
                else
                    unresolved.add(part);
            } catch (IllegalArgumentException ex) {
                unresolved.add(part);
            }
        }

        if (!unresolved.isEmpty())
            United.logger().warning("Could not resolve " + unresolved.size() + " entr(y/ies) of " + description
                    + ", skipping: " + String.join(", ", unresolved), "UnitedWars");

        return result;
    }
}
