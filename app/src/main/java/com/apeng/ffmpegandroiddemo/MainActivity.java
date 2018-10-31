package com.apeng.ffmpegandroiddemo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;
import com.tangxiaopeng.ffmpegandroiddemo.GetPathFromUri;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 *  @dec  首页
 *  @author apeng
 *  @date  2018/10/31 11:31
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    protected final String TAG = "MainActivity";//日志输出标志
    private Context context = MainActivity.this;

    private TextView mTextView;
    private Button btnSelectLocalVideo;
    private Button btnCompressorLocalVideo;

    private Compressor mCompressor;
    private String currentOutputVideoPath = "";//压缩后的视频地址

    private String videoTime = "";//获取视频时长
    private int videoWidth = 0;//获取视频的宽度
    private int videoHeight = 0;//获取视频的高度
    private int videoGotation = 0;//获取视频的角度
    private Bitmap mBitMap;
    private Double videoLength = 0.00;//视频时长 s

    private CustomProgressDialog mProcessingDialog;

    private String mVideoPath = "";//原视频地址

    public static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/apeng/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissions();
        initView();
        initListener();
        initFile();
        initVideo();
    }


    private void initView() {
        mTextView = findViewById(R.id.idSelectLocalVideo);
        btnSelectLocalVideo = findViewById(R.id.btnSelectLocalVideo);
        btnCompressorLocalVideo = findViewById(R.id.btnCompressorLocalVideo);
    }

    private void initListener() {
        btnSelectLocalVideo.setOnClickListener(this);
        btnCompressorLocalVideo.setOnClickListener(this);
    }

    private void initFile() {
        makeRootDirectory(PATH);
        currentOutputVideoPath = PATH + GetPathFromUri.getVideoFileName();
    }


    private void initVideo() {

        mProcessingDialog = new CustomProgressDialog(this);
        mProcessingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.i(TAG, "onDismiss");//如果取消压缩，那么需要销毁
                if (mCompressor != null) {
                    mCompressor.destory();
                }
            }
        });

        mCompressor = new Compressor(this);
        mCompressor.loadBinary(new InitListener() {
            @Override
            public void onLoadSuccess() {
                Log.v(TAG, "load library succeed");
            }

            @Override
            public void onLoadFail(String reason) {
                Log.i(TAG, "load library fail:" + reason);
            }
        });
    }


    /**
     * 视频压缩开始
     */
    private void startCompress() {
        try {
            if (TextUtils.isEmpty(mVideoPath)) {
                Toast.makeText(this, "请重新选择视频", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                File file = new File(currentOutputVideoPath);
                if (file.exists()) {
                    file.delete();
                }
                String cmd = "";
                Log.i(TAG, "startCompress=mVideoPath=" + mVideoPath);
                if (videoGotation == 90 || videoGotation == 270) {//之前以为和旋转的角度有关系，原来
                    Log.i(TAG, "videoGotation=90");
                    cmd = "-y -i " + mVideoPath + " -strict -2 -vcodec libx264 -preset ultrafast " +
                            "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 480x800 -aspect 9:16 " + currentOutputVideoPath;
                } else {
                    Log.i(TAG, "videoGotation=0");
                    if (videoWidth > videoHeight) {
                        cmd = "-y -i " + mVideoPath + " -strict -2 -vcodec libx264 -preset ultrafast " +
                                "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 800x480 -aspect 16:9 " + currentOutputVideoPath;
                    } else {
                        cmd = "-y -i " + mVideoPath + " -strict -2 -vcodec libx264 -preset ultrafast " +
                                "-crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 480x800 -aspect 9:16 " + currentOutputVideoPath;
                    }
                }

                mProcessingDialog.show();
                mProcessingDialog.setProgress(0);
                execCommand(cmd);
            }
        } catch (Exception e) {
            Log.i(TAG, "startCompress=e=" + e.getMessage());
        }

    }

    private void execCommand(final String cmd) {
        File mFile = new File(currentOutputVideoPath);
        if (mFile.exists()) {
            mFile.delete();
        }
        Log.i(TAG, "cmd= " + cmd);
        mCompressor.execCommand(cmd, new CompressListener() {
            @Override
            public void onExecSuccess(String message) {
                mProcessingDialog.dismiss();
                String result = getString(R.string.compress_result_input_output, mVideoPath
                        , getFileSize(mVideoPath), currentOutputVideoPath, getFileSize(currentOutputVideoPath));
                Log.i(TAG, "success " + result);
                Toast.makeText(context, "success " + result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onExecFail(String reason) {
                Log.i(TAG, "fail " + reason);
                Toast.makeText(context, "压缩失败", Toast.LENGTH_SHORT);
                mProcessingDialog.dismiss();
                finish();
            }

            @Override
            public void onExecProgress(String message) {
                try {
                    Log.i(TAG, "progress " + message);
                    double switchNum = getProgress(message);
                    if (switchNum == 10000) {
                        //如果找不到压缩的片段，返回为10000
                        Log.i(TAG, "10000");
                        mProcessingDialog.setProgress(0);
                    } else {
                        mProcessingDialog.setProgress((int) (getProgress(message) / 10));
                    }
                } catch (Exception e) {
                    mProcessingDialog.dismiss();
                    Log.i(TAG, "e=" + e.getMessage());
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSelectLocalVideo:
                addLoacalVideo();
                break;
            case R.id.btnCompressorLocalVideo:
                startCompress();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompressor != null) {
            mCompressor.destory();
        }
    }


    /**
     * 进度条，只能是整形，所以max为1000，最少为0
     *
     * @param source
     * @return
     */
    double getProgressNum = 0.0;

    private double getProgress(String source) {
        if (source.contains("too large")) {//当文件过大的时候，会会出现 Past duration x.y too large
            Log.i(TAG, "too large");
            return getProgressNum;
        }
        Pattern p = Pattern.compile("00:\\d{2}:\\d{2}");
        Matcher m = p.matcher(source);
        if (m.find()) {
            //00:00:00
            String result = m.group(0);
            String temp[] = result.split(":");
            double seconds = Double.parseDouble(temp[1]) * 60 + Double.parseDouble(temp[2]);
            if (0 != videoLength) {
                getProgressNum = seconds / videoLength * 1000;
                return seconds / videoLength * 1000;
            }
            if (seconds == videoLength) {
                return 1000;
            }
        }
//        MyLog.i(TAG, "!m.find()="+getProgressNum);
        return 10000;//出现异常的时候，返回为10000
//      return 0;//出现异常的时候，显示上一个进度条
    }

    private String getFileSize(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return "0 MB";
        } else {
            long size = f.length();
            return (size / 1024f) / 1024f + "MB";
        }
    }


    private void addLoacalVideo() {
        Intent intentvideo = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            intentvideo.setAction(Intent.ACTION_GET_CONTENT);
            intentvideo.setType("video/*");
        } else {
            intentvideo.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intentvideo.addCategory(Intent.CATEGORY_OPENABLE);
            intentvideo.setType("video/*");
        }
        startActivityForResult(Intent.createChooser(intentvideo, "选择要导入的视频"), 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mVideoPath = GetPathFromUri.getPath(context, data.getData());
            Log.i(TAG, "Select file: " + mVideoPath);
            mTextView.setText("" + mVideoPath);
            getVideoTime();
        }
    }

    /**
     * 获取视频的时长
     */
    void getVideoTime() {
        try {
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            retr.setDataSource(mVideoPath);
            videoTime = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);//获取视频时长
            videoWidth = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));//获取视频的宽度
            videoHeight = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));//获取视频的高度
            videoGotation = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));//获取视频的角度

            Log.i(TAG, "videoWidth=" + videoWidth);
            Log.i(TAG, "videoHeight=" + videoHeight);

//            MyLog.i(TAG, "videoTime=" + videoTime);
//            mBitMap = retr.getFrameAtTime();
            videoLength = Double.parseDouble(videoTime) / 1000.00;
            Log.i(TAG, "videoLength=" + videoLength);

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "e=" + e.getMessage());
            videoLength = 0.00;
            finish();
            Toast.makeText(context, "异常错误", Toast.LENGTH_SHORT);
        }

    }

    /**
     * 没有文件夹。创建文件夹
     *
     * @param filePath
     */
    public void makeRootDirectory(String filePath) {
        Log.i(TAG, "makeRootDirectory=");
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                Boolean isTrue = file.mkdir();
                Log.i(TAG, "istrue=" + isTrue + "");
            }
        } catch (Exception e) {

        }
    }


    /**
     * @dec 一句代码搞定权限问题
     * @author tangxiaopeng
     * @date 2018/10/31 10:54
     */
    public void getPermissions() {
        XXPermissions.with(this)
                .constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                .permission("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {

                    }
                });

    }
}
