package com.karbyshev.fileloader;


import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements FileLoader.DownloadingStateCallback {
    private final String MY = "https://www.sample-videos.com/video/mp4/240/big_buck_bunny_240p_1mb.mp4";

    private Button mDownloadButton;
    private TextView mShowFilePathTextView;
    private ProgressDialog mProgressDialog;
    private FileLoader fileLoader = FileLoader.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mShowFilePathTextView = (TextView)findViewById(R.id.myTextView);
        mShowFilePathTextView.setText(MY);
        mShowFilePathTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMedia(getExternalFilesDir(null) + "/local.mp4");
            }
        });

        mDownloadButton = (Button) findViewById(R.id.myButton);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fileLoader.isDownloadingInProgress()) {
                    showProgressDialog();
                    fileLoader.downloadFile(MY,
                            getExternalFilesDir(null) + "/local.mp4",
                            MainActivity.this);
                }
            }
        });

        if (fileLoader.isDownloadingInProgress()) {
            showProgressDialog();
            fileLoader.subscribe(this);
        }

        checkLocalFile();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fileLoader.unsubscribe();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkLocalFile();
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Downloading...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    public void onProgressUpdated(int progress) {
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgress(progress);
    }

    @Override
    public void onDownloadingComplete(String filePath) {
        Toast.makeText(this, "File downloaded", Toast.LENGTH_SHORT).show();
        mProgressDialog.dismiss();

        checkLocalFile();
    }

    @Override
    public void onDownloadingError(String error) {
        Toast.makeText(this, "Download error: " + error, Toast.LENGTH_LONG).show();
        mProgressDialog.dismiss();
    }

    private void playMedia(String newVideoPath){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newVideoPath));
        intent.setDataAndType(Uri.parse(newVideoPath), "video/mp4");
        startActivity(intent);
    }

    private void checkLocalFile(){
        File file = new File(getExternalFilesDir(null) + "/local.mp4");
        if (file.exists()){
            mShowFilePathTextView.setClickable(true);
            mShowFilePathTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
            mDownloadButton.setClickable(false);
            mDownloadButton.setBackgroundColor(getResources().getColor(R.color.colorDisable));
        } else {
            mShowFilePathTextView.setClickable(false);
            mShowFilePathTextView.setTextColor(getResources().getColor(R.color.colorDisable));
            mDownloadButton.setClickable(true);
        }
    }
}



