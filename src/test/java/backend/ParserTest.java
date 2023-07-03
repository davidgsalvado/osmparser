package backend;

import es.udc.fic.tfg.osmparser.DbConnection.DbParameters;
import es.udc.fic.tfg.osmparser.grammar.CustomVisitor;
import es.udc.fic.tfg.osmparser.grammar.MessageQuery;
import es.udc.fic.tfg.osmparser.grammar.OSMGrammarLexer;
import es.udc.fic.tfg.osmparser.grammar.OSMGrammarParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserTest {

    @Test
    public void testExample() throws Exception{
        Path path = Paths.get(System.getProperty("user.dir")+"/src/main/resources/prueba.grammar.OSMGrammar");
        CharStream stream = CharStreams.fromFileName(path.toAbsolutePath().toString());
        OSMGrammarLexer lexer = new OSMGrammarLexer(stream);
        OSMGrammarParser parser  = new OSMGrammarParser(new CommonTokenStream(lexer));

        parser.addErrorListener(new BaseErrorListener(){
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e){
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        parser.parse();
    }

    @Test
    public void testAntlrWithJava() throws Exception {
        Path path = Paths.get(System.getProperty("user.dir") + "/src/main/resources/prueba.grammar.OSMGrammar");
        CharStream stream = CharStreams.fromFileName(path.toAbsolutePath().toString());
        OSMGrammarLexer lexer = new OSMGrammarLexer(stream);
        OSMGrammarParser parser = new OSMGrammarParser(new CommonTokenStream(lexer));

        OSMGrammarParser.ParseContext ctx = parser.parse();

        CustomVisitor listener = new CustomVisitor();

        listener.visitParse(ctx);

        DbParameters dbParameters = listener.getDbParameters().get(0);
        assertEquals(dbParameters.getDatabase(), "osmparser");
        assertEquals(dbParameters.getHost(), "localhost");
        assertEquals(dbParameters.getUser(), "david");
        assertEquals(dbParameters.getPassword(), "password");
        assertEquals(dbParameters.getPort(), "5432");

        MessageQuery query = listener.getEntries().get(0);

        assertEquals(query.getEntity(), "CentroDeSalud");
        assertEquals(query.getBbox(), "(43.3709703,-8.3959425)");
        assertEquals(query.getAttributeDefinition().get(0), "name:es,name,\"sin nombre\" => name");
        assertEquals(query.getAttributeDefinition().get(1), "type => type");
        assertEquals(query.getAttributeDefinition().get(2), "toString(amenity) => typeAmenity");
        assertEquals(query.getFromExpression(), "(amenity=\"hospital\" AND name=\"chuac\") OR amenity=\"clinic\"");
        assertEquals(query.getElements().get(0), "node");
        assertEquals(query.getElements().get(1), "way");

        query = listener.getEntries().get(1);
        assertEquals(query.getEntity(), "CentroDeSalud");
        assertEquals(query.getBbox(), "(43.3709703,-8.3959425)");
        assertEquals(query.getAttributeDefinition().get(0), "name:es,\"sin nombre\" => name");
        assertEquals(query.getFromExpression(), "(amenity=\"hospital\" AND name=\"chuac\") OR (amenity=\"tourism\" AND addr:city=\"A Coruña\")");

        query = listener.getEntries().get(2);
        assertEquals(query.getEntity(), "Hospital");
        assertEquals(query.getBbox(), "(43.3709703,-8.3959425,134.3123901,-23.4710345)");
        assertEquals(query.getFromExpression(), "amenity=\"clinic\"");
        assertEquals(query.getAttributeDefinition().get(1), "type,operator:type,toString(operator:type) => type");

        query = listener.getEntries().get(3);
        assertEquals(query.getEntity(), "Museum");
        assertEquals(query.getAttributeDefinition().get(2), "addr:city => city");
        assertEquals(query.getAttributeDefinition().get(0), "description,\"sin descripción\" => description");

        query = listener.getEntries().get(5);
        assertEquals(query.getEntity(), "Restaurant");
        assertEquals(query.getFromExpression(), "amenity!");

        query = listener.getEntries().get(6);
        assertEquals(query.getFromExpression(), "amenity!");
        assertEquals(query.getEntity(), "AmenityEntity");

        query = listener.getEntries().get(7);
        assertEquals(query.getFromExpression(), "amenity! AND name!");

        query = listener.getEntries().get(8);
        assertEquals(query.getFromExpression(), "(amenity=\"hospital\" AND name!) OR amenity=\"clinic\"");
    }
}

