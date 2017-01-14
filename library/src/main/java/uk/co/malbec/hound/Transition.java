package uk.co.malbec.hound;

import org.joda.time.DateTime;

public class Transition {
    private OperationType operationType;
    private DateTime executeTime;

    public Transition(OperationType operationType, DateTime executeTime) {
        this.operationType = operationType;
        this.executeTime = executeTime;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public DateTime getExecuteTime() {
        return executeTime;
    }
}
