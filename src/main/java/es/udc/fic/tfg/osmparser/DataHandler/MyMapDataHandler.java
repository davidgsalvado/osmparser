/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.DataHandler;

import es.udc.fic.tfg.osmparser.DbConnection.DbElements;
import es.udc.fic.tfg.osmparser.OsmConnection.OSMToBd;
import es.udc.fic.tfg.osmparser.backend.model.util.ServerCallback;
import de.westnordost.osmapi.common.errors.OsmApiException;
import de.westnordost.osmapi.common.errors.OsmApiReadResponseException;
import de.westnordost.osmapi.common.errors.OsmConnectionException;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.RelationMember;
import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import org.locationtech.jts.geom.*;

public class MyMapDataHandler implements MapDataHandler {

    static final int SRID=4326;

    private List<String> attributesToSearch;
    private String entityName;
    private List<DbElements> values;
    private List<String> functions;
    private OverpassMapDataApi mapApi;
    private ServerCallback serverCallback;
    private Long taskId;

    public MyMapDataHandler(List<String> attributesToSearch, String entityName, OverpassMapDataApi mapApi){
        this.attributesToSearch = attributesToSearch; // name => name
        this.entityName = entityName;
        values = new ArrayList<>();
        functions = new ArrayList<>();
        this.mapApi = mapApi;
    }

    public MyMapDataHandler(List<String> attributesToSearch, String entityName, OverpassMapDataApi mapApi,
                            ServerCallback serverCallback, Long taskId){
        this(attributesToSearch, entityName, mapApi);
        this.serverCallback = serverCallback;
        this.taskId = taskId;
    }

    public OverpassMapDataApi getMapApi() {
        return mapApi;
    }

    public void setMapApi(OverpassMapDataApi mapApi) {
        this.mapApi = mapApi;
    }

    public List<String> getAttributesToSearch() {
        return attributesToSearch;
    }

    public void setAttributesToSearch(List<String> attributesToSearch) {
        this.attributesToSearch = attributesToSearch;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public List<DbElements> getMap() {
        return values;
    }

    public void setMap(List<DbElements> values) {
        this.values = values;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public void setFunctions(List<String> functions) {
        this.functions = functions;
    }

    public ServerCallback getServerCallback() {
        return serverCallback;
    }

    private boolean checkFunction(String value){
        boolean check = false;
        for(FunctionSelector.PredefinedFunctions function : FunctionSelector.PredefinedFunctions.values()){
            if(Objects.equals(function.toString(), value.substring(0, value.indexOf("(")))){
                check = true;
                break;
            }
        }
        return check;
    }

    private String getValueWithNoPar(String value){
        if(value.contains("("))
            return value.substring(value.indexOf("(")+1, value.indexOf(")"));
        else return value;
    }

    private List<String> getValuesToSearch(int attributePosition){
        List<String> values = new ArrayList<>();
        String attribute = attributesToSearch.get(attributePosition);
        String valueToSearch = attribute.split("=>")[0].trim(); //name || name, name:es, name:gl

        if(valueToSearch.contains(",")){
            for(String attr : valueToSearch.split(",")){
                String attrTrimed = attr.trim();
                String attrTrimedNoFunction = getValueWithNoPar(attrTrimed);
                if(attrTrimed.contains("(") && checkFunction(attrTrimed)){ // comprobamos si la función pasada existe
                    functions.add(attrTrimed); // add function with the parameters
                    values.add(attrTrimedNoFunction);
                }else {
                    if(!attrTrimed.contains("("))
                        values.add(attrTrimed);
                    else values.add(attrTrimedNoFunction);
                }
            }
        }else{
            String valueNoFunction = getValueWithNoPar(valueToSearch);
            if(valueToSearch.contains("(") && checkFunction(valueToSearch)){
                functions.add(valueToSearch); // add the function with the parameters (toBoolean(parameter))
                values.add(valueNoFunction); //parameter
            }else {
                if(!valueToSearch.contains("("))
                    values.add(valueToSearch);
                else values.add(valueNoFunction);
            }
        }
        return values;
    }

    private String getBdField(int attributePosition){
        return attributesToSearch.get(attributePosition).split("=>")[1];
    }

    private int getDefault(List<String> values){
        for(int i = 0; i < values.size(); i++){
            if(values.get(i).contains("\"")){
                return i;
            }
        }
        return -1;
    }

    private String getFunctionName(String value){
        for(String function : functions){
            if(function.contains(value))
                return function.substring(0, function.indexOf("("));
        }
        return "";
    }

    private String getGeomFunction(int position){
        return attributesToSearch.get(position).split("=>")[0]; // geom
    }

    public List<Map<String, Double>> getNodeCoordinates(Way way, List<String> wayNodesQueries) throws InterruptedException {
        StringBuilder query = new StringBuilder("[timeout:180];\nnode(id:");
        int count = 1;
        for(Long id : way.getNodeIds()){
            query.append(id.toString());
            if(count < way.getNodeIds().size())
                query.append(",");
            ++count;
        }
        query.append(");\nout body;\n>;\nout skel qt;\n");
        wayNodesQueries.add(query.toString());
        OSMToBd osmToBd = new OSMToBd(wayNodesQueries, mapApi);
        return osmToBd.doWayNodesOSMConnection(serverCallback, taskId, 0);
    }

    private void addNewValues(DbElements aux){
        values.add(aux);
    }

    @Override
    public void handle(BoundingBox boundingBox) {
        //
    }

    @Override
    public void handle(Node node) {
        process(node);
    }

    @Override
    public void handle(Way way) {
        try {
            process(way);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(Relation relation) {
        try {
            process(relation);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void process(Node node){
        boolean found;
        String currentBdField;
        List<String> currentNodeValues;
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Processing node: " + node.getId() + "\n", "NORMAL");
        System.out.println("Processing node: " + node.getId());
        DbElements aux = new DbElements();
        List<String> valuesToFunction = new ArrayList<>();
        for(int position = 0; position < attributesToSearch.size(); position++){
            found = false;
            currentBdField = getBdField(position).trim();
            currentNodeValues = getValuesToSearch(position);
            for(Map.Entry<String, String> entry : node.getTags().entrySet()){
                if(currentNodeValues.contains(entry.getKey())){
                    String function = getFunctionName(entry.getKey());
                    if(!function.equals("")){ //we need to apply function
                        valuesToFunction.add(entry.getValue());
                        aux.addValues(currentBdField, FunctionSelector.selectFunction(function, valuesToFunction), false);
                    }else{
                        aux.addValues(currentBdField, entry.getValue(), false);
                    }
                    found = true;
                    break;
                }
            }
            if(!found && getDefault(currentNodeValues) != -1){ //we add the default value in case no value was found on the feature
                aux.addValues(currentBdField, currentNodeValues.get(getDefault(currentNodeValues)).replace("\"", ""), false);
            }
            if(currentNodeValues.contains("geom")){
                String geomValue = getGeomFunction(position);
                Coordinate coordinate = new Coordinate(node.getPosition().getLongitude(), node.getPosition().getLatitude());
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), SRID);
                Point point = geometryFactory.createPoint(coordinate);
                if(geomValue.contains("("))
                    aux.addValues(currentBdField, point, true, geomValue.substring(0, geomValue.indexOf("(")));
                else aux.addValues(currentBdField, point, true);
            }
        }
        if(!values.contains(aux)){
            addNewValues(aux);
        }
    }

    private void process(Way way) throws InterruptedException {
        boolean found;
        String currentBdField;
        List<String> currentWayValues;
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Processing way: " + way.getId() + " with " + way.getNodeIds().size() + " nodes\n",
                    "NORMAL");
        System.out.println("Processing way: " + way.getId() + " with " + way.getNodeIds().size() + " nodes");
        DbElements aux = new DbElements();
        List<String> valuesToFunction = new ArrayList<>();
        List<String> wayNodesQueries = new ArrayList<>();
        for(int position = 0; position < attributesToSearch.size(); position++){
            found = false;
            currentBdField = getBdField(position).trim();
            currentWayValues = getValuesToSearch(position);
            for(Map.Entry<String, String> entry : way.getTags().entrySet()){
                if(currentWayValues.contains(entry.getKey())){
                    String function = getFunctionName(entry.getKey());
                    if(!function.equals("")){ //we need to apply function
                        valuesToFunction.add(entry.getValue());
                        aux.addValues(currentBdField, FunctionSelector.selectFunction(function, valuesToFunction), false);
                    }else {
                        aux.addValues(currentBdField, entry.getValue(), false);
                    }
                    found = true;
                    break;
                }
            }
            if(currentWayValues.contains("geom")){
                String geomValue = getGeomFunction(position);
                found = true;
                //we create the query with the id of each node and we obtain the coordinates of the way nodes
                List<Map<String, Double>> nodeCoordinates = getNodeCoordinates(way, wayNodesQueries);
                Coordinate[] coordinatesArr = new Coordinate[nodeCoordinates.size()]; // array de coordenadas que se usará para crear el linestring
                List<Coordinate> coordinates = new ArrayList<>(List.of());
                for(Map<String, Double> map : nodeCoordinates){
                    coordinates.add(new Coordinate(map.get("lon"), map.get("lat"))); //añadimos coordenadas a la lista
                }
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), SRID);
                //we convert the list into an array and use it to create the LineString
                LineString lineString = geometryFactory.createLineString(coordinates.toArray(coordinatesArr));
                if(geomValue.contains("(")) //hay función a aplicar
                    aux.addValues(currentBdField, lineString, true, geomValue.substring(0, geomValue.indexOf("(")));
                else
                    aux.addValues(currentBdField, lineString, true);
            }
            if(!found && getDefault(currentWayValues) != -1){
                aux.addValues(currentBdField, currentWayValues.get(getDefault(currentWayValues)).replace("\"", ""), false);
            }
        }
        if(!values.contains(aux)){
            addNewValues(aux);
        }
    }

    private void process(Relation relation) throws InterruptedException {
        boolean found;
        String currentBdField;
        List<String> currentRelationValues;
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Processing relation: " + relation.getId() + "\n", "NORMAL");
        System.out.println("Processing relation: " + relation.getId());
        DbElements aux = new DbElements();
        List<String> valuesToFunction = new ArrayList<>();
        for(RelationMember member : relation.getMembers()){
            if(member.getType().toString().equals("WAY")){
                System.out.println("Processing " + member.getType().toString()+ " from relation with id " + relation.getId());
                processWithType("way", member.getRef(), 0);
            }
            else if(member.getType().toString().equals("NODE")){
                System.out.println("Processing " + member.getType().toString()+ " from relation with id " + relation.getId());
                processWithType("node", member.getRef(), 0);
            }
        }
        for(int position = 0; position < attributesToSearch.size(); position++){
            found = false;
            currentBdField = getBdField(position).trim();
            currentRelationValues = getValuesToSearch(position);
            for(Map.Entry<String, String> entry : relation.getTags().entrySet()){
                if(currentRelationValues.contains(entry.getKey())){
                    String function = getFunctionName(entry.getKey());
                    if(!function.equals("")){ //we need to apply function
                        valuesToFunction.add(entry.getValue());
                        aux.addValues(currentBdField, FunctionSelector.selectFunction(function, valuesToFunction), false);
                    }else{
                        aux.addValues(currentBdField, entry.getValue(), false);
                    }
                    found = true;
                    break;
                }
            }
            if(!found && getDefault(currentRelationValues) != -1){
                aux.addValues(currentBdField, currentRelationValues.get(getDefault(currentRelationValues)).replace("\"", ""), false);
            }
            if(!values.contains(aux)){
                addNewValues(aux);
            }
        }
    }

    private void processWithType(String type, Long id, int times) throws InterruptedException {
        if(times == 0)
            TimeUnit.SECONDS.sleep(2);
        else if(times > 0)
            TimeUnit.SECONDS.sleep(10);
        if(times == OSMToBd.MAX_TRIES)
            throw new RuntimeException();
        try{
            mapApi.queryElements("[timeout:180];\n(" + type + "(" + id.toString() + ");\n);out body;\n>;\nout skel qt;", this);
        }catch (OsmConnectionException e){
            System.out.println("ERROR Code " + e.getErrorCode()+ ": OsmConnectionException; " + e.getDescription());
            processWithType(type, id, ++times);
        }catch (OsmApiException e){
            System.out.println("ERROR Code " + e.getErrorCode()+ ": OsmApiException; " + e.getDescription());
            processWithType(type, id, ++times);
        } catch (OsmApiReadResponseException e){
            System.out.println("OsmApiReadResponseException: ");
            e.printStackTrace();
            processWithType(type, id, ++times);
        }
    }
}
