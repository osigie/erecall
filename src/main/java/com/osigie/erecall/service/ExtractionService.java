package com.osigie.erecall.service;

public interface ExtractionService {
    //convert unstructured text into structured expense data
    /*
     * INPUT: paid netflix subscription 500
     *
     *
     * Prompt Extract structured expense information, return json only
     * FIELD:
     * - amount
     * - merchant
     * - category
     * - description
     * - date
     *
     * */

    String extract(String text);

}
