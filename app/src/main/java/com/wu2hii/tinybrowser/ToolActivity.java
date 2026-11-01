package com.wu2hii.cleanx;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ToolActivity extends Activity {
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    LinearLayout root; String tool;
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff0b1428);getWindow().setNavigationBarColor(0xff0b1428);
        tool=getIntent().getStringExtra("tool");root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(22),dp(20),dp(20));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff0b1428,0xff172b49,0xff0c1830}));
        Button back=new Button(this);back.setText("‹  返回工具箱");back.setAllCaps(false);back.setTextColor(Color.WHITE);back.setBackgroundColor(0x334d88aa);root.addView(back);back.setOnClickListener(v->finish());
        setContentView(root);showTool();
    }
    void showTool(){
        String name="工具详情",desc="";
        switch(tool==null?"":tool){
            case "device":name="设备信息";desc="设备型号："+Build.MODEL+"\nAndroid："+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\n制造商："+Build.MANUFACTURER+"\n设备："+Build.DEVICE;break;
            case "clean":name="CleanX 文件清理";desc="保留原有安全扫描流程。所有候选文件默认不勾选，必须由你逐项选择并二次确认。";break;
            case "calc":name="计算器";desc="输入两个数字，进行基础四则运算。";break;
            case "timer":name="秒表与计时器";desc="简单秒表，支持开始、暂停与重置。";break;
            case "random":name="随机数生成器";desc="生成指定范围内的随机整数。";break;
            case "compass":name="指南针";desc="指南针将在后续版本接入设备磁力传感器。";break;
            case "flash":name="手电筒";desc="手电筒将在后续版本接入设备闪光灯。";break;
        }
        TextView title=text(name,27);title.setPadding(0,dp(24),0,dp(10));root.addView(title);
        TextView info=text(desc,14);info.setTextColor(0xffb5c9df);info.setPadding(0,0,0,dp(18));root.addView(info);
        if("clean".equals(tool)){Button b=new Button(this);b.setText("打开 CleanX 扫描");b.setAllCaps(false);root.addView(b);b.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));return;}
        if("calc".equals(tool))calculator();
        if("timer".equals(tool))stopwatch();
        if("random".equals(tool))random();
    }
    void calculator(){
        EditText a=input("第一个数字"),b=input("第二个数字");root.addView(a);root.addView(b);
        Spinner op=new Spinner(this);String[] ops={"+","−","×","÷"};op.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,ops));root.addView(op);
        Button go=new Button(this);go.setText("计算");root.addView(go);TextView out=text("结果将在这里显示",20);out.setPadding(0,dp(18),0,0);root.addView(out);
        go.setOnClickListener(v->{try{double x=Double.parseDouble(a.getText().toString()),y=Double.parseDouble(b.getText().toString()),z;switch(op.getSelectedItemPosition()){case 0:z=x+y;break;case 1:z=x-y;break;case 2:z=x*y;break;default:if(y==0)throw new ArithmeticException();z=x/y;}out.setText("结果： "+z);}catch(Exception e){out.setText("请输入有效数字，且除数不能为 0。");}});
    }
    EditText input(String hint){EditText e=new EditText(this);e.setSingleLine();e.setHint(hint);e.setHintTextColor(0xff91a9c4);e.setTextColor(Color.WHITE);root.addView(e);return e;}
    void random(){
        EditText min=input("最小值"),max=input("最大值");Button go=new Button(this);go.setText("生成随机数");root.addView(go);TextView out=text("—",26);out.setPadding(0,dp(18),0,0);root.addView(out);
        go.setOnClickListener(v->{try{long x=Long.parseLong(min.getText().toString()),y=Long.parseLong(max.getText().toString());if(y<x)throw new Exception();long n=new java.security.SecureRandom().nextLong(y-x+1)+x;out.setText(String.valueOf(n));}catch(Exception e){out.setText("范围无效，请检查输入。");}});
    }
    void stopwatch(){
        TextView time=text("00:00.0",42);time.setPadding(0,dp(18),0,dp(18));root.addView(time);
        final long[] start={0},elapsed={0};final boolean[] running={false};
        Handler h=new Handler(Looper.getMainLooper());Runnable tick=new Runnable(){public void run(){if(running[0]){long ms=elapsed[0]+SystemClock.elapsedRealtime()-start[0];time.setText(String.format(Locale.US,"%02d:%02d.%d",ms/60000,(ms/1000)%60,(ms/100)%10));h.postDelayed(this,80);}}};
        Button toggle=new Button(this);toggle.setText("开始");root.addView(toggle);Button reset=new Button(this);reset.setText("重置");root.addView(reset);
        toggle.setOnClickListener(v->{if(running[0]){elapsed[0]+=SystemClock.elapsedRealtime()-start[0];running[0]=false;toggle.setText("继续");}else{start[0]=SystemClock.elapsedRealtime();running[0]=true;toggle.setText("暂停");h.post(tick);}});
        reset.setOnClickListener(v->{running[0]=false;elapsed[0]=0;time.setText("00:00.0");toggle.setText("开始");});
    }
}
