package com.wu2hii.cleanx;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.hardware.camera2.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ToolActivity extends Activity implements SensorEventListener {
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    LinearLayout root; String tool; Handler handler=new Handler(Looper.getMainLooper());
    CameraManager cameraManager; String cameraId; boolean torchOn=false;
    SensorManager sensorManager; Sensor accelerometer,magnetometer;
    float[] gravity,geomagnetic; TextView compassValue; boolean compassRegistered=false;
    Runnable clockTask; long startedAt,elapsed; boolean running=false; TextView watchText; Button watchToggle;
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff0b1428);getWindow().setNavigationBarColor(0xff0b1428);
        tool=getIntent().getStringExtra("tool");
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(22),dp(20),dp(28));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff0b1428,0xff172b49,0xff0c1830}));
        Button back=new Button(this);back.setText("‹  返回工具箱");back.setAllCaps(false);back.setTextColor(Color.WHITE);back.setBackgroundColor(0x334d88aa);root.addView(back);back.setOnClickListener(v->finish());
        scroll.addView(root);setContentView(scroll);showTool();
    }
    void showTool(){
        String name="工具详情",desc="";
        switch(tool==null?"":tool){
            case "device":name="设备信息";desc="设备型号："+Build.MODEL+"\nAndroid："+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\n制造商："+Build.MANUFACTURER+"\n设备："+Build.DEVICE;break;
            case "clean":name="CleanX 文件清理";desc="保留原有安全扫描流程。所有候选文件默认不勾选，必须由你逐项选择并二次确认。";break;
            case "calc":name="计算器";desc="输入两个数字，进行基础四则运算。";break;
            case "timer":name="秒表";desc="支持开始、暂停、继续与重置。";break;
            case "random":name="随机数生成器";desc="生成指定范围内的随机整数，包含边界值。";break;
            case "compass":name="指南针";desc="使用设备加速度计与磁力传感器计算方位。请远离磁铁和强磁场。";break;
            case "flash":name="手电筒";desc="通过设备后置闪光灯照明。仅在你点击开启时申请相机权限。";break;
        }
        TextView title=text(name,27);title.setPadding(0,dp(24),0,dp(10));root.addView(title);
        TextView info=text(desc,14);info.setTextColor(0xffb5c9df);info.setPadding(0,0,0,dp(18));root.addView(info);
        if("clean".equals(tool)){Button b=new Button(this);b.setText("打开 CleanX 扫描");b.setAllCaps(false);root.addView(b);b.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));return;}
        if("calc".equals(tool))calculator();
        if("timer".equals(tool))stopwatch();
        if("random".equals(tool))random();
        if("flash".equals(tool))flashlight();
        if("compass".equals(tool))compass();
    }
    void calculator(){
        EditText a=input("第一个数字"),b=input("第二个数字");root.addView(a);root.addView(b);
        Spinner op=new Spinner(this);String[] ops={"+","−","×","÷"};op.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,ops));root.addView(op);
        Button go=new Button(this);go.setText("计算");root.addView(go);TextView out=text("结果将在这里显示",20);out.setPadding(0,dp(18),0,0);root.addView(out);
        go.setOnClickListener(v->{try{double x=Double.parseDouble(a.getText().toString()),y=Double.parseDouble(b.getText().toString()),z;switch(op.getSelectedItemPosition()){case 0:z=x+y;break;case 1:z=x-y;break;case 2:z=x*y;break;default:if(y==0)throw new ArithmeticException();z=x/y;}out.setText("结果： "+z);}catch(Exception e){out.setText("请输入有效数字，且除数不能为 0。");}});
    }
    EditText input(String hint){EditText e=new EditText(this);e.setSingleLine();e.setHint(hint);e.setHintTextColor(0xff91a9c4);e.setTextColor(Color.WHITE);return e;}
    void random(){
        EditText min=input("最小值"),max=input("最大值");root.addView(min);root.addView(max);
        Button go=new Button(this);go.setText("生成随机数");root.addView(go);TextView out=text("—",26);out.setPadding(0,dp(18),0,0);root.addView(out);
        java.security.SecureRandom rng=new java.security.SecureRandom();
        go.setOnClickListener(v->{try{long x=Long.parseLong(min.getText().toString()),y=Long.parseLong(max.getText().toString());if(y<x)throw new Exception();java.math.BigInteger lo=java.math.BigInteger.valueOf(x),hi=java.math.BigInteger.valueOf(y),span=hi.subtract(lo).add(java.math.BigInteger.ONE),n;do{n=new java.math.BigInteger(span.bitLength(),rng);}while(n.compareTo(span)>=0);out.setText(n.add(lo).toString());}catch(Exception e){out.setText("范围无效，请检查输入。");}});
    }
    void stopwatch(){
        watchText=text("00:00.0",42);watchText.setPadding(0,dp(18),0,dp(18));root.addView(watchText);
        watchToggle=new Button(this);watchToggle.setText("开始");root.addView(watchToggle);Button reset=new Button(this);reset.setText("重置");root.addView(reset);
        clockTask=()->{if(running){long ms=elapsed+SystemClock.elapsedRealtime()-startedAt;watchText.setText(String.format(Locale.US,"%02d:%02d.%d",ms/60000,(ms/1000)%60,(ms/100)%10));handler.postDelayed(clockTask,80);}};
        watchToggle.setOnClickListener(v->{if(running){elapsed+=SystemClock.elapsedRealtime()-startedAt;running=false;watchToggle.setText("继续");}else{startedAt=SystemClock.elapsedRealtime();running=true;watchToggle.setText("暂停");handler.post(clockTask);}});
        reset.setOnClickListener(v->{running=false;elapsed=0;watchText.setText("00:00.0");watchToggle.setText("开始");handler.removeCallbacks(clockTask);});
    }
    void flashlight(){
        cameraManager=(CameraManager)getSystemService(CAMERA_SERVICE);
        Button toggle=new Button(this);toggle.setText("开启手电筒");root.addView(toggle);
        TextView status=text("当前状态：关闭",15);status.setPadding(0,dp(12),0,0);root.addView(status);
        try{for(String id:cameraManager.getCameraIdList()){CameraCharacteristics c=cameraManager.getCameraCharacteristics(id);Boolean flash=c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);Integer facing=c.get(CameraCharacteristics.LENS_FACING);if(Boolean.TRUE.equals(flash)&&facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK){cameraId=id;break;}}}catch(Exception ignored){}
        toggle.setOnClickListener(v->{if(cameraId==null){status.setText("此设备未提供可用的后置闪光灯。");return;}if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},41);return;}setTorch(!torchOn,toggle,status);});
    }
    void setTorch(boolean on,Button toggle,TextView status){try{cameraManager.setTorchMode(cameraId,on);torchOn=on;toggle.setText(on?"关闭手电筒":"开启手电筒");status.setText(on?"当前状态：开启":"当前状态：关闭");}catch(Exception e){status.setText("无法切换闪光灯，请检查相机是否正被其他应用占用。");}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==41&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){Button b=(Button)root.getChildAt(root.getChildCount()-2);TextView s=(TextView)root.getChildAt(root.getChildCount()-1);setTorch(!torchOn,b,s);}}
    void compass(){
        compassValue=text("—°",48);compassValue.setGravity(Gravity.CENTER);compassValue.setPadding(0,dp(28),0,dp(14));root.addView(compassValue);
        TextView hint=text("正在等待传感器数据…",13);hint.setGravity(Gravity.CENTER);root.addView(hint);
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);magnetometer=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(accelerometer==null||magnetometer==null){hint.setText("此设备缺少所需传感器，无法使用指南针。");return;}
        hint.setText("将手机平放，远离磁铁和金属干扰。");
    }
    @Override protected void onResume(){super.onResume();if("compass".equals(tool)&&sensorManager!=null&&accelerometer!=null&&magnetometer!=null){sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI);sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_UI);compassRegistered=true;}}
    @Override protected void onPause(){super.onPause();if(compassRegistered&&sensorManager!=null){sensorManager.unregisterListener(this);compassRegistered=false;}if(torchOn&&cameraManager!=null&&cameraId!=null){try{cameraManager.setTorchMode(cameraId,false);}catch(Exception ignored){}torchOn=false;}if(clockTask!=null)handler.removeCallbacks(clockTask);}
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER)gravity=e.values.clone();else if(e.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)geomagnetic=e.values.clone();if(gravity!=null&&geomagnetic!=null&&compassValue!=null){float[] R=new float[9],I=new float[9];if(SensorManager.getRotationMatrix(R,I,gravity,geomagnetic)){float[] o=new float[3];SensorManager.getOrientation(R,o);int degree=((int)Math.round(Math.toDegrees(o[0]))+360)%360;String direction=degree<23||degree>=338?"北":degree<68?"东北":degree<113?"东":degree<158?"东南":degree<203?"南":degree<248?"西南":degree<293?"西": "西北";compassValue.setText(degree+"°\n"+direction);}}}
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacksAndMessages(null);}
}