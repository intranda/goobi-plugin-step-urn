package de.intranda.goobi.plugins;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UrnGenerator {
private static HashMap<Character, Integer> checksumConversionTable = initializeUrnChecksumConversionTable();
    
    
    public static String generateUrn (String urn_prefix, String infix) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuuMMddhhmmssSSS");
        LocalDateTime localDate = LocalDateTime.now();
        String timeStamp = dtf.format(localDate);
        String urnWithoutCS = urn_prefix +"-"+infix+"-" + timeStamp;
        return urnWithoutCS + calculateUrnChecksum(urnWithoutCS);
    }
    /**
     * Helper method of createUrn. Creates JSON-String needed to create an URN
     * @author Robert Sehr
     * @param Urn
     * @param urls
     * @return
     * @throws IOException
     */
    private static int calculateUrnChecksum(String urn) {
        if (urn == null || urn.isEmpty()) {
            throw new IllegalArgumentException("URN string is null or empty.");
        }
        int result = -1; // default (illegal) value
        urn = urn.toUpperCase();
        char[] urnChars = urn.toCharArray();
        List<Integer> digits = new ArrayList<Integer>(urnChars.length * 2);

        for (char c : urnChars) {
            if (checksumConversionTable.get(c) == null) {
                throw new IllegalArgumentException("Urn Character " + c + " can not be mapped in the checksum calculation process.");
            }
            int currDigit = checksumConversionTable.get(c);
            if (currDigit < 10) {
                digits.add(currDigit);
            } else {
                digits.add(currDigit / 10); // add decade
                digits.add(currDigit % 10); // add unit
            }
        }
        int productSum = 0;
        int count = 0;
        for (int currDigit : digits) {
            count++;
            productSum += currDigit * count;
        }
        result = (productSum / digits.get(digits.size() - 1)) % 10;
        return result;
    }

    /**
     * @author Robert Sehr
     * @return see http://www.persistent-identifier.de/?link=316
     */
    private static HashMap<Character, Integer> initializeUrnChecksumConversionTable() {
        HashMap<Character, Integer> res = new HashMap<Character, Integer>();
        res.put('0', 1);
        res.put('1', 2);
        res.put('2', 3);
        res.put('3', 4);
        res.put('4', 5);
        res.put('5', 6);
        res.put('6', 7);
        res.put('7', 8);
        res.put('8', 9);
        res.put('9', 41);
        res.put('A', 18);
        res.put('B', 14);
        res.put('C', 19);
        res.put('D', 15);
        res.put('E', 16);
        res.put('F', 21);
        res.put('G', 22);
        res.put('H', 23);
        res.put('I', 24);
        res.put('_', 43);
        res.put('.', 47);
        res.put('J', 25);
        res.put('K', 42);
        res.put('L', 26);
        res.put('M', 27);
        res.put('N', 13);
        res.put('O', 28);
        res.put('P', 29);
        res.put('Q', 31);
        res.put('R', 12);
        res.put('S', 32);
        res.put('T', 33);
        res.put('U', 11);
        res.put('V', 34);
        res.put('W', 35);
        res.put('X', 36);
        res.put('Y', 37);
        res.put('Z', 38);
        res.put('+', 49);
        res.put(':', 17);
        res.put('-', 39);
        res.put('/', 45);
        return res;
    }
  }
}
