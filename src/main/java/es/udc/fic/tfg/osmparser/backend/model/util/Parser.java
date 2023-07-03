/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.backend.model.util;

import es.udc.fic.tfg.osmparser.DbConnection.BdConnection;
import es.udc.fic.tfg.osmparser.DbConnection.DbParameters;
import es.udc.fic.tfg.osmparser.OsmConnection.OSMToBd;
import es.udc.fic.tfg.osmparser.backend.model.exceptions.*;
import es.udc.fic.tfg.osmparser.grammar.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.postgresql.util.PSQLException;
import es.udc.fic.tfg.osmparser.backend.model.exceptions.*;
import es.udc.fic.tfg.osmparser.grammar.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class Parser {

    private static List<ParserTask> taskList;
    private static Map<Long, TaskInfo> threadList;

    public Parser(){
        taskList = new ArrayList<>();
        threadList = new HashMap<>();
    }

    private CreateQuery getQueries(ParserTask parserTask, ServerCallback serverCallback) throws FileNotFoundException {
        CharStream stream = CharStreams.fromString(parserTask.getTask());
        OSMGrammarLexer lexer = new OSMGrammarLexer(stream);
        OSMGrammarParser parser  = new OSMGrammarParser(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ErrorListener.INSTANCE);
        parser.removeErrorListeners();
        parser.addErrorListener(ErrorListener.INSTANCE);

        OSMGrammarParser.ParseContext ctx = parser.parse();

        CustomVisitor listener = new CustomVisitor(serverCallback, parserTask.getTaskId());

        listener.visitParse(ctx);

        return new CreateQuery(listener.getFinalEntries(), listener.getDbParameters());
    }

    public void setConnectionParameters(CurrentTaskConnection currentTaskConnection, DbParameters dbParameters)
            throws InvalidDatabaseException {
        DbConnectionContext context = new DbConnectionContext(dbParameters.getType());

        boolean canConnect = context.createAndTest(dbParameters.getDatabase(), dbParameters.getHost(),
                Integer.parseInt(dbParameters.getPort()), dbParameters.getUser(), dbParameters.getPassword()); // check database connection

        if(!canConnect)
            throw new InvalidDatabaseException("Database parameters are wrong"); // if we cannot connect using the given connection parameters, we cannot save the database

        currentTaskConnection.setUser(dbParameters.getUser());
        currentTaskConnection.setPassword(dbParameters.getPassword());
        currentTaskConnection.setCurrentConnection(context.getDbConnectionGenerator().getCompleteConnectionString());
    }

    public void setConnectionParameters(CurrentTaskConnection currentTaskConnection, DbParameters dbParameters,
                                        ServerCallback serverCallback, Long taskId)
            throws InvalidDatabaseException {
        DbConnectionContext context = new DbConnectionContext(dbParameters.getType());

        boolean canConnect = context.createAndTest(dbParameters.getDatabase(), dbParameters.getHost(),
                Integer.parseInt(dbParameters.getPort()), dbParameters.getUser(), dbParameters.getPassword()); // check database connection

        if(!canConnect){
            serverCallback.parseFinishWithError(taskId,
                    ExceptionUtils.getStackTrace(new InvalidDatabaseException("Database parameters are wrong")));
            System.out.println("THE TASK WITH ID " + taskId + " HAS FINISHED EXECUTION WITH AN ERROR");
            throw new InvalidDatabaseException("Database parameters are wrong"); // if we cannot connect using the given connection parameters, we cannot save the database
        }

        currentTaskConnection.setUser(dbParameters.getUser());
        currentTaskConnection.setPassword(dbParameters.getPassword());
        currentTaskConnection.setCurrentConnection(context.getDbConnectionGenerator().getCompleteConnectionString());
    }

    private Connection getConnection(CurrentTaskConnection currentTaskConnection) throws SQLException,
            InvalidQueryException {
        if (Objects.isNull(currentTaskConnection.getCurrentConnection()))
            throw new InvalidQueryException("You have not specified any database connection at the start of the query");
        return DriverManager.getConnection(currentTaskConnection.getCurrentConnection(),
                currentTaskConnection.getUser(), currentTaskConnection.getPassword());
    }

    public void parseQuery(Long taskId, String query, ServerCallback serverCallback){
        ParserTask parserTask = new ParserTask(taskId, query);
        taskList.add(parserTask);

        TaskInfo taskInfo = new TaskInfo();
        //executor manages requests' queue and assign tasks depending on thread availability on the pool
        Future<?> future = ExecutorSingleton.getInstance().submit(() -> {
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ": SLEEPING");
            Thread.sleep(1000);
            System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ": FINISHING SLEEPING");
            try {
                parse(parserTask, serverCallback);
                return null;
            } catch (InvalidQueryException | IOException | SQLException | InvalidDatabaseException |
                    InvalidDbParamsException | ClassNotFoundException | InterruptedException |
                    SyntaxErrorException | InstanceNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                cleanUpTaskList(parserTask);
                cleanUpThreadList(parserTask.getTaskId());
            }
        });
        taskInfo.setFuture(future);

        threadList.put(parserTask.getTaskId(), taskInfo);
    }

    public void parseQueryFromFile(CreateQuery queriesToParse) throws InvalidQueryException, InvalidDatabaseException,
            InvalidDbParamsException, InterruptedException {

        int connectCount = 0;
        int currentHandlerParameters = 0;
        OSMToBd osmToBd = new OSMToBd(queriesToParse.createQueries(), queriesToParse.getEntities(), queriesToParse.getAttributes(),
                OSMGrammar.createOsmConnection(), queriesToParse.getDbParametersList());

        CurrentTaskConnection currentTaskConnection = new CurrentTaskConnection();
        for (Request request : queriesToParse.getQueries()) {
            if(!request.getIsConnect()){ //assign new connection
                try (Connection connection = getConnection(currentTaskConnection)) {
                    if (request.getQuery().getEntity() == null || !BdConnection.checkTableExists(connection, request.getQuery().getEntity().toLowerCase())) {
                        throw new InvalidQueryException("You have not specified the table where you want to store the data or it does not exist");
                    }
                    DbParameters dbParameters = queriesToParse.getDbParametersList().get(connectCount - 1);
                    osmToBd.doConnection(connection, connectCount + currentHandlerParameters,
                            currentHandlerParameters, dbParameters);
                    ++currentHandlerParameters;
                } catch (SQLException e) {
                    if (e.getMessage().contains("no existe la base de datos")) {
                        throw new InvalidDatabaseException("The database you specified does not exist");
                    }else if (e.getMessage().contains("password")){
                        throw new InvalidDbParamsException("The user or password specified are not correct");
                    }
                }
            }else{ //we establish connection parameters
                setConnectionParameters(currentTaskConnection, queriesToParse.getDbParametersList().get(connectCount));
                ++connectCount;
            }
        }
    }

    public void parse(ParserTask parserTask, ServerCallback serverCallback) throws ParseCancellationException,
            InvalidQueryException, IOException, SQLException, InvalidDatabaseException, InvalidDbParamsException,
            ClassNotFoundException, InterruptedException, SyntaxErrorException, InstanceNotFoundException {
        if(threadList.get(parserTask.getTaskId()).getIsCanceled())
            return; //if the task was canceled before putting it in the running state we simply return
        serverCallback.sendLogs(parserTask.getTaskId(), "STARTING EXECUTION OF THE TASK\n", "INFO");
        System.out.println("STARTING EXECUTION OF TASK WITH ID: " + parserTask.getTaskId());
        serverCallback.parseStart(parserTask.getTaskId());
        CreateQuery queriesToParse = getQueries(parserTask, serverCallback);
        int connectCount = 0;
        int currentHandlerParameters = 0;
        OSMToBd osmToBd = new OSMToBd(queriesToParse.createQueries(), queriesToParse.getEntities(), queriesToParse.getAttributes(),
                OSMGrammar.createOsmConnection(), queriesToParse.getDbParametersList());
        CurrentTaskConnection currentTaskConnection = new CurrentTaskConnection();
        for (Request request : queriesToParse.getQueries()) {
            if(threadList.get(parserTask.getTaskId()).getIsCanceled())
                break;
            if(!request.getIsConnect()){ //assign new connection
                try (Connection connection = getConnection(currentTaskConnection)) {
                    if (request.getQuery().getEntity() == null || !BdConnection.checkTableExists(connection, request.getQuery().getEntity().toLowerCase())) {
                        serverCallback.parseFinishWithError(parserTask.getTaskId(),
                                ExceptionUtils.getStackTrace(new InvalidQueryException("You have not specified the table where you want " +
                                        "to store the data or it does not exist")));
                        serverCallback.sendLogs(parserTask.getTaskId(), "THE TASK FINISHED EXECUTION WITH AN ERROR\n" +
                                ExceptionUtils.getStackTrace(new InvalidQueryException("You have not specified the table where you want " +
                                        "to store the data or it does not exist")) + "\n", "INFO");
                        System.out.println("THE TASK WITH ID " + parserTask.getTaskId() + " HAS FINISHED EXECUTION WITH AN ERROR");
                        throw new InvalidQueryException("You have not specified the table where you want to store the data or it does not exist");
                    }
                    serverCallback.sendLogs(parserTask.getTaskId(), "GETTING INFORMATION FROM OpenStreetMap AND INSERT TO ENTITY " +
                                    request.getQuery().getEntity() + " \n",
                            "INFO");
                    System.out.println("GETTING INFORMATION FROM OpenStreetMap");
                    if (connectCount == 0){ //no connect in query
                        throw new InvalidQueryException("You have not specified any database connection");
                    }
                    DbParameters dbParameters = queriesToParse.getDbParametersList().get(connectCount - 1);
                    osmToBd.doConnection(connection, connectCount + currentHandlerParameters, currentHandlerParameters,
                            serverCallback, parserTask.getTaskId(), dbParameters);
                    ++currentHandlerParameters;
                    serverCallback.addNewEntityTable(parserTask.getTaskId(), dbParameters.getDatabase(),
                            Integer.parseInt(dbParameters.getPort()), dbParameters.getHost(), dbParameters.getUser(),
                            dbParameters.getPassword(), dbParameters.getType(), request.getQuery().getEntity());

                    serverCallback.sendLogs(parserTask.getTaskId(),"FINISHED INSERTION ON "
                            + request.getQuery().getEntity() + " TABLE\n", "INFO");
                    System.out.println("FINISHED INSERTION ON " + request.getQuery().getEntity() + " TABLE");
                } catch (PSQLException e) {
                    if (e.getMessage().contains("no existe la base de datos")) {
                        serverCallback.parseFinishWithError(parserTask.getTaskId(),
                                ExceptionUtils.getStackTrace(new InvalidDatabaseException("The database you specified does not exist")));
                        serverCallback.sendLogs(parserTask.getTaskId(), "THE TASK FINISHED EXECUTION WITH AN ERROR\n" +
                                ExceptionUtils.getStackTrace(new InvalidDatabaseException("The database you specified does not exist")) + "\n",
                                "ERROR");
                        System.out.println("THE TASK WITH ID " + parserTask.getTaskId() + " HAS FINISHED EXECUTION WITH AN ERROR");
                        throw new InvalidDatabaseException("The database you specified does not exist");
                    }else if (e.getMessage().contains("password")){
                        serverCallback.parseFinishWithError(parserTask.getTaskId(),
                                ExceptionUtils.getStackTrace(new InvalidDbParamsException("The user or password specified are not correct")));
                        serverCallback.sendLogs(parserTask.getTaskId(), ExceptionUtils.getStackTrace(
                                new InvalidDbParamsException("The user or password specified are not correct")) + "\n", "ERROR");
                        System.out.println("THE TASK WITH ID " + parserTask.getTaskId() + " HAS FINISHED EXECUTION WITH AN ERROR");
                        throw new InvalidDbParamsException("The user or password specified are not correct");
                    }
                }
            }else{ //we establish connection parameters
                DbParameters dbParameters = queriesToParse.getDbParametersList().get(connectCount);
                setConnectionParameters(currentTaskConnection, dbParameters, serverCallback,
                        parserTask.getTaskId());
                serverCallback.checkAndAddDatabase(parserTask.getTaskId(), dbParameters.getDatabase(),
                        Integer.parseInt(dbParameters.getPort()), dbParameters.getHost(), dbParameters.getUser(),
                        dbParameters.getPassword(), dbParameters.getType());
                ++connectCount;
            }
        }
        if(!threadList.get(parserTask.getTaskId()).getIsCanceled()){ //end without error and cancelling
            serverCallback.parseFinishedOk(parserTask.getTaskId());
            serverCallback.sendLogs(parserTask.getTaskId(), "THE TASK FINISHED SUCCESSFULLY\n", "INFO");
            System.out.println("THE TASK WITH ID " + parserTask.getTaskId() + " HAS FINISHED EXECUTION SUCCESSFULLY");
        }
    }

    public void cancelTask(Long taskId, ServerCallback serverCallback) {

        TaskInfo task = threadList.get(taskId);
        ParserTask parserTask = taskList.stream().filter(t -> Objects.equals(t.getTaskId(), taskId)).findFirst().orElse(null);
        ReentrantLock lock = new ReentrantLock();

        assert parserTask != null;

        serverCallback.sendLogs(parserTask.getTaskId(), "CANCELLING...\n", "INFO");
        if(Objects.equals(serverCallback.getTaskState(parserTask.getTaskId()), "WAITING")) {
            task.getFuture().cancel(true); //task will never begin its execution
            cleanUpTaskList(parserTask);
            cleanUpThreadList(taskId);
            serverCallback.sendLogs(parserTask.getTaskId(), "TASK CANCELLED SUCCESSFULLY\n", "INFO");
        }else{
            try{
                lock.lock();
                try{
                    task.setCanceled(true);
                    task.getFuture().get(); //wait for the task to end after cancellation
                    cleanUpTaskList(parserTask);
                    cleanUpThreadList(taskId);
                    serverCallback.sendLogs(parserTask.getTaskId(), "TASK CANCELLED SUCCESSFULLY\n", "INFO");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }finally {
                lock.unlock();
            }
        }
    }

    private void cleanUpTaskList(ParserTask parserTask){
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try{
            taskList.remove(parserTask);
        }finally {
            lock.unlock();
        }
    }

    private void cleanUpThreadList(Long taskId){
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try{
            threadList.remove(taskId);
        }finally {
            lock.unlock();
        }
    }

}
