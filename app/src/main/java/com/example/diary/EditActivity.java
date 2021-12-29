package com.example.diary;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditActivity extends BaseActivity {

    private EditText editText1;//编辑的笔记
    private EditText editText2;//编辑的author
    private EditText editText3;//编辑的tatil

    private String old_content = "";
    private String old_time = "";
    private String old_author = "";
    private String old_title = "";
    private int old_tag = 1;
    private long id = 0;
    private int openMode = 0;
    private int tag = 1;
    public Intent intent = new Intent();
    private Boolean tagChange = false;
    private Toolbar noteToolbar;



    /**
     * 引入menu菜单栏
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_layout);
        editText1 = findViewById(R.id.editText1);
        editText2 = findViewById(R.id.editText2);
        editText3 = findViewById(R.id.editText3);
        noteToolbar = findViewById(R.id.noteToolbar);


        //设置Action Bar，自定义Toolbar
        setSupportActionBar(noteToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//设置toolber取代action bar
        //设置Navigation返回键的事件监听，与系统的返回键功能相同
        noteToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoSetMessage(intent);//自动更新笔记
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Intent getIntent = getIntent();
        //定义意图
        openMode = getIntent.getIntExtra("mode", 0);

        Log.d("System=====", "openMode:" + openMode);
        if (openMode == 3) {
            //打开已存在的note，将内容写入到已编辑的笔记中，实现继续编辑
            id = getIntent.getLongExtra("id", 0);
            old_content = getIntent.getStringExtra("content");
            old_time = getIntent.getStringExtra("time");
            old_author  = getIntent.getStringExtra("author");
            old_title =getIntent.getStringExtra("title");
            old_tag = getIntent.getIntExtra("tag", 1);
            editText1.setText(old_content);//填充内容
            editText1.setSelection(old_content.length());//移动光标的位置（最后），方便再次书写
            editText2.setText(old_author );//填充内容
            editText2.setSelection(old_author.length());//移动光标的位置（最后），方便再次书写
            editText3.setText(old_title  );//填充内容
            editText3.setSelection(old_title .length());//移动光标的位置（最后），方便再次书写
        }

    }

    /**
     * 设置菜单栏按钮监听
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final Intent intent = new Intent();
        switch (item.getItemId()) {
            case R.id.delete:
                new AlertDialog.Builder(EditActivity.this)
                        .setMessage("您确定要删除吗？")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (openMode == 4) {//如果是新增笔记，则不创建
                                    intent.putExtra("mode", -1);
                                    setResult(RESULT_OK, intent);
                                } else {//如果是修改笔记，则删除
                                    intent.putExtra("mode", 2);
                                    intent.putExtra("id", id);
                                    setResult(RESULT_OK, intent);
                                }
                                finish();
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();//关闭窗口
                    }
                }).create().show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 系统按钮监听
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            autoSetMessage(intent);
            //按下返回键，将页面的文本获取，并返回
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 判断是新增笔记还是修改笔记,mode值是3为修改，为4是新增
     */
    public void autoSetMessage(Intent intent) {
        if (openMode == 4) {
            Log.d("input", "input11111");
            //判断笔记是否为空，若为空，则不新增笔记
            if (editText2.getText().toString().length() == 0) {
                intent.putExtra("mode", -1);
            } else {
                intent.putExtra("mode", 0);
                intent.putExtra("content", editText1.getText().toString());
                intent.putExtra("time", dateToStr());
                intent.putExtra("author", editText2.getText().toString());
                intent.putExtra("title", editText3.getText().toString());
                intent.putExtra("tag", tag);
            }
        } else {
            //判断笔记是否被修改，或者标签是否更换，否则不更新笔记
            if (editText2.getText().toString().equals(old_content) && !tagChange) {
                intent.putExtra("mode", -1);
            } else {
                intent.putExtra("mode", 1);
                intent.putExtra("content", editText1.getText().toString());
                intent.putExtra("time", dateToStr());
                intent.putExtra("author", editText2.getText().toString());
                intent.putExtra("title", editText3.getText().toString());
                intent.putExtra("id", id);
                intent.putExtra("tag", tag);
            }
            if (editText1.getText().toString().equals(old_author ) && !tagChange) {
                intent.putExtra("mode", -1);
            } else {
                intent.putExtra("mode", 1);
                intent.putExtra("content", editText1.getText().toString());
                intent.putExtra("time", dateToStr());
                intent.putExtra("author", editText2.getText().toString());
                intent.putExtra("title", editText3.getText().toString());
                intent.putExtra("id", id);
                intent.putExtra("tag", tag);
            }
            if (editText3.getText().toString().equals(old_title ) && !tagChange) {
                intent.putExtra("mode", -1);
            } else {
                intent.putExtra("mode", 1);
                intent.putExtra("content", editText1.getText().toString());
                intent.putExtra("time", dateToStr());
                intent.putExtra("author", editText2.getText().toString());
                intent.putExtra("title", editText3.getText().toString());
                intent.putExtra("id", id);
                intent.putExtra("tag", tag);
            }
        }

    }

    /**
     * 转换时间格式
     *
     * @return
     */
    public String dateToStr() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(date);
    }

}











