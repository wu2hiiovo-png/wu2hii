package com.wu2hii.cleanx;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import androidx.documentfile.provider.DocumentFile;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.hardware.camera2.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.math.*;
import java.text.DecimalFormat;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
import org.json.*;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class ToolActivity extends Activity implements SensorEventListener {
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    LinearLayout root; String tool; Handler handler=new Handler(Looper.getMainLooper());
    CameraManager cameraManager; String cameraId; boolean torchOn=false;
    SensorManager sensorManager; Sensor accelerometer,magnetometer;
    float[] gravity,geomagnetic; TextView compassValue; boolean compassRegistered=false;
    Runnable clockTask; long startedAt,elapsed; boolean running=false; TextView watchText; Button watchToggle;
    TextView calcExpression,calcResult,calcHistory; String expression=""; boolean justEvaluated=false;
    final ExecutorService network=Executors.newSingleThreadExecutor(); Calendar historyDate=Calendar.getInstance(); LinearLayout historyList; TextView historyStatus; int[][] board2048=new int[4][4]; int score2048=0; LinearLayout boardView; TextView scoreView; final Random gameRandom=new Random(); Handler gameHandler=new Handler(Looper.getMainLooper()); Runnable reactionTask; boolean reactionWaiting=false,reactionReady=false; long reactionStart=0;
    LinearLayout fileContainer; DocumentFile currentDir; final ArrayList<DocumentFile> folderStack=new ArrayList<>(); ImageView qrImage;
    DecimalFormat decimal=new DecimalFormat("0.##########");
    byte[] pendingImageBytes; String pendingImageName="toolbox-image.jpg";
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff0b1428);getWindow().setNavigationBarColor(0xff0b1428);
        tool=getIntent().getStringExtra("tool");ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(28));
        root.setBackgroundColor(Color.TRANSPARENT);
        Button back=new Button(this);back.setText("‹  返回工具箱");back.setAllCaps(false);back.setTextColor(Color.WHITE);back.setBackground(tint(0x335b9bc0,18));root.addView(back);back.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);finish();overridePendingTransition(0,0);});
        scroll.addView(root);setContentView(scroll);ReiWallpaper.apply(this,scroll);showTool();animatePage();
    }
    void animatePage(){
        for(int i=0;i<root.getChildCount();i++){
            View child=root.getChildAt(i);
            child.setAlpha(0f);
            child.animate().alpha(1f).setStartDelay(i*16L)
                .setDuration(160).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
    }
    void animateAdded(View view,long delay){
        view.setAlpha(0f);
        view.animate().alpha(1f).setStartDelay(delay).setDuration(160)
            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }
    GradientDrawable tint(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0x447bbbd6);return d;}
    void heading(String name,String desc){TextView title=text(name,27);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,dp(22),0,dp(7));root.addView(title);TextView info=text(desc,13);info.setTextColor(0xffa9bfd9);info.setPadding(0,0,0,dp(18));root.addView(info);}
    void showTool(){
        switch(tool==null?"":tool){
            case "device":heading("设备信息","设备基本信息与存储概览");deviceInfo();break;
            case "clean":heading("CleanX 文件清理","所有候选文件默认不勾选，必须由你逐项选择并二次确认。");Button clean=button("打开 CleanX 扫描",true);root.addView(clean);clean.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));break;
            case "file":heading("文件管理","只浏览你通过系统选择器授权的目录；删除操作会再次确认。");fileManager();break;
            case "qr":heading("二维码生成器","输入网址、文字或联系方式，生成可扫描的二维码。");qrGenerator();break;
            case "history":heading("历史上的今天","按日期查看历史事件、人物诞辰与纪念日。");historyToday();break;
            case "games":heading("小游戏","离线小游戏，不需要登录，也不上传游戏数据。");miniGames();break;
            case "calc":heading("计算器","支持括号、百分比、连续运算和科学函数。");calculator();break;
            case "timer":heading("秒表与计时器","正计时与倒计时，适合日常计时。");stopwatch();countdown();break;
            case "convert":heading("单位换算","长度、质量、温度、面积、体积与速度换算。");converter();break;
            case "text":heading("文本工具","快速统计字数，转换大小写与清理空白。");textTools();break;
            case "compass":heading("指南针","使用设备加速度计与磁力传感器计算方位。");compass();break;
            case "flash":heading("手电筒","点击开启时才申请相机权限；离开页面自动关闭。");flashlight();break;
            case "base64":heading("Base64 编解码","在本机完成编码与解码，支持 UTF-8 中文文本。");base64Tool();break;
            case "hash":heading("文本哈希","计算 SHA-256 摘要；相同输入会得到相同结果。");hashTool();break;
            case "timestamp":heading("时间戳转换","Unix 时间戳按 UTC 秒数定义，结果按设备当前时区显示。");timestampTool();break;
            case "password":heading("密码生成器","使用 SecureRandom 在本机生成随机密码。");passwordTool();break;
            case "url":heading("URL 编解码","对网址参数进行 UTF-8 百分号编码与解码。");urlTool();break;
            case "json":heading("JSON 格式化","格式化、压缩并校验 JSON。");jsonTool();break;
            case "radix":heading("进制转换","文本与十六进制、二进制互转。");radixTool();break;
            case "morse":heading("摩斯电码","支持英文字母和数字。");morseTool();break;
            case "image":heading("图片工具","本地压缩或缩放图片，另存新文件，不覆盖原图。");imageTool();break;
            case "filehash":heading("文件哈希","通过系统文件选择器计算文件 SHA-256。");fileHashTool();break;
            case "wallpaper":heading("自定义壁纸","从手机相册或文件中选择图片，设置为工具箱背景。图片仅保存在本机，不会上传。");wallpaperTool();break;
            case "settings":heading("外观设置","调整工具箱图标与文字的颜色、透明度，也可以更换背景壁纸。");appearanceSettings();break;
        }
    }
    Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackground(tint(primary?0x6659a9cb:0x334a739a,16));b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){v.animate().scaleX(.95f).scaleY(.95f).alpha(.86f).setDuration(75).start();}else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){if(e.getAction()==MotionEvent.ACTION_UP&&v.isPressed())v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start();}return false;});return b;}

    void appearanceSettings(){
        android.content.SharedPreferences prefs=getSharedPreferences("ui_settings",MODE_PRIVATE);
        int iconColor=prefs.getInt("icon_color",0xff9de6e7),textColor=prefs.getInt("text_color",0xffffffff);
        int iconAlpha=prefs.getInt("icon_alpha",255),textAlpha=prefs.getInt("text_alpha",255);
        TextView iconTitle=text("图标透明度："+Math.round(iconAlpha*100f/255)+"%",14);root.addView(iconTitle);
        SeekBar iconSeek=new SeekBar(this);iconSeek.setMax(255);iconSeek.setProgress(iconAlpha);root.addView(iconSeek);
        iconSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){iconTitle.setText("图标透明度："+Math.round(p*100f/255)+"%");prefs.edit().putInt("icon_alpha",p).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        TextView textTitle=text("文字透明度："+Math.round(textAlpha*100f/255)+"%",14);textTitle.setPadding(0,dp(18),0,0);root.addView(textTitle);
        SeekBar textSeek=new SeekBar(this);textSeek.setMax(255);textSeek.setProgress(textAlpha);root.addView(textSeek);
        textSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){textTitle.setText("文字透明度："+Math.round(p*100f/255)+"%");prefs.edit().putInt("text_alpha",p).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        TextView ic=text("图标颜色",15);ic.setPadding(0,dp(20),0,dp(8));root.addView(ic);
        addColorChoices(prefs,"icon_color",new int[]{0xff9de6e7,0xffffffff,0xffffd166,0xffff8fab,0xffb8a1ff,0xff7dd3fc,0xff8ee3a1});
        TextView tx=text("文字颜色",15);tx.setPadding(0,dp(20),0,dp(8));root.addView(tx);
        addColorChoices(prefs,"text_color",new int[]{0xffffffff,0xffd9e7f5,0xffffd166,0xffffb4c6,0xffb8a1ff,0xff7dd3fc,0xffa6e3a1});
        Button wallpaper=button("更换背景壁纸",true);wallpaper.setOnClickListener(v->{Intent i=new Intent(this,ToolActivity.class);i.putExtra("tool","wallpaper");startActivity(i);});root.addView(wallpaper);
        Button reset=button("恢复默认外观",false);root.addView(reset);reset.setOnClickListener(v->{prefs.edit().clear().apply();Toast.makeText(this,"外观设置已恢复默认",Toast.LENGTH_SHORT).show();recreate();});
        TextView note=text("透明度和颜色会保存在本机。返回首页后自动应用。",12);note.setTextColor(0xffa9bfd9);note.setPadding(0,dp(14),0,0);root.addView(note);
    }
    void addColorChoices(android.content.SharedPreferences prefs,String key,int[] colors){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(0);
        for(int color:colors){View swatch=new View(this);GradientDrawable bg=new GradientDrawable();bg.setShape(GradientDrawable.OVAL);bg.setColor(color);bg.setStroke(dp(1),0x99ffffff);swatch.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(34));lp.setMargins(0,0,dp(10),0);row.addView(swatch,lp);swatch.setOnClickListener(v->{prefs.edit().putInt(key,color).apply();Toast.makeText(this,"颜色已更新，返回首页查看",Toast.LENGTH_SHORT).show();});}
        root.addView(row);
    }

    void wallpaperTool(){
        TextView current=text(ReiWallpaper.selectedUri(this)==null?"当前：默认背景":"当前：已使用自定义壁纸",14);
        current.setTextColor(0xffa9bfd9);current.setPadding(0,dp(8),0,dp(16));root.addView(current);
        Button choose=button("从手机选择壁纸",true);root.addView(choose);
        choose.setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,77);
        });
        Button reset=button("恢复默认背景",false);root.addView(reset);
        reset.setOnClickListener(v->{ReiWallpaper.setSelectedUri(this,null);Toast.makeText(this,"已恢复默认背景",Toast.LENGTH_SHORT).show();recreate();});
        TextView note=text("支持常见 JPG、PNG、WebP 图片。选择后会自动记住授权；背景固定显示，页面内容独立滚动。建议使用竖版图片。",12);
        note.setTextColor(0xffa9bfd9);note.setPadding(0,dp(16),0,0);root.addView(note);
    }

    void deviceInfo(){
        android.os.StatFs fs=new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());long total=fs.getTotalBytes(),free=fs.getAvailableBytes();
        android.content.IntentFilter f=new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);Intent battery=registerReceiver(null,f);int level=battery==null?-1:battery.getIntExtra("level",-1),scale=battery==null?100:battery.getIntExtra("scale",100);
        int pct=level<0?-1:Math.round(level*100f/scale);
        String[] rows={"型号  "+Build.MODEL,"制造商  "+Build.MANUFACTURER,"Android  "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT,"设备代号  "+Build.DEVICE,"电池电量  "+(pct<0?"未知":pct+"%"),"内部存储  "+formatBytes(total-free)+" 已用 / "+formatBytes(total)+" 总量","可用空间  "+formatBytes(free)};
        for(String s:rows){TextView t=text(s,15);t.setPadding(dp(14),dp(13),dp(10),dp(13));t.setBackground(tint(0x244c82ad,15));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);root.addView(t,lp);}
    }
    String formatBytes(long n){if(n<1024)return n+" B";int exp=(int)(Math.log(n)/Math.log(1024));String[] u={"B","KB","MB","GB","TB"};return String.format(Locale.US,"%.1f %s",n/Math.pow(1024,exp),u[Math.min(exp,u.length-1)]);}
    void calculator(){
        calcExpression=text("0",23);calcExpression.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);calcExpression.setPadding(dp(12),dp(16),dp(12),dp(6));root.addView(calcExpression,new LinearLayout.LayoutParams(-1,dp(58)));
        calcResult=text("0",38);calcResult.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);calcResult.setTypeface(null,Typeface.BOLD);calcResult.setPadding(dp(12),0,dp(12),dp(10));root.addView(calcResult,new LinearLayout.LayoutParams(-1,dp(66)));
        calcHistory=text("",12);calcHistory.setTextColor(0xff8eabc8);calcHistory.setGravity(Gravity.RIGHT);root.addView(calcHistory,new LinearLayout.LayoutParams(-1,dp(28)));
        String[][] keys={{"sin","cos","tan","√"},{"ln","log","e","^"},{"(",")","%","÷"},{"7","8","9","×"},{"4","5","6","−"},{"1","2","3","+"},{"±","0",".","="},{"AC","DEL","π","x²"}};
        for(String[] row:keys){LinearLayout line=new LinearLayout(this);line.setOrientation(0);LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,dp(58));rowLp.bottomMargin=dp(7);for(String key:row){Button b=button(key,key.equals("="));b.setTextSize(key.length()>2?13:19);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(3),0,dp(3),0);line.addView(b,p);b.setOnClickListener(v->calcPress(key));}root.addView(line,rowLp);}
        TextView tip=text("科学函数默认使用角度制。点击结果可继续运算。",11);tip.setTextColor(0xff819bb8);tip.setPadding(dp(4),dp(8),0,0);root.addView(tip);
    }
    void calcPress(String k){
        if(k.equals("AC")){expression="";justEvaluated=false;calcExpression.setText("0");calcResult.setText("0");return;}
        if(k.equals("DEL")){if(!expression.isEmpty())expression=expression.substring(0,expression.length()-1);justEvaluated=false;calcExpression.setText(expression.isEmpty()?"0":expression);return;}
        if(k.equals("=")){try{double value=new Parser(expression).parse();if(!Double.isFinite(value))throw new Exception();String result=decimal.format(value);calcHistory.setText(expression+" =");calcResult.setText(result);expression=result;justEvaluated=true;}catch(Exception e){calcResult.setText("表达式有误");}return;}
        if(k.equals("±")){if(expression.isEmpty())expression="-";else if(expression.startsWith("-"))expression=expression.substring(1);else expression="-("+expression+")";}
        else if(k.equals("x²")){expression+="^2";}
        else if(k.equals("√")){expression+="sqrt(";}
        else if(k.equals("sin")||k.equals("cos")||k.equals("tan")||k.equals("ln")||k.equals("log")){expression+=k+"(";}
        else if(k.equals("π")){expression+="pi";}
        else {if(justEvaluated&&("0123456789.(".contains(k)||k.equals("π")))expression="";expression+=k;}
        justEvaluated=false;calcExpression.setText(expression.isEmpty()?"0":expression);
    }
    static class Parser{
        final String s;int p=0;Parser(String s){this.s=s.replace("×","*").replace("÷","/").replace("−","-").replace("π","pi");}
        double parse(){double v=add();skip();if(p!=s.length())throw new IllegalArgumentException();return v;}
        void skip(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
        boolean eat(char c){skip();if(p<s.length()&&s.charAt(p)==c){p++;return true;}return false;}
        double add(){double v=mul();while(true){if(eat('+'))v+=mul();else if(eat('-'))v-=mul();else return v;}}
        double mul(){double v=power();while(true){if(eat('*'))v*=power();else if(eat('/')){double d=power();if(d==0)throw new ArithmeticException();v/=d;}else return v;}}
        double power(){double v=unary();if(eat('^'))v=Math.pow(v,power());return v;}
        double unary(){if(eat('+'))return unary();if(eat('-'))return -unary();return postfix();}
        double postfix(){double v=atom();while(eat('%'))v/=100.0;return v;}
        double atom(){skip();if(eat('(')){double v=add();if(!eat(')'))throw new IllegalArgumentException();return v;}int start=p;while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'))p++;if(p>start)return Double.parseDouble(s.substring(start,p));while(p<s.length()&&Character.isLetter(s.charAt(p)))p++;if(p==start)throw new IllegalArgumentException();String id=s.substring(start,p).toLowerCase(Locale.ROOT);if(id.equals("pi"))return Math.PI;if(id.equals("e"))return Math.E;if(!eat('('))throw new IllegalArgumentException();double x=add();if(!eat(')'))throw new IllegalArgumentException();switch(id){case "sin":return Math.sin(Math.toRadians(x));case "cos":return Math.cos(Math.toRadians(x));case "tan":return Math.tan(Math.toRadians(x));case "sqrt":return Math.sqrt(x);case "log":return Math.log10(x);case "ln":return Math.log(x);case "abs":return Math.abs(x);default:throw new IllegalArgumentException();}}
    }
    void stopwatch(){
        TextView label=text("秒表",14);label.setTextColor(0xff91cbdc);root.addView(label);
        watchText=text("00:00.0",39);watchText.setGravity(Gravity.CENTER);watchText.setPadding(0,dp(12),0,dp(12));root.addView(watchText);
        LinearLayout controls=new LinearLayout(this);watchToggle=button("开始",true);Button reset=button("重置",false);controls.addView(watchToggle,new LinearLayout.LayoutParams(0,dp(52),1));controls.addView(reset,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(controls);
        clockTask=()->{if(running){long ms=elapsed+SystemClock.elapsedRealtime()-startedAt;watchText.setText(String.format(Locale.US,"%02d:%02d.%d",ms/60000,(ms/1000)%60,(ms/100)%10));handler.postDelayed(clockTask,80);}};
        watchToggle.setOnClickListener(v->{if(running){elapsed+=SystemClock.elapsedRealtime()-startedAt;running=false;watchToggle.setText("继续");}else{startedAt=SystemClock.elapsedRealtime();running=true;watchToggle.setText("暂停");handler.post(clockTask);}});
        reset.setOnClickListener(v->{running=false;elapsed=0;watchText.setText("00:00.0");watchToggle.setText("开始");handler.removeCallbacks(clockTask);});
    }
    void countdown(){
        TextView label=text("倒计时（分钟）",14);label.setTextColor(0xff91cbdc);label.setPadding(0,dp(24),0,dp(8));root.addView(label);
        EditText mins=input("例如 5");root.addView(mins);TextView value=text("尚未开始",22);value.setPadding(0,dp(8),0,dp(8));root.addView(value);
        Button start=button("开始倒计时",true);root.addView(start);final Runnable[] task={null};final long[] end={0};
        start.setOnClickListener(v->{if(task[0]!=null)handler.removeCallbacks(task[0]);try{long m=Long.parseLong(mins.getText().toString());if(m<1||m>1440)throw new Exception();end[0]=SystemClock.elapsedRealtime()+m*60000;start.setText("重新开始");task[0]=new Runnable(){public void run(){long left=end[0]-SystemClock.elapsedRealtime();if(left<=0){value.setText("时间到！");start.setText("开始倒计时");task[0]=null;return;}value.setText(String.format(Locale.US,"%02d:%02d",left/60000,(left/1000)%60));handler.postDelayed(this,250);}};handler.post(task[0]);}catch(Exception e){value.setText("请输入 1–1440 之间的分钟数。");}});
    }
    EditText input(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setHintTextColor(0xff91a9c4);e.setTextColor(Color.WHITE);e.setBackground(tint(0x224c82ad,12));e.setPadding(dp(12),dp(8),dp(12),dp(8));return e;}
    void converter(){
        String[] types={"长度","质量","温度","面积","体积","速度"};Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types));root.addView(type);
        EditText amount=input("输入数值");root.addView(amount);Spinner from=new Spinner(this),to=new Spinner(this);root.addView(from);root.addView(to);TextView result=text("换算结果",25);result.setPadding(0,dp(18),0,dp(10));root.addView(result);
        String[][] units={{"米","千米","厘米","毫米","英尺","英寸"},{"千克","克","毫克","磅"},{"摄氏度","华氏度","开尔文"},{"平方米","平方千米","公顷","平方英尺"},{"升","毫升","立方米","美制加仑"},{"米/秒","千米/小时","英里/小时","节"}};
        Runnable refresh=()->{int ix=type.getSelectedItemPosition();ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,units[ix]);from.setAdapter(ad);to.setAdapter(ad);if(units[ix].length>1)to.setSelection(1);};
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        Button go=button("换算",true);root.addView(go);go.setOnClickListener(v->{try{double x=Double.parseDouble(amount.getText().toString());int ix=type.getSelectedItemPosition();String[] u=units[ix];double base=toBase(x,ix,from.getSelectedItemPosition()),out=fromBase(base,ix,to.getSelectedItemPosition());result.setText(decimal.format(out)+" "+u[to.getSelectedItemPosition()]);}catch(Exception e){result.setText("请检查输入数值。");}});
    }
    double toBase(double x,int t,int u){switch(t){case 0:return x*new double[]{1,1000,.01,.001,.3048,.0254}[u];case 1:return x*new double[]{1,.001,.000001,.45359237}[u];case 2:return u==0?x:u==1?(x-32)*5/9+0:(x-273.15);case 3:return x*new double[]{1,1e6,10000,.09290304}[u];case 4:return x*new double[]{1,.001,1000,3.785411784}[u];default:return x*new double[]{1,1/3.6,0.44704,0.514444}[u];}}
    double fromBase(double x,int t,int u){switch(t){case 0:return x/new double[]{1,1000,.01,.001,.3048,.0254}[u];case 1:return x/new double[]{1,.001,.000001,.45359237}[u];case 2:return u==0?x:u==1?x*9/5+32:x+273.15;case 3:return x/new double[]{1,1e6,10000,.09290304}[u];case 4:return x/new double[]{1,.001,1000,3.785411784}[u];default:return x/new double[]{1,1/3.6,0.44704,0.514444}[u];}}
    void textTools(){
        EditText src=new EditText(this);src.setHint("在这里输入或粘贴文本…");src.setHintTextColor(0xff91a9c4);src.setTextColor(Color.WHITE);src.setGravity(Gravity.TOP);src.setMinLines(5);src.setPadding(dp(12),dp(12),dp(12),dp(12));src.setBackground(tint(0x224c82ad,14));root.addView(src);
        TextView count=text("字符：0   不含空格：0   单词：0",13);count.setPadding(0,dp(10),0,dp(12));root.addView(count);
        src.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){String v=s.toString();count.setText("字符："+v.length()+"   不含空格："+v.replaceAll("\\s","").length()+"   单词："+(v.trim().isEmpty()?0:v.trim().split("\\s+").length));}public void afterTextChanged(Editable e){}});
        String[] actions={"全部大写","全部小写","首字母大写","清除多余空白","复制文本"};for(String action:actions){Button b=button(action,false);root.addView(b);b.setOnClickListener(v->{String s=src.getText().toString();switch(action){case "全部大写":s=s.toUpperCase(Locale.ROOT);break;case "全部小写":s=s.toLowerCase(Locale.ROOT);break;case "首字母大写":s=s.isEmpty()?s:s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1);break;case "清除多余空白":s=s.trim().replaceAll("\\s+"," ");break;case "复制文本":((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("text",s));Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();return;}src.setText(s);});}
    }

    void fileManager(){
        Button choose=button("选择或更换文件夹",true);root.addView(choose);
        Button create=button("在当前目录新建文件夹",false);root.addView(create);
        TextView path=text("尚未选择目录",12);path.setTextColor(0xff9db7d2);path.setPadding(0,dp(8),0,dp(8));root.addView(path);
        Button up=button("返回上一级",false);root.addView(up);
        fileContainer=new LinearLayout(this);fileContainer.setOrientation(1);root.addView(fileContainer);
        choose.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,73);});
        create.setOnClickListener(v->{if(currentDir==null||!currentDir.canWrite()){Toast.makeText(this,"请先选择一个可写目录",Toast.LENGTH_SHORT).show();return;}EditText name=input("文件夹名称");new AlertDialog.Builder(this).setTitle("新建文件夹").setView(name).setNegativeButton("取消",null).setPositiveButton("创建",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"名称不能为空",Toast.LENGTH_SHORT).show();return;}if(currentDir.createDirectory(n)==null)Toast.makeText(this,"创建失败",Toast.LENGTH_SHORT).show();refreshFiles(path,up);}).show();});
        up.setOnClickListener(v->{if(folderStack.size()>1){folderStack.remove(folderStack.size()-1);currentDir=folderStack.get(folderStack.size()-1);refreshFiles(path,up);}else Toast.makeText(this,"已经是授权目录的顶层",Toast.LENGTH_SHORT).show();});
        String saved=getSharedPreferences("toolbox",MODE_PRIVATE).getString("tree_uri",null);
        if(saved!=null){try{Uri uri=Uri.parse(saved);currentDir=DocumentFile.fromTreeUri(this,uri);if(currentDir!=null){folderStack.clear();folderStack.add(currentDir);refreshFiles(path,up);}}catch(Exception ignored){}}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==77&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();int flags=data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION;
            try{getContentResolver().takePersistableUriPermission(uri,flags);ReiWallpaper.setSelectedUri(this,uri);Toast.makeText(this,"壁纸已设置",Toast.LENGTH_SHORT).show();recreate();}
            catch(Exception e){Toast.makeText(this,"无法保存图片授权，请重新选择",Toast.LENGTH_SHORT).show();}
        }else         if(requestCode==75&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try(InputStream in=getContentResolver().openInputStream(data.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()>30*1024*1024)throw new Exception();out.write(buf,0,n);}pendingImageBytes=out.toByteArray();Toast.makeText(this,"图片已载入",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"读取失败或图片超过 30 MB",Toast.LENGTH_SHORT).show();}
        }else if(requestCode==76&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new Exception();out.write(pendingImageBytes);Toast.makeText(this,"图片已保存",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show();}
        }else if(requestCode==74&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try(InputStream in=getContentResolver().openInputStream(data.getData())){MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)md.update(buf,0,n);StringBuilder hex=new StringBuilder();for(byte b:md.digest())hex.append(String.format(Locale.US,"%02x",b&255));new AlertDialog.Builder(this).setTitle("文件 SHA-256").setMessage(hex.toString()).setPositiveButton("复制",(a,b)->copyText(hex.toString())).setNegativeButton("关闭",null).show();}catch(Exception e){Toast.makeText(this,"文件读取或哈希失败",Toast.LENGTH_SHORT).show();}
        }else if(requestCode==73&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try{getContentResolver().takePersistableUriPermission(uri,flags);}catch(Exception ignored){}
            getSharedPreferences("toolbox",MODE_PRIVATE).edit().putString("tree_uri",uri.toString()).apply();
            currentDir=DocumentFile.fromTreeUri(this,uri);folderStack.clear();if(currentDir!=null)folderStack.add(currentDir);
            if(fileContainer!=null){View path=root.getChildAt(root.getChildCount()-3);View up=root.getChildAt(root.getChildCount()-2);if(path instanceof TextView&&up instanceof Button)refreshFiles((TextView)path,(Button)up);}
        }
    }
    void refreshFiles(TextView path,Button up){
        if(fileContainer==null)return;fileContainer.removeAllViews();
        path.setText(currentDir==null?"尚未选择目录":currentDir.getName());
        up.setEnabled(folderStack.size()>1);
        if(currentDir==null)return;
        DocumentFile[] files=currentDir.listFiles();
        Arrays.sort(files,(a,b)->{if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName()));});
        if(files.length==0){TextView empty=text("这个目录是空的。",14);empty.setPadding(0,dp(18),0,0);fileContainer.addView(empty);return;}
        for(DocumentFile file:files){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(9),dp(8),dp(9));row.setBackground(tint(0x224c82ad,12));
            LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,-2);rowLp.bottomMargin=dp(6);fileContainer.addView(row,rowLp);animateAdded(row,fileContainer.getChildCount()*16L);
            TextView label=text((file.isDirectory()?"📁  ":"📄  ")+file.getName()+(file.isDirectory()?"":"\n"+formatBytes(file.length())),13);label.setMaxLines(2);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            Button open=button(file.isDirectory()?"进入":"打开",false);row.addView(open);
            open.setOnClickListener(v->{if(file.isDirectory()){currentDir=file;folderStack.add(file);refreshFiles(path,up);}else{try{String mime=getContentResolver().getType(file.getUri());Intent view=new Intent(Intent.ACTION_VIEW);view.setDataAndType(file.getUri(),mime==null?"*/*":mime);view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(view,"打开文件"));}catch(Exception e){Toast.makeText(this,"没有可打开此文件的应用",Toast.LENGTH_SHORT).show();}}});
            if(file.isFile()&&file.canWrite()){Button del=button("删除",false);row.addView(del);del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("删除文件？").setMessage("将删除："+file.getName()).setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{if(!file.delete())Toast.makeText(this,"删除失败或权限不足",Toast.LENGTH_SHORT).show();refreshFiles(path,up);}).show());}
        }
    }
    void qrGenerator(){
        EditText value=input("输入网址或文字内容");root.addView(value);
        Button make=button("生成二维码",true);root.addView(make);
        qrImage=new ImageView(this);qrImage.setAdjustViewBounds(true);qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);qrImage.setPadding(dp(12),dp(12),dp(12),dp(12));qrImage.setBackground(tint(Color.WHITE,18));LinearLayout.LayoutParams imageLp=new LinearLayout.LayoutParams(-1,dp(270));imageLp.setMargins(dp(8),dp(16),dp(8),dp(12));root.addView(qrImage,imageLp);
        TextView note=text("二维码会在本机生成，不会上传输入内容。",12);note.setTextColor(0xff9db7d2);root.addView(note);
        Button copy=button("复制原始内容",false);root.addView(copy);copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("QR content",value.getText().toString()));Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();});
        make.setOnClickListener(v->{String content=value.getText().toString().trim();if(content.isEmpty()){Toast.makeText(this,"先输入需要生成二维码的内容",Toast.LENGTH_SHORT).show();return;}try{Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);hints.put(EncodeHintType.MARGIN,1);BitMatrix matrix=new QRCodeWriter().encode(content,BarcodeFormat.QR_CODE,720,720,hints);Bitmap bitmap=Bitmap.createBitmap(720,720,Bitmap.Config.ARGB_8888);for(int y=0;y<720;y++)for(int x=0;x<720;x++)bitmap.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);qrImage.setImageBitmap(bitmap);}catch(Exception e){Toast.makeText(this,"内容太长或无法生成二维码，请缩短内容。",Toast.LENGTH_LONG).show();}});
    }


    void historyToday(){
        LinearLayout controls=new LinearLayout(this);Button pick=button("选择日期",false);Button today=button("回到今天",false);controls.addView(pick,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(today,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(controls);
        historyStatus=text("正在读取历史事件…",13);historyStatus.setTextColor(0xffa9bfd9);historyStatus.setPadding(0,dp(12),0,dp(10));root.addView(historyStatus);
        historyList=new LinearLayout(this);historyList.setOrientation(1);root.addView(historyList);
        TextView source=text("资料来源：中文维基百科 On This Day · 内容由维基百科社群整理，部分条目可能不完整。",11);source.setTextColor(0xff819bb8);source.setPadding(0,dp(16),0,0);root.addView(source);
        pick.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{historyDate.set(y,m,d);loadHistory();},historyDate.get(Calendar.YEAR),historyDate.get(Calendar.MONTH),historyDate.get(Calendar.DAY_OF_MONTH)).show());
        today.setOnClickListener(v->{historyDate=Calendar.getInstance();loadHistory();});loadHistory();
    }
    void loadHistory(){
        if(historyList==null)return;historyList.removeAllViews();
        String date=new SimpleDateFormat("yyyy年M月d日 EEEE",Locale.CHINA).format(historyDate.getTime());historyStatus.setText(date+" · 正在加载…");
        final String month=String.format(Locale.US,"%02d",historyDate.get(Calendar.MONTH)+1),day=String.format(Locale.US,"%02d",historyDate.get(Calendar.DAY_OF_MONTH));
        network.execute(()->{HttpURLConnection c=null;try{
            URL url=new URL("https://zh.wikipedia.org/api/rest_v1/feed/onthisday/all/"+month+"/"+day);
            c=(HttpURLConnection)url.openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","wu2hii-Toolbox/3.3 (Android)");c.setRequestProperty("Accept","application/json");
            int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
            JSONObject json=new JSONObject(out.toString("UTF-8"));ArrayList<JSONObject> items=new ArrayList<>();HashSet<String> seen=new HashSet<>();
            for(String bucket:new String[]{"selected","events","births","deaths","holidays"}){JSONArray arr=json.optJSONArray(bucket);if(arr==null)continue;for(int i=0;i<arr.length();i++){JSONObject item=arr.optJSONObject(i);if(item==null)continue;String key=item.optString("year")+"|"+item.optString("text");if(seen.add(key))items.add(item);}}
            runOnUiThread(()->renderHistory(date,items));
        }catch(Exception e){runOnUiThread(()->{if(historyStatus!=null)historyStatus.setText(date+" · 暂时无法获取数据，请检查网络后重试。");});}finally{if(c!=null)c.disconnect();}});
    }
    void renderHistory(String date,ArrayList<JSONObject> items){
        if(isFinishing()||historyList==null)return;historyList.removeAllViews();historyStatus.setText(date+" · 找到 "+items.size()+" 条记录");
        if(items.isEmpty()){historyList.addView(text("这一天暂时没有收录的条目。",14));return;}
        int shown=0;for(JSONObject item:items){if(shown++>=45)break;int year=item.optInt("year",0);String desc=item.optString("text","");JSONArray pages=item.optJSONArray("pages");String url="";
            if(pages!=null&&pages.length()>0){JSONObject page=pages.optJSONObject(0);if(page!=null){String title=page.optString("title","");if(!title.isEmpty())try{url="https://zh.wikipedia.org/wiki/"+URLEncoder.encode(title,"UTF-8").replace("+","%20");}catch(Exception ignored){}}}
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(tint(0x244c82ad,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);historyList.addView(card,lp);animateAdded(card,shown*18L);
            TextView y=text(year==0?"历史记录":year+" 年",12);y.setTextColor(0xff91d7e5);y.setTypeface(null,Typeface.BOLD);card.addView(y);
            TextView body=text(desc,14);body.setPadding(0,dp(5),0,0);card.addView(body);
            if(!url.isEmpty()){TextView link=text("查看相关词条  ›",11);link.setTextColor(0xff8bc9f0);link.setPadding(0,dp(8),0,0);card.addView(link);final String target=url;card.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(target)));}catch(Exception ignored){}});}
        }
    }
    void miniGames(){TextView intro=text("选一个小游戏，随时玩一局。",13);intro.setTextColor(0xffa9bfd9);root.addView(intro);Button g=button("2048 · 数字合并",true);root.addView(g);g.setOnClickListener(v->game2048());Button r=button("反应速度测试",false);root.addView(r);r.setOnClickListener(v->reactionGame());game2048();}
    void game2048(){
        root.removeViews(6,Math.max(0,root.getChildCount()-6));heading("2048","滑动棋盘或使用方向按钮，把相同数字合并到 2048。");
        scoreView=text("分数：0",17);root.addView(scoreView);boardView=new LinearLayout(this);boardView.setOrientation(1);boardView.setPadding(dp(6),dp(6),dp(6),dp(6));boardView.setBackground(tint(0x554d7192,16));root.addView(boardView);
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(1);for(String[] row:new String[][]{{"↑"},{"←","↓","→"}}){LinearLayout line=new LinearLayout(this);for(String s:row){Button b=button(s,false);line.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));b.setOnClickListener(v->move2048(s.equals("↑")?0:s.equals("↓")?1:s.equals("←")?2:3));}controls.addView(line);}root.addView(controls);
        Button reset=button("重新开始",false);root.addView(reset);reset.setOnClickListener(v->new2048());
        boardView.setOnTouchListener(new View.OnTouchListener(){float x,y;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-x,dy=e.getY()-y;if(Math.max(Math.abs(dx),Math.abs(dy))>dp(24)){if(Math.abs(dx)>Math.abs(dy))move2048(dx>0?3:2);else move2048(dy>0?1:0);}return true;}return true;}});new2048();
    }
    void new2048(){board2048=new int[4][4];score2048=0;spawn2048();spawn2048();draw2048();}
    void spawn2048(){ArrayList<Integer> empty=new ArrayList<>();for(int i=0;i<4;i++)for(int j=0;j<4;j++)if(board2048[i][j]==0)empty.add(i*4+j);if(empty.isEmpty())return;int p=empty.get(gameRandom.nextInt(empty.size()));board2048[p/4][p%4]=gameRandom.nextInt(10)==0?4:2;}
    void move2048(int dir){boolean changed=false;for(int line=0;line<4;line++){int[] a=new int[4];for(int i=0;i<4;i++){int r,c;if(dir<2){r=dir==0?i:3-i;c=line;}else{r=line;c=dir==2?i:3-i;}a[i]=board2048[r][c];}
        int[] b=new int[4];int n=0;for(int x:a)if(x!=0)b[n++]=x;for(int i=0;i<3;i++)if(b[i]!=0&&b[i]==b[i+1]){b[i]*=2;score2048+=b[i];for(int j=i+1;j<3;j++)b[j]=b[j+1];b[3]=0;}
        for(int i=0;i<4;i++){int r,c;if(dir<2){r=dir==0?i:3-i;c=line;}else{r=line;c=dir==2?i:3-i;}if(board2048[r][c]!=b[i])changed=true;board2048[r][c]=b[i];}}
        if(changed){spawn2048();draw2048();if(!canMove2048())Toast.makeText(this,"没有可合并的方块了，重新开始试试。",Toast.LENGTH_LONG).show();}}
    boolean canMove2048(){for(int i=0;i<4;i++)for(int j=0;j<4;j++){if(board2048[i][j]==0)return true;if(i<3&&board2048[i][j]==board2048[i+1][j])return true;if(j<3&&board2048[i][j]==board2048[i][j+1])return true;}return false;}
    void draw2048(){if(boardView==null)return;scoreView.setText("分数："+score2048);boardView.removeAllViews();for(int i=0;i<4;i++){LinearLayout row=new LinearLayout(this);for(int j=0;j<4;j++){int val=board2048[i][j];TextView cell=text(val==0?"":String.valueOf(val),val>=1024?20:24);cell.setGravity(Gravity.CENTER);cell.setTypeface(null,Typeface.BOLD);cell.setBackground(tint(val==0?0x223e5874:val==2?0x5579a8bf:val==4?0x668ac5d2:val==8?0x7793d9d0:0x8898b9df,9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(68),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(cell,p);}boardView.addView(row);}}
    void reactionGame(){
        root.removeViews(6,Math.max(0,root.getChildCount()-6));heading("反应速度测试","点击开始，等待屏幕变绿后尽快点击。抢先点击会判定失败。");
        TextView display=text("准备好了吗？",25);display.setGravity(Gravity.CENTER);display.setPadding(0,dp(40),0,dp(40));display.setBackground(tint(0x334a739a,18));root.addView(display);
        Button start=button("开始测试",true);root.addView(start);final Random random=new Random();
        start.setOnClickListener(v->{if(reactionTask!=null)gameHandler.removeCallbacks(reactionTask);reactionWaiting=true;reactionReady=false;display.setText("等待绿色出现…");display.setBackground(tint(0xff9b6a42,18));start.setText("现在点击这里");reactionTask=()->{reactionReady=true;reactionWaiting=false;reactionStart=SystemClock.elapsedRealtime();display.setText("现在！点击！");display.setBackground(tint(0xff287c57,18));};gameHandler.postDelayed(reactionTask,1200+random.nextInt(2800));});
        display.setOnClickListener(v->{if(reactionWaiting){gameHandler.removeCallbacks(reactionTask);reactionWaiting=false;display.setText("太早了！再试一次。");display.setBackground(tint(0xff9b4141,18));start.setText("重新开始");}else if(reactionReady){long ms=SystemClock.elapsedRealtime()-reactionStart;reactionReady=false;display.setText("你的反应时间："+ms+" ms");display.setBackground(tint(0x334a739a,18));start.setText("再测一次");}else display.setText("先点击开始测试。");});
    }


    void base64Tool(){
        EditText input=input("输入需要编码或解码的文本");
        root.addView(input);
        EditText output=input("结果会显示在这里");output.setMinLines(3);root.addView(output);
        LinearLayout row=new LinearLayout(this);
        Button encode=button("编码 → Base64",true),decode=button("解码 → 文本",false);
        row.addView(encode,new LinearLayout.LayoutParams(0,dp(50),1));
        row.addView(decode,new LinearLayout.LayoutParams(0,dp(50),1));root.addView(row);
        encode.setOnClickListener(v->{try{String s=android.util.Base64.encodeToString(input.getText().toString().getBytes("UTF-8"),android.util.Base64.NO_WRAP);output.setText(s);}catch(Exception e){output.setText("编码失败");}});
        decode.setOnClickListener(v->{try{byte[] bytes=android.util.Base64.decode(input.getText().toString().trim(),android.util.Base64.DEFAULT);output.setText(new String(bytes,"UTF-8"));}catch(Exception e){output.setText("解码失败：请检查 Base64 内容是否完整。");}});
        Button copy=button("复制结果",false);root.addView(copy);copy.setOnClickListener(v->copyText(output.getText().toString()));
    }
    void hashTool(){
        EditText input=input("输入文本");root.addView(input);
        TextView result=text("SHA-256：",13);result.setTextIsSelectable(true);result.setPadding(dp(12),dp(16),dp(12),dp(16));result.setBackground(tint(0x244c82ad,14));root.addView(result);
        Button calc=button("计算 SHA-256",true);root.addView(calc);
        calc.setOnClickListener(v->{try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(input.getText().toString().getBytes("UTF-8"));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(Locale.US,"%02x",b&255));result.setText("SHA-256：\n"+hex);}catch(Exception e){result.setText("计算失败");}});
        Button copy=button("复制摘要",false);root.addView(copy);copy.setOnClickListener(v->copyText(result.getText().toString().replace("SHA-256：\n","")));
    }
    void timestampTool(){
        EditText value=input("输入 Unix 时间戳（秒或毫秒）");value.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);root.addView(value);
        TextView result=text("转换结果会显示在这里",14);result.setPadding(dp(12),dp(16),dp(12),dp(16));result.setBackground(tint(0x244c82ad,14));root.addView(result);
        Button convert=button("时间戳 → 日期",true);root.addView(convert);
        convert.setOnClickListener(v->{try{long n=Long.parseLong(value.getText().toString().trim());if(Math.abs(n)<100000000000L)n*=1000L;result.setText(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",Locale.getDefault()).format(new Date(n)));}catch(Exception e){result.setText("请输入有效的整数时间戳。");}});
        Button now=button("填入当前时间戳（秒）",false);root.addView(now);now.setOnClickListener(v->{long n=System.currentTimeMillis()/1000L;value.setText(String.valueOf(n));result.setText("当前时间戳："+n);});
        Button copy=button("复制转换结果",false);root.addView(copy);copy.setOnClickListener(v->copyText(result.getText().toString()));
    }
    void copyText(String s){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("toolbox",s));Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();}


    void passwordTool(){
        TextView label=text("密码长度：20",14);root.addView(label);
        SeekBar seek=new SeekBar(this);seek.setMax(48);seek.setProgress(12);root.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){label.setText("密码长度："+(8+p));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        CheckBox upper=new CheckBox(this),lower=new CheckBox(this),digits=new CheckBox(this),symbols=new CheckBox(this);
        upper.setText("大写字母");lower.setText("小写字母");digits.setText("数字");symbols.setText("特殊符号");
        for(CheckBox c:new CheckBox[]{upper,lower,digits,symbols}){c.setTextColor(Color.WHITE);c.setChecked(true);root.addView(c);}
        EditText output=input("生成的密码");output.setTextIsSelectable(true);root.addView(output);
        Button make=button("生成密码",true);root.addView(make);
        make.setOnClickListener(v->{String chars="";if(upper.isChecked())chars+="ABCDEFGHJKLMNPQRSTUVWXYZ";if(lower.isChecked())chars+="abcdefghijkmnopqrstuvwxyz";if(digits.isChecked())chars+="23456789";if(symbols.isChecked())chars+="!@#$%^&*()-_=+[]{}";if(chars.isEmpty()){output.setText("请至少选择一种字符类型");return;}SecureRandom r=new SecureRandom();StringBuilder b=new StringBuilder();for(int i=0;i<8+seek.getProgress();i++)b.append(chars.charAt(r.nextInt(chars.length())));output.setText(b.toString());});
        Button copy=button("复制密码",false);root.addView(copy);copy.setOnClickListener(v->copyText(output.getText().toString()));
    }
    void urlTool(){
        EditText src=input("输入文本或 URL 参数");root.addView(src);EditText out=input("转换结果");out.setMinLines(3);root.addView(out);
        Button enc=button("URL 编码",true),dec=button("URL 解码",false);root.addView(enc);root.addView(dec);
        enc.setOnClickListener(v->{try{out.setText(java.net.URLEncoder.encode(src.getText().toString(),"UTF-8").replace("+","%20"));}catch(Exception e){out.setText("编码失败");}});
        dec.setOnClickListener(v->{try{out.setText(java.net.URLDecoder.decode(src.getText().toString(),"UTF-8"));}catch(Exception e){out.setText("解码失败，请检查格式");}});
        Button copy=button("复制结果",false);root.addView(copy);copy.setOnClickListener(v->copyText(out.getText().toString()));
    }
    void jsonTool(){
        EditText src=input("粘贴 JSON");src.setSingleLine(false);src.setMinLines(5);root.addView(src);
        EditText out=input("处理结果");out.setSingleLine(false);out.setMinLines(7);out.setTextIsSelectable(true);root.addView(out);
        Button pretty=button("格式化",true),compact=button("压缩",false);root.addView(pretty);root.addView(compact);
        pretty.setOnClickListener(v->formatJson(src,out,true));compact.setOnClickListener(v->formatJson(src,out,false));
        Button copy=button("复制结果",false);root.addView(copy);copy.setOnClickListener(v->copyText(out.getText().toString()));
    }
    void formatJson(EditText src,EditText out,boolean pretty){
        try{Object o=new JSONTokener(src.getText().toString().trim()).nextValue();if(o instanceof JSONObject)out.setText(pretty?((JSONObject)o).toString(2):((JSONObject)o).toString());else if(o instanceof JSONArray)out.setText(pretty?((JSONArray)o).toString(2):((JSONArray)o).toString());else out.setText("JSON 顶层必须是对象或数组");}catch(Exception e){out.setText("JSON 格式错误："+e.getMessage());}
    }
    void radixTool(){
        EditText src=input("输入文本 / HEX / 二进制");root.addView(src);EditText out=input("转换结果");out.setMinLines(4);out.setTextIsSelectable(true);root.addView(out);
        Button a=button("文本 → HEX",true),b=button("HEX → 文本",false),c=button("文本 → 二进制",false),d=button("二进制 → 文本",false);
        root.addView(a);root.addView(b);root.addView(c);root.addView(d);
        a.setOnClickListener(v->{StringBuilder x=new StringBuilder();for(byte q:src.getText().toString().getBytes(StandardCharsets.UTF_8))x.append(String.format(Locale.US,"%02X ",q&255));out.setText(x.toString().trim());});
        b.setOnClickListener(v->{try{String[] q=src.getText().toString().trim().split("\\s+");byte[] z=new byte[q.length];for(int i=0;i<q.length;i++)z[i]=(byte)Integer.parseInt(q[i],16);out.setText(new String(z,StandardCharsets.UTF_8));}catch(Exception e){out.setText("HEX 格式错误，请用空格分隔字节");}});
        c.setOnClickListener(v->{StringBuilder x=new StringBuilder();for(byte q:src.getText().toString().getBytes(StandardCharsets.UTF_8)){for(int i=7;i>=0;i--)x.append((q>>i)&1);x.append(' ');}out.setText(x.toString().trim());});
        d.setOnClickListener(v->{try{String[] q=src.getText().toString().trim().split("\\s+");byte[] z=new byte[q.length];for(int i=0;i<q.length;i++){if(q[i].length()!=8)throw new Exception();z[i]=(byte)Integer.parseInt(q[i],2);}out.setText(new String(z,StandardCharsets.UTF_8));}catch(Exception e){out.setText("二进制需要按 8 位一组输入");}});
    }
    void morseTool(){
        final String letters="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final String[] codes={".-","-...","-.-.","-..",".","..-.","--.","....","..",".---","-.-",".-..","--","-.","---",".--.","--.-",".-.","...","-","..-","...-",".--","-..-","-.--","--..","-----",".----","..---","...--","....-",".....","-....","--...","---..","----."};
        EditText src=input("输入英文或摩斯电码");root.addView(src);EditText out=input("转换结果");out.setMinLines(3);root.addView(out);
        Button enc=button("文字 → 摩斯",true),dec=button("摩斯 → 文字",false);root.addView(enc);root.addView(dec);
        enc.setOnClickListener(v->{StringBuilder b=new StringBuilder();for(char c:src.getText().toString().toUpperCase(Locale.US).toCharArray()){if(c==' ')b.append("/ ");else{int i=letters.indexOf(c);b.append(i<0?"?":codes[i]).append(' ');}}out.setText(b.toString().trim());});
        dec.setOnClickListener(v->{StringBuilder b=new StringBuilder();String[] words=src.getText().toString().trim().split("\\s*/\\s*");for(int w=0;w<words.length;w++){if(w>0)b.append(' ');for(String token:words[w].trim().split("\\s+")){int i=Arrays.asList(codes).indexOf(token);b.append(i<0?'?':letters.charAt(i));}}out.setText(b.toString());});
    }

    void imageTool(){
        Button choose=button("选择图片",true);root.addView(choose);
        EditText quality=input("JPEG 质量 1–100（默认 85）");quality.setText("85");root.addView(quality);
        EditText width=input("缩放目标宽度（像素）");root.addView(width);
        TextView status=text("图片仅在本机处理。输出为 JPEG，原图保留。",12);root.addView(status);
        Button compress=button("压缩并另存",false),resize=button("缩放并另存",false);root.addView(compress);root.addView(resize);
        choose.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,75);});
        compress.setOnClickListener(v->{try{if(pendingImageBytes==null)throw new Exception();int q=Math.max(1,Math.min(100,Integer.parseInt(quality.getText().toString())));Bitmap b=decodeForEdit(pendingImageBytes);if(b==null)throw new Exception();ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,q,o);pendingImageBytes=o.toByteArray();pendingImageName="compressed-"+System.currentTimeMillis()+".jpg";saveEditedImage();}catch(Exception e){status.setText("请先选择有效图片，并输入 1–100 的质量值。");}});
        resize.setOnClickListener(v->{try{if(pendingImageBytes==null)throw new Exception();int w=Integer.parseInt(width.getText().toString());if(w<1||w>4096)throw new Exception();Bitmap b=decodeForEdit(pendingImageBytes);int h=Math.max(1,Math.round(b.getHeight()*(w/(float)b.getWidth())));Bitmap scaled=Bitmap.createScaledBitmap(b,w,h,true);ByteArrayOutputStream o=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,92,o);pendingImageBytes=o.toByteArray();pendingImageName="resized-"+System.currentTimeMillis()+".jpg";saveEditedImage();}catch(Exception e){status.setText("请先选择图片，并输入 1–4096 的目标宽度。");}});
    }
    Bitmap decodeForEdit(byte[] bytes){BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,b);if(b.outWidth<=0)return null;BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=Math.max(1,Math.max(b.outWidth,b.outHeight)/2400);o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);}
    void saveEditedImage(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/jpeg");i.putExtra(Intent.EXTRA_TITLE,pendingImageName);startActivityForResult(i,76);}
    void fileHashTool(){Button choose=button("选择文件并计算 SHA-256",true);root.addView(choose);choose.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,74);});}
    void flashlight(){
        cameraManager=(CameraManager)getSystemService(CAMERA_SERVICE);Button toggle=button("开启手电筒",true);root.addView(toggle);TextView status=text("当前状态：关闭",15);status.setPadding(0,dp(12),0,0);root.addView(status);
        try{for(String id:cameraManager.getCameraIdList()){CameraCharacteristics c=cameraManager.getCameraCharacteristics(id);Boolean flash=c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);Integer facing=c.get(CameraCharacteristics.LENS_FACING);if(Boolean.TRUE.equals(flash)&&facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK){cameraId=id;break;}}}catch(Exception ignored){}
        toggle.setOnClickListener(v->{if(cameraId==null){status.setText("此设备未提供可用的后置闪光灯。");return;}if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},41);return;}setTorch(!torchOn,toggle,status);});
    }
    void setTorch(boolean on,Button toggle,TextView status){try{cameraManager.setTorchMode(cameraId,on);torchOn=on;toggle.setText(on?"关闭手电筒":"开启手电筒");status.setText(on?"当前状态：开启":"当前状态：关闭");}catch(Exception e){status.setText("无法切换闪光灯，请检查相机是否正被其他应用占用。");}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==41&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){Button b=null;TextView s=null;for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);if(v instanceof Button&&((Button)v).getText().toString().contains("手电筒"))b=(Button)v;if(v instanceof TextView&&((TextView)v).getText().toString().startsWith("当前状态"))s=(TextView)v;}if(b!=null&&s!=null)setTorch(!torchOn,b,s);}}
    void compass(){
        compassValue=text("—°",48);compassValue.setGravity(Gravity.CENTER);compassValue.setPadding(0,dp(28),0,dp(14));root.addView(compassValue);TextView hint=text("将手机平放，远离磁铁和金属干扰。",13);hint.setGravity(Gravity.CENTER);root.addView(hint);
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);magnetometer=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(accelerometer==null||magnetometer==null)hint.setText("此设备缺少所需传感器，无法使用指南针。");
    }
    @Override protected void onResume(){super.onResume();if("compass".equals(tool)&&sensorManager!=null&&accelerometer!=null&&magnetometer!=null){sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI);sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_UI);compassRegistered=true;}}
    @Override protected void onPause(){super.onPause();if(compassRegistered&&sensorManager!=null){sensorManager.unregisterListener(this);compassRegistered=false;}if(torchOn&&cameraManager!=null&&cameraId!=null){try{cameraManager.setTorchMode(cameraId,false);}catch(Exception ignored){}torchOn=false;}if(clockTask!=null)handler.removeCallbacks(clockTask);}
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER)gravity=e.values.clone();else if(e.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)geomagnetic=e.values.clone();if(gravity!=null&&geomagnetic!=null&&compassValue!=null){float[] R=new float[9],I=new float[9];if(SensorManager.getRotationMatrix(R,I,gravity,geomagnetic)){float[] o=new float[3];SensorManager.getOrientation(R,o);int degree=((int)Math.round(Math.toDegrees(o[0]))+360)%360;String direction=degree<23||degree>=338?"北":degree<68?"东北":degree<113?"东":degree<158?"东南":degree<203?"南":degree<248?"西南":degree<293?"西":"西北";compassValue.setText(degree+"°\n"+direction);}}}
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacksAndMessages(null);gameHandler.removeCallbacksAndMessages(null);network.shutdownNow();}
}