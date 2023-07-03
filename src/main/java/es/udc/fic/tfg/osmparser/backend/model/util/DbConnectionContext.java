/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

public class DbConnectionContext {

    private DbConnectionGenerator dbConnectionGenerator;
    private String databaseType;

    public DbConnectionContext(String databaseType){
        this.databaseType = databaseType;
    }

    public DbConnectionGenerator getDbConnectionGenerator() {
        return dbConnectionGenerator;
    }

    public void setDbConnectionGenerator(DbConnectionGenerator dbConnectionGenerator) {
        this.dbConnectionGenerator = dbConnectionGenerator;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public void create(String databaseName, String host, int port){
        switch(databaseType){
            case "PostgreSQL": {
                setDbConnectionGenerator(new PostgreDatabase(databaseName, host, port));
                break;
            }
            case "MySQL": {
                setDbConnectionGenerator(new MySQLDatabase(databaseName, host, port));
                break;
            }
            default: break;
        }
    }

    public boolean createAndTest(String databaseName, String host, int port, String user, String password){
        create(databaseName, host, port);

        return dbConnectionGenerator.testConnection(user, password);
    }
}
