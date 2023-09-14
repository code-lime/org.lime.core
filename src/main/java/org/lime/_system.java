package org.lime;

import com.google.common.collect.Streams;
import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.system.delete.DeleteHandle;
import org.lime.system.execute.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("unchecked")
public class _system {


    //  <editor-fold desc="Actions & Funcs">
    //  <generator name="system-invoke.js">
    /*
    public interface Action2<T0, T1> extends Action1<Toast2<T0,T1>> {
        void invoke(T0 arg0, T1 arg1);
        @Override default void invoke(Toast2<T0,T1> arg0) { invoke(arg0.val0, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1]); return null; }
        static <T0, T1>Action2<T0, T1> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1) -> _system.invoke(method, null, new Object[] { val0, val1 }) :
                    (val0, val1) -> _system.invoke(method, val0, new Object[] { val1 });
        }
    }
    public static <T0, T1>Action2<T0, T1> action(Action2<T0, T1> action) { return action; }
    public interface Func2<T0, T1, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1]); }
        static <T0, T1, TResult>Func2<T0, T1, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1) -> _system.invoke(method, null, new Object[] { val0, val1 }) :
                    (val0, val1) -> _system.invoke(method, val0, new Object[] { val1 });
        }
    }
    public static <T0, T1, TResult>Func2<T0, T1, TResult> func(Func2<T0, T1, TResult> func) { return func; }
    public interface ActionEx2<T0, T1> {
        void invoke(T0 arg0, T1 arg1) throws Throwable;
        default Action2<T0, T1> throwable() {
            return (val0, val1) -> { try { this.invoke(val0, val1); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func2<T0, T1, Boolean> optional() {
            return (val0, val1) -> { try { this.invoke(val0, val1); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1>ActionEx2<T0, T1> actionEx(ActionEx2<T0, T1> action) { return action; }
    public interface FuncEx2<T0, T1, TResult> {
        TResult invoke(T0 arg0, T1 arg1) throws Throwable;
        default Func2<T0, T1, TResult> throwable() {
            return (val0, val1) -> { try { return this.invoke(val0, val1); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func2<T0, T1, Optional<TResult>> optional() {
            return (val0, val1) -> { try { return Optional.ofNullable(this.invoke(val0, val1)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, TResult>FuncEx2<T0, T1, TResult> funcEx(FuncEx2<T0, T1, TResult> func) { return func; }
    
    public interface Action3<T0, T1, T2> extends Action1<Toast2<Toast2<T0,T1>,T2>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2);
        @Override default void invoke(Toast2<Toast2<T0,T1>,T2> arg0) { invoke(arg0.val0.val0, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2]); return null; }
        static <T0, T1, T2>Action3<T0, T1, T2> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2) -> _system.invoke(method, null, new Object[] { val0, val1, val2 }) :
                    (val0, val1, val2) -> _system.invoke(method, val0, new Object[] { val1, val2 });
        }
    }
    public static <T0, T1, T2>Action3<T0, T1, T2> action(Action3<T0, T1, T2> action) { return action; }
    public interface Func3<T0, T1, T2, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2]); }
        static <T0, T1, T2, TResult>Func3<T0, T1, T2, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2) -> _system.invoke(method, null, new Object[] { val0, val1, val2 }) :
                    (val0, val1, val2) -> _system.invoke(method, val0, new Object[] { val1, val2 });
        }
    }
    public static <T0, T1, T2, TResult>Func3<T0, T1, T2, TResult> func(Func3<T0, T1, T2, TResult> func) { return func; }
    public interface ActionEx3<T0, T1, T2> {
        void invoke(T0 arg0, T1 arg1, T2 arg2) throws Throwable;
        default Action3<T0, T1, T2> throwable() {
            return (val0, val1, val2) -> { try { this.invoke(val0, val1, val2); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func3<T0, T1, T2, Boolean> optional() {
            return (val0, val1, val2) -> { try { this.invoke(val0, val1, val2); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2>ActionEx3<T0, T1, T2> actionEx(ActionEx3<T0, T1, T2> action) { return action; }
    public interface FuncEx3<T0, T1, T2, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2) throws Throwable;
        default Func3<T0, T1, T2, TResult> throwable() {
            return (val0, val1, val2) -> { try { return this.invoke(val0, val1, val2); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func3<T0, T1, T2, Optional<TResult>> optional() {
            return (val0, val1, val2) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, TResult>FuncEx3<T0, T1, T2, TResult> funcEx(FuncEx3<T0, T1, T2, TResult> func) { return func; }
    
    public interface Action4<T0, T1, T2, T3> extends Action1<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3);
        @Override default void invoke(Toast2<Toast2<Toast2<T0,T1>,T2>,T3> arg0) { invoke(arg0.val0.val0.val0, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3]); return null; }
        static <T0, T1, T2, T3>Action4<T0, T1, T2, T3> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3 }) :
                    (val0, val1, val2, val3) -> _system.invoke(method, val0, new Object[] { val1, val2, val3 });
        }
    }
    public static <T0, T1, T2, T3>Action4<T0, T1, T2, T3> action(Action4<T0, T1, T2, T3> action) { return action; }
    public interface Func4<T0, T1, T2, T3, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3]); }
        static <T0, T1, T2, T3, TResult>Func4<T0, T1, T2, T3, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3 }) :
                    (val0, val1, val2, val3) -> _system.invoke(method, val0, new Object[] { val1, val2, val3 });
        }
    }
    public static <T0, T1, T2, T3, TResult>Func4<T0, T1, T2, T3, TResult> func(Func4<T0, T1, T2, T3, TResult> func) { return func; }
    public interface ActionEx4<T0, T1, T2, T3> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Throwable;
        default Action4<T0, T1, T2, T3> throwable() {
            return (val0, val1, val2, val3) -> { try { this.invoke(val0, val1, val2, val3); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func4<T0, T1, T2, T3, Boolean> optional() {
            return (val0, val1, val2, val3) -> { try { this.invoke(val0, val1, val2, val3); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3>ActionEx4<T0, T1, T2, T3> actionEx(ActionEx4<T0, T1, T2, T3> action) { return action; }
    public interface FuncEx4<T0, T1, T2, T3, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Throwable;
        default Func4<T0, T1, T2, T3, TResult> throwable() {
            return (val0, val1, val2, val3) -> { try { return this.invoke(val0, val1, val2, val3); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func4<T0, T1, T2, T3, Optional<TResult>> optional() {
            return (val0, val1, val2, val3) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, TResult>FuncEx4<T0, T1, T2, T3, TResult> funcEx(FuncEx4<T0, T1, T2, T3, TResult> func) { return func; }
    
    public interface Action5<T0, T1, T2, T3, T4> extends Action1<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4);
        @Override default void invoke(Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4> arg0) { invoke(arg0.val0.val0.val0.val0, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4]); return null; }
        static <T0, T1, T2, T3, T4>Action5<T0, T1, T2, T3, T4> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4 }) :
                    (val0, val1, val2, val3, val4) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4 });
        }
    }
    public static <T0, T1, T2, T3, T4>Action5<T0, T1, T2, T3, T4> action(Action5<T0, T1, T2, T3, T4> action) { return action; }
    public interface Func5<T0, T1, T2, T3, T4, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4]); }
        static <T0, T1, T2, T3, T4, TResult>Func5<T0, T1, T2, T3, T4, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4 }) :
                    (val0, val1, val2, val3, val4) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4 });
        }
    }
    public static <T0, T1, T2, T3, T4, TResult>Func5<T0, T1, T2, T3, T4, TResult> func(Func5<T0, T1, T2, T3, T4, TResult> func) { return func; }
    public interface ActionEx5<T0, T1, T2, T3, T4> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) throws Throwable;
        default Action5<T0, T1, T2, T3, T4> throwable() {
            return (val0, val1, val2, val3, val4) -> { try { this.invoke(val0, val1, val2, val3, val4); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func5<T0, T1, T2, T3, T4, Boolean> optional() {
            return (val0, val1, val2, val3, val4) -> { try { this.invoke(val0, val1, val2, val3, val4); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4>ActionEx5<T0, T1, T2, T3, T4> actionEx(ActionEx5<T0, T1, T2, T3, T4> action) { return action; }
    public interface FuncEx5<T0, T1, T2, T3, T4, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) throws Throwable;
        default Func5<T0, T1, T2, T3, T4, TResult> throwable() {
            return (val0, val1, val2, val3, val4) -> { try { return this.invoke(val0, val1, val2, val3, val4); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func5<T0, T1, T2, T3, T4, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, TResult>FuncEx5<T0, T1, T2, T3, T4, TResult> funcEx(FuncEx5<T0, T1, T2, T3, T4, TResult> func) { return func; }
    
    public interface Action6<T0, T1, T2, T3, T4, T5> extends Action1<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5);
        @Override default void invoke(Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5> arg0) { invoke(arg0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5]); return null; }
        static <T0, T1, T2, T3, T4, T5>Action6<T0, T1, T2, T3, T4, T5> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5 }) :
                    (val0, val1, val2, val3, val4, val5) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5>Action6<T0, T1, T2, T3, T4, T5> action(Action6<T0, T1, T2, T3, T4, T5> action) { return action; }
    public interface Func6<T0, T1, T2, T3, T4, T5, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5]); }
        static <T0, T1, T2, T3, T4, T5, TResult>Func6<T0, T1, T2, T3, T4, T5, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5 }) :
                    (val0, val1, val2, val3, val4, val5) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, TResult>Func6<T0, T1, T2, T3, T4, T5, TResult> func(Func6<T0, T1, T2, T3, T4, T5, TResult> func) { return func; }
    public interface ActionEx6<T0, T1, T2, T3, T4, T5> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) throws Throwable;
        default Action6<T0, T1, T2, T3, T4, T5> throwable() {
            return (val0, val1, val2, val3, val4, val5) -> { try { this.invoke(val0, val1, val2, val3, val4, val5); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func6<T0, T1, T2, T3, T4, T5, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5) -> { try { this.invoke(val0, val1, val2, val3, val4, val5); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5>ActionEx6<T0, T1, T2, T3, T4, T5> actionEx(ActionEx6<T0, T1, T2, T3, T4, T5> action) { return action; }
    public interface FuncEx6<T0, T1, T2, T3, T4, T5, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) throws Throwable;
        default Func6<T0, T1, T2, T3, T4, T5, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func6<T0, T1, T2, T3, T4, T5, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, TResult>FuncEx6<T0, T1, T2, T3, T4, T5, TResult> funcEx(FuncEx6<T0, T1, T2, T3, T4, T5, TResult> func) { return func; }
    
    public interface Action7<T0, T1, T2, T3, T4, T5, T6> extends Action1<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6);
        @Override default void invoke(Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6> arg0) { invoke(arg0.val0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6]); return null; }
        static <T0, T1, T2, T3, T4, T5, T6>Action7<T0, T1, T2, T3, T4, T5, T6> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6 }) :
                    (val0, val1, val2, val3, val4, val5, val6) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6>Action7<T0, T1, T2, T3, T4, T5, T6> action(Action7<T0, T1, T2, T3, T4, T5, T6> action) { return action; }
    public interface Func7<T0, T1, T2, T3, T4, T5, T6, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6]); }
        static <T0, T1, T2, T3, T4, T5, T6, TResult>Func7<T0, T1, T2, T3, T4, T5, T6, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6 }) :
                    (val0, val1, val2, val3, val4, val5, val6) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, TResult>Func7<T0, T1, T2, T3, T4, T5, T6, TResult> func(Func7<T0, T1, T2, T3, T4, T5, T6, TResult> func) { return func; }
    public interface ActionEx7<T0, T1, T2, T3, T4, T5, T6> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) throws Throwable;
        default Action7<T0, T1, T2, T3, T4, T5, T6> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func7<T0, T1, T2, T3, T4, T5, T6, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6>ActionEx7<T0, T1, T2, T3, T4, T5, T6> actionEx(ActionEx7<T0, T1, T2, T3, T4, T5, T6> action) { return action; }
    public interface FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) throws Throwable;
        default Func7<T0, T1, T2, T3, T4, T5, T6, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5, val6); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func7<T0, T1, T2, T3, T4, T5, T6, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5, val6)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, TResult>FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> funcEx(FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> func) { return func; }
    
    public interface Action8<T0, T1, T2, T3, T4, T5, T6, T7> extends Action1<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7);
        @Override default void invoke(Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7> arg0) { invoke(arg0.val0.val0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7]); return null; }
        static <T0, T1, T2, T3, T4, T5, T6, T7>Action8<T0, T1, T2, T3, T4, T5, T6, T7> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>Action8<T0, T1, T2, T3, T4, T5, T6, T7> action(Action8<T0, T1, T2, T3, T4, T5, T6, T7> action) { return action; }
    public interface Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7]); }
        static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func(Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func) { return func; }
    public interface ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7) throws Throwable;
        default Action8<T0, T1, T2, T3, T4, T5, T6, T7> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func8<T0, T1, T2, T3, T4, T5, T6, T7, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> actionEx(ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> action) { return action; }
    public interface FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7) throws Throwable;
        default Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5, val6, val7); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func8<T0, T1, T2, T3, T4, T5, T6, T7, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5, val6, val7)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> funcEx(FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func) { return func; }
    
    public interface Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends Action1<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8);
        @Override default void invoke(Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8> arg0) { invoke(arg0.val0.val0.val0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7], (T8)args[8]); return null; }
        static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7, val8 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7, val8 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action(Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { return action; }
    public interface Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7], (T8)args[8]); }
        static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> _system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7, val8 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> _system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7, val8 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func(Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func) { return func; }
    public interface ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8) throws Throwable;
        default Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> actionEx(ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { return action; }
    public interface FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8) throws Throwable;
        default Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> funcEx(FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func) { return func; }
    */
    //  </generator>
    //  </editor-fold>

    //  <editor-fold desc="Toasts">
    //  <generator name="system-toast.js">
/*
    public static <T0>Toast1<T0> toast(T0 val0){ return new Toast1<>(val0); }
    public static class Toast1<T0> extends IToast {
        public LockToast1<T0> lock() { return new LockToast1<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0 }; }
        public Toast1(T0 val0) { this.val0 = val0; }
        public T0 val0;
        @Override public int size() { return 1; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0>Toast1<A0> map(Func1<T0, A0> map0) { return Toast.of(map0.invoke(val0)); }
        public void invoke(Action1<T0> action) { action.invoke(val0); }
        public <T>T invokeGet(Func1<T0, T> func) { return func.invoke(val0); }
    }
    public static class LockToast1<T0> extends ILockToast<Toast1<T0>> {
        public LockToast1(Toast1<T0> base) { super(base); }
        public T0 get0() { return (T0)get(0); }
        public void set0(T0 value) { set(0, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); }
    }
    public static <T0, T1>Toast2<T0, T1> toast(T0 val0, T1 val1){ return new Toast2<>(val0, val1); }
    public static class Toast2<T0, T1> extends IToast {
        public LockToast2<T0, T1> lock() { return new LockToast2<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1 }; }
        public Toast2(T0 val0, T1 val1) { this.val0 = val0; this.val1 = val1; }
        public T0 val0; public T1 val1;
        @Override public int size() { return 2; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1>Toast2<A0, A1> map(Func1<T0, A0> map0, Func1<T1, A1> map1) { return Toast.of(map0.invoke(val0), map1.invoke(val1)); }
        public void invoke(Action2<T0, T1> action) { action.invoke(val0, val1); }
        public <T>T invokeGet(Func2<T0, T1, T> func) { return func.invoke(val0, val1); }
    }
    public static class LockToast2<T0, T1> extends ILockToast<Toast2<T0, T1>> {
        public LockToast2(Toast2<T0, T1> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); }
    }
    public static <T0, T1, T2>Toast3<T0, T1, T2> toast(T0 val0, T1 val1, T2 val2){ return new Toast3<>(val0, val1, val2); }
    public static class Toast3<T0, T1, T2> extends IToast {
        public LockToast3<T0, T1, T2> lock() { return new LockToast3<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2 }; }
        public Toast3(T0 val0, T1 val1, T2 val2) { this.val0 = val0; this.val1 = val1; this.val2 = val2; }
        public T0 val0; public T1 val1; public T2 val2;
        @Override public int size() { return 3; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2>Toast3<A0, A1, A2> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2)); }
        public void invoke(Action3<T0, T1, T2> action) { action.invoke(val0, val1, val2); }
        public <T>T invokeGet(Func3<T0, T1, T2, T> func) { return func.invoke(val0, val1, val2); }
    }
    public static class LockToast3<T0, T1, T2> extends ILockToast<Toast3<T0, T1, T2>> {
        public LockToast3(Toast3<T0, T1, T2> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); }
    }
    public static <T0, T1, T2, T3>Toast4<T0, T1, T2, T3> toast(T0 val0, T1 val1, T2 val2, T3 val3){ return new Toast4<>(val0, val1, val2, val3); }
    public static class Toast4<T0, T1, T2, T3> extends IToast {
        public LockToast4<T0, T1, T2, T3> lock() { return new LockToast4<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3 }; }
        public Toast4(T0 val0, T1 val1, T2 val2, T3 val3) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3;
        @Override public int size() { return 4; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3>Toast4<A0, A1, A2, A3> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3)); }
        public void invoke(Action4<T0, T1, T2, T3> action) { action.invoke(val0, val1, val2, val3); }
        public <T>T invokeGet(Func4<T0, T1, T2, T3, T> func) { return func.invoke(val0, val1, val2, val3); }
    }
    public static class LockToast4<T0, T1, T2, T3> extends ILockToast<Toast4<T0, T1, T2, T3>> {
        public LockToast4(Toast4<T0, T1, T2, T3> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); }
    }
    public static <T0, T1, T2, T3, T4>Toast5<T0, T1, T2, T3, T4> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4){ return new Toast5<>(val0, val1, val2, val3, val4); }
    public static class Toast5<T0, T1, T2, T3, T4> extends IToast {
        public LockToast5<T0, T1, T2, T3, T4> lock() { return new LockToast5<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4 }; }
        public Toast5(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4;
        @Override public int size() { return 5; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4>Toast5<A0, A1, A2, A3, A4> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3, Func1<T4, A4> map4) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4)); }
        public void invoke(Action5<T0, T1, T2, T3, T4> action) { action.invoke(val0, val1, val2, val3, val4); }
        public <T>T invokeGet(Func5<T0, T1, T2, T3, T4, T> func) { return func.invoke(val0, val1, val2, val3, val4); }
    }
    public static class LockToast5<T0, T1, T2, T3, T4> extends ILockToast<Toast5<T0, T1, T2, T3, T4>> {
        public LockToast5(Toast5<T0, T1, T2, T3, T4> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5>Toast6<T0, T1, T2, T3, T4, T5> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5){ return new Toast6<>(val0, val1, val2, val3, val4, val5); }
    public static class Toast6<T0, T1, T2, T3, T4, T5> extends IToast {
        public LockToast6<T0, T1, T2, T3, T4, T5> lock() { return new LockToast6<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5 }; }
        public Toast6(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5;
        @Override public int size() { return 6; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5>Toast6<A0, A1, A2, A3, A4, A5> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3, Func1<T4, A4> map4, Func1<T5, A5> map5) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5)); }
        public void invoke(Action6<T0, T1, T2, T3, T4, T5> action) { action.invoke(val0, val1, val2, val3, val4, val5); }
        public <T>T invokeGet(Func6<T0, T1, T2, T3, T4, T5, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5); }
    }
    public static class LockToast6<T0, T1, T2, T3, T4, T5> extends ILockToast<Toast6<T0, T1, T2, T3, T4, T5>> {
        public LockToast6(Toast6<T0, T1, T2, T3, T4, T5> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5, T6>Toast7<T0, T1, T2, T3, T4, T5, T6> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6){ return new Toast7<>(val0, val1, val2, val3, val4, val5, val6); }
    public static class Toast7<T0, T1, T2, T3, T4, T5, T6> extends IToast {
        public LockToast7<T0, T1, T2, T3, T4, T5, T6> lock() { return new LockToast7<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5, val6 }; }
        public Toast7(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; this.val6 = val6; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5; public T6 val6;
        @Override public int size() { return 7; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; case 6: return val6; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; case 6: val6 = (T6)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5, A6>Toast7<A0, A1, A2, A3, A4, A5, A6> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3, Func1<T4, A4> map4, Func1<T5, A5> map5, Func1<T6, A6> map6) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5), map6.invoke(val6)); }
        public void invoke(Action7<T0, T1, T2, T3, T4, T5, T6> action) { action.invoke(val0, val1, val2, val3, val4, val5, val6); }
        public <T>T invokeGet(Func7<T0, T1, T2, T3, T4, T5, T6, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5, val6); }
    }
    public static class LockToast7<T0, T1, T2, T3, T4, T5, T6> extends ILockToast<Toast7<T0, T1, T2, T3, T4, T5, T6>> {
        public LockToast7(Toast7<T0, T1, T2, T3, T4, T5, T6> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); } public T6 get6() { return (T6)get(6); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); } public void set6(T6 value) { set(6, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); } public T6 edit6(Func1<T6, T6> func) { return (T6)edit(6, v -> func.invoke((T6)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>Toast8<T0, T1, T2, T3, T4, T5, T6, T7> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7){ return new Toast8<>(val0, val1, val2, val3, val4, val5, val6, val7); }
    public static class Toast8<T0, T1, T2, T3, T4, T5, T6, T7> extends IToast {
        public LockToast8<T0, T1, T2, T3, T4, T5, T6, T7> lock() { return new LockToast8<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }; }
        public Toast8(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; this.val6 = val6; this.val7 = val7; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5; public T6 val6; public T7 val7;
        @Override public int size() { return 8; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; case 6: return val6; case 7: return val7; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; case 6: val6 = (T6)value; break; case 7: val7 = (T7)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5, A6, A7>Toast8<A0, A1, A2, A3, A4, A5, A6, A7> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3, Func1<T4, A4> map4, Func1<T5, A5> map5, Func1<T6, A6> map6, Func1<T7, A7> map7) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5), map6.invoke(val6), map7.invoke(val7)); }
        public void invoke(Action8<T0, T1, T2, T3, T4, T5, T6, T7> action) { action.invoke(val0, val1, val2, val3, val4, val5, val6, val7); }
        public <T>T invokeGet(Func8<T0, T1, T2, T3, T4, T5, T6, T7, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5, val6, val7); }
    }
    public static class LockToast8<T0, T1, T2, T3, T4, T5, T6, T7> extends ILockToast<Toast8<T0, T1, T2, T3, T4, T5, T6, T7>> {
        public LockToast8(Toast8<T0, T1, T2, T3, T4, T5, T6, T7> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); } public T6 get6() { return (T6)get(6); } public T7 get7() { return (T7)get(7); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); } public void set6(T6 value) { set(6, value); } public void set7(T7 value) { set(7, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); } public T6 edit6(Func1<T6, T6> func) { return (T6)edit(6, v -> func.invoke((T6)v)); } public T7 edit7(Func1<T7, T7> func) { return (T7)edit(7, v -> func.invoke((T7)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7, T8 val8){ return new Toast9<>(val0, val1, val2, val3, val4, val5, val6, val7, val8); }
    public static class Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends IToast {
        public LockToast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> lock() { return new LockToast9<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5, val6, val7, val8 }; }
        public Toast9(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7, T8 val8) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; this.val6 = val6; this.val7 = val7; this.val8 = val8; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5; public T6 val6; public T7 val7; public T8 val8;
        @Override public int size() { return 9; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; case 6: return val6; case 7: return val7; case 8: return val8; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; case 6: val6 = (T6)value; break; case 7: val7 = (T7)value; break; case 8: val8 = (T8)value; break; } }
        @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5, A6, A7, A8>Toast9<A0, A1, A2, A3, A4, A5, A6, A7, A8> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3, Func1<T4, A4> map4, Func1<T5, A5> map5, Func1<T6, A6> map6, Func1<T7, A7> map7, Func1<T8, A8> map8) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5), map6.invoke(val6), map7.invoke(val7), map8.invoke(val8)); }
        public void invoke(Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { action.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); }
        public <T>T invokeGet(Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); }
    }
    public static class LockToast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends ILockToast<Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8>> {
        public LockToast9(Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); } public T6 get6() { return (T6)get(6); } public T7 get7() { return (T7)get(7); } public T8 get8() { return (T8)get(8); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); } public void set6(T6 value) { set(6, value); } public void set7(T7 value) { set(7, value); } public void set8(T8 value) { set(8, value); }
        public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); } public T6 edit6(Func1<T6, T6> func) { return (T6)edit(6, v -> func.invoke((T6)v)); } public T7 edit7(Func1<T7, T7> func) { return (T7)edit(7, v -> func.invoke((T7)v)); } public T8 edit8(Func1<T8, T8> func) { return (T8)edit(8, v -> func.invoke((T8)v)); }
    }
    */
    //  </generator>
    //  </editor-fold>


    /*
    public static class PostToast<T0> {
        public Func0<T0> func;
        private T0 value;
        private boolean isInit = false;
        private boolean isUpdate = false;
        public PostToast.of(Func0<T0> func) {
            this.func = func;
        }
        public T0 get() {
            return isInit ? value : (value = func.invoke());
        }
        public boolean isInited() {
            return isInit;
        }
        public boolean isUpdated() {
            return isUpdate;
        }
        public void set(T0 value) {
            this.value = value;
            this.isInit = true;
            this.isUpdate = true;
        }
    }

    public static class Property<T> {
        private final Func0<T> _get;
        private final Action1<T> _set;
        private T _obj;

        public Property() {
            this._get = () -> _obj;
            this._set = (value) -> _obj = value;
        }
        public Property(T _value) {
            this();
            _obj = _value;
        }
        public Property(Action1<T> _set)
        {
            this(null, _set);
        }
        public Property(Func0<T> _get)
        {
            this(_get, null);
        }
        public Property(Func0<T> _get, Action1<T> _set) {
            this._get = _get;
            this._set = _set;
        }

        public T get()
        {
            return _get.invoke();
        }
        public void set(T value)
        {
            _set.invoke(value);
        }
    }
*/

/*
    public static double GetProgress(double value, double min, double max, double max_value) {
        return Math.min(max_value, Math.max(0, Math.round((value - min) / (max - min) * max_value)));
    }
    public static int GetProgress(double value, double min, double max, int max_value) {
        return (int)Math.min(max_value, Math.max(0, Math.round((value - min) / (max - min) * max_value)));
    }
*/
}