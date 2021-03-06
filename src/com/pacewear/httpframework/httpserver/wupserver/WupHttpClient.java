
package com.pacewear.httpframework.httpserver.wupserver;

import android.content.Context;

import com.pacewear.httpframework.apachehttp.ApacheHttpClientImpl;
import com.pacewear.httpframework.core.IHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpParams;

import qrom.component.wup.apiv2.IApacheHttpClient;

public class WupHttpClient implements IApacheHttpClient {
    private Context mContext = null;

    public WupHttpClient(Context context) {
        mContext = context;
    }

    @Override
    public HttpResponse execute(HttpParams arg0, HttpPost arg1) {
        // TODO Auto-generated method stub
        IHttpClient<HttpResponse, HttpParams, HttpPost> httpClient = new ApacheHttpClientImpl(
                mContext);
        return httpClient.execute(arg0, arg1);
    }
}
