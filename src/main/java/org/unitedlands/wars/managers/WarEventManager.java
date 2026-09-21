package org.unitedlands.wars.managers;

public class WarEventManager {
    
    private static WarEventManager instance;
    public static WarEventManager instance() {
        return instance;
    }

    public WarEventManager() {
        instance = this;
    }

    public void handleEvents() {

    }

}
