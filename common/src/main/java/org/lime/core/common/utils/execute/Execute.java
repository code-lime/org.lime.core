package org.lime.core.common.utils.execute;

import java.lang.reflect.Method;

public class Execute {
    static <T>T invoke(Method method, Object instance, Object[] args) {
        try { return (T)method.invoke(instance, args); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    public static Func0<Boolean> negative(Func0<Boolean> func) { return () -> !func.invoke(); }
    public static <T0> Func1<T0, Boolean> negative(Func1<T0, Boolean> func) { return (v0) -> !func.invoke(v0); }
    public static <T0,T1> Func2<T0, T1, Boolean> negative(Func2<T0, T1, Boolean> func) { return (v0,v1) -> !func.invoke(v0,v1); }

    private static final Action0 EMPTY = () -> {};

    public static Class<? extends Callable> findClass(int count, boolean withReturn, boolean withExceptions) {
        return switch (count) {
            //<generator name="system-invoke.js:findClassSwitch()">
            case 0 -> withExceptions
                    ? (withReturn ? FuncEx0.class : ActionEx0.class)
                    : (withReturn ? Func0.class : Action0.class);
            case 1 -> withExceptions
                    ? (withReturn ? FuncEx1.class : ActionEx1.class)
                    : (withReturn ? Func1.class : Action1.class);
            case 2 -> withExceptions
                    ? (withReturn ? FuncEx2.class : ActionEx2.class)
                    : (withReturn ? Func2.class : Action2.class);
            case 3 -> withExceptions
                    ? (withReturn ? FuncEx3.class : ActionEx3.class)
                    : (withReturn ? Func3.class : Action3.class);
            case 4 -> withExceptions
                    ? (withReturn ? FuncEx4.class : ActionEx4.class)
                    : (withReturn ? Func4.class : Action4.class);
            case 5 -> withExceptions
                    ? (withReturn ? FuncEx5.class : ActionEx5.class)
                    : (withReturn ? Func5.class : Action5.class);
            case 6 -> withExceptions
                    ? (withReturn ? FuncEx6.class : ActionEx6.class)
                    : (withReturn ? Func6.class : Action6.class);
            case 7 -> withExceptions
                    ? (withReturn ? FuncEx7.class : ActionEx7.class)
                    : (withReturn ? Func7.class : Action7.class);
            case 8 -> withExceptions
                    ? (withReturn ? FuncEx8.class : ActionEx8.class)
                    : (withReturn ? Func8.class : Action8.class);
            case 9 -> withExceptions
                    ? (withReturn ? FuncEx9.class : ActionEx9.class)
                    : (withReturn ? Func9.class : Action9.class);
            case 10 -> withExceptions
                    ? (withReturn ? FuncEx10.class : ActionEx10.class)
                    : (withReturn ? Func10.class : Action10.class);
            case 11 -> withExceptions
                    ? (withReturn ? FuncEx11.class : ActionEx11.class)
                    : (withReturn ? Func11.class : Action11.class);
            case 12 -> withExceptions
                    ? (withReturn ? FuncEx12.class : ActionEx12.class)
                    : (withReturn ? Func12.class : Action12.class);
            case 13 -> withExceptions
                    ? (withReturn ? FuncEx13.class : ActionEx13.class)
                    : (withReturn ? Func13.class : Action13.class);
            case 14 -> withExceptions
                    ? (withReturn ? FuncEx14.class : ActionEx14.class)
                    : (withReturn ? Func14.class : Action14.class);
            case 15 -> withExceptions
                    ? (withReturn ? FuncEx15.class : ActionEx15.class)
                    : (withReturn ? Func15.class : Action15.class);
            case 16 -> withExceptions
                    ? (withReturn ? FuncEx16.class : ActionEx16.class)
                    : (withReturn ? Func16.class : Action16.class);
            case 17 -> withExceptions
                    ? (withReturn ? FuncEx17.class : ActionEx17.class)
                    : (withReturn ? Func17.class : Action17.class);
            case 18 -> withExceptions
                    ? (withReturn ? FuncEx18.class : ActionEx18.class)
                    : (withReturn ? Func18.class : Action18.class);
            case 19 -> withExceptions
                    ? (withReturn ? FuncEx19.class : ActionEx19.class)
                    : (withReturn ? Func19.class : Action19.class);
            //</generator>
            default -> throw new IllegalArgumentException("Unsupported count: " + count);
        };
    }

    public static Action0 actionEmpty() { return EMPTY; }
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
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>Action10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> action(Action10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, TResult>Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, TResult> func(Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>ActionEx10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> actionEx(ActionEx10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, TResult>FuncEx10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, TResult> funcEx(FuncEx10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>Action11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> action(Action11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, TResult>Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, TResult> func(Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>ActionEx11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> actionEx(ActionEx11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, TResult>FuncEx11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, TResult> funcEx(FuncEx11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>Action12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> action(Action12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, TResult>Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, TResult> func(Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>ActionEx12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> actionEx(ActionEx12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, TResult>FuncEx12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, TResult> funcEx(FuncEx12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>Action13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> action(Action13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, TResult>Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, TResult> func(Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>ActionEx13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> actionEx(ActionEx13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, TResult>FuncEx13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, TResult> funcEx(FuncEx13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>Action14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> action(Action14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, TResult>Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, TResult> func(Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>ActionEx14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> actionEx(ActionEx14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, TResult>FuncEx14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, TResult> funcEx(FuncEx14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>Action15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> action(Action15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, TResult>Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, TResult> func(Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>ActionEx15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> actionEx(ActionEx15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, TResult>FuncEx15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, TResult> funcEx(FuncEx15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>Action16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> action(Action16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, TResult>Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, TResult> func(Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>ActionEx16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> actionEx(ActionEx16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, TResult>FuncEx16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, TResult> funcEx(FuncEx16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>Action17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> action(Action17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, TResult>Func17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, TResult> func(Func17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>ActionEx17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> actionEx(ActionEx17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, TResult>FuncEx17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, TResult> funcEx(FuncEx17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>Action18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> action(Action18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, TResult>Func18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, TResult> func(Func18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>ActionEx18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> actionEx(ActionEx18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, TResult>FuncEx18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, TResult> funcEx(FuncEx18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>Action19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> action(Action19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, TResult>Func19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, TResult> func(Func19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, TResult> func) { return func; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>ActionEx19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> actionEx(ActionEx19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> action) { return action; }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, TResult>FuncEx19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, TResult> funcEx(FuncEx19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, TResult> func) { return func; }
    //</generator>
    //</editor-fold>
}