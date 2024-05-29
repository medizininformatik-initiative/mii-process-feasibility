package de.medizininformatik_initiative.process.feasibility.client.store;

public class OAuth2ClientException extends RuntimeException {

    private static final long serialVersionUID = -5840162115734733430L;

    public OAuth2ClientException(String message) {
        super(message);
    }

    public OAuth2ClientException(String message, Exception cause) {
        super(message, cause);
    }

}
