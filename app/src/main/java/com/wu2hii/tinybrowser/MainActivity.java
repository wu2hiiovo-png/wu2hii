package com.wu2hii.cleanx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.text.DecimalFormat;

public class MainActivity extends Activity {
    private static final int PICK_TREE=41;
    private LinearLayout root, list;
    private TextView summary, storage;
    private Button scanButton, cleanButton;
    private Uri treeUri;
    private final ArrayList<Item> candidates=new ArrayList<>();
    private final ArrayList<CheckBox> checks=new ArrayList<>();
    private long total=0;
    private static class Item {
        Uri uri; String name,path,reason; long size;
        Item(Uri u,String n,String p,long s,String r){uri=u;name=n;path=p;size=s;reason=r;}
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(13,20,38));
        getWindow().setNavigationBarColor(Color.rgb(13,20,38));
        buildUi(); refreshStorage();
    }
    private void buildUi(){
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(18),dp(18),dp(18),dp(18)); root.setBackgroundColor(Color.rgb(12,18,34));
        TextView brand=text("CleanX",30,Color.WHITE); brand.setTypeface(null,1); root.addView(brand);
        TextView sub=text("轻量 · 安全 · 可控的存储清理工具",14,0xff9eacc8); root.addView(sub);
        storage=text("正在读取存储空间…",14,Color.WHITE); storage.setPadding(0,dp(18),0,dp(8)); root.addView(storage);
        summary=text("选择存储目录后开始扫描",16,0xffdce6ff); summary.setPadding(0,dp(12),0,dp(12)); root.addView(summary);
        scanButton=button("选择存储目录并扫描"); root.addView(scanButton);
        cleanButton=button("删除已选文件"); cleanButton.setEnabled(false); root.addView(cleanButton);
        Button system=button("打开系统缓存清理"); root.addView(system);
        TextView hint=text("只会列出可识别的临时文件、日志、备份和空文件。删除前请逐项确认。不会扫描或删除 Android/data、Android/obb、应用私有数据。",12,0xff9eacc8);
        hint.setPadding(0,dp(12),0,dp(10)); root.addView(hint);
        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(1); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        scanButton.setOnClickListener(v->chooseFolder());
        cleanButton.setOnClickListener(v->confirmDelete());
        system.setOnClickListener(v->openSystemCleaner());
    }
    private void chooseFolder(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK_TREE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_TREE && result==RESULT_OK && data!=null && data.getData()!=null){
            treeUri=data.getData();
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try{getContentResolver().takePersistableUriPermission(treeUri,flags);}catch(Exception ignored){}
            scan();
        }
    }
    private void scan(){
        candidates.clear(); checks.clear(); list.removeAllViews(); total=0;
        summary.setText("正在扫描…"); scanButton.setEnabled(false);
        new Thread(()->{
            try{
                String rootId=DocumentsContract.getTreeDocumentId(treeUri);
                walk(DocumentsContract.buildDocumentUriUsingTree(treeUri,rootId),"",0);
            }catch(Exception e){}
            runOnUiThread(()->{
                scanButton.setEnabled(true);
                summary.setText("发现 "+candidates.size()+" 个候选项目 · 共 "+fmt(total));
                if(candidates.isEmpty()) addMessage("没有发现符合规则的临时文件。");
                else for(Item item:candidates) addItem(item);
                cleanButton.setEnabled(!candidates.isEmpty());
            });
        }).start();
    }
    private void walk(Uri dir,String path,int depth){
        if(depth>12 || candidates.size()>1500)return;
        String id=DocumentsContract.getDocumentId(dir);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,id);
        String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        try(Cursor c=getContentResolver().query(children,cols,null,null,null)){
            if(c==null)return;
            while(c.moveToNext() && candidates.size()<1500){
                String docId=c.getString(0), name=c.getString(1); String mime=c.getString(2);
                long size=c.isNull(3)?0:c.getLong(3);
                String childPath=path.isEmpty()?name:path+"/"+name;
                Uri u=DocumentsContract.buildDocumentUriUsingTree(treeUri,docId);
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){
                    if(!name.equalsIgnoreCase("Android")) walk(u,childPath,depth+1);
                    continue;
                }
                String reason=classify(name,size);
                if(reason!=null){candidates.add(new Item(u,name,childPath,size,reason));total+=Math.max(0,size);}
            }
        }catch(Exception ignored){}
    }
    private String classify(String name,long size){
        String n=name.toLowerCase(Locale.ROOT);
        if(size==0)return "空文件";
        if(n.endsWith(".tmp")||n.endsWith(".temp")||n.endsWith(".log")||n.endsWith(".bak")||n.endsWith(".old")||n.endsWith(".cache"))return "临时/日志/备份文件";
        if(n.contains("cache")||n.contains("temp")||n.contains("thumbnail"))return "名称疑似缓存（请核对）";
        return null;
    }
    private void addItem(Item item){
        CheckBox cb=new CheckBox(this); cb.setText(item.name+"  ·  "+fmt(item.size)+"\n"+item.reason+"\n"+item.path);
        cb.setTextColor(Color.WHITE); cb.setTextSize(13); cb.setPadding(dp(4),dp(8),dp(4),dp(8)); cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff64d8cb));
        list.addView(cb); checks.add(cb);
    }
    private void addMessage(String s){TextView t=text(s,14,0xffc6d2e8);t.setPadding(0,dp(18),0,dp(18));list.addView(t);}
    private void confirmDelete(){
        ArrayList<Integer> selected=new ArrayList<>(); long bytes=0;
        for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()){selected.add(i);bytes+=candidates.get(i).size;}
        if(selected.isEmpty()){toast("请先勾选要删除的项目");return;}
        new AlertDialog.Builder(this).setTitle("确认删除")
          .setMessage("将尝试删除 "+selected.size()+" 个文件，约 "+fmt(bytes)+"。此操作不可撤销，请确认文件确实无用。")
          .setNegativeButton("取消",null).setPositiveButton("删除", (d,w)->{
            int ok=0;
            for(int idx:selected){try{if(DocumentsContract.deleteDocument(getContentResolver(),candidates.get(idx).uri))ok++;}catch(Exception ignored){}}
            toast("已删除 "+ok+" / "+selected.size()+" 个文件");
            scan();
          }).show();
    }
    private void openSystemCleaner(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            try{startActivity(new Intent("android.os.storage.action.CLEAR_APP_CACHE"));}catch(Exception e){
                try{startActivity(new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));}catch(Exception ignored){toast("当前系统不支持该入口");}
            }
        }else toast("请在系统设置的存储空间中清理应用缓存");
    }
    private void refreshStorage(){
        try{StatFs s=new StatFs(Environment.getDataDirectory().getPath());long total=s.getTotalBytes(),free=s.getAvailableBytes();storage.setText("内部存储  ·  已用 "+fmt(total-free)+" / "+fmt(total)+"\n可用空间 "+fmt(free));}catch(Exception ignored){}
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff246b78));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(p);return b;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private String fmt(long n){if(n<1024)return n+" B";String[] u={"KB","MB","GB","TB"};double v=n;int i=-1;do{v/=1024;i++;}while(v>=1024&&i<u.length-1);return new DecimalFormat("0.##").format(v)+" "+u[i];}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
