package com.wu2hii.cleanx;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.util.*;

public class DashboardActivity extends Activity {
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout page;
    private int white=Color.WHITE, muted=0xffa9bdd8;
    @Override public void onCreate(Bundle b){super.onCreate(b);
        getWindow().setStatusBarColor(0xff0b1428);getWindow().setNavigationBarColor(0xff0b1428);
        splash();
    }
    private void splash(){
        FrameLayout frame=new FrameLayout(this);
        frame.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff0b1428,0xff172d50,0xff0b1428}));
        LinearLayout center=new LinearLayout(this);center.setGravity(Gravity.CENTER);center.setOrientation(1);
        TextView logo=new TextView(this);logo.setText("wu2hii");logo.setTextColor(white);logo.setTextSize(43);logo.setTypeface(Typeface.create("serif",Typeface.ITALIC));
        logo.setLetterSpacing(.035f);logo.setGravity(Gravity.CENTER);
        TextView sub=new TextView(this);sub.setText("PERSONAL TOOLBOX");sub.setTextColor(0xff9bc9e7);sub.setTextSize(10);sub.setLetterSpacing(.32f);sub.setGravity(Gravity.CENTER);
        center.addView(logo);center.addView(sub);frame.addView(center,new FrameLayout.LayoutParams(-1,-1));
        logo.setAlpha(0f);logo.setScaleX(.88f);logo.setScaleY(.88f);
        setContentView(frame);
        logo.animate().alpha(1).scaleX(1).scaleY(1).setDuration(950).setInterpolator(new OvershootInterpolator(1.1f)).withEndAction(()->{
            sub.animate().alpha(1).setDuration(500).withEndAction(()->frame.animate().alpha(0).setDuration(350).withEndAction(this::home).start()).start();
        }).start();
    }
    private void home(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(18),dp(18),dp(28));
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff0b1428,0xff172b49,0xff0c1830}));
        scroll.addView(page);setContentView(scroll);
        TextView eyebrow=label("PERSONAL TOOLBOX  /  01",10,0xff86c9df);eyebrow.setLetterSpacing(.2f);page.addView(eyebrow);
        TextView title=label("wu2hii",36,white);title.setTypeface(Typeface.create("serif",Typeface.ITALIC));page.addView(title);
        TextView desc=label("把常用工具，收进一个清透的空间。",13,muted);desc.setPadding(0,dp(2),0,dp(18));page.addView(desc);
        glassPanel("QUICK ACCESS","轻量、离线优先 · 按需开启权限");
        section("系统与清理");
        addTool("✦","设备信息","电池、系统版本、存储概览","device");
        addTool("⌁","CleanX 文件清理","扫描候选文件，逐项确认后处理","clean");
        addTool("▤","文件管理","授权目录浏览、创建文件夹、打开与删除","file");
        section("计算与效率");
        addTool("＋","计算器","大按键布局 · 科学计算 · 历史结果","calc");
        addTool("◷","秒表与计时器","正计时、倒计时","timer");
        addTool("⇄","单位换算","长度、质量、温度、面积、体积与速度","convert");
        addTool("Aa","文本工具","字数统计、大小写转换、空白整理","text");
        addTool("▦","二维码生成器","离线生成网址和文本二维码","qr");
        addTool("◴","历史上的今天","回看这一天发生的历史事件","history");
        addTool("♟","小游戏","2048 数字合并、反应速度测试","games");
        section("传感器与实用工具");
        addTool("⌖","指南针","使用设备磁力传感器","compass");
        addTool("▣","手电筒","使用设备闪光灯","flash");
        TextView foot=label("DESIGNED BY wu2hii  ·  OFFLINE FIRST",9,0xff7189a9);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(24),0,dp(4));page.addView(foot);
        for(int i=0;i<page.getChildCount();i++){View v=page.getChildAt(i);v.setAlpha(0);v.setTranslationY(dp(12));v.animate().alpha(1).translationY(0).setStartDelay(i*35L).setDuration(360).start();}
    }
    private void glassPanel(String a,String b){
        LinearLayout box=panel();box.setPadding(dp(17),dp(15),dp(17),dp(15));
        TextView t=label(a,10,0xff8bd7e6);t.setLetterSpacing(.18f);box.addView(t);
        TextView s=label(b,13,white);s.setPadding(0,dp(7),0,0);box.addView(s);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(18);page.addView(box,lp);
    }
    private void section(String s){TextView t=label(s,15,white);t.setTypeface(null,1);t.setPadding(dp(2),dp(12),0,dp(7));page.addView(t);}
    private void addTool(String icon,String name,String detail,String key){
        LinearLayout box=panel();box.setOrientation(0);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(14),dp(13),dp(12),dp(13));
        TextView symbol=label(icon,23,0xff9de6e7);symbol.setGravity(Gravity.CENTER);box.addView(symbol,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(1);texts.setPadding(dp(10),0,0,0);
        texts.addView(label(name,14,white));TextView sub=label(detail,11,muted);sub.setPadding(0,dp(3),0,0);texts.addView(sub);
        box.addView(texts,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=label("›",25,0xff91b9d8);box.addView(arrow);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);page.addView(box,lp);
        box.setOnClickListener(v->{Intent i=new Intent(this,ToolActivity.class);i.putExtra("tool",key);startActivity(i);overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);});
        box.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);v.animate().scaleX(.965f).scaleY(.965f).alpha(.88f).setDuration(80).start();}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1).scaleY(1).alpha(1f).setDuration(170).setInterpolator(new OvershootInterpolator(1.15f)).start();return false;});
    }
    private LinearLayout panel(){
        LinearLayout b=new LinearLayout(this);b.setOrientation(1);
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0x3d9bd5ed,0x1f739fc7,0x183b6c9d});d.setCornerRadius(dp(22));d.setStroke(dp(1),0x668bd7ed);
        b.setBackground(d);b.setElevation(dp(3));return b;
    }
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
}
