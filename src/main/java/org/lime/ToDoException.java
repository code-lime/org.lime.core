package org.lime;

public final class ToDoException extends RuntimeException {
    public ToDoException(String todo) { super(todo); }
}
