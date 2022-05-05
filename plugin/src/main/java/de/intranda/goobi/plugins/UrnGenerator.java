package de.intranda.goobi.plugins;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.sub.goobi.persistence.managers.MySQLHelper;
import ugh.dl.DocStructType;

public class UrnGenerator {
    private Connection con;
    private static HashMap<Character, Integer> checksumConversionTable = initializeUrnChecksumConversionTable();
    private final String URN_TABLE_NAME = "urn_table";
    private final String URN_COLUMN_NAME = "urn_id";
    private final String WORKID_COLUMN_NAME = "werk_id";
    private final String STRUCT_COLUMN_NAME = "struktur_typ";

    public UrnGenerator() throws SQLException {
        con = MySQLHelper.getInstance().getConnection();
    }

    //    public static String generateUrn(String urn_prefix, String infix) {
    //        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuuMMddhhmmss");
    //        LocalDateTime localDate = LocalDateTime.now();
    //        String timeStamp = dtf.format(localDate);
    //        String urnWithoutCS = urn_prefix + "-" + infix + "-" + timeStamp;
    //        return urnWithoutCS; //  + calculateUrnChecksum(urnWithoutCS);
    //    }

    /**
     * Either adds the new element to the database and returns the newly created UrnID or just leaves the database unchanged and returns the
     * corresponding UrnId If the element is not listed there, it is always added to the database and a newly generated URN is returned.
     * 
     * @param workID id of the work (ppn), null will be replaced with empty string
     * @param structType structure type of the work ('Chapter' ...), null will be replaced with empty string
     * @return the unique URN value (derived from the primary key of the database)
     * @throws SQLException if the database requests could not be processed
     * @throws UrnDatabaseException if the database is corrupted
     */
    public int getUrnId(String workID, DocStructType struct) throws SQLException, UrnDatabaseException {
        int resultInt = -1; // default (illegal) return value - this should never happen
        if (workID == null) {
            workID = "";
        }
        String structType;
        if (struct == null) {
            structType = "";
        } else {
            structType = struct.getName();
        }
        // lock table in order to prevent race conditions
        Statement sLock = con.createStatement();
        sLock.executeUpdate("LOCK TABLE " + URN_TABLE_NAME + " WRITE");
        sLock.close();

        if (struct.isAnchor() || struct.isTopmost()) {
            PreparedStatement sQuery1 = con.prepareStatement("SELECT " + URN_COLUMN_NAME + " FROM " + URN_TABLE_NAME + " WHERE " + WORKID_COLUMN_NAME
                    + " = ? AND " + STRUCT_COLUMN_NAME + " = ? ;");
            sQuery1.setString(1, workID);
            sQuery1.setString(2, structType);
            ResultSet resultS = sQuery1.executeQuery();
            resultS.last();
            if (resultS.getRow() > 1) { // DB contains unique structType-workID combination multiple times
                throw new UrnDatabaseException("URN database in inconsistent state");
            }
            if (resultS.getRow() == 1) { // DB contains structType-workID combination already. No insertion.
                resultInt = resultS.getInt(URN_COLUMN_NAME);
            }
            if (resultS.getRow() == 0) { // DB does not contain structType-workID combination. Insert the new row.
                PreparedStatement sUpdate1 =
                        con.prepareStatement("INSERT INTO " + URN_TABLE_NAME + " VALUES(?,?,?);", Statement.RETURN_GENERATED_KEYS);
                sUpdate1.setString(1, null);
                sUpdate1.setString(2, workID);
                sUpdate1.setString(3, structType);
                sUpdate1.executeUpdate();
                ResultSet rs = sUpdate1.getGeneratedKeys();
                if (rs.next()) {
                    resultInt = rs.getInt(1);
                } else {
                    throw new SQLException("Could not retreive newly generated URN from database");
                }
                sUpdate1.close();
            }
            sQuery1.close();
        } else { // multiple entries of same structType-workID combination possible
            PreparedStatement sUpdate2 = con.prepareStatement("INSERT INTO " + URN_TABLE_NAME + " VALUES(?,?,?);", Statement.RETURN_GENERATED_KEYS);
            sUpdate2.setString(1, null);
            sUpdate2.setString(2, workID);
            sUpdate2.setString(3, structType);
            sUpdate2.executeUpdate();
            ResultSet rs = sUpdate2.getGeneratedKeys();
            if (rs.next()) {
                resultInt = rs.getInt(1);
            } else {
                throw new SQLException("Could not retreive newly generated URN from database");
            }
            sUpdate2.close();
        }

        Statement sLock2 = con.createStatement();
        sLock2.executeUpdate("UNLOCK TABLES;");
        sLock2.close();
       
        return resultInt;
    }
    
    public static String generateUrn(String prefix, String infix, int urnId) {
        if (infix!=null && !infix.isEmpty()) {
            return prefix + infix + urnId + calculateUrnChecksum(prefix + infix + urnId);
        }else
            return prefix + urnId + calculateUrnChecksum(prefix + urnId);
    }

    public boolean removeUrnId(int urnId) {

        try {
            PreparedStatement deleteQuery = con.prepareStatement("DELETE FROM " + URN_TABLE_NAME + "WHERE " + URN_COLUMN_NAME + "=?");
            deleteQuery.setInt(1, urnId);
            deleteQuery.executeUpdate();
            deleteQuery.close();
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }

    /**
     * Helper method of createUrn. Creates JSON-String needed to create an URN
     * 
     * @author Fabian Sudau
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
     * @author Fabian Sudau
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