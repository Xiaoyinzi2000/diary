package com.example.diary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.diary.NoteAdapter;
import com.example.diary.CRUD;
import com.example.diary.NoteDatabase;
import com.example.diary.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    private NoteDatabase dbHelper;


    private Context context = this;
    private Context  author = this;
    private Context title = this;
    final String TAG = "System===";//输出
    private FloatingActionButton fa_btn;//悬浮按钮组件
    private ListView listView;
    private NoteAdapter adapter;
    private List<Note> noteList = new ArrayList<Note>();
    //private List<Note> noteList2 = new ArrayList<Note>();
    private Toolbar myToolbar;
    private Intent intent = new Intent();
    int returnMode;//返回的mode值，-1表示无操作，0表示新增，1表示修改，2表示删除

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取组件
        fa_btn = findViewById(R.id.fab);
        listView = findViewById(R.id.lv);
        adapter = new NoteAdapter(getApplicationContext(), noteList);
        listView.setAdapter(adapter);
        myToolbar = findViewById(R.id.myToolbar);

        //设置Action Bar，自定义Toolbar
        setSupportActionBar(myToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//设置toolber取代action bar
        //刷新页面
        refreshListView();//访问数据库后就进行刷新

        //给lv设置点击事件
        listView.setOnItemClickListener(this);
        //点击悬浮按钮，设置点击事件
        fa_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建意图
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                //设置新建笔记的mode值，与修改笔记的mode值为3进行区分
                intent.putExtra("mode", 4);
                //启动活动，获取结果
                startActivityForResult(intent, 0);
            }
        });
        //设置事件监听
        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    /**
     * 引入menu菜单栏
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        //显示搜索栏
        MenuItem mySearch = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) mySearch.getActionView();
        //设置搜索默认提示文字
        searchView.setQueryHint("搜索");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            //提交是进行搜索
            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }
            //输入字符改变是进行搜索
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                if(returnMode!=-1)refreshListView();
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 设置菜单栏按钮监听
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_all:
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("确定要全部删除吗？")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper = new NoteDatabase(context);
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                db.delete("notes", null, null);//删除数据表的所有数据
                                db.execSQL("update sqlite_sequence set seq=0 where name='notes'"); //将索引设置为1
                                refreshListView();
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //接收startActivityForResult的结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        long note_Id;//笔记的id，主键
        //接收结果
        returnMode = data.getExtras().getInt("mode", -1);
        String content = data.getExtras().getString("content");
        String time = data.getExtras().getString("time");
        String author = data.getExtras().getString("author");
        String title = data.getExtras().getString("title");
        int tag = data.getExtras().getInt("tag", 1);
        note_Id = data.getExtras().getLong("id", 0);
        Log.d(TAG, "returnMode:" + returnMode);
        if (returnMode == 1) {//修改笔记
            //将结果写入Note实体类
            Note newNote = new Note(content, time,author,title, tag);
            newNote.setId(note_Id);//需要通过id进行修改
            CRUD op = new CRUD(context);
            op.open();
            op.updateNote(newNote);
            op.close();
        } else if (returnMode == 0) {//新增笔记
            //将结果写入Note实体类
            Note newNote = new Note(content, time,author,title , tag);
            CRUD op = new CRUD(context);
            op.open();
            op.addNote(newNote);
            op.close();
        } else if (returnMode == 2) {//删除笔记
            Log.d(TAG,"returnMode:"+returnMode);
            Note curNote = new Note();
            curNote.setId(note_Id);
            CRUD op = new CRUD(context);
            op.open();
            op.removeNote(curNote);
            op.close();
        } else{}

        refreshListView();//访问数据库后就进行刷新

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 更新内容
     */
    public void refreshListView() {
        CRUD op = new CRUD(context);
        op.open();
        //设置adapter
        if (noteList.size() > 0) {
            noteList.clear();
        }

        noteList.addAll(op.getAllNotes());
        op.close();
        adapter.notifyDataSetChanged();
    }

    /**
     * 重写点击事件
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.lv:
                Note curNote = (Note) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("content", curNote.getContent());
                intent.putExtra("id", curNote.getId());
                intent.putExtra("time", curNote.getTime());
                intent.putExtra("author", curNote.getAuthor() );
                intent.putExtra("title", curNote.getTitle() );
                //修改笔记的mode设置为3，与新建笔记的mode值为4进行区分
                intent.putExtra("mode", 3);
                intent.putExtra("tag", curNote.getTog());
                startActivityForResult(intent, 1);
                Log.d(TAG, "onItemClick" + (position + 1));
                break;
        }
    }
}