package com.wu2hii.cleanx;

import android.Manifest;
import android.app.*;
import android.content.*;
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
import java.math.*;
import java.text.DecimalFormat;

public class ToolActivity extends Activity implements SensorEventListener {
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    LinearLayout root; String tool; Handler handler=new Handler(Looper.getMainLooper());
    CameraManager cameraManager; String cameraId; boolean torchOn=false;
    SensorManager sensorManager; Sensor accelerometer,magnetometer;
    float[] gravity,geomagnetic; TextView compassValue; boolean compassRegistered=false;
    Runnable clockTask; long startedAt,elapsed; boolean running=false; TextView watchText; Button watchToggle;
    TextView calcExpression,calcResult,calcHistory; String expression=""; boolean justEvaluated=false;
    DecimalFormat decimal=new DecimalFormat("0.##########");
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff0b1428);getWindow().setNavigationBarColor(0xff0b1428);
        tool=getIntent().getStringExtra("tool");ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(28));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff0b1428,0xff172b49,0xff0c1830}));
        Button back=new Button(this);back.setText("‹  返回工具箱");back.setAllCaps(false);back.setTextColor(Color.WHITE);back.setBackground(tint(0x335b9bc0,18));root.addView(back);back.setOnClickListener(v->finish());
        scroll.addView(root);setContentView(scroll);showTool();
    }
    GradientDrawable tint(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0x447bbbd6);return d;}
    void heading(String name,String desc){TextView title=text(name,27);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,dp(22),0,dp(7));root.addView(title);TextView info=text(desc,13);info.setTextColor(0xffa9bfd9);info.setPadding(0,0,0,dp(18));root.addView(info);}
    void showTool(){
        switch(tool==null?"":tool){
            case "device":heading("设备信息","设备基本信息与存储概览");deviceInfo();break;
            case "clean":heading("CleanX 文件清理","所有候选文件默认不勾选，必须由你逐项选择并二次确认。");Button clean=button("打开 CleanX 扫描",true);root.addView(clean);clean.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));break;
            case "calc":heading("计算器","支持括号、百分比、连续运算和科学函数。");calculator();break;
            case "timer":heading("秒表与计时器","正计时与倒计时，适合日常计时。");stopwatch();countdown();break;
            case "convert":heading("单位换算","长度、质量、温度、面积、体积与速度换算。");converter();break;
            case "text":heading("文本工具","快速统计字数，转换大小写与清理空白。");textTools();break;
            case "compass":heading("指南针","使用设备加速度计与磁力传感器计算方位。");compass();break;
            case "flash":heading("手电筒","点击开启时才申请相机权限；离开页面自动关闭。");flashlight();break;
        }
    }
    Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackground(tint(primary?0x6659a9cb:0x334a739a,16));return b;}
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
        String[][] keys={{"sin","cos","tan","√"},{"(",")","%","÷"},{"7","8","9","×"},{"4","5","6","−"},{"1","2","3","+"},{"±","0",".","="},{"AC","DEL","π","x²"}};
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
        else if(k.equals("sin")||k.equals("cos")||k.equals("tan")){expression+=k+"(";}
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
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacksAndMessages(null);}
}