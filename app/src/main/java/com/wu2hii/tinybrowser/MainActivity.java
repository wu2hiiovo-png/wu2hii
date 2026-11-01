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
import android.widget.*;
import java.util.*;
import java.text.DecimalFormat;
import java.io.*;

public class MainActivity extends Activity {
    private static final int PICK_TREE=41, MAX_ITEMS=2000;
    private static final String[] ROOT_BASES={"/data/user/0","/data/user_de/0","/storage/emulated/0/Android/data"};
    private LinearLayout root,list;
    private TextView summary,storage,modeLabel;
    private Button scanButton,rootButton,cleanButton;
    private Uri treeUri;
    private final ArrayList<Item> candidates=new ArrayList<>();
    private final ArrayList<CheckBox> checks=new ArrayList<>();
    private long total=0;
    private boolean scanning=false;
    private static class Item {
        Uri uri; String name,path,reason; long size; boolean rootFile;
        Item(Uri u,String n,String p,long s,String r,boolean rf){uri=u;name=n;path=p;size=s;reason=r;rootFile=rf;}
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(13,20,38));
        getWindow().setNavigationBarColor(Color.rgb(13,20,38));
        buildUi(); refreshStorage();
    }
    private void buildUi(){
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(18),dp(18),dp(18),dp(18)); root.setBackgroundColor(Color.rgb(12,18,34));
        TextView brand=text("CleanX 2.0",28,Color.WHITE); brand.setTypeface(null,1); root.addView(brand);
        root.addView(text("双模式存储清理 · 手动选择 · 删除前确认",13,0xff9eacc8));
        storage=text("正在读取存储空间…",14,Color.WHITE); storage.setPadding(0,dp(14),0,dp(4)); root.addView(storage);
        modeLabel=text("当前模式：普通模式",14,0xff64d8cb); modeLabel.setPadding(0,dp(10),0,dp(4)); root.addView(modeLabel);
        summary=text("选择普通扫描目录，或使用 Root 增强扫描",15,0xffdce6ff); summary.setPadding(0,dp(8),0,dp(8)); root.addView(summary);
        scanButton=button("普通模式：选择目录并扫描"); root.addView(scanButton);
        rootButton=button("Root 增强模式：扫描应用缓存"); root.addView(rootButton);
        cleanButton=button("删除已选文件"); cleanButton.setEnabled(false); root.addView(cleanButton);
        Button system=button("打开 Android 系统存储管理"); root.addView(system);
        TextView hint=text("Root 模式仅检查已知 cache 目录中的普通文件，不扫描数据库、账号数据、应用设置或系统分区。所有结果默认不勾选，必须手动选择并二次确认。",11,0xff9eacc8);
        hint.setPadding(0,dp(8),0,dp(8)); root.addView(hint);
        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(1); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        scanButton.setOnClickListener(v->chooseFolder());
        rootButton.setOnClickListener(v->scanRoot());
        cleanButton.setOnClickListener(v->confirmDelete());
        system.setOnClickListener(v->openSystemCleaner());
    }
    private void chooseFolder(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(i,PICK_TREE);}catch(Exception e){toast("无法打开系统文件选择器");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_TREE && result==RESULT_OK && data!=null && data.getData()!=null){
            treeUri=data.getData();
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try{getContentResolver().takePersistableUriPermission(treeUri,flags);}catch(Exception ignored){}
            scanNormal();
        }
    }
    private void resetResults(String label){
        candidates.clear(); checks.clear(); list.removeAllViews(); total=0;
        modeLabel.setText("当前模式："+label);
        cleanButton.setEnabled(false);
    }
    private void scanNormal(){
        if(treeUri==null)return;
        resetResults("普通模式"); beginScan();
        new Thread(()->{
            try{
                String rootId=DocumentsContract.getTreeDocumentId(treeUri);
                walk(DocumentsContract.buildDocumentUriUsingTree(treeUri,rootId),"",0);
            }catch(Exception ignored){}
            finishScan("普通模式扫描完成");
        }).start();
    }
    private void walk(Uri dir,String path,int depth){
        if(depth>16 || candidates.size()>=MAX_ITEMS)return;
        String id=DocumentsContract.getDocumentId(dir);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,id);
        String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE};
        try(Cursor c=getContentResolver().query(children,cols,null,null,null)){
            if(c==null)return;
            while(c.moveToNext() && candidates.size()<MAX_ITEMS){
                String docId=c.getString(0), name=c.getString(1), mime=c.getString(2);
                long size=c.isNull(3)?0:c.getLong(3);
                if(name==null)continue;
                String childPath=path.isEmpty()?name:path+"/"+name;
                Uri u=DocumentsContract.buildDocumentUriUsingTree(treeUri,docId);
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){
                    walk(u,childPath,depth+1); continue;
                }
                String reason=classify(name,size);
                if(reason!=null){candidates.add(new Item(u,name,childPath,size,reason,false));total+=Math.max(0,size);}
            }
        }catch(Exception ignored){}
    }
    private void scanRoot(){
        if(scanning)return;
        resetResults("Root 增强模式"); beginScan();
        new Thread(()->{
            try{
                String script="id; for base in /data/user/0 /data/user_de/0 /storage/emulated/0/Android/data; do [ -d \"$base\" ] || continue; find \"$base\" -type d -name cache -print 2>/dev/null | while IFS= read -r d; do find \"$d\" -type f -print 2>/dev/null | while IFS= read -r f; do s=$(stat -c %s \"$f\" 2>/dev/null); [ -n \"$s\" ] && [ \"$s\" -gt 0 ] 2>/dev/null && printf '%s\\t%s\\n' \"$s\" \"$f\"; done; done; done";
                Process p=new ProcessBuilder("su","-c",script).redirectErrorStream(true).start();
                BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line; boolean rootOk=false;
                while((line=reader.readLine())!=null && candidates.size()<MAX_ITEMS){
                    if(line.startsWith("uid=0")){rootOk=true;continue;}
                    int tab=line.indexOf('\t'); if(tab<=0)continue;
                    try{
                        long size=Long.parseLong(line.substring(0,tab).trim());
                        String path=line.substring(tab+1).trim();
                        if(size<=0 || !isAllowedRootCache(path))continue;
                        String name=path.substring(path.lastIndexOf('/')+1);
                        candidates.add(new Item(null,name,path,size,"应用缓存目录文件（Root 可访问）",true)); total+=size;
                    }catch(Exception ignored){}
                }
                int code=p.waitFor();
                if(!rootOk && code!=0)throw new SecurityException("未获得 Root 授权");
                runOnUiThread(()->{if(candidates.isEmpty())summary.setText("Root 扫描完成：未发现缓存文件，或 Root 未授权");});
            }catch(Exception e){
                runOnUiThread(()->{summary.setText("Root 扫描失败："+(e instanceof SecurityException?"请在 Magisk 中允许 CleanX 获取 Root 权限":"请确认设备已安装并启用 Magisk"));});
            }
            finishScan("Root 增强扫描完成");
        }).start();
    }
    private boolean isAllowedRootCache(String p){
        if(p==null || p.contains("\n") || p.contains("\r"))return false;
        boolean base=false;
        for(String b:ROOT_BASES)if(p.startsWith(b+"/"))base=true;
        return base && p.contains("/cache/") && !p.contains("/../");
    }
    private String classify(String name,long size){
        String n=name.toLowerCase(Locale.ROOT);
        if(size==0)return "空文件";
        if(n.endsWith(".tmp")||n.endsWith(".temp")||n.endsWith(".log")||n.endsWith(".bak")||n.endsWith(".old")||n.endsWith(".cache"))return "临时/日志/备份文件";
        if(n.contains("cache")||n.contains("temp")||n.contains("thumbnail"))return "名称疑似缓存（请核对）";
        return null;
    }
    private void beginScan(){
        scanning=true; scanButton.setEnabled(false); rootButton.setEnabled(false); summary.setText("正在扫描，请稍候…");
    }
    private void finishScan(String label){
        runOnUiThread(()->{
            scanning=false; scanButton.setEnabled(true); rootButton.setEnabled(true);
            summary.setText(label+"：发现 "+candidates.size()+" 个候选文件 · "+fmt(total));
            if(candidates.isEmpty())addMessage("没有发现符合规则的文件。若 Root 扫描为空，请检查 Magisk 授权记录。");
            else for(Item item:candidates)addItem(item);
            cleanButton.setEnabled(!candidates.isEmpty());
        });
    }
    private void addItem(Item item){
        CheckBox cb=new CheckBox(this);
        cb.setText((item.rootFile?"[ROOT] ":"[普通] ")+item.name+" · "+fmt(item.size)+"\n"+item.reason+"\n"+item.path);
        cb.setTextColor(Color.WHITE); cb.setTextSize(12); cb.setPadding(dp(4),dp(7),dp(4),dp(7));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff64d8cb));
        list.addView(cb); checks.add(cb);
    }
    private void addMessage(String s){TextView t=text(s,13,0xffc6d2e8);t.setPadding(0,dp(18),0,dp(18));list.addView(t);}
    private void confirmDelete(){
        ArrayList<Integer> selected=new ArrayList<>(); long bytes=0; int rootCount=0;
        for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()){selected.add(i);bytes+=candidates.get(i).size;if(candidates.get(i).rootFile)rootCount++;}
        if(selected.isEmpty()){toast("请先手动勾选要删除的文件");return;}
        String message="将尝试删除 "+selected.size()+" 个文件（普通模式 "+(selected.size()-rootCount)+" 个，Root "+rootCount+" 个），约 "+fmt(bytes)+"。此操作不可撤销。";
        new AlertDialog.Builder(this).setTitle("再次确认删除").setMessage(message)
          .setNegativeButton("取消",null).setPositiveButton("确认删除", (d,w)->deleteSelected(selected)).show();
    }
    private void deleteSelected(ArrayList<Integer> selected){
        new Thread(()->{
            int ok=0,failed=0;
            for(int idx:selected){
                Item item=candidates.get(idx);
                try{
                    boolean deleted;
                    if(item.rootFile)deleted=rootDelete(item.path);
                    else deleted=DocumentsContract.deleteDocument(getContentResolver(),item.uri);
                    if(deleted)ok++;else failed++;
                }catch(Exception e){failed++;}
            }
            final int success=ok,errors=failed;
            runOnUiThread(()->{toast("删除完成：成功 "+success+" 个，失败 "+errors+" 个");if(treeUri!=null)scanNormal();else scanRoot();});
        }).start();
    }
    private boolean rootDelete(String path)throws Exception{
        if(!isAllowedRootCache(path))return false;
        String quoted="'"+path.replace("'","'\\''")+"'";
        Process p=new ProcessBuilder("su","-c","[ -f "+quoted+" ] && rm -f -- "+quoted+" && [ ! -e "+quoted+" ]").redirectErrorStream(true).start();
        byte[] out=new byte[1024]; while(p.getInputStream().read(out)!=-1){}
        return p.waitFor()==0;
    }
    private void openSystemCleaner(){
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)startActivity(new Intent("android.os.storage.action.CLEAR_APP_CACHE"));
            else startActivity(new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        }catch(Exception e){try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}catch(Exception ignored){toast("当前系统不支持该入口");}}
    }
    private void refreshStorage(){
        try{StatFs s=new StatFs(Environment.getDataDirectory().getPath());long all=s.getTotalBytes(),free=s.getAvailableBytes();storage.setText("内部存储：已用 "+fmt(all-free)+" / "+fmt(all)+" · 可用 "+fmt(free));}catch(Exception ignored){}
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff246b78));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(p);return b;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private String fmt(long n){if(n<1024)return n+" B";String[] u={"KB","MB","GB","TB"};double v=n;int i=-1;do{v/=1024;i++;}while(v>=1024&&i<u.length-1);return new DecimalFormat("0.##").format(v)+" "+u[i];}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
