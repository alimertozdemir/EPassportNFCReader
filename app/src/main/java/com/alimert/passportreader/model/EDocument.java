package com.alimert.passportreader.model;

import java.security.PublicKey;

public class EDocument {

    private DocType docType;
    private PersonDetails personDetails;
    private AdditionalPersonDetails additionalPersonDetails;
    private boolean passiveAuth;
    private boolean activeAuth;
    private boolean chipAuth;
    private boolean docSignatureValid;
    private PublicKey docPublicKey;

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public PersonDetails getPersonDetails() {
        return personDetails;
    }

    public void setPersonDetails(PersonDetails personDetails) {
        this.personDetails = personDetails;
    }

    public AdditionalPersonDetails getAdditionalPersonDetails() {
        return additionalPersonDetails;
    }

    public void setAdditionalPersonDetails(AdditionalPersonDetails additionalPersonDetails) {
        this.additionalPersonDetails = additionalPersonDetails;
    }

    public boolean isPassiveAuth() {
        return passiveAuth;
    }

    public void setPassiveAuth(boolean passiveAuth) {
        this.passiveAuth = passiveAuth;
    }

    public boolean isActiveAuth() {
        return activeAuth;
    }

    public void setActiveAuth(boolean activeAuth) {
        this.activeAuth = activeAuth;
    }

    public boolean isChipAuth() {
        return chipAuth;
    }

    public void setChipAuth(boolean chipAuth) {
        this.chipAuth = chipAuth;
    }

    public boolean isDocSignatureValid() {
        return docSignatureValid;
    }

    public void setDocSignatureValid(boolean docSignatureValid) {
        this.docSignatureValid = docSignatureValid;
    }

    public PublicKey getDocPublicKey() {
        return docPublicKey;
    }

    public void setDocPublicKey(PublicKey docPublicKey) {
        this.docPublicKey = docPublicKey;
    }
}
