package org.col.es.dsl;

import org.col.es.ddl.MultiField;

public class CaseInsensitiveQuery extends TermQuery {
  
  public CaseInsensitiveQuery(String field, Object value) {
    super(multi(field), value);
  }
  
  public CaseInsensitiveQuery(String field, String value, Float boost) {
    super(multi(field), value, boost);
  }
  
  private static String multi(String field) {
    return field + "." + MultiField.IGNORE_CASE.getName();
  }
  
}