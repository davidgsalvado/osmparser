/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import es.udc.fic.tfg.osmparser.DbConnection.DbElements;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLDatabase implements DbConnectionGenerator{

    private final static String BASE_STRING = "jdbc:mysql://";
    private final static String CONFIG = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Madrid";

    private String connectionString;

    public MySQLDatabase(String databaseName, String host, int port){
        StringBuilder builder = new StringBuilder();

        builder.append(BASE_STRING);
        builder.append(host).append(":").append(port).append("/");
        builder.append(databaseName).append(CONFIG);

        this.connectionString = builder.toString();
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public boolean testConnection(String user, String password) {
        try{
            DriverManager.setLoginTimeout(5);
            Connection conn = DriverManager.getConnection(connectionString, user, password);

            return conn != null;
        }catch (SQLException ex){
            System.out.println(ex.getMessage());
            return false;
        }
    }

    @Override
    public String getCompleteConnectionString(){
        return connectionString;
    }

    @Override
    public String createQueryStringForGeoJson(String entityName, String user, String password) {
        String locationAttribute = getGeometryAttribute(entityName, user, password);
        StringBuilder queryString = new StringBuilder();
        List<String> attributes = getEntityAttributes(entityName, user, password, locationAttribute);

        queryString.append("SELECT JSON_OBJECT(\n").append("'type', 'FeatureCollection',\n").
                append("'features', JSON_ARRAYAGG(\n").append("JSON_OBJECT(\n").append("'type', 'Feature',\n").
                append("'geometry', ST_AsGeoJSON(").append(locationAttribute).append("),\n")
                .append("'properties', JSON_OBJECT(\n");

        for (int i = 0; i < attributes.size(); i++){
            queryString.append("'").append(attributes.get(i)).append("', ").append(attributes.get(i));
            if (i < attributes.size() - 1)
                queryString.append(",\n");
        }
        queryString.append("\n)))) as geojson\nFROM ").append(entityName);

        return queryString.toString();
    }

    private String getGeometryAttribute(String entityName, String user, String password){
        try (Connection connection = DriverManager.getConnection(connectionString, user, password)){
            StringBuilder queryString = new StringBuilder();
            queryString.append("SELECT column_name FROM information_schema.columns WHERE table_name = '").
                    append(entityName.toLowerCase()).append("' AND data_type = 'geometry'");
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryString.toString())){
                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getEntityAttributes(String entityName, String user, String password, String locationAttribute){
        List<String> attributes = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(connectionString, user, password)){
            StringBuilder queryString = new StringBuilder();
            queryString.append("SELECT column_name FROM information_schema.columns WHERE table_name = '").
                    append(entityName.toLowerCase()).append("'");
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryString.toString())){
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()){
                    if (!resultSet.getString(1).equalsIgnoreCase(locationAttribute))
                        attributes.add(resultSet.getString(1));
                }
                return attributes;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StringBuilder addValuesQueryString(StringBuilder queryString, DbElements values, int count, List<DbElements> attributesListMap) {
        boolean wayWithoutNodes = false;
        boolean pointPartOfNode = false;
        for (String key : values.getMap().keySet()) {
            if (values.getMap().get(key).isGeom()) {
                if (values.getMap().get(key).getValue() instanceof Point) {//node
                    Point point = (Point) values.getMap().get(key).getValue();
                    boolean isPart = checkPointPartOfWay(point, attributesListMap);
                    if (isPart){
                        pointPartOfNode = true;
                        break; //go to next element
                    }
                    if (values.getMap().get(key).getFunction().equals("")) //no function to apply
                        queryString.append("POINT(?, ?)");
                    else queryString.append(values.getMap().get(key).getFunction()).append("(POINT(?, ?))");
                } else if (values.getMap().get(key).getValue() instanceof LineString) { //way
                    LineString lineString = (LineString) values.getMap().get(key).getValue();
                    if (lineString.getCoordinates().length == 0){
                        wayWithoutNodes = true;
                        continue;
                    }
                    if (values.getMap().get(key).getFunction().equals(""))
                        queryString.append("LINESTRING(");
                    else queryString.append(values.getMap().get(key).getFunction()).append("(LINESTRING(");
                    for (int i = 0; i < lineString.getCoordinates().length; i++) {
                        queryString.append("POINT(?, ?)");
                        if (i < lineString.getCoordinates().length - 1)
                            queryString.append(", ");
                    }
                    if (values.getMap().get(key).getFunction().equals(""))
                        queryString.append(")");
                    else queryString.append("))");
                }
            } else queryString.append("?");

            if (count < values.getMap().size()){
                queryString.append(", ");
            }
            ++count;
        }
        if(wayWithoutNodes || pointPartOfNode)
            return new StringBuilder();
        return queryString;
    }

}
