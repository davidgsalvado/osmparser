/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import es.udc.fic.tfg.osmparser.DbConnection.DbElements;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class PostgreDatabase implements DbConnectionGenerator{

    private final static String BASE_STRING = "jdbc:postgresql://";
    private final static String CONFIG = "?useSSL=false&amp;allowPublicKeyRetrieval=true&amp;serverTimezone=Europe/Madrid";

    private String connectionString;

    public PostgreDatabase(String databaseName, String host, int port){
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
            DriverManager.setLoginTimeout(5); //maximum waiting time to establish a connection with the database
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
    public String createQueryStringForGeoJson(String entityName, String user, String password){
        String locationAttribute = getGeometryAttribute(entityName, user, password);
        StringBuilder queryString = new StringBuilder();

        queryString.append("SELECT jsonb_build_object(\n").append("'type', 'FeatureCollection',\n").
                append("'features', jsonb_agg(features.feature)\n) FROM (\n");
        queryString.append("SELECT jsonb_build_object(\n").append("'type'" + ", " + "'Feature'" + ",\n");
        queryString.append("'geometry', ST_AsGeoJSON(").append(locationAttribute).append(")::jsonb,\n");
        queryString.append("'properties', to_jsonb(inputs) - ").
                append("'").append(locationAttribute).append("'").append(") AS feature FROM (SELECT * FROM ")
                .append(entityName).append(") inputs) features;");

        return queryString.toString();
    }

    private String getGeometryAttribute(String entityName, String user, String password){
        try (Connection connection = DriverManager.getConnection(connectionString, user, password)){
            StringBuilder queryString = new StringBuilder();
            queryString.append("SELECT column_name FROM information_schema.columns WHERE table_name = '").
                    append(entityName.toLowerCase()).append("' AND data_type = 'USER-DEFINED'");
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryString.toString())){
                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StringBuilder addValuesQueryString(StringBuilder queryString, DbElements values, int count,
                                              List<DbElements> attributesListMap){
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
                        queryString.append("ST_MakePoint(?, ?)");
                    else queryString.append(values.getMap().get(key).getFunction()).append("(ST_MakePoint(?, ?))");
                } else if (values.getMap().get(key).getValue() instanceof LineString) { //way
                    LineString lineString = (LineString) values.getMap().get(key).getValue();
                    if (lineString.getCoordinates().length == 0){
                        wayWithoutNodes = true;
                        continue;
                    }
                    if (values.getMap().get(key).getFunction().equals(""))
                        queryString.append("ST_CENTROID(ST_MakeLine(ARRAY[");
                    else queryString.append(values.getMap().get(key).getFunction()).append("(ST_MakeLine(ARRAY[");
                    for (int i = 0; i < lineString.getCoordinates().length; i++) {
                        queryString.append("ST_MakePoint(?, ?)");
                        if (i < lineString.getCoordinates().length - 1)
                            queryString.append(", ");
                    }
                    if (values.getMap().get(key).getFunction().equals(""))
                        queryString.append("]))");
                    else queryString.append("]))");
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

