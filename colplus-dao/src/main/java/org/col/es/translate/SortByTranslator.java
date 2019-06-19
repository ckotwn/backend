package org.col.es.translate;

import java.util.List;

import org.col.api.search.NameSearchRequest;
import org.col.api.search.NameSearchRequest.SortBy;
import org.col.es.query.CollapsibleList;
import org.col.es.query.SortField;

class SortByTranslator {

  private final NameSearchRequest request;

  SortByTranslator(NameSearchRequest request) {
    this.request = request;
  }

  List<SortField> translate() {
    if (request.getSortBy() == SortBy.NAME) {
      return CollapsibleList.of(new SortField("scientificNameWN", !request.isReverse()));
    }
    if (request.getSortBy() == SortBy.TAXONOMIC) {
      return CollapsibleList.of(new SortField("rank", !request.isReverse()), new SortField("scientificNameWN"));
    }
    return CollapsibleList.of(SortField.DOC);
  }

}
