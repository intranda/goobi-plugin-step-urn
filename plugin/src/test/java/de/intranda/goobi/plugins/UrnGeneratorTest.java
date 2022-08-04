package de.intranda.goobi.plugins;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.junit.Test;
import org.junit.Assert;

public class UrnGeneratorTest {

    @Test
   public void testGenerateTimeStamp() {
        String TimeStamp = UrnGenerator.generateTimeStamp();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(TimeStamp,formatter);
        } catch (DateTimeParseException ex) {
            Assert.fail("The Format String was changed. Please create a new Method if you need another Date Format! "+ TimeStamp);
        }

    }

}
