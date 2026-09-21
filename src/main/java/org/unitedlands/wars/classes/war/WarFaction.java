package org.unitedlands.wars.classes.war;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.unitedlands.unitedlands.classes.Citizen;
import org.unitedlands.unitedlands.classes.Country;
import org.unitedlands.unitedlands.classes.Settlement;
import org.unitedlands.unitedlands.classes.db.Identifiable;
import org.unitedlands.unitedlands.libs.ormlite.field.DataType;
import org.unitedlands.unitedlands.libs.ormlite.field.DatabaseField;
import org.unitedlands.unitedlands.managers.UnitedLandsDataManager;
import org.unitedlands.unitedlands.utils.ColorUtils;
import org.unitedlands.unitedlands.utils.SerializationUtils;
import org.unitedlands.wars.managers.WarManager;

public class WarFaction implements Identifiable {

    @DatabaseField(id = true, width = 36, canBeNull = false)
    private UUID uuid;

    @DatabaseField(width = 36, canBeNull = false)
    private UUID warId;
    public transient War war;

    @DatabaseField(width = 36, canBeNull = false, columnName = "faction_leader_id")
    private UUID factionLeaderId;

    @DatabaseField(dataType = DataType.ENUM_STRING, canBeNull = false, columnName = "faction_role")
    private WarFactionRole role;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "countries_serialized")
    private String countriesSerialized;
    private transient Set<Country> countries;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "settlements_serialized")
    private String settlementsSerialized;
    private transient Set<Settlement> settlements;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "mercenaries_serialized")
    private String mercenariesSerialized;
    private transient Set<Citizen> mercenaries;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "win_conditions_serialized")
    private String winConditionsSerialized;
    private transient Map<String, Integer> winConditions;

    @DatabaseField(dataType = DataType.LONG_STRING, columnName = "lose_conditions_serialized")
    private String loseConditionsSerialized;
    private transient Map<String, Integer> loseConditions;

    @DatabaseField
    private String name;
    @DatabaseField
    private int color = 0;
    @DatabaseField
    private int score = 0;

    @DatabaseField(columnName = "lost")
    private boolean lost = false;

    private boolean stateChanged;

    public WarFaction() {

    }

    public WarFaction(War war, WarFactionRole role, String name, int color) {
        this.uuid = UUID.randomUUID();
        this.war = war;
        this.warId = war.getUuid();
        this.role = role;
        this.name = name;
        this.color = color;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public War getWar() {
        if (war == null && warId != null)
            war = WarManager.instance().getWar(warId);
        return war;
    }

    public void setWar(War war) {
        this.warId = war.getUuid();
        this.war = war;
    }

    public UUID getFactionLeaderId() {
        return factionLeaderId;
    }

    public void setFactionLeaderId(UUID factionLeaderId) {
        this.factionLeaderId = factionLeaderId;
    }

    public WarFactionRole getRole() {
        return role;
    }

    public void setRole(WarFactionRole role) {
        this.role = role;
    }

    public boolean hasCountry(Country country) {
        return getCountries().contains(country);
    }

    public void addCountry(Country country) {
        var c = new HashSet<>(getCountries());
        c.add(country);
        setCountries(c);
        this.stateChanged = true;
    }

    public void removeCountry(Country country) {
        var c = new HashSet<>(getCountries());
        c.remove(country);
        setCountries(c);
        this.stateChanged = true;
    }

    public Set<Country> getCountries() {
        if (this.countries == null) {
            this.countries = SerializationUtils.deserializeUuidListToSet(countriesSerialized, UnitedLandsDataManager.instance()::getCountry);
        }
        return this.countries;
    }

    public void setCountries(Set<Country> countries) {
        this.countries = countries;
        this.countriesSerialized = SerializationUtils.serializeIdentifiableList(this.countries);
    }

    public boolean hasSettlement(Settlement settlement) {
        return getSettlements().contains(settlement);
    }

    public void addSettlement(Settlement settlement) {
        var c = new HashSet<>(getSettlements());
        c.add(settlement);
        setSettlements(c);
        this.stateChanged = true;
    }

    public void removeSettlement(Settlement settlement) {
        var c = new HashSet<>(getSettlements());
        c.remove(settlement);
        setSettlements(c);
        this.stateChanged = true;
    }

    public Set<Settlement> getSettlements() {
        if (this.settlements == null) {
            this.settlements = SerializationUtils.deserializeUuidListToSet(settlementsSerialized, UnitedLandsDataManager.instance()::getSettlement);
        }
        return this.settlements;
    }

    public void setSettlements(Set<Settlement> settlements) {
        this.settlements = settlements;
        this.settlementsSerialized = SerializationUtils.serializeIdentifiableList(settlements);
    }

    public boolean hasMercenary(Citizen mercenary) {
        return getMercenaries().contains(mercenary);
    }

    public void addMercenary(Citizen mercenary) {
        var c = new HashSet<>(getMercenaries());
        c.add(mercenary);
        setMercenaries(c);
        this.stateChanged = true;
    }

    public void removeMercenary(Citizen mercenary) {
        var c = new HashSet<>(getMercenaries());
        c.remove(mercenary);
        setMercenaries(c);
        this.stateChanged = true;
    }

    public Set<Citizen> getMercenaries() {
        if (this.mercenaries == null) {
            this.mercenaries = SerializationUtils.deserializeUuidListToSet(mercenariesSerialized, UnitedLandsDataManager.instance()::getCitizen);
        }
        return this.mercenaries;
    }

    public void setMercenaries(Set<Citizen> mercenaries) {
        this.mercenaries = mercenaries;
        this.mercenariesSerialized = SerializationUtils.serializeIdentifiableList(mercenaries);
    }

    public void addWinCondition(String key) {
        addWinCondition(key, null);
    }

    public void addWinCondition(String key, Integer val) {
        var c = new HashMap<>(getWinConditions());
        c.put(key, val);
        setWinConditions(c);
        this.stateChanged = true;
    }

    public void removeWinCondition(String key) {
        var c = new HashMap<>(getWinConditions());
        c.remove(key);
        setWinConditions(c);
        this.stateChanged = true;
    }

    public Map<String, Integer> getWinConditions() {
        if (this.winConditions == null) {
            this.winConditions = SerializationUtils.deserializeStringIntegerMap(winConditionsSerialized);
        }
        return this.winConditions;
    }

    public void setWinConditions(Map<String, Integer> winConditions) {
        this.winConditions = winConditions;
        this.winConditionsSerialized = SerializationUtils.serializeStringIntegerMap(winConditions);
    }

    public void addLoseCondition(String key) {
        addLoseCondition(key, null);
    }

    public void addLoseCondition(String key, Integer val) {
        var c = new HashMap<>(getLoseConditions());
        c.put(key, val);
        setLoseConditions(c);
        this.stateChanged = true;
    }

    public void removeLoseCondition(String key) {
        var c = new HashMap<>(getLoseConditions());
        c.remove(key);
        setLoseConditions(c);
        this.stateChanged = true;
    }

    public Map<String, Integer> getLoseConditions() {
        if (this.loseConditions == null) {
            this.loseConditions = SerializationUtils.deserializeStringIntegerMap(loseConditionsSerialized);
        }
        return this.loseConditions;
    }

    public void setLoseConditions(Map<String, Integer> loseConditions) {
        this.loseConditions = loseConditions;
        this.loseConditionsSerialized = SerializationUtils.serializeStringIntegerMap(loseConditions);
    }

    public String getName() {
        return name;
    }

    public String getColoredName() {
        var colorStr = ColorUtils.argbToHex(color);
        return "<" + colorStr + ">" + name + "</" + colorStr + ">";
    }

    public void setName(String name) {
        this.name = name;
        this.stateChanged = true;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        this.stateChanged = true;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int score) {
        this.score += score;
        this.stateChanged = true;
    }

    public void setScore(int score) {
        this.score = score;
        this.stateChanged = true;
    }

    public boolean stateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean stateChanged) {
        this.stateChanged = stateChanged;
    }

    public boolean hasLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
        this.stateChanged = true;
    }

    public Map<String, String> getMessageReplacements() {
        return Map.of("faction-name", getName(), "faction-name-colored", getColoredName(), "faction-color", ColorUtils.argbToHex(getColor()), "faction-score",
                String.valueOf(score), "faction-role", getRole().toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WarFaction other = (WarFaction) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
