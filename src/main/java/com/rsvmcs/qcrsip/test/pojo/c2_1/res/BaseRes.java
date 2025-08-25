package com.rsvmcs.qcrsip.test.pojo.c2_1.res;

import com.alibaba.fastjson2.annotation.JSONField;

public class BaseRes {

    /**
     * 请求内容
     */
    @JSONField(name = "response")
    private BaseResponse response;

    public BaseResponse getResponse() {
        return response;
    }

    public void setResponse(BaseResponse response) {
        this.response = response;
    }
}