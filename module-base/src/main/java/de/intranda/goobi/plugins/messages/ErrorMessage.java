package de.intranda.goobi.plugins.messages;

import lombok.Getter;

public class ErrorMessage {
    @Getter
    private String code;
    @Getter
    private String developerMessage;
    @Getter
    private int status;

}
