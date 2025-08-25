package com.rsvmcs.qcrsip.test.pojo.c2_1.req;


import com.rsvmcs.qcrsip.test.pojo.c2_1.Parameters;

public class BaseRequestDto {

    private String _command;

    private Parameters parameters;

    public String get_command() {
        return _command;
    }

    public void set_command(String _command) {
        this._command = _command;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}
