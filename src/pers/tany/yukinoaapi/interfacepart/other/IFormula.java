package pers.tany.yukinoaapi.interfacepart.other;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 公式操作相关
 */
public interface IFormula {

    /**
     * 根据字符串的公式进行运算，保留精度
     *
     * @param formula 运算公式
     * @return 结果
     */
    static double numericalFormula(String formula) {
        ScriptEngine javaScript = new ScriptEngineManager().getEngineByName("JavaScript");
        try {
            return Double.parseDouble(javaScript.eval(formula).toString());
        } catch (ScriptException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 根据字符串的公式进行判断
     *
     * @param formula 运算公式
     * @return 结果
     */
    static boolean judgmentFormula(String formula) {
        ScriptEngine javaScript = new ScriptEngineManager().getEngineByName("JavaScript");
        try {
            return Boolean.parseBoolean(javaScript.eval(formula).toString());
        } catch (ScriptException e) {
            e.printStackTrace();
            return false;
        }
    }

}
