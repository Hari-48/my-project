package com.finsurge.tmr_portal.mx_superview.exceptions;

public class NotACsvFileException extends Exception {

    public NotACsvFileException(String filePath) {
        super(filePath + " is not a CSV file path.");
    }

}
