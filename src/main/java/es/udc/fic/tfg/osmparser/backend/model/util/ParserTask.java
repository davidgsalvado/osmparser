/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParserTask {

    private Long taskId;
    private String task;
    private AtomicBoolean waiting;

    public ParserTask(Long taskId, String task) {
        this.taskId = taskId;
        this.task = task;
        this.waiting = new AtomicBoolean(true);
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public AtomicBoolean getIsWaiting(){
        return waiting;
    }

    public void setWaiting(AtomicBoolean waiting){
        this.waiting = waiting;
    }
}
