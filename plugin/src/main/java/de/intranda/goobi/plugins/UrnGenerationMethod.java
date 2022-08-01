package de.intranda.goobi.plugins;

public enum UrnGenerationMethod {
    INCREMENT("increment"),
    TIMESTAMP("timestamp");
    
    private final String name;
    UrnGenerationMethod(String name) {
        this.name= name;
    }
    @Override
    public String toString() {
        return name;
    }
}
