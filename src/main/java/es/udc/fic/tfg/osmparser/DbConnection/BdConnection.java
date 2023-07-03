/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.DbConnection;

import es.udc.fic.tfg.osmparser.backend.model.util.DbConnectionContext;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.sql.*;
import java.util.*;

public class BdConnection {

    private String entityName;
    private List<DbElements> attributesListMap; //pares clave-valor con los atributos a guardar y los valores recibidos
    private final DbConnectionContext context;

    public BdConnection(String entityName, List<DbElements> attributesListMap, DbParameters dbParameters){
        this.entityName = entityName;
        this.attributesListMap = attributesListMap;
        this.context = new DbConnectionContext(dbParameters.getType());
        context.create(dbParameters.getDatabase(), dbParameters.getHost(), Integer.parseInt(dbParameters.getPort()));
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public List<DbElements> getAttributesMap() {
        return attributesListMap;
    }

    public void setAttributesMap(List<DbElements> attributesMap) {
        this.attributesListMap = attributesMap;
    }

    public static Boolean checkTableExists(Connection connection, String tableName) throws SQLException{
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet res = meta.getTables(null, null, tableName,
                new String[] {"TABLE"});
        if(res.next())
            return true;
        return false;
    }

    public synchronized void create(Connection connection){
        StringBuilder queryString;
        int count;
        for (DbElements values : attributesListMap) {
            if(values.getMap().isEmpty()){
                break;
            }
            count = 1;
            queryString = new StringBuilder("INSERT INTO " + entityName + " (");
            for (String key : values.getMap().keySet()) {
                queryString.append(key);
                if (count < values.getMap().size()){
                    queryString.append(", ");
                }
                ++count;
            }
            queryString.append(") VALUES (");
            count = 1;
            queryString = context.getDbConnectionGenerator().
                    addValuesQueryString(queryString, values, count, attributesListMap).append(")");
            if(queryString.charAt(0) == ')')
                continue;
            System.out.println(queryString);
            try(PreparedStatement preparedStatement = connection.prepareStatement(queryString.toString())){

                int i = 1;

                for(String key : values.getMap().keySet()){
                    if(values.getMap().get(key).getValue() instanceof String)
                        preparedStatement.setString(i++, values.getMap().get(key).getValue().toString());
                    else if(values.getMap().get(key).getValue() instanceof Boolean){
                        Boolean value = (Boolean) values.getMap().get(key).getValue();
                        preparedStatement.setBoolean(i++, value);
                    }else if(values.getMap().get(key).getValue() instanceof Point){
                        Point point = (Point) values.getMap().get(key).getValue();
                        preparedStatement.setDouble(i++, point.getX());
                        preparedStatement.setDouble(i++, point.getY());
                    }else if(values.getMap().get(key).getValue() instanceof LineString){
                        LineString lineString = (LineString) values.getMap().get(key).getValue();
                        for(Coordinate coordinate : lineString.getCoordinates()){
                            preparedStatement.setDouble(i++, coordinate.getX());
                            preparedStatement.setDouble(i++, coordinate.getY());
                        }
                    }
                }
                preparedStatement.executeUpdate();
                System.out.println("Insertion completed succesfully");
            } catch (SQLException e){
                throw new RuntimeException(e);
            }
        }
    }
}
