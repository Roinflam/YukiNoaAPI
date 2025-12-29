package pers.tany.yukinoaapi.interfacepart.other;

import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 公式计算工具接口
 * 提供字符串公式的计算功能
 * 兼容Java 8-21+版本
 *
 * @author tany
 * @since 1.0.0
 */
public interface IFormula {

    /**
     * 根据字符串的公式进行运算
     *
     * @param formula 运算公式（如："1+2*3"）
     * @return 计算结果，失败返回0
     */
    static double numericalFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return 0;
        }

        // 首先尝试使用脚本引擎
        Double result = evaluateWithScriptEngine(formula);
        if (result != null) {
            return result;
        }

        // 如果脚本引擎不可用，使用内置解析器
        try {
            return evaluateExpression(formula);
        } catch (Exception e) {
            LogUtil.error(LangUtil.get("formula.calc-fail", formula), e);
            return 0;
        }
    }

    /**
     * 根据字符串的公式进行布尔判断
     *
     * @param formula 判断公式（如："1>0"、"5==5"）
     * @return 判断结果，失败返回false
     */
    static boolean judgmentFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return false;
        }

        // 首先尝试使用脚本引擎
        Boolean result = evaluateBooleanWithScriptEngine(formula);
        if (result != null) {
            return result;
        }

        // 如果脚本引擎不可用，使用内置解析器
        try {
            return evaluateBooleanExpression(formula);
        } catch (Exception e) {
            LogUtil.error(LangUtil.get("formula.judge-fail", formula), e);
            return false;
        }
    }

    /**
     * 使用脚本引擎计算数值表达式
     *
     * @param formula 公式
     * @return 计算结果，失败返回null
     */
    static Double evaluateWithScriptEngine(String formula) {
        ScriptEngine engine = getScriptEngine();
        if (engine == null) {
            return null;
        }

        try {
            Object result = engine.eval(formula);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
            return Double.parseDouble(result.toString());
        } catch (ScriptException e) {
            LogUtil.debug(LangUtil.get("formula.engine-fail", e.getMessage()));
            return null;
        }
    }

    /**
     * 使用脚本引擎计算布尔表达式
     *
     * @param formula 公式
     * @return 计算结果，失败返回null
     */
    static Boolean evaluateBooleanWithScriptEngine(String formula) {
        ScriptEngine engine = getScriptEngine();
        if (engine == null) {
            return null;
        }

        try {
            Object result = engine.eval(formula);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return Boolean.parseBoolean(result.toString());
        } catch (ScriptException e) {
            LogUtil.debug(LangUtil.get("formula.engine-judge-fail", e.getMessage()));
            return null;
        }
    }

    /**
     * 获取可用的脚本引擎
     * 优先级：GraalJS > Nashorn > JavaScript
     *
     * @return 脚本引擎，不可用返回null
     */
    static ScriptEngine getScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();

        // 尝试获取各种引擎
        String[] engineNames = {"graal.js", "nashorn", "JavaScript", "js"};
        for (String name : engineNames) {
            ScriptEngine engine = manager.getEngineByName(name);
            if (engine != null) {
                return engine;
            }
        }

        LogUtil.debug(LangUtil.get("formula.no-engine"));
        return null;
    }

    /**
     * 内置表达式解析器（支持 + - * / % 和括号）
     * 用于在脚本引擎不可用时进行计算
     *
     * @param expression 表达式
     * @return 计算结果
     */
    static double evaluateExpression(String expression) {
        expression = expression.replaceAll("\\s+", "");
        return parseExpression(expression, new int[]{0});
    }

    /**
     * 解析表达式（处理加减）
     *
     * @param expr 表达式
     * @param pos  当前位置
     * @return 计算结果
     */
    static double parseExpression(String expr, int[] pos) {
        double result = parseTerm(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '+' && op != '-') {
                break;
            }
            pos[0]++;
            double term = parseTerm(expr, pos);
            if (op == '+') {
                result += term;
            } else {
                result -= term;
            }
        }
        return result;
    }

    /**
     * 解析项（处理乘除模）
     *
     * @param expr 表达式
     * @param pos  当前位置
     * @return 计算结果
     */
    static double parseTerm(String expr, int[] pos) {
        double result = parseFactor(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '*' && op != '/' && op != '%') {
                break;
            }
            pos[0]++;
            double factor = parseFactor(expr, pos);
            if (op == '*') {
                result *= factor;
            } else if (op == '/') {
                result /= factor;
            } else {
                result %= factor;
            }
        }
        return result;
    }

    /**
     * 解析因子（处理数字和括号）
     *
     * @param expr 表达式
     * @param pos  当前位置
     * @return 计算结果
     */
    static double parseFactor(String expr, int[] pos) {
        // 处理负号
        boolean negative = false;
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            negative = true;
            pos[0]++;
        }

        double result;

        // 处理括号
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            result = parseExpression(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') {
                pos[0]++;
            }
        } else {
            // 解析数字
            int start = pos[0];
            while (pos[0] < expr.length() &&
                    (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
                pos[0]++;
            }
            result = Double.parseDouble(expr.substring(start, pos[0]));
        }

        return negative ? -result : result;
    }

    /**
     * 内置布尔表达式解析器
     * 支持 > < >= <= == != && ||
     *
     * @param expression 表达式
     * @return 计算结果
     */
    static boolean evaluateBooleanExpression(String expression) {
        expression = expression.replaceAll("\\s+", "");

        // 处理 && 和 ||
        if (expression.contains("&&")) {
            String[] parts = expression.split("&&", 2);
            return evaluateBooleanExpression(parts[0]) && evaluateBooleanExpression(parts[1]);
        }
        if (expression.contains("||")) {
            String[] parts = expression.split("\\|\\|", 2);
            return evaluateBooleanExpression(parts[0]) || evaluateBooleanExpression(parts[1]);
        }

        // 处理比较运算符
        String[] operators = {">=", "<=", "==", "!=", ">", "<"};
        for (String op : operators) {
            int index = expression.indexOf(op);
            if (index > 0) {
                double left = evaluateExpression(expression.substring(0, index));
                double right = evaluateExpression(expression.substring(index + op.length()));

                switch (op) {
                    case ">=": return left >= right;
                    case "<=": return left <= right;
                    case "==": return Math.abs(left - right) < 1e-9;
                    case "!=": return Math.abs(left - right) >= 1e-9;
                    case ">": return left > right;
                    case "<": return left < right;
                }
            }
        }

        // 尝试解析为布尔值
        return Boolean.parseBoolean(expression);
    }
}