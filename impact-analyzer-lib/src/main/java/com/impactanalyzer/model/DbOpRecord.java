package com.impactanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DbOpRecord {
    private String operation;    // SELECT | INSERT | UPDATE | DELETE
    private String table;
    private String sql;
    private String callerFrame;
}
