/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.grammar;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import es.udc.fic.tfg.osmparser.backend.model.exceptions.InvalidDatabaseException;
import es.udc.fic.tfg.osmparser.backend.model.exceptions.InvalidDbParamsException;
import es.udc.fic.tfg.osmparser.backend.model.exceptions.InvalidQueryException;
import es.udc.fic.tfg.osmparser.backend.model.util.Parser;
import es.udc.fic.tfg.osmparser.grammar.OSMGrammarLexer;
import es.udc.fic.tfg.osmparser.grammar.OSMGrammarParser;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OSMGrammar {

    public static OverpassMapDataApi createOsmConnection(){
        OsmConnection connection = new OsmConnection("https://overpass-api.de/api/interpreter", "OsmParser in Java", null);
        return new OverpassMapDataApi(connection);
    }

    public static void main(String[] args) throws IOException, InvalidDatabaseException, InvalidDbParamsException,
            InterruptedException, InvalidQueryException {

        if(args.length < 1){
            throw new IllegalArgumentException();
        }

        Path path = Paths.get(args[0]);
        CharStream stream = CharStreams.fromFileName(path.toAbsolutePath().toString());
        OSMGrammarLexer lexer = new OSMGrammarLexer(stream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ErrorListener.INSTANCE);

        OSMGrammarParser parser  = new OSMGrammarParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ErrorListener.INSTANCE);

        OSMGrammarParser.ParseContext ctx = parser.parse();

        CustomVisitor listener = new CustomVisitor();

        listener.visitParse(ctx);

        CreateQuery queries = new CreateQuery(listener.getFinalEntries(), listener.getDbParameters());

        Parser osmParser = new Parser();

        osmParser.parseQueryFromFile(queries);
    }
}
