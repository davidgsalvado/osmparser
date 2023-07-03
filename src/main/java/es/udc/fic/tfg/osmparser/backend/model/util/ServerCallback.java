/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

public interface ServerCallback {

    void parseStart(Long taskId);

    void parseFinishWithError(Long taskId, String error);

    void parseFinishedOk(Long taskId);

    String getTaskState(Long taskId);

    void sendLogs(Long taskId, String log, String type);

    void checkAndAddDatabase(Long taskId, String database, int port, String host, String userDb, String password, String type);

    void addNewEntityTable(Long taskId, String database, int port, String host, String userDb, String password, String type,
                           String entityName);

}
