package com.polimarket.movil;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class PantallaActivity extends AppCompatActivity {
    protected LinearLayout contenido;
    protected TextView estado;
    private ProgressBar progreso;
    protected boolean ocupado;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Runnable accionPendiente;
    private final ActivityResultLauncher<String> permiso = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                Runnable accion = accionPendiente; accionPendiente = null;
                if (granted && accion != null) accion.run();
                else estado.setText(R.string.local_permission_denied);
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244,245,239));
        contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        contenido.setPadding(pad,pad,pad,pad);
        scroll.addView(contenido);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)-> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        if (!(this instanceof MainActivity)) {
            Button volver = boton("← Inicio");
            volver.setOnClickListener(v -> finish());
        }
        texto("PoliMarket Móvil",26);
        progreso = new ProgressBar(this);
        progreso.setVisibility(View.GONE); contenido.addView(progreso);
        estado = texto("",16);
        estado.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    protected TextView texto(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.rgb(24,55,44));
        view.setPadding(0,12,0,12); contenido.addView(view); return view;
    }
    protected Button boton(String label) {
        Button view = new Button(this); view.setText(label);
        view.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
        contenido.addView(view); return view;
    }
    protected <T> void ejecutar(Callable<T> trabajo, Consumer<T> mostrar) {
        if (ocupado) return;
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") != PackageManager.PERMISSION_GRANTED) {
            accionPendiente = () -> ejecutar(trabajo,mostrar);
            permiso.launch("android.permission.ACCESS_LOCAL_NETWORK"); return;
        }
        ocupado = true; progreso.setVisibility(View.VISIBLE); estado.setText("Consultando…");
        executor.execute(() -> {
            try {
                T result = trabajo.call();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    ocupado = false; progreso.setVisibility(View.GONE);
                    estado.setText("");
                    mostrar.accept(result);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    ocupado = false; progreso.setVisibility(View.GONE);
                    estado.setText(error.getMessage() == null ? getString(R.string.network_error) : error.getMessage());
                });
            }
        });
    }
    @Override protected void onDestroy() {
        accionPendiente = null; executor.shutdownNow(); super.onDestroy();
    }
}
