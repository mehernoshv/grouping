package com.samples.grouping;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSVGrouper {

    private static String REGEX_EMAIL =
            "(?=[A-Z0-9][A-Z0-9@._%+-]{5,253}+$)[A-Z0-9._%+-]{1,64}+@" +
            "(?:(?=[A-Z0-9-]{1,63}+\\.)[A-Z0-9]++(?:-[A-Z0-9]++)*+\\.){1,8}+" +
            "[A-Z]{2,63}+";

    //15556549873

    private static String REGEX_US_PHONE = "1?\\s?\\(?(\\d{3})\\)?[\\s.-]?(\\d{3})[\\s.-]?(\\d{4})";

    // This is okay for a prototype. In a production system, where the number of records in the CSV could be very large
    // We would need a different structure that would persist reside in some distributed cache


    public void parse(String inputFile, String outputFile, int matchingType) throws Exception{
        BufferedReader reader = null;
        Writer writer = null;
        Map<String, UUID> keysCache = new HashMap<String, UUID>();

        try {
            reader = Files.newBufferedReader(Paths.get(inputFile));
            writer = Files.newBufferedWriter(Paths.get(outputFile), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

            Pattern emailPattern = Pattern.compile(REGEX_EMAIL, Pattern.CASE_INSENSITIVE);
            Pattern phonePattern = Pattern.compile(REGEX_US_PHONE, Pattern.CASE_INSENSITIVE);

            // CSV file is parsed so that regex pattern matching is not done on full record
            // but just on individual fields at have "@" or start with digit for phone number
            boolean header = true;
            List<Integer> emailFieldNumbers = new ArrayList<>();
            List<Integer> phoneFieldNumbers = new ArrayList<>();
            List<String> recordKeys = null;

            for (CSVRecord csvRecord : csvParser) {
                StringBuilder outputRecord = new StringBuilder();
                int fieldNumber = 0;
                recordKeys = new ArrayList<>();

                for (String field : csvRecord) {
                    fieldNumber++;
                    outputRecord.append(',').append(field);

                    if (header) {
                        if (field.toLowerCase().contains("email")) {
                            emailFieldNumbers.add(fieldNumber);
                        }
                        else if (field.toLowerCase().contains("phone")) {
                            phoneFieldNumbers.add(fieldNumber);
                        }
                        continue;
                    }

                    field = field.trim();
                    if (StringUtils.isEmpty(field)) {
                        continue;
                    }

                    if (matchingType == 0 || matchingType == 2) {
                        // email
                        if (emailFieldNumbers.contains(fieldNumber)) {
                            Matcher matcher = emailPattern.matcher(field);
                            if (matcher.find()) {
                                recordKeys.add(matcher.group().toLowerCase());
                            } else {
                                System.err.println("RecordNo: " + csvRecord.getRecordNumber() + " has no valid email at field no: " + fieldNumber);
                            }
                        }
                    }
                    if (matchingType == 1 || matchingType == 2) {
                        // Phone
                        if (phoneFieldNumbers.contains(fieldNumber)) {
                            Matcher matcher = phonePattern.matcher(field);
                            if (matcher.find()) {
                                // Build the phone number ignoring country code 1 and other formatting characters
                                String phone = matcher.group(1) + matcher.group(2) + matcher.group(3);
                                recordKeys.add(phone);
                            } else {
                                System.err.println("RecordNo: " + csvRecord.getRecordNumber() + " has no valid phone at field no: " + fieldNumber);
                            }
                        }
                    }
                }

                UUID uuid = null;
                if (!header) {
                    // Now check for existing of email/phone numbers
                    for (String key : recordKeys) {
                        uuid = keysCache.get(key);
                        if (uuid != null) {
                            break;
                        }
                    }

                    // No cached keys found for any of the record keys
                    if (uuid == null) {
                        uuid = UUID.randomUUID();
                    }

                    // Set the same UUID for all record keys
                    for (String key : recordKeys) {
                        keysCache.put(key, uuid);
                    }
                }

                outputRecord.insert( 0, header ? "UUID" : uuid.toString());
                header = false;
                writer.append(outputRecord).append(System.lineSeparator());
                writer.flush();
            }
        }
        finally {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        }
    }

    public static void main (String[] args) {
        CSVGrouper csvGrouper = new CSVGrouper();
        String inputFile, outputFile, matchingType;
        int matchingTypeValue = 0;

        if (args.length > 0) {
            if (args.length != 3) {
                System.err.println("Usage: csvGrouper [Input CSV file pathname] [Output CSV file pathname] [matchEmail | matchPhone]");
            }
            inputFile = args[0];
            outputFile = args[1];
            matchingType = args[2];
        } else {
            Scanner in = new Scanner(System.in);
            System.out.print("\nEnter input CSV file pathname: ");
            inputFile = in.nextLine();
            System.out.print("\nEnter output CSV file pathname: ");
            outputFile = in.nextLine();
            System.out.print("\nEnter matching type (MatchEmail | MatchPhone | Both ): ");
            matchingType = in.nextLine();
        }

        if (matchingType.equalsIgnoreCase("matchemail")) {
            matchingTypeValue = 0;
        } else if (matchingType.equalsIgnoreCase("matchphone")) {
            matchingTypeValue = 1;
        } else if (matchingType.equalsIgnoreCase("both")) {
            matchingTypeValue = 2;
        } else {
            System.err.println("Incorrect type for matching type. Value can be either 'MatchEmail', or 'MatchPhone' or 'Both'");
        }


        try {
            csvGrouper.parse(inputFile, outputFile, matchingTypeValue);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
