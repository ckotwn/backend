package org.col.admin.task.importer.dwca;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.col.admin.task.importer.InsertMetadata;
import org.col.admin.task.importer.NormalizationFailedException;
import org.gbif.dwc.terms.AcefTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic validation of all existing mappings found in a dwc archive
 * to make sure the required minimum exists.
 */
public class DwcaMetaValidator {
  private static final Logger LOG = LoggerFactory.getLogger(DwcaMetaValidator.class);

  public static void check(Archive arch, InsertMetadata meta) throws NormalizationFailedException.SourceInvalidException {
    // check for a minimal parsed name
    if ((arch.getCore().hasTerm(DwcTerm.genus) || arch.getCore().hasTerm(GbifTerm.genericName))
        && arch.getCore().hasTerm(DwcTerm.specificEpithet)
    ) {
      meta.setParsedNameMapped(true);
    }

    // make sure either scientificName or genus & specificEpithet are mapped
    if (!arch.getCore().hasTerm(DwcTerm.scientificName)) {
      LOG.warn("No scientificName mapped");
      if (!meta.isParsedNameMapped()) {
        // no name to work with!!!
        throw new NormalizationFailedException.SourceInvalidException("No scientificName nor parsed name mapped");
      } else {
        // warn if there is no author mapped for a parsed name
        if (!arch.getCore().hasTerm(DwcTerm.scientificNameAuthorship)) {
          LOG.warn("No scientificNameAuthorship mapped for parsed name");
        }
      }
    }

    // warn if highly recommended terms are missing
    if (!arch.getCore().hasTerm(DwcTerm.taxonRank)) {
      LOG.warn("No taxonRank mapped");
    }

    // check
    if (!arch.getCore().hasTerm(DwcTerm.taxonID)) {
      LOG.warn("Using core ID for taxonID");
      meta.setCoreIdUsed(true);
    }
    // multi values in use for acceptedID?
    for (Term t : arch.getCore().getTerms()) {
      String delim = arch.getCore().getField(t).getDelimitedBy();
      if (!Strings.isNullOrEmpty(delim)) {
        meta.getMultiValueDelimiters().put(t, Splitter.on(delim).omitEmptyStrings());
      }
    }
    for (Term t : DwcTerm.HIGHER_RANKS) {
      if (arch.getCore().hasTerm(t)) {
        meta.setDenormedClassificationMapped(true);
        break;
      }
    }
    if (arch.getCore().hasTerm(AcefTerm.Superfamily)) {
      meta.setDenormedClassificationMapped(true);
    }
    if (arch.getCore().hasTerm(DwcTerm.parentNameUsageID) || arch.getCore().hasTerm(DwcTerm.parentNameUsage)) {
      meta.setParentNameMapped(true);
    }
    if (arch.getCore().hasTerm(DwcTerm.acceptedNameUsageID) || arch.getCore().hasTerm(DwcTerm.acceptedNameUsage)) {
      meta.setAcceptedNameMapped(true);
    } else {
      if (arch.getCore().hasTerm(AcefTerm.AcceptedTaxonID)) {
        // this sometimes gets confused with dwc - translate into dwc as we read dwc archives here
        ArchiveField af = arch.getCore().getField(AcefTerm.AcceptedTaxonID);
        af.setTerm(DwcTerm.acceptedNameUsageID);
        arch.getCore().addField(af);
        meta.setAcceptedNameMapped(true);
      } else {
        LOG.warn("No accepted name terms mapped");
      }
    }
    if (arch.getCore().hasTerm(DwcTerm.originalNameUsageID) || arch.getCore().hasTerm(DwcTerm.originalNameUsage)) {
      meta.setOriginalNameMapped(true);
    }
    // any classification?
    if (!meta.isParentNameMapped() && !meta.isDenormedClassificationMapped()) {
      LOG.warn("No higher classification mapped");
    }

    //TODO: validate extensions:
    // vernacular name: vernacularName
    // distribution: some area (locationID, countryCode, etc)
  }

}
