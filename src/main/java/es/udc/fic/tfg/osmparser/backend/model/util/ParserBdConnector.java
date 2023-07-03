/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ParserBdConnector {

    private final DbConnectionContext context;

    public ParserBdConnector(String type){
        this.context = new DbConnectionContext(type);
    }

    public Connection getConnection(String database, String host, int port, String user, String password)
            throws SQLException {

        context.create(database, host, port);

        return DriverManager.getConnection(context.getDbConnectionGenerator().getCompleteConnectionString(), user, password);
    }

    public String getQueryStringGeoJson(String entityName, String user, String password){
        return context.getDbConnectionGenerator().createQueryStringForGeoJson(entityName, user, password);
    }

}
