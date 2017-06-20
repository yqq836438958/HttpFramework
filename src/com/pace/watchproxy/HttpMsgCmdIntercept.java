
package com.pace.watchproxy;

import com.pace.httpserver.BaseTosService;
import com.pace.httpserver.IResponseObserver;
import com.qq.taf.jce.JceStruct;

public class HttpMsgCmdIntercept extends BaseHttpIntercept {

    public HttpMsgCmdIntercept(boolean selfHandle) {
        super(selfHandle);
    }

    @Override
    protected boolean onIntercept(JceStruct data) {
        BaseTosService service = mClientHandler.newTosService(data);
        if (service == null) {
            return false;
        }
        final long lReq = service.getUniqueSeq();
        boolean handle = service.invoke(new IResponseObserver() {
            @Override
            public void onResponseSucceed(long uniqueSeq, int operType, JceStruct response) {
                if (lReq == uniqueSeq) {
                    mClientHandler.onSuccess(response);
                }
            }

            @Override
            public void onResponseFailed(long uniqueSeq, int operType, int errorCode,
                    String description) {
                if (lReq == uniqueSeq) {
                    mClientHandler.onFail(errorCode, description);
                }
            }
        });
        return handle;
    }
}
