package org.lime.core.common;

public final class ToDoException extends RuntimeException {
    public ToDoException(String todo) { super(todo); }
}
