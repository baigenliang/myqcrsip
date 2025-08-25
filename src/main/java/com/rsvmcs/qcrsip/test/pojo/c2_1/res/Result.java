package com.rsvmcs.qcrsip.test.pojo.c2_1.res;

public class Result {

    //成功0,失败1
    private Integer _code = 0;

    private String text;

    public Integer get_code() {
        return _code;
    }

    public void set_code(Integer _code) {
        this._code = _code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
