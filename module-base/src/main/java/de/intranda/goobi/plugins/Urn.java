package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Urn {
    int id;
    String urn;
    boolean oldEntry;
}
