/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package es.udc.fic.tfg.osmparser.grammar;

import org.antlr.v4.runtime.tree.ParseTree;
import es.udc.fic.tfg.osmparser.DbConnection.DbParameters;
import es.udc.fic.tfg.osmparser.backend.model.util.ServerCallback;
import es.udc.fic.tfg.osmparser.grammar.OSMGrammarBaseVisitor;
import es.udc.fic.tfg.osmparser.grammar.OSMGrammarParser;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class CustomVisitor extends OSMGrammarBaseVisitor<List<MessageQuery>> {

    private List<Request> finalEntries;
    private List<MessageQuery> entries;
    private MessageQuery current; //current query
    private List<DbParameters> dbParameters = new ArrayList<>(); //several connection parameters
    private HandleGrammarSyntaxError handler;
    private ServerCallback serverCallback;
    private Long taskId;

    public CustomVisitor() throws FileNotFoundException {
        this.entries = new ArrayList<>();
        this.handler = new HandleGrammarSyntaxError();
    }

    public CustomVisitor(ServerCallback serverCallback, Long taskId) throws FileNotFoundException {
        this();
        this.serverCallback = serverCallback;
        this.taskId = taskId;
    }

    public List<MessageQuery> getEntries() {
        return entries;
    }

    public void setEntries(List<MessageQuery> entries) {
        this.entries = entries;
    }

    public MessageQuery getCurrent() {
        return current;
    }

    public void setCurrent(MessageQuery current) {
        this.current = current;
    }

    public List<DbParameters> getDbParameters() {
        return dbParameters;
    }

    public void setDbParameters(List<DbParameters> dbParameters) {
        this.dbParameters = dbParameters;
    }

    public List<Request> getFinalEntries() {
        return finalEntries;
    }

    public void setFinalEntries(List<Request> finalEntries) {
        this.finalEntries = finalEntries;
    }

    @Override
    public List<MessageQuery> visitConnectStatement(OSMGrammarParser.ConnectStatementContext ctx){
        dbParameters.add(new DbParameters());
        for(int i = 2; i < ctx.getChildCount(); i++){
            if(ctx.getChild(i).getClass() == OSMGrammarParser.PasswordElementContext.class){
                dbParameters.get(dbParameters.size()-1).setPassword(ctx.getChild(i).getText().split("=")[1]);
            }else if(ctx.getChild(i).getClass() == OSMGrammarParser.UserElementContext.class){
                dbParameters.get(dbParameters.size()-1).setUser(ctx.getChild(i).getText().split("=")[1]);
            }else if(ctx.getChild(i).getClass() == OSMGrammarParser.HostElementContext.class){
                dbParameters.get(dbParameters.size()-1).setHost(ctx.getChild(i).getText().split("=")[1]);
            }else if(ctx.getChild(i).getClass() == OSMGrammarParser.PortElementContext.class){
                dbParameters.get(dbParameters.size()-1).setPort(ctx.getChild(i).getText().split("=")[1]);
            }else if(ctx.getChild(i).getClass() == OSMGrammarParser.DbaseElementContext.class){
                dbParameters.get(dbParameters.size()-1).setDatabase(ctx.getChild(i).getText().split("=")[1]);
            }else if(ctx.getChild(i).getClass() == OSMGrammarParser.TypeElementContext.class){
                dbParameters.get(dbParameters.size()-1).setType(ctx.getChild(i).getText().split("=")[1]);
            }
        }
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitParse(OSMGrammarParser.ParseContext ctx){
        int maxCount = ctx.getChildCount();
        int currentConnect = 0;
        int currentStatement = 0;
        this.finalEntries = new ArrayList<>();
        for(int i = 0; i < maxCount; i++){
            if(ctx.getChild(i).getClass() == OSMGrammarParser.ConnectStatementContext.class){
                visitConnectStatement(ctx.connectStatement(currentConnect)); //we get the different connection parameters
                this.finalEntries.add(new Request(ctx.connectStatement(currentConnect).getText()));
                ++currentConnect;
            }else{
                this.entries = visitStatement(ctx.statement(currentStatement)); //we get the different queries
                ++currentStatement;
                this.finalEntries.add(new Request(this.entries.get(this.entries.size()-1))); //we add the last query
            }
        }
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitStatement(OSMGrammarParser.StatementContext ctx){
        int currentAttributeDef = 0;
        this.current = new MessageQuery();
        this.entries.add(this.current);
        visitElements(ctx.elements());
        ParseTree tree = ctx.getChild(2);
        for(int i = 1; i < tree.getChildCount(); i++){ // the child 0 of each statement is { so we avoid it
            if(tree.getChild(i).getClass() == OSMGrammarParser.BboxStatementContext.class){
                visitBboxDefinition(ctx.selectStatement().bboxStatement().bboxDefinition());
            }else if(tree.getChild(i).getClass() == OSMGrammarParser.EntityStatementContext.class){
                visitEntity(ctx.selectStatement().entityStatement().entity());
            }else if(tree.getChild(i).getClass() == OSMGrammarParser.AttributeDefinitionContext.class){
                visitAttributeDefinition(ctx.selectStatement().attributeDefinition(currentAttributeDef));
                ++currentAttributeDef;
            }else if(tree.getChild(i).getClass() == OSMGrammarParser.FromStatementContext.class){
                visitValueExpression(ctx.selectStatement().fromStatement().valueExpression());
            }
        }
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitBboxDefinition(OSMGrammarParser.BboxDefinitionContext ctx){
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Visiting Bbox: " + ctx.getText() + "\n", "NORMAL");
        System.out.println("Visiting Bbox: " + ctx.getText());
        this.current.setBbox(ctx.getText().split("=")[1]);
        this.entries.set(this.entries.indexOf(this.current), this.current);
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitEntity(OSMGrammarParser.EntityContext ctx){
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Visiting entity: " + ctx.getText() + "\n", "NORMAL");
        System.out.println("Visiting entity: " + ctx.getText());
        this.current.setEntity(ctx.getText());
        this.entries.set(this.entries.indexOf(this.current), this.current);
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitEntityStatement(OSMGrammarParser.EntityStatementContext ctx){
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, ctx.getText() + "\n", "NORMAL");
        System.out.println(ctx.getText());
        return this.entries;
    }


    @Override
    public List<MessageQuery> visitValueExpression(OSMGrammarParser.ValueExpressionContext ctx){
        int currentValueDef = 0;
        StringBuilder newValueExpression = new StringBuilder();
        String current;
        for(int i = 0; i < ctx.getChildCount(); i++){
            current = ctx.getChild(i).getText();
            if(current.contains("AND")){
                current = current.replace("AND", " AND ");
            }else if(current.contains("OR")){
                current = current.replace("OR", " OR ");
            } //unique element (no OR nor AND)
            if(current.contains("is not null")){
                current = current.replace("is not null", "!");
            }else if(!current.contains("=") &&
                    (ctx.getChild(i).getClass() == OSMGrammarParser.ValueDefinitionContext.class
                            || ctx.getChild(i).getClass() == OSMGrammarParser.ValueExpressionContext.class)){
                current = current.concat("!");
            }
            newValueExpression.append(current);
        }
        this.current.setFromExpression(newValueExpression.toString());
        this.entries.set(this.entries.indexOf(this.current), this.current);
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitValueDefinition(OSMGrammarParser.ValueDefinitionContext ctx){
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Visiting valueDefinition: " + ctx.getText() + "\n", "NORMAL");
        System.out.println("Visiting valueDefinition " + ctx.getText());
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitAttributeDefinition(OSMGrammarParser.AttributeDefinitionContext ctx){
        StringBuilder newAttribute = new StringBuilder();
        String attributeName = ctx.getChild(2).getText();
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Visiting map of " + attributeName + "\n", "NORMAL");
        System.out.println("Visiting map of " + attributeName);
        for(int i = 0; i < ctx.getChildCount(); i++){
            newAttribute.append(ctx.getChild(i).getText().replace("=>", " => "));
        }
        this.current.getAttributeDefinition().add(newAttribute.toString());
        this.entries.set(this.entries.indexOf(this.current), this.current);
        return this.entries;
    }

    @Override
    public List<MessageQuery> visitElements(OSMGrammarParser.ElementsContext ctx){
        if(serverCallback != null)
            serverCallback.sendLogs(taskId, "Visiting elements: " + ctx.getText() + "\n", "NORMAL");
        System.out.println("Visiting elements: " + ctx.getText());
        this.current.setElements(List.of(ctx.getText().split(",")));
        this.entries.set(this.entries.indexOf(this.current), this.current);
        return this.entries;
    }
}
