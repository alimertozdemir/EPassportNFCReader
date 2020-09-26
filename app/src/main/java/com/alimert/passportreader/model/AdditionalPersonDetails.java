package com.alimert.passportreader.model;

import java.util.List;

public class AdditionalPersonDetails {

    private String custodyInformation;
    private String fullDateOfBirth;
    private String nameOfHolder;
    private List<String> otherNames;
    private List<String> otherValidTDNumbers;
    private List<String> permanentAddress;
    private String personalNumber;
    private String personalSummary;
    private List<String> placeOfBirth;
    private String profession;
    private byte[] proofOfCitizenship;
    private int tag;
    private List<Integer> tagPresenceList;
    private String telephone;
    private String title;

    public String getCustodyInformation() {
        return custodyInformation;
    }

    public void setCustodyInformation(String custodyInformation) {
        this.custodyInformation = custodyInformation;
    }

    public String getFullDateOfBirth() {
        return fullDateOfBirth;
    }

    public void setFullDateOfBirth(String fullDateOfBirth) {
        this.fullDateOfBirth = fullDateOfBirth;
    }

    public String getNameOfHolder() {
        return nameOfHolder;
    }

    public void setNameOfHolder(String nameOfHolder) {
        this.nameOfHolder = nameOfHolder;
    }

    public List<String> getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(List<String> otherNames) {
        this.otherNames = otherNames;
    }

    public List<String> getOtherValidTDNumbers() {
        return otherValidTDNumbers;
    }

    public void setOtherValidTDNumbers(List<String> otherValidTDNumbers) {
        this.otherValidTDNumbers = otherValidTDNumbers;
    }

    public List<String> getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(List<String> permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }

    public String getPersonalSummary() {
        return personalSummary;
    }

    public void setPersonalSummary(String personalSummary) {
        this.personalSummary = personalSummary;
    }

    public List<String> getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(List<String> placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public byte[] getProofOfCitizenship() {
        return proofOfCitizenship;
    }

    public void setProofOfCitizenship(byte[] proofOfCitizenship) {
        this.proofOfCitizenship = proofOfCitizenship;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public List<Integer> getTagPresenceList() {
        return tagPresenceList;
    }

    public void setTagPresenceList(List<Integer> tagPresenceList) {
        this.tagPresenceList = tagPresenceList;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
