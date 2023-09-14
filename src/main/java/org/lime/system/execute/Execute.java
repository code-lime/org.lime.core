package org.lime.system.execute;

import java.lang.reflect.Method;

public class Execute {
    static <T>T invoke(Method method, Object instance, Object[] args) {
        try { return (T)method.invoke(instance, args); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    public static Func0<Boolean> negative(Func0<Boolean> func) { return () -> !func.invoke(); }
    public static <T0> Func1<T0, Boolean> negative(Func1<T0, Boolean> func) { return (v0) -> !func.invoke(v0); }
    public static <T0,T1> Func2<T0, T1, Boolean> negative(Func2<T0, T1, Boolean> func) { return (v0,v1) -> !func.invoke(v0,v1); }

    public static Action0 action(Action0 action) { return action; }
    public static <T0> Action1<T0> action(Action1<T0> action) { return action; }
    public static ActionEx0 actionEx(ActionEx0 action) { return action; }
    public static <T0> ActionEx1<T0> actionEx(ActionEx1<T0> action) { return action; }

    public static <TResult> Func0<TResult> func(Func0<TResult> action) { return action; }
    public static <T0, TResult>Func1<T0, TResult> func(Func1<T0, TResult> action) { return action; }
    public static <TResult>FuncEx0<TResult> funcEx(FuncEx0<TResult> action) { return action; }
    public static <T0, TResult>FuncEx1<T0, TResult> funcEx(FuncEx1<T0, TResult> action) { return action; }

    //<editor-fold desc="Actions & Funcs">
    //<generator name="system-invoke.js:getAllFunctions()">
    public static <T0, T1>Action2<T0, T1> action(Action2<T0, T1> action) { return action; }
    public static <T0, T1, TResult>Func2<T0, T1, TResult> func(Func2<T0, T1, TResult> func) { return func; }
    public static <T0, T1>ActionEx2<T0, T1> actionEx(ActionEx2<T0, T1> action) { return action; }
    public static <T0, T1, TResult>FuncEx2<T0, T1, TResult> funcEx(FuncEx2<T0, T1, TResult> func) { return func; }
    public static <T0, T1, T2>Action3<T0, T1, T2> action(Action3<T0, T1, T2> action) { return action; }
    public static <T0, T1, T2, TResult>Func3<T0, T1, T2, TResult> func(Func3<T0, T1, T2, TResult> func) { return func; }
    public static <T0, T1, T2>ActionEx3<T0, T1, T2> actionEx(ActionEx3<T0, T1, T2> action) { return action; }
    public static <T0, T1, T2, TResult>FuncEx3<T0, T1, T2, TResult> funcEx(FuncEx3<T0, T1, T2, TResult> func) { return func; }
    public static <T0, T1, T2, T3>Action4<T0, T1, T2, T3> action(Action4<T0, T1, T2, T3> action) { return action; }
    public static <T0, T1, T2, T3, TResult>Func4<T0, T1, T2, T3, TResult> func(Func4<T0, T1, T2, T3, TResult> func) { return func; }
    public static <T0, T1, T2, T3>ActionEx4<T0, T1, T2, T3> actionEx(ActionEx4<T0, T1, T2, T3> action) { return action; }
    public static <T0, T1, T2, T3, TResult>FuncEx4<T0, T1, T2, T3, TResult> funcEx(FuncEx4<T0, T1, T2, T3, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4>Action5<T0, T1, T2, T3, T4> action(Action5<T0, T1, T2, T3, T4> action) { return action; }
    public static <T0, T1, T2, T3, T4, TResult>Func5<T0, T1, T2, T3, T4, TResult> func(Func5<T0, T1, T2, T3, T4, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4>ActionEx5<T0, T1, T2, T3, T4> actionEx(ActionEx5<T0, T1, T2, T3, T4> action) { return action; }
    public static <T0, T1, T2, T3, T4, TResult>FuncEx5<T0, T1, T2, T3, T4, TResult> funcEx(FuncEx5<T0, T1, T2, T3, T4, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5>Action6<T0, T1, T2, T3, T4, T5> action(Action6<T0, T1, T2, T3, T4, T5> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, TResult>Func6<T0, T1, T2, T3, T4, T5, TResult> func(Func6<T0, T1, T2, T3, T4, T5, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5>ActionEx6<T0, T1, T2, T3, T4, T5> actionEx(ActionEx6<T0, T1, T2, T3, T4, T5> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, TResult>FuncEx6<T0, T1, T2, T3, T4, T5, TResult> funcEx(FuncEx6<T0, T1, T2, T3, T4, T5, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6>Action7<T0, T1, T2, T3, T4, T5, T6> action(Action7<T0, T1, T2, T3, T4, T5, T6> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, TResult>Func7<T0, T1, T2, T3, T4, T5, T6, TResult> func(Func7<T0, T1, T2, T3, T4, T5, T6, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6>ActionEx7<T0, T1, T2, T3, T4, T5, T6> actionEx(ActionEx7<T0, T1, T2, T3, T4, T5, T6> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, TResult>FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> funcEx(FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>Action8<T0, T1, T2, T3, T4, T5, T6, T7> action(Action8<T0, T1, T2, T3, T4, T5, T6, T7> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func(Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> actionEx(ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> funcEx(FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action(Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func(Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> actionEx(ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> funcEx(FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func) { return func; }
    //</generator>
    //</editor-fold>
}