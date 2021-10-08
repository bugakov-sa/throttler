package test.task.throttling;

public interface Throttler {

    boolean checkRequest(String name);
}
