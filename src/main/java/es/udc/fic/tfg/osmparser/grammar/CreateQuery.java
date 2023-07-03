/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.grammar;

import es.udc.fic.tfg.osmparser.DbConnection.DbParameters;
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateQuery {

    enum Separator{
        AND,
        OR
    }

    private List<Request> queries;
    private OverpassMapDataApi mapApi;
    private List<String> overpassQueries;
    private DbParameters dbParameters;
    private List<DbParameters> dbParametersList;

    public CreateQuery(List<Request> queries, DbParameters dbParameters){
        this.queries = queries;
        this.dbParameters = dbParameters;
        this.mapApi = new OverpassMapDataApi(new OsmConnection("https://overpass-api.de/api/", "my user agent"));
    }

    public CreateQuery(List<Request> queries){
        this.queries = queries;
        this.mapApi = new OverpassMapDataApi(new OsmConnection("https://overpass-api.de/api/", "my user agent"));
    }

    public CreateQuery(List<Request> queries, List<DbParameters> dbParametersList){
        this.queries = queries;
        this.dbParametersList = dbParametersList;
        this.mapApi = new OverpassMapDataApi(new OsmConnection("https://overpass-api.de/api/", "my user agent"));
    }

    public OverpassMapDataApi getMapApi(){
        return mapApi;
    }

    public List<Request> getQueries(){
        return queries;
    }

    public void setQueries(List<Request> queries){
        this.queries = queries;
    }

    public void setMapApi(OverpassMapDataApi mapApi){
        this.mapApi = mapApi;
    }

    public List<String> getOverpassQueries(){
        return overpassQueries;
    }

    public void setOverpassQueries(List<String> overpassQueries){
        this.overpassQueries = overpassQueries;
    }

    public DbParameters getDbParameters() {
        return dbParameters;
    }

    public void setDbParameters(DbParameters dbParameters) {
        this.dbParameters = dbParameters;
    }

    public List<DbParameters> getDbParametersList() {
        return dbParametersList;
    }

    public void setDbParametersList(List<DbParameters> dbParametersList) {
        this.dbParametersList = dbParametersList;
    }

    public List<String> createQueries(){
        List<String> queries = new ArrayList<>();
        String osmQuery;
        for(Request request : this.queries){
            if(!request.getIsConnect()){
                osmQuery = createFrom(request.getQuery());
                System.out.println(osmQuery);
                queries.add(osmQuery);
            }else{
                queries.add(request.getConnectStatement());
            }
        }
        return queries;
    }

    public List<String> getEntities(){
        List<String> entities = new ArrayList<>();
        for(Request request : this.queries){
            if(!request.getIsConnect())
                entities.add(request.getQuery().getEntity());
        }
        return entities;
    }

    public List<List<String>> getAttributes(){
        List<List<String>> attributesToSearch = new ArrayList<>();
        for(Request request : this.queries){
            if(!request.getIsConnect())
                attributesToSearch.add(request.getQuery().getAttributeDefinition());
        }
        return attributesToSearch;
    }

    public String createFrom(MessageQuery query){
        StringBuilder from = new StringBuilder();
        from.append("[timeout:180];\n(\n");
        if(query.getFromExpression().contains("(")){
            query.setFromExpression(query.getFromExpression().replace("(", "").replace(")", ""));
        }
        if(!query.getFromExpression().contains("AND") && !query.getFromExpression().contains("OR")){//only one attribute
            String[] attributes = query.getFromExpression().split("=");
            from.append(doFrom(query, attributes));
        }else{ //there are AND, OR
            List<String> listQueryParts = splitOr(query.getFromExpression());
            if(listQueryParts.size() == 1){ // no OR but must be an AND
                List<String> trozos = splitAnd(new ArrayList<>(), query.getFromExpression());
                from.append(doFromOnlyAnd(query, trozos));
            }else{
                List<List<String>> finalList = splitAll(listQueryParts);
                from.append(doFromWithOr(finalList, query.getBbox(), query.getElements()));
            }
        }
        return from.toString();
    }

    private List<List<String>> splitAll(List<String> listQueryParts){
        List<List<String>> finalList = new ArrayList<>();
        for(String element : listQueryParts){
            List<String> andElements = splitAnd(new ArrayList<>(), element);
            finalList.add(andElements);
        }
        //list that, in each position, has a list with the elements of each OR and each position of the inner
        // lists is one of the elements of the AND
        return finalList;
    }

    private List<String> splitAnd(List<String> list, String fromDefinition){
        List<String> aux = List.of(fromDefinition.split(Separator.AND.toString()));
        for(String trozo : aux){
            if(trozo.contains(Separator.AND.toString())) splitAnd(list, trozo);
            else list.add(trozo);
        }
        return list;
    }

    private List<String> splitOr(String fromDefinition){
        List<String> orList = new ArrayList<>(List.of(fromDefinition.split(" ")));
        orList.removeAll(Collections.singletonList("OR"));
        return orList;
    }

    private String doFromOnlyAnd(MessageQuery query, List<String> trozos){
        StringBuilder from = new StringBuilder();
        for(String element : query.getElements()){
            from.append(element);
            for(String trozo : trozos){
                from.append("[").append("\"");
                if(trozo.contains("!")){ //is not null
                    from.append(trozo.replace("!", "").trim()).append("\"").append("]");
                    continue;
                }else{
                    from.append(trozo.split("=")[0].trim()).append("\"").append("=");
                    if(!trozo.split("=")[1].contains("\""))
                        from.append("\"").append(trozo.split("=")[1].trim()).append("\"").append("]");
                    else from.append(trozo.split("=")[1].trim()).append("]");
                }
            }
            from.append(query.getBbox()).append(";\n");
        }
        from.append(");\nout body;\n>;\nout skel qt;\n");
        return from.toString();
    }

    private String doFrom(MessageQuery query, String[] attributes){
        StringBuilder from = new StringBuilder();
        for(String element : query.getElements()){
            from.append(element);
            from.append("[").append("\"");
            if(attributes[0].contains("!")){ //attribute contains not null
                from.append(attributes[0].replace("!", "")).append("\"").append("]");
            }else{
                from.append(attributes[0]).append("\"");
                from.append("=").append(attributes[1]).append("]");
            }
            from.append(query.getBbox()).append(";\n");
        }
        from.append(");\nout body;\n>;\nout skel qt;\n");
        return from.toString();
    }

    private String doFromWithOr(List<List<String>> finalList, String bbox, List<String> elements){
        StringBuilder from = new StringBuilder();
        for(List<String> listElement : finalList){ //for each one the OR
            for(String nwr : elements){ // node, way, relation
                from.append(nwr);
                for(String element : listElement){ // add each AND element
                    from.append("[").append("\"");
                    if(element.contains("!")){
                        from.append(element.trim().replace("!", "")).append("\"").append("]");
                    }else{
                        from.append(element.split("=")[0].trim()).append("\"");
                        from.append("=").append(element.split("=")[1].trim()).append("]");
                    }
                }
                from.append(bbox).append(";\n");
            }
            from.append("\n");
        }
        from.append(");\nout body;\n>;\nout skel qt;\n");
        return from.toString();
    }
}
