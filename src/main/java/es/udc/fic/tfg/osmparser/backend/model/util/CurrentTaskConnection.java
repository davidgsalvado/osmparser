/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

public class CurrentTaskConnection {

    private String currentConnection;
    private String user;
    private String password;

    public CurrentTaskConnection(){
        this.currentConnection = null;
        this.user = null;
        this.password = null;
    }

    public String getCurrentConnection() {
        return currentConnection;
    }

    public void setCurrentConnection(String currentConnection) {
        this.currentConnection = currentConnection;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
