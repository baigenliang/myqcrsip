package com.rsvmcs.qcrsip.test.pojo.c2_1.res;

import com.rsvmcs.qcrsip.test.pojo.c2_1.Parameters;

public class BaseResponse {

    private String _Command;

    private Result result;

    private Parameters parameters;

    public String get_Command() {
        return _Command;
    }

    public void set_Command(String _Command) {
        this._Command = _Command;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}
