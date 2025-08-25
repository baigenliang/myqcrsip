package com.rsvmcs.qcrsip.test.pojo.c2_1.req;

import com.alibaba.fastjson2.annotation.JSONField;

public class BaseReqDto {

    /**
     * 请求内容
     */
    @JSONField(name = "request")
    private BaseRequestDto request;

    public BaseRequestDto getRequest() {
        return request;
    }

    public void setRequest(BaseRequestDto request) {
        this.request = request;
    }
}