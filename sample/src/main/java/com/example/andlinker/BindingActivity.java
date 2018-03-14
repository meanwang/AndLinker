package com.example.andlinker;

import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.codezjx.andlinker.AndLinker;
import com.codezjx.andlinker.Call;
import com.codezjx.andlinker.Callback;
import com.codezjx.andlinker.adapter.OriginalCallAdapterFactory;
import com.codezjx.andlinker.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.codezjx.andlinker.annotation.ClassName;
import com.codezjx.andlinker.annotation.MethodName;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BindingActivity extends AppCompatActivity {

    private static final String REMOTE_SERVICE_PKG = "com.example.andlinker";
    public static final String REMOTE_SERVICE_ACTION = "com.example.andlinker.REMOTE_SERVICE_ACTION";

    private AndLinker mLinker;
    private IRemoteService mRemoteService;
    private IRemoteTask mRemoteTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binding);
        ButterKnife.bind(this);

        mLinker = new AndLinker.Builder(this)
                .packageName(REMOTE_SERVICE_PKG)
                .action(REMOTE_SERVICE_ACTION)
                // Specify the callback executor by yourself
                //.addCallAdapterFactory(OriginalCallAdapterFactory.create(callbackExecutor))
                .addCallAdapterFactory(OriginalCallAdapterFactory.create()) // Basic
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())  // RxJava2
                .build();
        mLinker.bind();
        mLinker.registerObject(mRemoteCallback);

        mRemoteService = mLinker.create(IRemoteService.class);
        mRemoteTask = mLinker.create(IRemoteTask.class);
    }

    @OnClick({R.id.btn_pid, R.id.btn_basic_types, R.id.btn_call_adapter, R.id.btn_rxjava2_call_adapter,
            R.id.btn_callback, R.id.btn_directional, R.id.btn_oneway})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_pid:
                Toast.makeText(this, "Server pid: " + mRemoteService.getPid(), Toast.LENGTH_SHORT).show();
                mRemoteService.getPid();
                break;
            case R.id.btn_basic_types:
                mRemoteService.basicTypes(1, 2L, true, 3.0f, 4.0d, "str");
                break;
            case R.id.btn_call_adapter:
                Call<Integer> call = mRemoteTask.remoteCalculate(10, 20);
                
                // Synchronous Request
                // int result = call.execute();
                // Log.d("BindingActivity", "remoteCalculate() result:" + result);

                // Asynchronous Request
                call.enqueue(new Callback<Integer>() {
                    @Override
                    public void onResponse(Call<Integer> call, Integer response) {
                        Toast.makeText(BindingActivity.this, "remoteCalculate() onResponse: " + response, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Integer> call, Throwable t) {
                        Toast.makeText(BindingActivity.this, "remoteCalculate() failure: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.btn_rxjava2_call_adapter:
                mRemoteTask.getDatas()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(datas -> Toast.makeText(this, "getDatas() return: " + datas, Toast.LENGTH_LONG).show());
                break;
            case R.id.btn_callback:
                mRemoteService.registerCallback(mRemoteCallback);
                break;
            case R.id.btn_directional:
                int[] arr = {1, 2, 3};
                ParcelableObj parcelableObj = new ParcelableObj();
                Rect rect = new Rect(10, 20, 30, 40);
                mRemoteService.directionalParamMethod(arr, parcelableObj, rect);
                Toast.makeText(this, "After directionalParamMethod parcelableObj: " + parcelableObj
                    + " rect: " + rect, Toast.LENGTH_LONG).show();
                break;
            case R.id.btn_oneway:
                mRemoteService.onewayMethod("oneway");
                Toast.makeText(this, "After oneway method client.", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLinker.unbind();
    }

    private final IRemoteCallback mRemoteCallback = new IRemoteCallback() {
        @Override
        public void onValueChange(int value) {
            // Invoke when server side callback
            Toast.makeText(BindingActivity.this, "Server callback value: " + value, Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * Copy the original interface, wrap the return type of the method, keep the original @ClassName and @MethodName.
     */
    @ClassName("com.example.andlinker.IRemoteTask")
    public interface IRemoteTask {

        @MethodName("remoteCalculate")
        Call<Integer> remoteCalculate(int a, int b);

        @MethodName("getDatas")
        Observable<List<ParcelableObj>> getDatas();

    }
}
