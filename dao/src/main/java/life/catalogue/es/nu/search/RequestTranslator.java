package life.catalogue.es.nu.search;

import life.catalogue.api.model.Page;
import life.catalogue.api.search.NameUsageSearchParameter;
import life.catalogue.api.search.NameUsageSearchRequest;
import life.catalogue.common.tax.RankUtils;
import life.catalogue.es.DownwardConverter;
import life.catalogue.es.query.BoolQuery;
import life.catalogue.es.query.EsSearchRequest;
import life.catalogue.es.query.MatchAllQuery;
import life.catalogue.es.query.Query;

import java.util.HashSet;
import java.util.Set;

import static life.catalogue.api.search.NameUsageSearchParameter.*;

/**
 * Translates a {@link NameUsageSearchRequest} into a native Elasticsearch search request. Mostly manages the other translators in this
 * package.
 */
class RequestTranslator implements DownwardConverter<NameUsageSearchRequest, EsSearchRequest> {

  static Query generateQuery(NameUsageSearchRequest request) {
    if (request.hasFilter(USAGE_ID)) {
      return BoolQuery.withFilters(
          new FilterTranslator(request).translate(DATASET_KEY),
          new FilterTranslator(request).translate(USAGE_ID));
    } else if (mustGenerateFilters(request)) {
      if (request.hasQ()) {
        return BoolQuery.withFilters(
            new FiltersTranslator(request).translate(),
            new QTranslator(request).translate());
      }
      return new FiltersTranslator(request).translate();
    } else if (request.hasQ()) {
      return new QTranslator(request).translate();
    }
    return new MatchAllQuery();
  }

  private final NameUsageSearchRequest request;
  private final Page page;

  RequestTranslator(NameUsageSearchRequest request, Page page) {
    this.request = request;
    this.page = page;
  }

  /**
   * Translates the NameUsageSearchRequest into a real Elasticsearch search request.
   * 
   * @return
   */
  EsSearchRequest translateRequest() {
    expandMinMaxRanks();
    EsSearchRequest es = new EsSearchRequest();
    es.setFrom(page.getOffset());
    es.setSize(page.getLimit());
    es.setQuery(generateQuery(request));
    es.setSort(new SortByTranslator(request).translate());
    // Unless explicitly specified otherwise, set to true:
    if (es.getTrackTotalHits() == null) {
      es.setTrackTotalHits(Boolean.TRUE);
    }
    if (!request.getFacets().isEmpty()) {
      FacetsTranslator ft = new FacetsTranslator(request);
      es.setAggregations(ft.translate());
    }
    return es;
  }

  private void expandMinMaxRanks(){
    if (request.getMinRank() != null || request.getMaxRank() != null) {
      Set<Object> ranks;
      if (request.getMinRank() != null && request.getMaxRank() != null) {
        // both given, use intersection only
        ranks = new HashSet<>(RankUtils.between(request.getMinRank(), request.getMaxRank(), true));
      } else if (request.getMinRank() != null){
        ranks = new HashSet<>(RankUtils.minRanks(request.getMinRank()));
      } else {
        ranks = new HashSet<>(RankUtils.maxRanks(request.getMaxRank()));
      }
      request.getFilters().put(NameUsageSearchParameter.RANK, ranks);
    }

  }
  private static boolean mustGenerateFilters(NameUsageSearchRequest request) {
    return request.getFilters().size() > 1 || (request.getFilters().size() == 1 && !request.hasFilter(CATALOGUE_KEY));
  }

}
