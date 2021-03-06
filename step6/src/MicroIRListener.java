import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class MicroIRListener extends MicroBaseListener{
	private ArrayList<IRNode> IRList = new ArrayList<IRNode>();
	private ArrayList<TinyNode> TNList = new ArrayList<TinyNode>();
	private ParseTreeProperty<Node> PTProperty =  new ParseTreeProperty<Node>();
	private HashMap<String, String> typeMap = new HashMap<String, String>();//Identifier type
	private HashMap<String, HashMap<String,String>> functionTypeMap = new HashMap<String, HashMap<String,String>>();
	private HashMap<String, HashMap<String,String>> exprMap = new HashMap<String, HashMap<String,String>>();
	private HashMap<String, String> regMap = new HashMap<String, String>();//Tiny
	private HashMap<String, String> tinyMap = new HashMap<String, String>();//Tiny
	private HashMap<String, String> IRregMap = new HashMap<String, String>();
	private HashMap<String, Function> functionMap = new HashMap<String, Function>();//All functions
	private HashSet<String>globalVar = new HashSet<String>();
	private ArrayList<String> opList = new ArrayList<String>();
	private Stack<LabelNode> labelStack = new Stack<LabelNode>();
	private String currFunction = "global";
	private String currLabel;
	private int registerCount = 1;
	private int tinyCount = 0;
	private int labelNum = 1;
	private int localNum = 0;
	private int paraNum = 1;
	private int pushflag = 0;
	private int globalStringCount = 0;
	

	private void addopList() {
		opList.add("ADDI");
		opList.add("ADDF");
		opList.add("SUBI");
		opList.add("SUBF");
		opList.add("MULTI");
		opList.add("MULTF");
		opList.add("DIVI");
		opList.add("DIVF");
		return;
	}

	private String getTinyReg(String str) {
		if(str.contains("$") && regMap.containsKey(str)) {
			return regMap.get(str);
		} else if(str.contains("$") && str.length() > 2){
			if (str.charAt(1) == 'L') {
				regMap.put(str,("$-" + str.substring(2)));
				return ("$-" + str.substring(2));
			} else if(str.charAt(1) == 'P') {
				regMap.put(str, ("$" + (6 + paraNum - Integer.parseInt(str.substring(2)))));
				return ("$" + (6 + paraNum - Integer.parseInt(str.substring(2))));
			} else if (str.charAt(1) == 'R') {
				regMap.put(str, "$" + (6 + paraNum));
				return ("$" + (6 + paraNum));
			} else {
				String s = "r" + tinyCount++;
				regMap.put(str, s);
				return s;
			}
		} else {
			String s = "r" + tinyCount++;
			regMap.put(str, s);
			return s;
		}
	}

	private String getReg(){
		return "$T" + Integer.toString(registerCount++);
	}

	private String convertTinyReg(String input){
		if (input == null) {
			return null;
		}
		if (input.contains("$")) {
			return "r" + Integer.toString(Integer.parseInt(input.substring(2))-1);
		}
		return input;
	}

	private Node getNode(ParseTree ctx) {

		if (ctx == null || ctx.getText().equals("")) {
			return null;
		} else {
			return PTProperty.get(ctx);
		}
	}

	private String getLabel() {
		return "label" + (Integer.toString(labelNum++));
	}

	private String getLocalNum() {
		return (Integer.toString(labelNum));
	}

	private String checkType(String input) {

		if (typeMap.get(input) == null) {
			if(input.matches("[0-9]+")){
				return "INT";
			} else if(input.matches("[0-9]*\\.[0-9]+")){
				return "FLOAT";
			} else{
				return "STRING";
			}
		}else{
			return "ID";
		}
	}

	//Macth operators with opCodes
	private String matchOpCode(String opCode, String type) {
		switch (opCode) {
			case "+": return (type.equals("INT")) ? "ADDI" : "ADDF";
			case "-": return (type.equals("INT")) ? "SUBI" : "SUBF";
			case "*": return (type.equals("INT")) ? "MULTI" : "MULTF";
			case "/": return (type.equals("INT")) ? "DIVI" : "DIVF";
		}

		return "OpCode not valid";

	}
	//Convert IR opCode to Tiny opCode
	private String getOp(String opCode) {
		switch (opCode) {
			case "ADDI": return "addi";
			case "SUBI": return "subi";
			case "ADDF": return "addr";
			case "SUBF": return "subr";
			case "MULTI": return "muli";
			case "DIVI": return "divi";
			case "MULTF": return "mulr";
			case "DIVF": return "divr";
			case "WRITEI": return "sys writei";
			case "WRITEF": return "sys writer";
			case "READI": return "readi";
			case "READF": return "readr";
			case "EQ": return "jeq";
			case "NE": return "jne";
			case "GT": return "jgt";
			case "GE": return "jge";
			case "LT": return "jlt";
			case "LE": return "jle";
			case "JUMP": return "jmp";
			case "LABEL": return "label";
			case "LINK": return "link";
			case "WRITES": return "writes";
			case "PUSH": return "push";
			case "POP": return "pop";
		}
		return opCode;
	}

	public void convertIRtoTiny(IRNode irNode) {
		addopList();
		String opCode = irNode.getOpCode();
		String operand1 = irNode.getOperand1();
		String operand2 = irNode.getOperand2();
		String result = irNode.getResult();
		String temp;
		String type;
		String strOp1;
		String strOp2;
		if(opCode == null) {

		} else if(opCode.equals("STOREI") || opCode.equals("STOREF")) {
			if(operand1.contains("$")) {
				strOp1 = getTinyReg(operand1);
				if(result.contains("$") && !(result.equals("$R") && operand1.startsWith("$T"))){
					strOp2 = getTinyReg(result);

				} else {

					strOp2 = result;
				}
			} else if (result.contains("$")) {
				strOp1 = operand1;
				strOp2 = getTinyReg(result);
			} else {
				strOp1 = getTinyReg(operand1);
				TNList.add(new TinyNode("move", operand1, strOp1));
				strOp2 = result;
			}
			if (result.equals("$R") && !operand1.startsWith("$L")) {
				TNList.add(new TinyNode("move", strOp1, getTinyReg(result + "F")));
			} else {
				TNList.add(new TinyNode("move", strOp1, strOp2));
				if(result.equals("$R")) {
					TNList.add(new TinyNode("move", strOp2, getTinyReg(result + "R")));
				}
			}
			if(opCode.equals("STOREI") && result.contains("$")){
				tinyMap.put(result,"INT");
			} else if (opCode.equals("STOREF") && result.contains("$")){
				tinyMap.put(result,"FLOAT");
			}
		} else if(opCode.equals("WRITEI") || opCode.equals("WRITEF")) {
			if(result.contains("$")) {
				TNList.add(new TinyNode(getOp(opCode), null, getTinyReg(result)));
			} else {
				TNList.add(new TinyNode(getOp(opCode), null, result));
			}
		} else if(opCode.equals("WRITES") || opCode.equals("READI") || opCode.equals("READF")) {
			if(result.contains("$")) {
				TNList.add(new TinyNode("sys", getOp(opCode), getTinyReg(result)));
			} else {
				TNList.add(new TinyNode("sys", getOp(opCode), result));
			}
		} else if(opCode.equals("LINK")) {
			TNList.add(new TinyNode(getOp(opCode), null, "" + localNum));
		} else if(opCode.equals("PUSH")){
			if(result == null){
				TNList.add(new TinyNode(getOp(opCode), null, null));
				pushflag = 1;
			} else {
				TNList.add(new TinyNode(getOp(opCode), null, getTinyReg(result)));
			}
		} else if(opCode.equals("POP")) {
			if(result != null && result.contains("$")){
				TNList.add(new TinyNode(getOp(opCode), null, getTinyReg(result)));
			} else {
				pushflag = 0;
				TNList.add(new TinyNode(getOp(opCode), null, null));
			}
		} else if(opCode.equals("JSR")) {
			TNList.add(new TinyNode("push", null, "r0"));
			TNList.add(new TinyNode("push", null, "r1"));
			TNList.add(new TinyNode("push", null, "r2"));
			TNList.add(new TinyNode("push", null, "r3"));
			TNList.add(new TinyNode("jsr", null, result));
			TNList.add(new TinyNode("pop", null, "r3"));
			TNList.add(new TinyNode("pop", null, "r2"));
			TNList.add(new TinyNode("pop", null, "r1"));
			TNList.add(new TinyNode("pop", null, "r0"));
		} else if(opCode.equals("RET")) {
			TNList.add(new TinyNode("unlnk", null, null));
			TNList.add(new TinyNode("ret", null, null));
		} else if(opList.contains(opCode)){
			if (operand1.contains("$") && operand2.contains("$") && result.contains("$")) {
				temp = getTinyReg(result);
				TNList.add(new TinyNode("move", getTinyReg(operand1), temp));
				TNList.add(new TinyNode(getOp(opCode), getTinyReg(operand2), temp));
			} else if (operand1.contains("$") && operand2.contains("$")) {
				temp = getTinyReg(operand1);
				TNList.add(new TinyNode(getOp(opCode), getTinyReg(operand2), temp));
			} else if (operand1.contains("$")) {
				temp = getTinyReg(operand1);
				TNList.add(new TinyNode(getOp(opCode), operand2, temp));
			} else if (operand2.contains("$")) {
				temp = getTinyReg(operand1);
				TNList.add(new TinyNode("move", operand1, temp));
				TNList.add(new TinyNode(getOp(opCode), getTinyReg(operand2), temp));
			} else {
				temp = getTinyReg(operand1);
				TNList.add(new TinyNode("move", operand1, temp));
				TNList.add(new TinyNode(getOp(opCode), operand2, temp));
			}
			regMap.put(result,temp);
		} else if (opCode.equals("LABEL") || opCode.equals("JUMP")) {
			if (functionMap.containsKey(result)){
				currLabel = result;
				localNum = functionMap.get(currLabel).getLocalVar().size();
				paraNum = functionMap.get(currLabel).getParamVar().size();
			}
			TNList.add(new TinyNode(getOp(opCode), null, result));
		} else {
			//case op2 is constant
			temp = getTinyReg(operand2);
			if (!operand2.contains("$")) {
				TNList.add(new TinyNode("move", operand2, temp));
			}
			// cmp type
			type = functionTypeMap.get(currLabel).get(operand1);
			/*if(checkType(operand1).equals("ID")){
				type = typeMap.get(operand1);
			} else if (checkType(operand1).equals("INT")){
				type = "INT";
			} else if (checkType(operand1).equals("FLOAT")) {
				type = "FLOAT";
			} else {
				type = "ERROR";
			}

			/*if (tinyMap.get(operand1) != null && tinyMap.get(operand1).equals("INT") && tinyMap.get(operand2).equals("INT")) {
				TNList.add(new TinyNode("cmpi", getTinyReg(operand1), temp));
			} else if(tinyMap.get(operand1) != null && tinyMap.get(operand1).equals("FLOAT") && tinyMap.get(operand2).equals("FLOAT")) {
				TNList.add(new TinyNode("cmpr", getTinyReg(operand1), temp));
			} else if (tinyMap.get(operand2) != null && tinyMap.get(operand2).equals("INT")) {
				TNList.add(new TinyNode("cmpi", operand1, temp));
			} else if(tinyMap.get(operand2) != null && tinyMap.get(operand2).equals("FLOAT")) {
				TNList.add(new TinyNode("cmpr", operand1, temp));
			} else if(type.equals("INT")) {
				TNList.add(new TinyNode("cmpi", operand1, temp));
			} else if(type.equals("FLOAT")) {
				TNList.add(new TinyNode("cmpr", operand1, temp));
			}
			TNList.add(new TinyNode(getOp(opCode), null, result));*/
			if (operand1 != null && operand1.contains("$")) {
				operand1 = getTinyReg(operand1);
			}
			if (!operand2.startsWith("$T")) {
				String temp2 = getTinyReg(operand2);
				temp = "r" + tinyCount++;
				TNList.add(new TinyNode("move", temp2, temp));
			}
			if (type.equals("INT")) {
				TNList.add(new TinyNode("cmpi", operand1, temp));
			}
			else {
				TNList.add(new TinyNode("cmpr", operand1, temp));
			}
			//Jump 
			TNList.add(new TinyNode(getOp(opCode), null, result));
		}

	}

//Generate IR Node
	public void replaceVar(IRNode irNode, HashMap<String,String> varMap) {
		
		String operand1 = irNode.getOperand1();
		String operand2 = irNode.getOperand2();
		String result = irNode.getResult();

		if (operand1 != null) {
			if (varMap.containsKey(operand1)) {
				irNode.setOperand1(varMap.get(operand1));
			}
		}
		if (operand2 != null) {
			if (varMap.containsKey(operand2)) {
				irNode.setOperand2(varMap.get(operand2));
			}
		}
		if (result != null) {
			if (varMap.containsKey(result)) {
				irNode.setResult(varMap.get(result));
			}
		}
	}
	@Override public void exitPgm_body(MicroParser.Pgm_bodyContext ctx) { 
		System.out.println(";IR code");
		HashMap<String,String> varMap = null;
		for (int i = 0; i < IRList.size(); i++) {
			if (IRList.get(i).getOpCode() == "LABEL" && functionMap.containsKey(IRList.get(i).getResult())) {
				varMap = functionMap.get(IRList.get(i).getResult()).getVarMap();
			}
			if (varMap != null) {
				replaceVar(IRList.get(i), varMap);
			}
			
			IRList.get(i).printNode();

			convertIRtoTiny(IRList.get(i));
		}
		TNList.add(new TinyNode("end", null, null));
		System.out.println(";tiny code");
		for (int i = 0; i < globalStringCount; i++) {
			TNList.get(i).printNode();
		}
		System.out.println("push");
		System.out.println("push r0");
		System.out.println("push r1");
		System.out.println("push r2");
		System.out.println("push r3");
		System.out.println("jsr main");
		System.out.println("sys halt");
		for (int i = globalStringCount; i < TNList.size(); i++) {
			TNList.get(i).printNode();
		}

		/*
		Iterator<String> it = functionTypeMap.keySet().iterator();
		while(it.hasNext()) {
			String curr = it.next();
			System.out.println(curr);
			Iterator<String> it_name = functionTypeMap.get(curr).keySet().iterator();
			while(it_name.hasNext()) {
				String name = it_name.next();
				System.out.println(name + " "+functionTypeMap.get(curr).get(name));
			}
		}*/

	}

	@Override public void enterProgram(MicroParser.ProgramContext ctx) {
		functionTypeMap.put(currFunction, new HashMap<String,String>());
		while(true) {
			SymbolTable st = SymbolTableStack.stack.pop();
			String scope = st.getScope();
			if(scope.equals("GLOBAL")){
				globalVar.add(scope);
				break;
			}
			Iterator<Symbol> it = st.getEntryMap().values().iterator();
			functionMap.put(scope, new Function(scope));
			while(it.hasNext()) {
				Symbol currSymbol = it.next();
				functionMap.get(scope).addLocalVar(currSymbol.getName());
			}
		}
	}
	@Override public void exitCall_expr(MicroParser.Call_exprContext ctx) {
		String functionName = ctx.getChild(0).getText();
		String returnType = functionMap.get(functionName).getRetureType();
		IRList.add(new IRNode("PUSH", null, null, null));

		String[] exprList = ctx.getChild(2).getText().trim().split(",");
		for (int i = 0; i < exprList.length; i++) {
			if (exprMap.get(currFunction).containsKey(exprList[i])) {
				IRList.add(new IRNode("PUSH", null, null, exprMap.get(currFunction).get(exprList[i])));
			} else {
				IRList.add(new IRNode("PUSH", null, null, exprList[i]));
			}
			
		}	
		IRList.add(new IRNode("JSR", null, null, functionName));
		
		for (int i = 0; i < exprList.length; i++) {
			IRList.add(new IRNode("POP", null, null, null));
		}	
		
		String regName = getReg();
		IRList.add(new IRNode("POP", null, null, regName));
		Node node = new Node(null, regName, returnType);
		functionTypeMap.get(currFunction).put(regName, returnType);
		PTProperty.put(ctx, node);
		
	}

	@Override public void enterFunc_decl(MicroParser.Func_declContext ctx) {
		String functionName = ctx.getChild(2).getText();
		String returnType = ctx.getChild(1).getText();
		currFunction = functionName;
		functionMap.get(functionName).setReturnType(returnType);
		functionTypeMap.put(functionName, new HashMap<String,String>());
		exprMap.put(functionName, new HashMap<String,String>());
		IRList.add(new IRNode("LABEL", null, null, functionName));
		IRList.add(new IRNode("LINK", null, null, null));
		registerCount = 1;
	}

	@Override public void exitFunc_body(MicroParser.Func_bodyContext ctx) {
		functionMap.get(currFunction).createMap();
		HashMap<String,String> varMap = functionMap.get(currFunction).getVarMap();
		Iterator<String> it = varMap.keySet().iterator();
		while(it.hasNext()) {
			String name = it.next();
			String type = functionTypeMap.get(currFunction).get(name);
			functionTypeMap.get(currFunction).put(varMap.get(name), type);
			functionTypeMap.get(currFunction).remove(name);
		}
		IRList.add(new IRNode(null,null,null,null));
	}

	@Override public void exitReturn_stmt(MicroParser.Return_stmtContext ctx) {
		if(ctx.getChild(1).getText().length() != 0) {
			String expr = ctx.getChild(1).getText();
			String type;
			String value;
			if (exprMap.get(currFunction).containsKey(expr)) {
				value = exprMap.get(currFunction).get(expr);			
			} else {
				value = getNode(ctx.getChild(1)).getValue();
			}
			if (value.contains("$")) {
				type = functionTypeMap.get(currFunction).get(value);

			} else if(checkType(value).equals("ID")){
				type = typeMap.get(value);
			} else {
				type = checkType(value);
			}
			if(type.equals("INT")) {
				IRList.add(new IRNode("STOREI", value, null, "$R"));
			} else if (type.equals("FLOAT")) {
				IRList.add(new IRNode("STOREF", value, null, "$R"));
			}
		}
		IRList.add(new IRNode("RET", null, null, ""));
		
	}
	@Override public void exitParam_decl(MicroParser.Param_declContext ctx) {
		String type = ctx.getChild(0).getText();
		String name = ctx.getChild(1).getText();
		typeMap.put(name, type);
		functionTypeMap.get(currFunction).put(name, type);
		functionMap.get(currFunction).addParamVar(name);
		
	}

	@Override public void exitVar_decl(MicroParser.Var_declContext ctx) {
		String type = ctx.getChild(0).getText();
		String[] idList = ctx.getChild(1).getText().trim().split(",");
		for (int i = 0; i < idList.length; i++) {
			if (currFunction == "global") {
				TNList.add(new TinyNode("var", idList[i], null));
			}
			functionTypeMap.get(currFunction).put(idList[i], type);
			typeMap.put(idList[i], type);
		}	
		

	}

	@Override public void exitString_decl(MicroParser.String_declContext ctx) {
		String name = ctx.getChild(1).getText();
		String value = ctx.getChild(3).getText();
		typeMap.put(name, "STRING");
		functionTypeMap.get(currFunction).put(name, "STRING");
		if (currFunction == "global") {
			TNList.add(new TinyNode("str", name, value));
			globalStringCount++;
		}
	}


	@Override public void exitId(MicroParser.IdContext ctx) {
		Node node = new Node(null, ctx.getText(),typeMap.get(ctx.getText()));
		PTProperty.put(ctx,node);
		
	}

	@Override public void exitPrimary(MicroParser.PrimaryContext ctx){

		Node expr = getNode(ctx.getChild(1));

		if (expr == null) {
			String primary = ctx.getChild(0).getText();
			String type = checkType(primary);
			
			if(type.equals("FLOAT") || type.equals("INT")) {
				String regName = getReg();
				String opCode = (type.equals("INT")) ? "STOREI" : "STOREF";
				IRNode irNode = new IRNode(opCode, primary, null, regName);
				IRList.add(irNode);
				Node node = new Node(null, regName, type);
				functionTypeMap.get(currFunction).put(regName,type);
				PTProperty.put(ctx,node);
			} else {
				Node node = getNode(ctx.getChild(0));
				PTProperty.put(ctx,node);
			}
		} else {
			PTProperty.put(ctx, expr);
		}
	}

	@Override public void exitFactor(MicroParser.FactorContext ctx){
		Node factor_prefix = getNode(ctx.getChild(0));
		Node postfix_expr = getNode(ctx.getChild(1));
		String postfixText = postfix_expr.getValue();
		String postfixType = postfix_expr.getType();

		if(factor_prefix == null) {
			Node factor = new Node(null, postfixText, postfixType);
			PTProperty.put(ctx, factor);
		} else {
			String regName = getReg();
			String opCode = matchOpCode(factor_prefix.getOpCode(), postfixType);
			IRNode irNode = new IRNode(opCode, factor_prefix.getValue(), postfixText, regName);
			IRList.add(irNode);
			Node factor = new Node(null, regName, postfixType);
			functionTypeMap.get(currFunction).put(regName,postfixType);
			PTProperty.put(ctx, factor);
		}
	}

	@Override public void exitExpr_prefix(MicroParser.Expr_prefixContext ctx){
		if(ctx.getText() == ""){
			return;
		}
		Node expr_prefix = getNode(ctx.getChild(0));
		Node factor = getNode(ctx.getChild(1));

		if(expr_prefix == null) {
			Node expr_prefixNew = new Node(ctx.getChild(2).getText(), factor.getValue(), factor.getType());
			PTProperty.put(ctx, expr_prefixNew);
		} else {
			String regName = getReg();
			String opCode = matchOpCode(expr_prefix.getOpCode(), factor.getType());
			IRNode irNode = new IRNode(opCode, expr_prefix.getValue(), factor.getValue(), regName);
			IRList.add(irNode);
			Node expr_prefixNew = new Node(ctx.getChild(2).getText(), regName, factor.getType());
			functionTypeMap.get(currFunction).put(regName,factor.getType());
			PTProperty.put(ctx, expr_prefixNew);
		}

	}

	@Override public void exitPostfix_expr(MicroParser.Postfix_exprContext ctx) {
		PTProperty.put(ctx, getNode(ctx.getChild(0)));
	}

	@Override public void exitFactor_prefix(MicroParser.Factor_prefixContext ctx) {

		if (ctx.getText() == "") {
			return;
		} else {
			Node factor_prefix = getNode(ctx.getChild(0));
			Node postfix_expr = getNode(ctx.getChild(1));
			String mulop = ctx.getChild(2).getText();
			if(factor_prefix == null) {
				Node node = new Node(mulop, postfix_expr.getValue(), postfix_expr.getType());
				PTProperty.put(ctx, node);
			} else {
				String regName = getReg();
				String value = factor_prefix.getValue();
				String opCode = matchOpCode(factor_prefix.getOpCode(), postfix_expr.getType());
				IRNode irNode = new IRNode(opCode, value, postfix_expr.getValue(), regName);
				IRList.add(irNode);
				Node node = new Node(mulop, value, postfix_expr.getType());
				functionTypeMap.get(currFunction).put(regName, postfix_expr.getType());
				PTProperty.put(ctx, node);

			}
		}
	}

	@Override public void exitExpr(MicroParser.ExprContext ctx){
		Node expr_prefix = getNode(ctx.getChild(0));
		Node factor = getNode(ctx.getChild(1));
		String factorValue = factor.getValue();
		String factorType = factor.getType();

		if (expr_prefix != null) {
			String regName = getReg();
			String value = expr_prefix.getValue();
			String addop = expr_prefix.getOpCode();
			String opCode = matchOpCode(addop, factorType);
			IRNode irNode = new IRNode(opCode, value, factorValue, regName);
			IRList.add(irNode);
			Node node = new Node(null, regName, factorType);
			functionTypeMap.get(currFunction).put(regName,factorType);
			exprMap.get(currFunction).put(ctx.getChild(0).getText()+ctx.getChild(1).getText(), regName);
			PTProperty.put(ctx, node);
		} else {
			Node node = new Node(null, factorValue, factorType);
			PTProperty.put(ctx, node);

		}
	}


	@Override public void exitAssign_expr(MicroParser.Assign_exprContext ctx){
		String type = typeMap.get(ctx.getChild(0).getText());
		Node expr = getNode(ctx.getChild(2));
		String opName = "";
		if(type == null) {
			return;
		} else if(type.equals("INT")) {
			opName = "STOREI";
		} else if(type.equals("FLOAT")) {
			opName = "STOREF";
		} else {
			return;
		}
		IRNode irNode = new IRNode(opName,expr.getValue(), null, ctx.getChild(0).getText());
		IRList.add(irNode);
	}

	@Override public void exitWrite_stmt(MicroParser.Write_stmtContext ctx){
		if (ctx.getChild(2) == null || ctx.getChild(2).getText().length() == 0) {
			return;
		} else {
			String[] idList = ctx.getChild(2).getText().trim().split(",");
			for (int i = 0; i < idList.length; i++) {
				String type = typeMap.get(idList[i]);
				if(type.equals("INT")) {
					IRNode irNode = new IRNode("WRITEI", null, null, idList[i]);
					IRList.add(irNode);
				} else if (type.equals("FLOAT")) {
					IRNode irNode = new IRNode("WRITEF", null, null, idList[i]);
					IRList.add(irNode);
				} else if (type.equals("STRING")) {
					IRNode irNode = new IRNode("WRITES", null, null, idList[i]);
					IRList.add(irNode);
				}else {
					System.out.println("Invalid type to write");
				}
			}
		}
	}
	@Override public void exitRead_stmt(MicroParser.Read_stmtContext ctx){
		if (ctx.getChild(2) == null || ctx.getChild(2).getText().length() == 0) {
			return;
		} else {
			String[] idList = ctx.getChild(2).getText().trim().split(",");
			for (int i = 0; i < idList.length; i++) {
				String type = typeMap.get(idList[i]);
				if(type.equals("INT")) {
					IRNode irNode = new IRNode("READI", null, null, idList[i]);
					IRList.add(irNode);
				} else if (type.equals("FLOAT")) {
					IRNode irNode = new IRNode("READF", null, null, idList[i]);
					IRList.add(irNode);
				} else if (type.equals("STRING")) {
					IRNode irNode = new IRNode("READS", null, null, idList[i]);
					IRList.add(irNode);
				} else {
					System.out.println("Invalid type to read");
				}
			}
		}
	}

	@Override public void enterDo_while_stmt(MicroParser.Do_while_stmtContext ctx) {
		String headLabel = getLabel();
		String outLabel = getLabel();
		labelStack.push(new LabelNode("do_while", outLabel, headLabel));
		IRList.add(new IRNode("LABEL", null, null, headLabel));
	}

	@Override public void exitDo_while_stmt(MicroParser.Do_while_stmtContext ctx) {
		String outLabel = labelStack.peek().getOutLabel();
		String headLabel = labelStack.pop().getHeadLabel();
		IRList.add(new IRNode("JUMP", null, null, outLabel));
		IRList.add(new IRNode("LABEL", null, null, headLabel));
	}

	//if statement
	@Override public void enterIf_stmt(MicroParser.If_stmtContext ctx) {
		String headLabel = getLabel();
		String outLabel = getLabel();
		labelStack.push(new LabelNode("if", headLabel, outLabel));
	}
	@Override public void exitIf_stmt(MicroParser.If_stmtContext ctx) {
		while(labelStack.peek().getName() != "if") {
			labelStack.pop();
		}
		IRList.add(new IRNode("LABEL", null, null, labelStack.pop().getOutLabel()));
	}
	//else if statement
	@Override public void enterElse_part(MicroParser.Else_partContext ctx) {
		String headLabel = labelStack.peek().getHeadLabel();
		String outLabel = labelStack.peek().getOutLabel();
		IRList.add(new IRNode("JUMP", null, null, outLabel));
		IRList.add(new IRNode("LABEL", null, null, headLabel));
		if (ctx.getChildCount() > 1) {
			labelStack.push(new LabelNode("else_if", getLabel(), outLabel));
		}
	}
	//condition block
	@Override public void exitCond(MicroParser.CondContext ctx) {
		String headLabel = labelStack.peek().getHeadLabel();
		//True or False
		if (ctx.getChildCount() == 1) {
			String regName1 = getReg();
			String regName2 = getReg();
			IRList.add(new IRNode("STOREI", "1", null, regName1));
			IRList.add(new IRNode("STOREI", "1", null, regName2));
			functionTypeMap.get(currFunction).put(regName1, "INT");
			functionTypeMap.get(currFunction).put(regName2, "INT");
			String trueFalse = ctx.getChild(0).getText();
			
			if (trueFalse.equals("TRUE")) {
				IRList.add(new IRNode("NE", regName1, regName2, headLabel));
			} else if (trueFalse.equals("FALSE")){
				IRList.add(new IRNode("EQ", regName1, regName2, headLabel));
			}

			return;
		} else {
			Node expr1 = getNode(ctx.getChild(0));
			Node expr2 = getNode(ctx.getChild(2));
			String compop = ctx.getChild(1).getText();

			if (compop.equals("<")) {
				IRList.add(new IRNode("GE", expr1.getValue(), expr2.getValue(), headLabel));
			}else if (compop.equals(">")) {
				IRList.add(new IRNode("LE", expr1.getValue(), expr2.getValue(), headLabel));
			}else if (compop.equals("=")) {
				IRList.add(new IRNode("NE", expr1.getValue(), expr2.getValue(), headLabel));
			}else if (compop.equals("!=")) {
				IRList.add(new IRNode("EQ", expr1.getValue(), expr2.getValue(), headLabel));
			}else if (compop.equals("<=")) {
				IRList.add(new IRNode("GT", expr1.getValue(), expr2.getValue(), headLabel));
			}else if (compop.equals(">=")) {
				IRList.add(new IRNode("LT", expr1.getValue(), expr2.getValue(), headLabel));
			}
		}

	}


//END Generate IR Node
}



class Node {
	private String opCode, value, type;

	public Node(String inputOpCode, String inputValue, String inputType) {
		this.opCode = inputOpCode;
		this.value = inputValue;
		this.type = inputType;
	}

	public String getOpCode() {
		return this.opCode;
	}

	public String getValue() {
		return this.value;
	}

	public String getType() {
		return this.type;
	}
}

class LabelNode {
	private String name, headLabel, outLabel;
	public LabelNode(String name, String headLabel, String outLabel) {
		this.name = name;
		this.headLabel = headLabel;
		this.outLabel = outLabel;
	}

	public String getName() {
		return this.name;
	}
	public String getHeadLabel() {
		return this.headLabel;
	}
	public String getOutLabel() {
		return this.outLabel;
	}
}

class Function {
	private String name;
	private ArrayList<IRNode> IRList;
	private ArrayList<String> localVar ;
	private ArrayList<String> paramVar;
	private HashMap<String, String>varMap;
	private String returnType;

	public Function(String name) {
		this.name = name;
		this.IRList = new ArrayList<IRNode>();
		this.localVar = new ArrayList<String>();
		this.paramVar = new ArrayList<String>();
		this.varMap = new HashMap<String, String>();
		this.returnType = null;
	}

	public void addIRNode(IRNode node) {
		this.IRList.add(node);
	}

	public void addLocalVar(String localVar) {
		this.localVar.add(localVar);
	}
	public void addParamVar(String paramVar) {
		this.paramVar.add(paramVar);
		this.localVar.remove(paramVar);
	}
	public HashMap<String,String> getVarMap() {
		return this.varMap;
	}

	public String getName() {
		return this.name;
	}

	public ArrayList<String> getLocalVar() {
		return this.localVar;
	}
	public ArrayList<String> getParamVar() {
		return this.paramVar;
	}
	public String getRetureType() {
		return this.returnType;
	}
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
	public void createMap() {
		int parameterRegCount = 1;
		int localRegCount = 1;
		for (int i = 0; i < paramVar.size(); i++) {
			varMap.put(paramVar.get(i), "$P" + parameterRegCount++);
		}
		for (int i = 0; i < localVar.size(); i++) {
			varMap.put(localVar.get(i), "$L" + localRegCount++);
		}
	}
}