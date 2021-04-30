package com.llw.asrdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EventListener {

    protected TextView txtResult;//识别结果
    protected Button startBtn;//开始识别  一直不说话会自动停止，需要再次打开
    protected Button stopBtn;//停止识别

    private EventManager asr;//语音识别核心库

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initPermission();

        //初始化EventManager对象
        asr = EventManagerFactory.create(this, "asr");
        //注册自己的输出事件类
        asr.registerListener(this); //  EventListener 中 onEvent方法
    }

    /**
     * 初始化控件
     */
    private void initView() {
        txtResult = (TextView) findViewById(R.id.tv_txt);
        startBtn = (Button) findViewById(R.id.btn_start);
        stopBtn = (Button) findViewById(R.id.btn_stop);

        startBtn.setOnClickListener(new View.OnClickListener() {//开始
            @Override
            public void onClick(View v) {
                asr.send(SpeechConstant.ASR_START, "{}", null, 0, 0);
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {//停止
            @Override
            public void onClick(View v) {
                asr.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
            }
        });
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
                Toast.makeText(this,"没有权限",Toast.LENGTH_SHORT).show();
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    /**
     * 权限申请回调，可以作进一步处理
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }


    /**
     * 自定义输出事件类 EventListener 回调方法
     */
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {

        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            // 识别相关的结果都在这里
            if (params == null || params.isEmpty()) {
                return;
            }
            if (params.contains("\"final_result\"")) {
                // 一句话的最终识别结果
                Log.i("ars.event",params);

                Gson gson = new Gson();
                ASRresponse asRresponse = gson.fromJson(params, ASRresponse.class);//数据解析转实体bean

                if(asRresponse == null) return;
                //从日志中，得出Best_result的值才是需要的，但是后面跟了一个中文输入法下的逗号，
                if(asRresponse.getBest_result().contains("，")){//包含逗号  则将逗号替换为空格，这个地方还会问题，还可以进一步做出来，你知道吗？
                    txtResult.setText(asRresponse.getBest_result().replace('，',' ').trim());//替换为空格之后，通过trim去掉字符串的首尾空格
                }else {//不包含
                    txtResult.setText(asRresponse.getBest_result().trim());
                }
            }
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 基于SDK集成4.2 发送取消事件
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        // 基于SDK集成5.2 退出事件管理器
        // 必须与registerListener成对出现，否则可能造成内存泄露
        asr.unregisterListener(this);
    }
}
