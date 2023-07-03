/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import java.util.concurrent.Future;

public class TaskInfo {

    private Future<?> future;
    private boolean canceled;

    public TaskInfo(){
        this.canceled = false;
    }

    public Future<?> getFuture() {
        return future;
    }

    public void setFuture(Future<?> future){
        this.future = future;
    }

    public boolean getIsCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
