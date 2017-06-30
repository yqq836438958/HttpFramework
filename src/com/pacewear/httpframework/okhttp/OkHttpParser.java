
package com.pacewear.httpframework.okhttp;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.pacewear.httpframework.common.ByteUtil;
import com.pacewear.httpframework.common.FileUtil;
import com.tencent.tws.api.HttpPackage;
import com.tencent.tws.api.HttpRequestCommand;
import com.tencent.tws.api.HttpRequestGeneralParams;
import com.tencent.tws.api.HttpResponseResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// bt通道走正常的http请求，转换为okhttp请求
public class OkHttpParser {
    private static final String TAG = "OkHttpUtil";

    public static OkHttpClient.Builder getClientBuilderFromPacket(HttpPackage e1) {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        HttpRequestGeneralParams param = HttpRequestGeneralParams
                .StringToHttpRequestGeneralParams(e1.getHttpData());
        RequstConvert.parseHttpParam(builder, param, param.mMaskFlag);
        return builder;
    }

    public static Request getRequestFromPacket(HttpPackage e1) {
        HttpRequestGeneralParams param = HttpRequestGeneralParams
                .StringToHttpRequestGeneralParams(e1.getHttpData());
        Request.Builder requestBuilder = new Request.Builder();
        RequstConvert.parseHttpRequest(requestBuilder, param, param.mMaskFlag);
        return requestBuilder.build();
    }

    public static String getHeadersFromPacket(Response response) {
        JSONObject json = new JSONObject();
        Headers headers = response.request().headers();
        int size = headers.size();
        for (int i = 0; i < size; i++) {
            try {
                json.put(headers.name(i), headers.value(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return json.toString();
    }

    public static void onParseResponse(Response response, HttpPackage e1) {
        e1.setResponseExtra(getHeadersFromPacket(response));
        e1.setStatusCode(response.code());
        switch (e1.getType()) {
            case HttpRequestCommand.GET_WITH_STREAMRETURN:
            case HttpRequestCommand.POST_WITH_STRAMRETURN:
                e1.setHttpData(onParseInputStream(response));
                break;
            case HttpRequestCommand.GET_TEXT:
                try {
                    e1.setHttpData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case HttpRequestCommand.POST:
            case HttpRequestCommand.GET_WITH_GENERAL_TEXT:
            case HttpRequestCommand.POST_WITH_GENERAL:
                e1.setHttpData(onParseCommonResponse(response));
                break;
            case HttpRequestCommand.GET_WITH_GENERAL_FILE:
            case HttpRequestCommand.GET_PNG_IMAGE:
                onParseDownloadRsp(e1, response);
                break;
            default:
                break;
        }
    }

    private static String onParseInputStream(Response response) {
        ResponseBody body = response.body();
        byte[] bs = ByteUtil.toBytes(body.byteStream());
        return Base64.encodeToString(bs, Base64.DEFAULT);
    }

    private static String onParseCommonResponse(Response response) {
        Headers headers = response.headers();
        String strBody = "";
        try {
            strBody = response.body().string();

            // encode HttpResponce into jsonobject
            JSONObject jsonObject = new JSONObject();// main object
            JSONObject jsonHead = new JSONObject();
            for (int i = 0; i < headers.size(); i++) {
                try {
                    jsonHead.put(headers.name(i), headers.value(i));
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            try {
                jsonObject.put("ResponseHead", jsonHead);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i(TAG, "put responseHead error");
            }

            try {
                jsonObject.put("ResponseBody", strBody);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i(TAG, "put responseBody error");
            }

            return jsonObject.toString();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

    private static void onParseDownloadRsp(HttpPackage e1, Response response) {
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try {
            is = response.body().byteStream();
            long fileName = System.currentTimeMillis();
            String extensionName = FileUtil.getFileExtensionFromUrl(e1.getHttpData());
            String destFileDir = Environment.getExternalStorageDirectory()
                    .getPath() + "/" + fileName + "." + extensionName;
            File file = FileUtil.createFile(destFileDir);
            if (file == null) {
                throw new FileNotFoundException("file create fail");
            }
            fos = new FileOutputStream(file);
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            e1.setReplyType(HttpResponseResult.isFileDatas);
            e1.setHttpData(destFileDir);
            // 如果下载文件成功，第一个参数为文件的绝对路径
        } catch (IOException e) {
            e1.setStatusCode(HttpRequestCommand.NETWORKFAIL_STATUS);
            e1.setHttpData(e.getMessage());
            e1.setReplyType(HttpResponseResult.inDirectDatas);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
    }
}
