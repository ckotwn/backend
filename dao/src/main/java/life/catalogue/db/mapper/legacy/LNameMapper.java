package life.catalogue.db.mapper.legacy;

import life.catalogue.db.mapper.legacy.model.LName;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LNameMapper {

  LName get(@Param("full") boolean full,
            @Param("datasetKey") int datasetKey,
            @Param("id") String id);

  int count(@Param("datasetKey") int datasetKey,
            @Param("prefix") boolean prefix,
            @Param("name") String name);

  List<LName> search(@Param("full") boolean full,
                     @Param("datasetKey") int datasetKey,
                     @Param("prefix") boolean prefix,
                     @Param("name") String name,
                     @Param("start") int start,
                     @Param("limit") int limit);

}
