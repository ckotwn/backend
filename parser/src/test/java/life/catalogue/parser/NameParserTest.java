package life.catalogue.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import life.catalogue.api.model.IssueContainer;
import life.catalogue.api.model.Name;
import life.catalogue.api.model.ParsedNameUsage;
import life.catalogue.api.model.ParserConfig;
import life.catalogue.api.vocab.NomStatus;
import org.gbif.nameparser.api.*;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * The bulk of parsing tests are part of the GBIF Name Parser project.
 * Some name parsing tests kept in this project.
 */
public class NameParserTest {
  static final NameParser parser = new NameParser();
  
  @Test
  public void parseAuthorship() throws Exception {
    assertAuthorship("L.f", null, "L.f");
    assertAuthorship("DC.", null, "DC.");
  }

  @Test
  public void parseVirusConfig() throws Exception {
    // no configs yet
    assertName("Aspilota vector Belokobylskij, 2007", "Aspilota vector Belokobylskij, 2007", NameType.VIRUS)
        .nothingElse();

    // add parser config
    ParserConfig cfg = new ParserConfig();
    cfg.updateID("Aspilota vector",  "Belokobylskij, 2007");
    cfg.setGenus("Aspilota");
    cfg.setSpecificEpithet("vector");
    cfg.setCombinationAuthorship(Authorship.yearAuthors("2007", "Belokobylskij"));
    cfg.setType(NameType.SCIENTIFIC);
    cfg.setRank(Rank.SPECIES);
    addToParser(cfg);

    assertName("Aspilota vector Belokobylskij, 2007", "Aspilota vector")
        .species("Aspilota", "vector")
        .combAuthors("2007", "Belokobylskij")
        .type(NameType.SCIENTIFIC)
        .nothingElse();
  }

  private static void addToParser(ParserConfig obj){
    ParsedName pn = Name.toParsedName(obj);
    pn.setTaxonomicNote(obj.getTaxonomicNote());
    NameParser.configs().setName(obj.getScientificName() + " " + obj.getAuthorship(), pn);
  }

  @Test
  public void normalizeAuthorship() throws Exception {
    assertEquals("(Huteret ali)", NameParser.normalizeAuthorship("(Huteret ali)", null));
    assertEquals("(Huter et al.) P. D. Sell & Whitehead", NameParser.normalizeAuthorship("(Huter et al.) P. D. Sell & Whitehead", null));
    assertNull(NameParser.normalizeAuthorship("sensu Wilson & Henderson [Brit. Rust Fungi:47-48 (1966) ]; fide Checklist of Basidiomycota of Great", "sensu Wilson & Henderson[Brit. Rust Fungi:47-48 (1966) ]; fide Checklist of Basidiomycota of Great"));
    assertEquals("Brouss. ex Willd.", NameParser.normalizeAuthorship("Brouss. ex Willd.", null));
    assertEquals("(St.John) Sachet", NameParser.normalizeAuthorship("(St.John) Sachet", null));
    assertEquals("Trautv. & Meyer", NameParser.normalizeAuthorship("Trautv.&Meyer", null));
    assertEquals("Trautv. & Meyer", NameParser.normalizeAuthorship("Trautv. & Meyer", null));
    assertEquals("Rossi, 1988", NameParser.normalizeAuthorship("Rossi 1988 non DC.1988", "non DC. 1988"));
    assertEquals("Rossi, 1790", NameParser.normalizeAuthorship("Rossi, 1790", null));
    assertEquals("Rossi, 1790", NameParser.normalizeAuthorship("Rossi 1790", null));
    assertEquals("(Rossi, 1790)", NameParser.normalizeAuthorship("(Rossi 1790)", null));
    assertEquals("(Ridl.) ined.", NameParser.normalizeAuthorship("(Ridl.) ined.", null));
    assertEquals("(L.) DC", NameParser.normalizeAuthorship("( L.)DC ", null));
    assertEquals("(Walther & Rück) van der Damme & Resorbin, 1999", NameParser.normalizeAuthorship("( Walther&Rück ) van der Damme and Resorbin 1999", null));
    assertEquals("Miller, 1989", NameParser.normalizeAuthorship("Miller 1989 sensu Carol 2001", "sensu Carol 2001"));

    assertNull(NameParser.normalizeAuthorship("(non Scacchi, 1836) sensu Zibrowius, 1968", "(non Scacchi, 1836) sensu Zibrowius, 1968"));
    assertEquals("Fischer-Le Saux et al., 1999", NameParser.normalizeAuthorship("Fischer-Le Saux et al., 1999 emend. Akhurst et al., 2004", "emend. Akhurst et al. , 2004"));
    assertEquals("Engl., nom. illeg.", NameParser.normalizeAuthorship("Engl., nom. illeg., non. A. lancea.", "non. A.lancea."));
  }

  @Test
  public void parseManuscript() throws Exception {
    assertName("Acranthera virescens (Ridl.) ined.", "Acranthera virescens")
          .species("Acranthera", "virescens")
          .basAuthors(null, "Ridl.")
          .type(NameType.SCIENTIFIC)
          .nomNote("ined.")
          .status(NomStatus.MANUSCRIPT)
          .nothingElse();
  }
  
  @Test
  public void parseSubgenera() throws Exception {
    assertName("Eteone subgen. Mysta", "Eteone subgen. Mysta")
        .infraGeneric("Eteone", Rank.SUBGENUS, "Mysta")
        .nothingElse();
    
    assertName("Eteone (Mysta)", Rank.SUBGENUS, NomCode.ZOOLOGICAL, "Eteone (Mysta)")
        .infraGeneric("Eteone", Rank.SUBGENUS, "Mysta")
        .nothingElse();
  }
  
  @Test
  public void parseSpecies() throws Exception {
    
    assertName("Zophosis persis (Chatanay 1914)", "Zophosis persis")
        .species("Zophosis", "persis")
        .basAuthors("1914", "Chatanay")
        .nothingElse();
    
    assertName("Abies alba Mill.", "Abies alba")
        .species("Abies", "alba")
        .combAuthors(null, "Mill.")
        .nothingElse();
  
    assertName("Acranthera virescens (Ridl.) ined.", "Acranthera virescens")
        .species("Acranthera", "virescens")
        .basAuthors(null, "Ridl.")
        .status(NomStatus.MANUSCRIPT)
        .nomNote("ined.")
        .nothingElse();

    assertName("Alstonia vieillardii Van Heurck & Müll.Arg.", "Alstonia vieillardii")
        .species("Alstonia", "vieillardii")
        .combAuthors(null, "Van Heurck", "Müll.Arg.")
        .nothingElse();
    //TODO: do we expect d'urvilleana or durvilleana ???
    assertName("Angiopteris d'urvilleana de Vriese", "Angiopteris d'urvilleana")
        .species("Angiopteris", "d'urvilleana")
        .combAuthors(null, "de Vriese")
        .nothingElse();
  }
  
  @Test
  public void parseInfraSpecies() throws Exception {
    
    assertName("Abies alba ssp. alpina Mill.", "Abies alba alpina")
        .infraSpecies("Abies", "alba", Rank.SUBSPECIES, "alpina")
        .combAuthors(null, "Mill.")
        .nothingElse();
    
    assertName("Festuca ovina L. subvar. gracilis Hackel", "Festuca ovina subvar. gracilis")
        .infraSpecies("Festuca", "ovina", Rank.SUBVARIETY, "gracilis")
        .combAuthors(null, "Hackel")
        .nothingElse();
    
    assertName("Pseudomonas syringae pv. aceris (Ark, 1939) Young, Dye & Wilkie, 1978", "Pseudomonas syringae pv. aceris")
        .infraSpecies("Pseudomonas", "syringae", Rank.PATHOVAR, "aceris")
        .combAuthors("1978", "Young", "Dye", "Wilkie")
        .basAuthors("1939", "Ark");
    
    assertName("Baccharis microphylla Kunth var. rhomboidea Wedd. ex Sch. Bip. (nom. nud.)", "Baccharis microphylla var. rhomboidea")
        .infraSpecies("Baccharis", "microphylla", Rank.VARIETY, "rhomboidea")
        .combAuthors(null, "Sch.Bip.")
        .combExAuthors("Wedd.")
        .nomNote("nom.nud.")
        .nothingElse();
    
    assertName("Achillea millefolium subsp. pallidotegula B. Boivin var. pallidotegula", "Achillea millefolium var. pallidotegula")
        .infraSpecies("Achillea", "millefolium", Rank.VARIETY, "pallidotegula")
        .nothingElse();
    
  }
  
  @Test
  public void test4PartedNames() throws Exception {
    assertName("Bombus sichelii alticola latofasciatus", "Bombus sichelii latofasciatus")
        .infraSpecies("Bombus", "sichelii", Rank.INFRASUBSPECIFIC_NAME, "latofasciatus")
        .nothingElse();
    
    assertName("Poa pratensis kewensis primula (L.) Rouy, 1913", "Poa pratensis primula")
        .infraSpecies("Poa", "pratensis", Rank.INFRASUBSPECIFIC_NAME, "primula")
        .combAuthors("1913", "Rouy")
        .basAuthors(null, "L.")
        .nothingElse();
    
    assertName("Acipenser gueldenstaedti colchicus natio danubicus Movchan, 1967", "Acipenser gueldenstaedti natio danubicus")
        .infraSpecies("Acipenser", "gueldenstaedti", Rank.NATIO, "danubicus")
        .combAuthors("1967", "Movchan");
  }
  
  @Test
  public void parseMonomial() throws Exception {
    
    assertName("Acripeza Guérin-Ménéville 1838", "Acripeza")
        .monomial("Acripeza", Rank.UNRANKED)
        .combAuthors("1838", "Guérin-Ménéville")
        .nothingElse();
    
  }
  
  @Test
  public void parseInfraGeneric() throws Exception {
    
    assertName("Zignoella subgen. Trematostoma Sacc.", "Zignoella subgen. Trematostoma")
        .infraGeneric("Zignoella", Rank.SUBGENUS, "Trematostoma")
        .combAuthors(null, "Sacc.")
        .nothingElse();
    
    assertName("subgen. Trematostoma Sacc.", "subgen. Trematostoma")
        .infraGeneric(null, Rank.SUBGENUS, "Trematostoma")
        .combAuthors(null, "Sacc.")
        .nothingElse();
    
  }
  
  @Test
  public void parsePlaceholder() throws Exception {
    
    assertName("[unassigned] Cladobranchia", "[unassigned] Cladobranchia", NameType.PLACEHOLDER)
        .nothingElse();
    
    assertName("Biota incertae sedis", "Biota incertae sedis", NameType.PLACEHOLDER)
        .nothingElse();
    
    assertName("Mollusca not assigned", "Mollusca not assigned", NameType.PLACEHOLDER)
        .nothingElse();
  }
  
  /**
   * Expect empty results for nothing or whitespace
   */
  @Test
  public void testEmpty() throws Exception {
    assertEquals(Optional.empty(), parser.parse(null));
    assertEquals(Optional.empty(), parser.parse(""));
    assertEquals(Optional.empty(), parser.parse(" "));
    assertEquals(Optional.empty(), parser.parse("\t"));
    assertEquals(Optional.empty(), parser.parse("\n"));
    assertEquals(Optional.empty(), parser.parse("\t\n"));
  }
  
  /**
   * Avoid NPEs and other exceptions for very short non names and other extremes found in occurrences.
   */
  @Test
  public void testAvoidNPE() throws Exception {
    assertNoName("\\");
    assertNoName(".");
    assertNoName("a");
    assertNoName("X");
    assertNoName("@");
    assertNoName("&nbsp;");
  }
  
  private void assertNoName(String name) throws UnparsableException {
    assertName(name, name, NameType.NO_NAME)
        .nothingElse();
  }
  
  @Test
  public void parseSanctioned() throws Exception {
    // sanctioning authors not supported
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/409
    assertName("Agaricus compactus sarcocephalus (Fr. : Fr.) Fr. ", "Agaricus compactus sarcocephalus")
        .infraSpecies("Agaricus", "compactus", Rank.INFRASPECIFIC_NAME, "sarcocephalus")
        .combAuthors(null, "Fr.")
        .basAuthors(null, "Fr.")
        .nothingElse();
    
    assertName("Boletus versicolor L. : Fr.", "Boletus versicolor")
        .species("Boletus", "versicolor")
        .combAuthors(null, "L.")
        .sanctAuthor("Fr.")
        .nothingElse();
  }
  
  @Test
  public void parseNothotaxa() throws Exception {
    // https://github.com/GlobalNamesArchitecture/gnparser/issues/410
    assertName("Iris germanica nothovar. florentina", "Iris germanica nothovar. florentina")
        .infraSpecies("Iris", "germanica", Rank.VARIETY, "florentina")
        .notho(NamePart.INFRASPECIFIC)
        .nothingElse();
    
    assertName("Abies alba var. ×alpina L.", "Abies alba nothovar. alpina")
        .infraSpecies("Abies", "alba", Rank.VARIETY, "alpina")
        .notho(NamePart.INFRASPECIFIC)
        .combAuthors(null, "L.")
        .nothingElse();
  }
  
  @Test
  public void parseHybridFormulas() throws Exception {
    // fix hybrids formulas
    assertName("Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939", "Asplenium rhizophyllum DC. x ruta-muraria E.L. Braun 1939", NameType.HYBRID_FORMULA)
        .nothingElse();
    
  }
  
  
  static void assertAuthorship(String authorship, String year, String... authors) throws UnparsableException {
    ParsedAuthorship pa = parser.parseAuthorship(authorship).get();
    Authorship a = new Authorship();
    a.setYear(year);
    for (String x : authors) {
      a.getAuthors().add(x);
    }
    assertEquals(a, pa.getCombinationAuthorship());
  }
  
  static NameAssertion assertName(String rawName, String sciname) throws UnparsableException {
    return assertName(rawName, sciname, NameType.SCIENTIFIC);
  }
  
  static NameAssertion assertName(String rawName, String sciname, NameType type) throws UnparsableException {
    return assertName(rawName, null, null, sciname, type);
  }
  
  static NameAssertion assertName(String rawName, Rank rank, NomCode code, String sciname) throws UnparsableException {
    return assertName(rawName, rank, code, sciname, NameType.SCIENTIFIC);
  }
  
  static NameAssertion assertName(String rawName, Rank rank, NomCode code, String sciname, NameType type) throws UnparsableException {
    ParsedNameUsage n = parser.parse(rawName, rank, code, IssueContainer.VOID).get();
    assertEquals(sciname, n.getName().getScientificName());
    return new NameAssertion(n.getName()).type(type);
  }

  static class NameAssertion {
    private final Name n;
    private Set<NP> tested = Sets.newHashSet();
    
    private enum NP {
      EPITHETS,
      NOTHO,
      AUTH,
      EXAUTH,
      BAS,
      EXBAS,
      SANCT,
      RANK,
      TYPE,
      STATUS,
      NOMNOTE,
      UNPARSED,
      REMARKS
    }
    
    public NameAssertion(Name n) {
      this.n = n;
    }
    
    void nothingElse() {
      for (NP p : NP.values()) {
        if (!tested.contains(p)) {
          switch (p) {
            case EPITHETS:
              assertNull(n.getGenus());
              assertNull(n.getInfragenericEpithet());
              assertNull(n.getSpecificEpithet());
              assertNull(n.getInfraspecificEpithet());
              break;
            case NOTHO:
              assertNull(n.getNotho());
              break;
            case AUTH:
              assertNull(n.getCombinationAuthorship().getYear());
              assertTrue(n.getCombinationAuthorship().getAuthors().isEmpty());
              break;
            case EXAUTH:
              assertTrue(n.getCombinationAuthorship().getExAuthors().isEmpty());
              break;
            case BAS:
              assertNull(n.getBasionymAuthorship().getYear());
              assertTrue(n.getBasionymAuthorship().getAuthors().isEmpty());
              break;
            case EXBAS:
              assertTrue(n.getBasionymAuthorship().getExAuthors().isEmpty());
              break;
            case SANCT:
              assertNull(n.getSanctioningAuthor());
              break;
            case RANK:
              assertEquals(Rank.UNRANKED, n.getRank());
              break;
            case TYPE:
              assertEquals(NameType.SCIENTIFIC, n.getType());
              break;
            case STATUS:
              assertNull(n.getNomStatus());
              break;
            case NOMNOTE:
              assertNull(n.getNomenclaturalNote());
              break;
            case UNPARSED:
              assertNull(n.getUnparsed());
              break;
            case REMARKS:
              assertNull(n.getUnparsed());
          }
        }
      }
    }
    
    private NameAssertion add(NP... props) {
      for (NP p : props) {
        tested.add(p);
      }
      return this;
    }
    
    NameAssertion monomial(String monomial, Rank rank) {
      assertEquals(monomial, n.getUninomial());
      assertNull(n.getGenus());
      assertNull(n.getInfragenericEpithet());
      assertNull(n.getSpecificEpithet());
      assertNull(n.getInfraspecificEpithet());
      assertEquals(rank, n.getRank());
      return add(NP.EPITHETS, NP.RANK);
    }
    
    NameAssertion infraGeneric(String genus, Rank rank, String infraGeneric) {
      assertEquals(genus, n.getGenus());
      assertEquals(infraGeneric, n.getInfragenericEpithet());
      assertNull(n.getSpecificEpithet());
      assertNull(n.getInfraspecificEpithet());
      assertEquals(rank, n.getRank());
      return add(NP.EPITHETS, NP.RANK);
    }
    
    NameAssertion species(String genus, String epithet) {
      assertEquals(genus, n.getGenus());
      assertNull(n.getInfragenericEpithet());
      assertEquals(epithet, n.getSpecificEpithet());
      assertNull(n.getInfraspecificEpithet());
      assertEquals(Rank.SPECIES, n.getRank());
      return add(NP.EPITHETS, NP.RANK);
    }
    
    NameAssertion infraSpecies(String genus, String epithet, Rank rank, String infraEpithet) {
      assertEquals(genus, n.getGenus());
      assertNull(n.getInfragenericEpithet());
      assertEquals(epithet, n.getSpecificEpithet());
      assertEquals(infraEpithet, n.getInfraspecificEpithet());
      assertEquals(rank, n.getRank());
      return add(NP.EPITHETS, NP.RANK);
    }
    
    NameAssertion combAuthors(String year, String... authors) {
      assertEquals(year, n.getCombinationAuthorship().getYear());
      assertEquals(Lists.newArrayList(authors), n.getCombinationAuthorship().getAuthors());
      return add(NP.AUTH);
    }
    
    NameAssertion notho(NamePart notho) {
      assertEquals(notho, n.getNotho());
      return add(NP.NOTHO);
    }
    
    NameAssertion sanctAuthor(String author) {
      assertEquals(author, n.getSanctioningAuthor());
      return add(NP.SANCT);
    }
    
    NameAssertion combExAuthors(String... authors) {
      assertEquals(Lists.newArrayList(authors), n.getCombinationAuthorship().getExAuthors());
      return add(NP.EXAUTH);
    }
    
    NameAssertion basAuthors(String year, String... authors) {
      assertEquals(year, n.getBasionymAuthorship().getYear());
      assertEquals(Lists.newArrayList(authors), n.getBasionymAuthorship().getAuthors());
      return add(NP.BAS);
    }
    
    NameAssertion basExAuthors(String year, String... authors) {
      assertEquals(Lists.newArrayList(authors), n.getBasionymAuthorship().getExAuthors());
      return add(NP.EXBAS);
    }

    NameAssertion type(NameType type) {
      assertEquals(type, n.getType());
      return add(NP.TYPE);
    }
  
    NameAssertion status(NomStatus status) {
      assertEquals(status, n.getNomStatus());
      return add(NP.STATUS);
    }

    NameAssertion remarks(String remarks) {
      assertEquals(remarks, n.getRemarks());
      return add(NP.REMARKS);
    }

    NameAssertion nomNote(String nomNote) {
      assertEquals(nomNote, n.getNomenclaturalNote());
      return add(NP.NOMNOTE);
    }

    NameAssertion unparsed(String unparsed) {
      assertEquals(unparsed, n.getUnparsed());
      return add(NP.UNPARSED);
    }
  }
}