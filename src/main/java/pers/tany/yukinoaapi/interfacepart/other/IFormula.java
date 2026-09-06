package pers.tany.yukinoaapi.interfacepart.other;

import pers.tany.yukinoaapi.util.LogUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公式计算工具接口
 * 提供字符串公式的计算功能
 * 兼容Java 8-21+版本
 * 使用缓存机制优化 ScriptEngine 性能
 */
public final class IFormula {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IFormula() {
        throw new UnsupportedOperationException("IFormula 是工具类,不允许实例化");
    }


    /**
     * ScriptEngine 缓存
     * 创建 ScriptEngine 非常耗时，必须缓存复用
     * 使用 ThreadLocal 保证线程安全
     */
    public static final ThreadLocal<ScriptEngine> ENGINE_CACHE = new ThreadLocal<>();

    /**
     * 表达式结果缓存
     * 缓存常用表达式的计算结果
     */
    public static final ConcurrentHashMap<String, Double> RESULT_CACHE = new ConcurrentHashMap<>(64);

    /**
     * 布尔表达式结果缓存
     */
    public static final ConcurrentHashMap<String, Boolean> BOOLEAN_CACHE = new ConcurrentHashMap<>(64);

    /**
     * 缓存最大容量
     */
    public static final int MAX_CACHE_SIZE = 256;

    /**
     * 根据字符串的公式进行运算
     *
     * @param formula 运算公式（如："1+2*3"）
     * @return 计算结果，失败返回0
     */
    public static double numericalFormula(String formula) {
        if (formula == null || formula.isEmpty()) {
            return 0;
        }

        String trimmed = formula.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }

        Double cached = RESULT_CACHE.get(trimmed);
        if (cached != null) {
            return cached;
        }

        Double result = evaluateWithScriptEngine(trimmed);
        if (result != null) {
            cacheResult(trimmed, result);
            return result;
        }

        try {
            double evaluated = evaluateExpression(trimmed);
            cacheResult(trimmed, evaluated);
            return evaluated;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 缓存计算结果
     *
     * @param formula 公式
     * @param result  结果
     */
    public static void cacheResult(String formula, double result) {
        if (RESULT_CACHE.size() < MAX_CACHE_SIZE) {
            RESULT_CACHE.put(formula, result);
        }
    }

    /**
     * 根据字符串的公式进行布尔判断
     *
     * @param formula 判断公式（如："1>0"、"5==5"）
     * @return 判断结果，失败返回false
     */
    public static boolean judgmentFormula(String formula) {
        if (formula == null || formula.isEmpty()) {
            return false;
        }

        String trimmed = formula.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        Boolean cached = BOOLEAN_CACHE.get(trimmed);
        if (cached != null) {
            return cached;
        }

        Boolean result = evaluateBooleanWithScriptEngine(trimmed);
        if (result != null) {
            cacheBooleanResult(trimmed, result);
            return result;
        }

        try {
            boolean evaluated = evaluateBooleanExpression(trimmed);
            cacheBooleanResult(trimmed, evaluated);
            return evaluated;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 缓存布尔计算结果
     *
     * @param formula 公式
     * @param result  结果
     */
    public static void cacheBooleanResult(String formula, boolean result) {
        if (BOOLEAN_CACHE.size() < MAX_CACHE_SIZE) {
            BOOLEAN_CACHE.put(formula, result);
        }
    }

    /**
     * 使用脚本引擎计算数值表达式（带缓存）
     *
     * @param formula 公式
     * @return 计算结果，失败返回null
     */
    public static Double evaluateWithScriptEngine(String formula) {
        ScriptEngine engine = getOrCreateEngine();
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
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 使用脚本引擎计算布尔表达式（带缓存）
     *
     * @param formula 公式
     * @return 计算结果，失败返回null
     */
    public static Boolean evaluateBooleanWithScriptEngine(String formula) {
        ScriptEngine engine = getOrCreateEngine();
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
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取或创建脚本引擎（使用 ThreadLocal 缓存）
     *
     * @return 脚本引擎，不可用返回null
     */
    public static ScriptEngine getOrCreateEngine() {
        ScriptEngine engine = ENGINE_CACHE.get();
        if (engine != null) {
            return engine;
        }

        engine = getScriptEngine();
        if (engine != null) {
            ENGINE_CACHE.set(engine);
        }
        return engine;
    }

    /**
     * 获取可用的脚本引擎
     * 优先级：GraalJS > Nashorn > JavaScript
     *
     * @return 脚本引擎，不可用返回null
     */
    public static ScriptEngine getScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();

        String[] engineNames = {"graal.js", "nashorn", "JavaScript", "js"};
        for (String name : engineNames) {
            ScriptEngine engine = manager.getEngineByName(name);
            if (engine != null) {
                return engine;
            }
        }
        return null;
    }

    /**
     * 内置表达式解析器（支持 + - * / % 和括号）
     * 用于在脚本引擎不可用时进行计算
     *
     * @param expression 表达式
     * @return 计算结果
     */
    public static double evaluateExpression(String expression) {
        String cleaned = expression.replace(" ", "");
        return parseExpression(cleaned, new int[]{0});
    }

    /**
     * 解析表达式（处理加减）
     *
     * @param expr 表达式
     * @param pos  当前位置
     * @return 计算结果
     */
    public static double parseExpression(String expr, int[] pos) {
        double result = parseTerm(expr, pos);
        int len = expr.length();

        while (pos[0] < len) {
            char op = expr.charAt(pos[0]);
            if (op != '+' && op != '-') {
                break;
            }
            pos[0]++;
            double term = parseTerm(expr, pos);
            result = (op == '+') ? result + term : result - term;
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
    public static double parseTerm(String expr, int[] pos) {
        double result = parseFactor(expr, pos);
        int len = expr.length();

        while (pos[0] < len) {
            char op = expr.charAt(pos[0]);
            if (op != '*' && op != '/' && op != '%') {
                break;
            }
            pos[0]++;
            double factor = parseFactor(expr, pos);

            switch (op) {
                case '*':
                    result *= factor;
                    break;
                case '/':
                    result /= factor;
                    break;
                case '%':
                    result %= factor;
                    break;
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
    public static double parseFactor(String expr, int[] pos) {
        boolean negative = false;
        int len = expr.length();

        if (pos[0] < len && expr.charAt(pos[0]) == '-') {
            negative = true;
            pos[0]++;
        }

        double result;

        if (pos[0] < len && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            result = parseExpression(expr, pos);
            if (pos[0] < len && expr.charAt(pos[0]) == ')') {
                pos[0]++;
            }
        } else {
            int start = pos[0];
            while (pos[0] < len) {
                char c = expr.charAt(pos[0]);
                if (!Character.isDigit(c) && c != '.') {
                    break;
                }
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
    public static boolean evaluateBooleanExpression(String expression) {
        String cleaned = expression.replace(" ", "");

        int andIdx = cleaned.indexOf("&&");
        if (andIdx > 0) {
            return evaluateBooleanExpression(cleaned.substring(0, andIdx))
                    && evaluateBooleanExpression(cleaned.substring(andIdx + 2));
        }

        int orIdx = cleaned.indexOf("||");
        if (orIdx > 0) {
            return evaluateBooleanExpression(cleaned.substring(0, orIdx))
                    || evaluateBooleanExpression(cleaned.substring(orIdx + 2));
        }

        String[][] operators = {{">=", "GE"}, {"<=", "LE"}, {"==", "EQ"}, {"!=", "NE"}, {">", "GT"}, {"<", "LT"}};

        for (String[] opPair : operators) {
            String op = opPair[0];
            int index = cleaned.indexOf(op);
            if (index > 0) {
                double left = evaluateExpression(cleaned.substring(0, index));
                double right = evaluateExpression(cleaned.substring(index + op.length()));

                switch (opPair[1]) {
                    case "GE": return left >= right;
                    case "LE": return left <= right;
                    case "EQ": return Math.abs(left - right) < 1e-9;
                    case "NE": return Math.abs(left - right) >= 1e-9;
                    case "GT": return left > right;
                    case "LT": return left < right;
                }
            }
        }

        return Boolean.parseBoolean(cleaned);
    }

    /**
     * 清空所有缓存
     * 在内存紧张时可调用此方法
     */
    public static void clearCache() {
        RESULT_CACHE.clear();
        BOOLEAN_CACHE.clear();
        ENGINE_CACHE.remove();
    }
}

