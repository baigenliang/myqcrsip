package com.rsvmcs.qcrsip.test.pojo.c2_1.req;

import com.alibaba.fastjson2.annotation.JSONField;

public class BaseReq {

    /**
     * 请求内容
     */
    @JSONField(name = "request")
    private BaseRequest request;

    public BaseRequest getRequest() {
        return request;
    }

    public void setRequest(BaseRequest request) {
        this.request = request;
    }
}