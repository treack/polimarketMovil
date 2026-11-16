package com.polimarket.movil;

import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.widget.*;
import com.polimarket.movil.data.OperacionesApi;
import org.json.JSONArray;
import org.json.JSONException;

public class ProveedoresActivity extends PantallaActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Bodega · Proveedores");
        texto("Proveedores por producto",24);
        texto("ID del producto",16);
        EditText id = new EditText(this); id.setInputType(InputType.TYPE_CLASS_NUMBER);
        id.setHint("Ejemplo: 1");
        float density = getResources().getDisplayMetrics().density;
        id.setSingleLine(true);
        id.setTextSize(18);
        id.setMinHeight((int)(56 * density));
        id.setTextColor(new ColorStateList(new int[][] {
                new int[] { -android.R.attr.state_enabled }, new int[] {}
        }, new int[] { Color.rgb(82,99,90), Color.rgb(24,55,44) }));
        id.setHintTextColor(Color.rgb(102,117,110));
        id.setHighlightColor(Color.rgb(205,229,211));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(8 * density);
        background.setStroke(Math.max(1,(int)density), Color.rgb(168,185,173));
        id.setBackgroundTintList(null);
        id.setBackground(background);
        id.setPadding((int)(14*density),(int)(12*density),(int)(14*density),(int)(12*density));
        if (Build.VERSION.SDK_INT >= 29) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(Color.rgb(36,107,78));
            cursor.setSize(Math.max(2,(int)(2*density)),(int)(24*density));
            id.setTextCursorDrawable(cursor);
        }
        contenido.addView(id);
        LinearLayout lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL);
        boton("Consultar proveedores").setOnClickListener(v -> {
            if (ocupado) return;
            long productoId;
            try { productoId = Long.parseLong(id.getText().toString().trim()); if (productoId<=0) throw new NumberFormatException(); }
            catch(NumberFormatException error) { id.setError(getString(R.string.invalid_product)); return; }
            lista.removeAllViews();
            id.setEnabled(false);
            ejecutar(() -> {
                try { return new JSONArray(new OperacionesApi(getString(R.string.server_url))
                        .solicitar("/api/proveedores/productos/"+productoId,false)); }
                finally { runOnUiThread(() -> { if (!isDestroyed()) id.setEnabled(true); }); }
            }, proveedores -> {
                if (proveedores.length()==0) estado.setText("No hay proveedores asociados a este producto.");
                for(int i=0;i<proveedores.length();i++) {
                    try {
                        var proveedor = proveedores.getJSONObject(i);
                        TextView item = new TextView(this);
                        item.setText("\u2022 " + proveedor.getString("nombre") + " \u00B7 ID " + proveedor.getLong("id"));
                        item.setTextColor(Color.rgb(24,55,44));
                        item.setTextSize(18); item.setPadding(0,12,0,12); lista.addView(item);
                    } catch(JSONException error) { estado.setText(R.string.response_error); }
                }
            });
            // Si falta permiso, la tarea se ejecutara al concederlo.
            if (!ocupado) id.setEnabled(true);
        });
        contenido.addView(lista);
    }
}
